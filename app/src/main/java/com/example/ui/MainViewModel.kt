package com.example.ui

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.service.NotificationReceiver
import com.example.service.PdfReportGenerator
import com.example.service.ServerTimeService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Google Sheets App Script Deployment Template:
 * --------------------------------------------
 * function doPost(e) {
 *   var ss = SpreadsheetApp.getActiveSpreadsheet();
 *   var sheet = ss.getSheetByName("Kehadiran") || ss.insertSheet("Kehadiran");
 *   
 *   // Setup Header if new
 *   if (sheet.getLastRow() === 0) {
 *     sheet.appendRow(["Tarikh", "Nama Pekerja", "Pilihan", "Waktu", "Status", "Wifi", "Dikemaskini Pada"]);
 *   }
 *   
 *   var action = e.parameter.action;
 *   if (action === "punch") {
 *     sheet.appendRow([
 *       e.parameter.date,
 *       e.parameter.workerName,
 *       e.parameter.type,
 *       e.parameter.timeDisplay,
 *       e.parameter.status,
 *       e.parameter.wifiSsid,
 *       new Date()
 *     ]);
 *     return ContentService.createTextOutput("Rekod berjaya disimpan.");
 *   } else if (action === "addWorker") {
 *     var wSheet = ss.getSheetByName("Profil_Kakitangan") || ss.insertSheet("Profil_Kakitangan");
 *     if (wSheet.getLastRow() === 0) {
 *       wSheet.appendRow(["ID Pekerja", "Nama", "Username", "Password", "IsAdmin", "Email", "Dibuat Pada"]);
 *     }
 *     wSheet.appendRow([
 *       e.parameter.workerId,
 *       e.parameter.name,
 *       e.parameter.username,
 *       e.parameter.password,
 *       e.parameter.isAdmin,
 *       e.parameter.email,
 *       new Date()
 *     ]);
 *     return ContentService.createTextOutput("Pekerja berjaya didaftar.");
 *   }
 *   return ContentService.createTextOutput("Aksi tidak dikenali.");
 * }
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = Repository(db)

    // Logged-in User Session (null means on Login Landing Screen)
    private val _loggedInUser = MutableStateFlow<Worker?>(null)
    val loggedInUser: StateFlow<Worker?> = _loggedInUser.asStateFlow()

    // UI Navigation tabs
    private val _currentTab = MutableStateFlow("punch")
    val currentTab: StateFlow<String> = _currentTab.asStateFlow()

    // Loaded app data
    val workers: StateFlow<List<Worker>> = repository.allWorkers
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allLogs: StateFlow<List<PunchLog>> = repository.allLogs
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allEvents: StateFlow<List<PunchEvent>> = repository.allEvents
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Selected active worker for Admin view. For normal workers, this is locked to their logged-in ID.
    private val _selectedWorkerId = MutableStateFlow<Int>(1)
    val selectedWorkerId: StateFlow<Int> = _selectedWorkerId.asStateFlow()

    val currentWorker: StateFlow<Worker?> = combine(workers, selectedWorkerId) { workerList, selectedId ->
        workerList.find { it.id == selectedId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Wifi / Sync settings and triggers
    val designatedSsid: StateFlow<String> = repository.getSettingValueFlow("wifi_ssid", "Sulaiman_Home_WiFi")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Sulaiman_Home_WiFi")

    val googleSheetsUrl: StateFlow<String> = repository.getSettingValueFlow("google_sheets_url", "")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val emailDestination: StateFlow<String> = repository.getSettingValueFlow("email_report", "sulaimansabikan@gmail.com")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "sulaimansabikan@gmail.com")

    val isDeveloperBypass: StateFlow<String> = repository.getSettingValueFlow("developer_bypass", "true")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "true")

    val reminderTimeSetting: StateFlow<String> = repository.getSettingValueFlow("reminder_time", "07:45")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "07:45")

    val companyName: StateFlow<String> = repository.getSettingValueFlow("company_name", "SULAIMAN INTEGRATED SERVICES")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "SULAIMAN INTEGRATED SERVICES")

    val companyAddress: StateFlow<String> = repository.getSettingValueFlow("company_address", "No 15, Jalan Perusahaan 2, Kawasan Perindustrian,\n43000 Kajang, Selangor Darul Ehsan")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "No 15, Jalan Perusahaan 2, Kawasan Perindustrian,\n43000 Kajang, Selangor Darul Ehsan")

    val companyLogoIcon: StateFlow<String> = repository.getSettingValueFlow("company_logo_icon", "business")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "business")

    // Simulator input for WiFi SSID
    private val _simulatedSsid = MutableStateFlow("Sulaiman_Home_WiFi")
    val simulatedSsid: StateFlow<String> = _simulatedSsid.asStateFlow()

    // Real detected WiFi SSID
    private val _detectedSsid = MutableStateFlow("")
    val detectedSsid: StateFlow<String> = _detectedSsid.asStateFlow()

    private val _isConnectedToInternet = MutableStateFlow(false)
    val isConnectedToInternet: StateFlow<Boolean> = _isConnectedToInternet.asStateFlow()

    // Server time sync
    private val _isServerTimeSyncing = MutableStateFlow(false)
    val isServerTimeSyncing: StateFlow<Boolean> = _isServerTimeSyncing.asStateFlow()

    private val _isServerTimeSynced = MutableStateFlow(false)
    val isServerTimeSynced: StateFlow<Boolean> = _isServerTimeSynced.asStateFlow()

    private val _serverTimeMs = MutableStateFlow(System.currentTimeMillis())
    val serverTimeMs: StateFlow<Long> = _serverTimeMs.asStateFlow()

    // Toasts queue
    private val _uiToastMessage = MutableSharedFlow<String>()
    val uiToastMessage: SharedFlow<String> = _uiToastMessage.asSharedFlow()

    init {
        viewModelScope.launch {
            repository.preseedDatabaseIfEmpty()
            syncServerTime()
            updateNetworkStatus()
            
            // Loop clock trigger every second
            while (true) {
                kotlinx.coroutines.delay(1000)
                _serverTimeMs.value = ServerTimeService.currentTimeMillis()
            }
        }
    }

    fun setTab(tab: String) {
        _currentTab.value = tab
    }

    fun selectWorker(id: Int) {
        _selectedWorkerId.value = id
    }

    fun updateSimulatedSsid(ssid: String) {
        _simulatedSsid.value = ssid
    }

    // Authenticate user
    fun loginUser(usernameStr: String, passwordStr: String): String? {
        updateNetworkStatus()
        
        val match = workers.value.find { 
            it.username.trim().equals(usernameStr.trim(), ignoreCase = true) && 
            it.password == passwordStr &&
            it.isActive
        }

        return if (match != null) {
            _loggedInUser.value = match
            _selectedWorkerId.value = match.id // Force active profile selection to the logged-in user
            
            // If normal user logs in, ensure we switch to punch tab and restrict view
            if (!match.isAdmin) {
                _currentTab.value = "punch"
            }
            null // No error
        } else {
            "Nama pengguna atau kata laluan salah!"
        }
    }

    fun logout() {
        _loggedInUser.value = null
        _currentTab.value = "punch"
    }

    fun syncServerTime() {
        viewModelScope.launch {
            _isServerTimeSyncing.value = true
            ServerTimeService.syncTime()
            _isServerTimeSynced.value = ServerTimeService.isTimeSynced()
            _serverTimeMs.value = ServerTimeService.currentTimeMillis()
            _isServerTimeSyncing.value = false
            if (_isServerTimeSynced.value) {
                emitMessage("Masa pelayan diselaras!")
            } else {
                emitMessage("Gagal menyelaraskan masa pelayan. Guna waktu peranti.")
            }
        }
    }

    fun saveDesignatedSsid(ssid: String) {
        viewModelScope.launch {
            repository.saveSetting("wifi_ssid", ssid)
            emitMessage("Dikemaskini: Wifi rumah dilaras ke $ssid")
        }
    }

    fun saveGoogleSheetsUrl(url: String) {
        viewModelScope.launch {
            repository.saveSetting("google_sheets_url", url)
            emitMessage("Dikemaskini: URL Google Sheets disimpan!")
        }
    }

    fun saveEmailDestination(email: String) {
        viewModelScope.launch {
            repository.saveSetting("email_report", email)
            emitMessage("Dikemaskini: Email laporan dilaras ke $email")
        }
    }

    fun saveDeveloperBypass(enabled: Boolean) {
        viewModelScope.launch {
            repository.saveSetting("developer_bypass", enabled.toString())
            emitMessage("Mod simulasi ditukar ke: ${if (enabled) "Aktif" else "Tidak Aktif"}")
        }
    }

    fun saveReminderTime(timeStr: String) {
        viewModelScope.launch {
            repository.saveSetting("reminder_time", timeStr)
            NotificationReceiver.scheduleDailyReminder(getApplication(), timeStr)
            emitMessage("Jadual peringatan harian dilaras ke jam $timeStr Pagi")
        }
    }

    fun saveCompanyName(name: String) {
        viewModelScope.launch {
            repository.saveSetting("company_name", name)
            emitMessage("Dikemaskini: Nama syarikat dinaiktaraf ke '$name'")
        }
    }

    fun saveCompanyAddress(address: String) {
        viewModelScope.launch {
            repository.saveSetting("company_address", address)
            emitMessage("Dikemaskini: Alamat syarikat dilaras")
        }
    }

    fun saveCompanyLogoIcon(iconKey: String) {
        viewModelScope.launch {
            repository.saveSetting("company_logo_icon", iconKey)
            emitMessage("Dikemaskini: Logo korporat ditukar!")
        }
    }

    fun updateNetworkStatus() {
        val context = getApplication<Application>()
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        
        val hasInternet = capabilities?.let {
            it.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            it.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } ?: false
        
        _isConnectedToInternet.value = hasInternet

        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wifiInfo = wifiManager.connectionInfo
        var ssid = ""
        if (wifiInfo != null) {
            val name = wifiInfo.ssid ?: ""
            ssid = if (name.startsWith("\"") && name.endsWith("\"")) {
                name.substring(1, name.length - 1)
            } else {
                name
            }
        }
        
        if (ssid == "<unknown ssid>" || ssid == "0x") {
            ssid = ""
        }
        _detectedSsid.value = ssid
    }

    fun canPunchCard(): Boolean {
        if (!_isConnectedToInternet.value) return false
        if (isDeveloperBypass.value == "true") {
            return _simulatedSsid.value == designatedSsid.value
        }
        return _detectedSsid.value == designatedSsid.value
    }

    // Primary action: PUNCH IN
    fun punchInActiveWorker() {
        val workerId = _selectedWorkerId.value
        val workerObj = currentWorker.value ?: return

        viewModelScope.launch {
            val nowMs = ServerTimeService.currentTimeMillis()
            val todayDate = ServerTimeService.formatToMalaysiaDate(nowMs, "yyyy-MM-dd")

            if (!canPunchCard()) {
                emitMessage("Sekatan Gagal: Guna Wifi Rumah (${designatedSsid.value})!")
                return@launch
            }

            val isLate = checkIsLate(nowMs)
            val activeSsid = if (isDeveloperBypass.value == "true") _simulatedSsid.value else _detectedSsid.value

            val success = repository.punchIn(
                workerId = workerId,
                name = workerObj.name,
                date = todayDate,
                timestamp = nowMs,
                isLate = isLate,
                ssid = activeSsid
            )

            if (success) {
                val formattedTime = ServerTimeService.formatToMalaysiaTime(nowMs, "hh:mm:ss a")
                emitMessage("Daftar Masuk Berjaya: $formattedTime!")
                
                // Upload to Google Sheets
                uploadPunchToGoogleSheetsRemote(
                    workerId = workerId,
                    workerName = workerObj.name,
                    date = todayDate,
                    timestamp = nowMs,
                    type = "IN",
                    status = if (isLate) "LEWAT" else "TEPAT WAKTU",
                    ssid = activeSsid
                )
            } else {
                emitMessage("Gagal mendaftar masuk.")
            }
        }
    }

    // Primary action: PUNCH OUT
    fun punchOutActiveWorker() {
        val workerId = _selectedWorkerId.value
        val workerObj = currentWorker.value ?: return

        viewModelScope.launch {
            val nowMs = ServerTimeService.currentTimeMillis()
            val todayDate = ServerTimeService.formatToMalaysiaDate(nowMs, "yyyy-MM-dd")

            if (!canPunchCard()) {
                emitMessage("Sekatan Gagal: Guna Wifi Rumah (${designatedSsid.value})!")
                return@launch
            }

            val isEarlyOut = checkIsEarlyOut(nowMs)
            val activeSsid = if (isDeveloperBypass.value == "true") _simulatedSsid.value else _detectedSsid.value

            val success = repository.punchOut(
                workerId = workerId,
                name = workerObj.name,
                date = todayDate,
                timestamp = nowMs,
                isEarlyOut = isEarlyOut,
                ssid = activeSsid
            )

            if (success) {
                val formattedTime = ServerTimeService.formatToMalaysiaTime(nowMs, "hh:mm:ss a")
                emitMessage("Daftar Keluar Berjaya: $formattedTime!")

                // Upload to Google Sheets
                uploadPunchToGoogleSheetsRemote(
                    workerId = workerId,
                    workerName = workerObj.name,
                    date = todayDate,
                    timestamp = nowMs,
                    type = "OUT",
                    status = if (isEarlyOut) "PULANG AWAL" else "TEPAT WAKTU",
                    ssid = activeSsid
                )
            } else {
                emitMessage("Gagal mendaftar keluar.")
            }
        }
    }

    // OkHttp Sheets Upload Core
    private fun uploadPunchToGoogleSheetsRemote(
        workerId: Int,
        workerName: String,
        date: String,
        timestamp: Long,
        type: String,
        status: String,
        ssid: String
    ) {
        val sheetsUrl = googleSheetsUrl.value
        if (sheetsUrl.isBlank()) {
            Log.d("SheetsSync", "URL Google Sheets kosong. Log disimpan secara offline harian.")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val client = OkHttpClient()
                val formBody = FormBody.Builder()
                    .add("action", "punch")
                    .add("workerId", workerId.toString())
                    .add("workerName", workerName)
                    .add("date", date)
                    .add("timestamp", timestamp.toString())
                    .add("timeDisplay", ServerTimeService.formatToMalaysiaTime(timestamp, "hh:mm:ss a"))
                    .add("type", type)
                    .add("status", status)
                    .add("wifiSsid", ssid)
                    .build()

                val request = Request.Builder()
                    .url(sheetsUrl)
                    .post(formBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        Log.d("SheetsSync", "Success upload punch log event to Google Sheets")
                    } else {
                        Log.e("SheetsSync", "Failed to upload to Google Sheets. Code: ${response.code}")
                    }
                }
            } catch (e: Exception) {
                Log.e("SheetsSync", "Error sending data to Google Sheets", e)
            }
        }
    }

    fun addNewWorker(name: String, email: String, username: String, password: String, isAdmin: Boolean) {
        viewModelScope.launch {
            val worker = Worker(
                name = name,
                email = email,
                username = username,
                password = password,
                isAdmin = isAdmin,
                isActive = true
            )
            val newId = repository.insertWorker(worker)
            emitMessage("Pekerja $name berjaya didaftar!")
            
            // Upload worker profile to Google Sheets as well
            val sheetUrl = googleSheetsUrl.value
            if (sheetUrl.isNotBlank()) {
                uploadWorkerToGoogleSheetsRemote(worker.copy(id = newId.toInt()))
            }
        }
    }

    private fun uploadWorkerToGoogleSheetsRemote(worker: Worker) {
        val sheetsUrl = googleSheetsUrl.value
        if (sheetsUrl.isBlank()) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val client = OkHttpClient()
                val formBody = FormBody.Builder()
                    .add("action", "addWorker")
                    .add("workerId", worker.id.toString())
                    .add("name", worker.name)
                    .add("username", worker.username)
                    .add("password", worker.password)
                    .add("isAdmin", worker.isAdmin.toString())
                    .add("email", worker.email)
                    .build()

                val request = Request.Builder()
                    .url(sheetsUrl)
                    .post(formBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        Log.d("SheetsSync", "Success upload worker profile to Google Sheets")
                    }
                }
            } catch (e: Exception) {
                Log.e("SheetsSync", "Failed to add worker to Google Sheets", e)
            }
        }
    }

    fun addManualWorkerLog(workerId: Int, date: String, inTimeStr: String, outTimeStr: String) {
        viewModelScope.launch {
            val worker = workers.value.find { it.id == workerId } ?: return@launch
            try {
                val sdfDateTime = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
                sdfDateTime.timeZone = TimeZone.getTimeZone("Asia/Kuala_Lumpur")

                val inTimestamp = if (inTimeStr.isNotBlank()) {
                    sdfDateTime.parse("$date $inTimeStr")?.time
                } else null

                val outTimestamp = if (outTimeStr.isNotBlank()) {
                    sdfDateTime.parse("$date $outTimeStr")?.time
                } else null

                // Insert manual logic
                inTimestamp?.let {
                    val isLate = checkIsLate(it)
                    repository.insertEvent(
                        PunchEvent(
                            workerId = workerId,
                            workerName = worker.name,
                            date = date,
                            timestamp = it,
                            type = "IN",
                            isLate = isLate,
                            wifiSsid = "Manual_Override"
                        )
                    )
                }

                outTimestamp?.let {
                    val isEarly = checkIsEarlyOut(it)
                    repository.insertEvent(
                        PunchEvent(
                            workerId = workerId,
                            workerName = worker.name,
                            date = date,
                            timestamp = it,
                            type = "OUT",
                            isEarlyOut = isEarly,
                            wifiSsid = "Manual_Override"
                        )
                    )
                }

                emitMessage("Log bertarikh $date untuk ${worker.name} dikemaskini!")
            } catch (e: Exception) {
                emitMessage("Ralat format. Sila semak semula.")
            }
        }
    }

    fun clearAllLogs() {
        viewModelScope.launch {
            repository.clearAllEvents()
            val all = allLogs.value
            for (log in all) {
                repository.deleteLog(log)
            }
            emitMessage("Semua rekod kehadiran dipadamkan!")
        }
    }

    fun updateWorkerInfo(id: Int, name: String, email: String) {
        viewModelScope.launch {
            val existing = workers.value.find { it.id == id }
            if (existing != null) {
                val updated = existing.copy(name = name, email = email)
                repository.updateWorker(updated)
                emitMessage("Pekerja ${name} berjaya dikemaskini!")
            }
        }
    }

    fun updateWorkerWithCredentials(id: Int, name: String, username: String, password: String, email: String, isAdmin: Boolean) {
        viewModelScope.launch {
            val existing = workers.value.find { it.id == id }
            if (existing != null) {
                val updated = existing.copy(
                    name = name,
                    username = username.trim(),
                    password = password,
                    email = email,
                    isAdmin = isAdmin
                )
                repository.updateWorker(updated)
                emitMessage("Profil '$name' berjaya dikemaskini!")
            }
        }
    }

    // Get weekly total hours of current week (Monday to Saturday)
    fun getWeeklyHours(workerId: Int, logs: List<PunchLog>): Map<String, Double> {
        val currentWeekDates = getMondayToSaturdayDatesOfCurrentWeek()
        val weeklyLogs = logs.filter { it.workerId == workerId && it.date in currentWeekDates }
        
        val hoursMap = mutableMapOf<String, Double>()
        val malayDays = listOf("Isnin", "Selasa", "Rabu", "Khamis", "Jumaat", "Sabtu")
        for (day in malayDays) {
            hoursMap[day] = 0.0
        }

        for (log in weeklyLogs) {
            val dayName = getMalayDayName(log.date)
            if (dayName in hoursMap) {
                hoursMap[dayName] = (hoursMap[dayName] ?: 0.0) + (log.durationMinutes / 60.0)
            }
        }
        return hoursMap
    }

    fun generateAndSendPDF(context: Context, monthYear: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val activeWorkers = workers.value
            val activeLogs = allLogs.value
            val cName = companyName.value
            val cAddress = companyAddress.value
            val rEmail = emailDestination.value
            
            val pdfFile = PdfReportGenerator.generateMonthlyReport(context, monthYear, activeWorkers, activeLogs, cName, cAddress, rEmail)
            
            withContext(Dispatchers.Main) {
                if (pdfFile != null && pdfFile.exists()) {
                    emitMessage("PDF Laporan Bulanan ($monthYear) Berjaya Dijana!")
                    PdfReportGenerator.shareReportEmail(context, pdfFile, monthYear, rEmail)
                } else {
                    emitMessage("Gagal menjana PDF laporan.")
                }
            }
        }
    }

    fun triggerInstantCheckInReminders() {
        val todayDate = ServerTimeService.formatToMalaysiaDate(ServerTimeService.currentTimeMillis(), "yyyy-MM-dd")
        val activeWorkers = workers.value
        val activeLogs = allLogs.value

        val todayLogs = activeLogs.filter { it.date == todayDate }
        val checkedInIds = todayLogs.filter { it.punchInTime != null }.map { it.workerId }
        val absentWorkers = activeWorkers.filter { it.id !in checkedInIds }

        if (absentWorkers.isNotEmpty()) {
            val absentNames = absentWorkers.joinToString(", ") { it.name }
            NotificationReceiver.sendAbsentNotification(
                getApplication(),
                "Peringatan: Kehadiran Punch Card",
                "Kakitangan ($absentNames) belum mendaftar masuk!"
            )
            emitMessage("Peringatan daftar masuk dihantar ke peranti!")
        } else {
            NotificationReceiver.sendAbsentNotification(
                getApplication(),
                "Peringatan: Punch Card Rumah",
                "Semua pekerja aktif sudah mendaftar masuk hari ini!"
            )
            emitMessage("Pemberitahuan dihantar (Semua pekerja sudah hadir).")
        }
    }

    private fun emitMessage(msg: String) {
        viewModelScope.launch {
            _uiToastMessage.emit(msg)
        }
    }

    private fun checkIsLate(timestampMs: Long): Boolean {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kuala_Lumpur"))
        cal.timeInMillis = timestampMs
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        return hour > 8 || (hour == 8 && minute > 0)
    }

    private fun checkIsEarlyOut(timestampMs: Long): Boolean {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kuala_Lumpur"))
        cal.timeInMillis = timestampMs
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        return hour < 17
    }

    private fun getMondayToSaturdayDatesOfCurrentWeek(): List<String> {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kuala_Lumpur"))
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val offset = when(dayOfWeek) {
            Calendar.SUNDAY -> -6
            else -> Calendar.MONDAY - dayOfWeek
        }
        cal.add(Calendar.DAY_OF_YEAR, offset)
        
        val dates = ArrayList<String>()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("Asia/Kuala_Lumpur")
        for (i in 0..5) {
            dates.add(sdf.format(cal.time))
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return dates
    }

    private fun getMalayDayName(dateStr: String): String {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("Asia/Kuala_Lumpur")
            val date = sdf.parse(dateStr) ?: return ""
            val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kuala_Lumpur"))
            cal.time = date
            when (cal.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> "Isnin"
                Calendar.TUESDAY -> "Selasa"
                Calendar.WEDNESDAY -> "Rabu"
                Calendar.THURSDAY -> "Khamis"
                Calendar.FRIDAY -> "Jumaat"
                Calendar.SATURDAY -> "Sabtu"
                Calendar.SUNDAY -> "Ahad"
                else -> ""
            }
        } catch (e: Exception) {
            ""
        }
    }
}
