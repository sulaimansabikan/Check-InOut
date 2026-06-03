package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

class Repository(private val database: AppDatabase) {
    private val workerDao = database.workerDao()
    private val punchLogDao = database.punchLogDao()
    private val settingDao = database.settingDao()
    private val punchEventDao = database.punchEventDao()

    val allWorkers: Flow<List<Worker>> = workerDao.getAllWorkers()
    val allLogs: Flow<List<PunchLog>> = punchLogDao.getAllLogs()
    val allEvents: Flow<List<PunchEvent>> = punchEventDao.getAllEvents()

    fun getLogsByWorker(workerId: Int): Flow<List<PunchLog>> = punchLogDao.getLogsByWorker(workerId)
    fun getEventsByWorker(workerId: Int): Flow<List<PunchEvent>> = punchEventDao.getEventsByWorker(workerId)

    suspend fun getLogForDate(workerId: Int, date: String): PunchLog? = punchLogDao.getLogForDate(workerId, date)

    suspend fun insertLog(log: PunchLog) = punchLogDao.insertLog(log)
    suspend fun updateLog(log: PunchLog) = punchLogDao.updateLog(log)
    suspend fun deleteLog(log: PunchLog) = punchLogDao.deleteLog(log)

    suspend fun insertWorker(worker: Worker): Long = workerDao.insertWorker(worker)
    suspend fun updateWorker(worker: Worker) = workerDao.updateWorker(worker)
    suspend fun deleteWorker(worker: Worker) = workerDao.deleteWorker(worker)

    suspend fun saveSetting(key: String, value: String) {
        settingDao.insertSetting(AppSetting(key, value))
    }

    suspend fun getSettingValue(key: String, defaultValue: String): String {
        return settingDao.getSetting(key)?.value ?: defaultValue
    }

    fun getSettingValueFlow(key: String, defaultValue: String): Flow<String> {
        return settingDao.getSettingFlow(key).map { it?.value ?: defaultValue }
    }

    suspend fun getLogsBetweenDatesDirect(startDate: String, endDate: String): List<PunchLog> {
        return punchLogDao.getLogsBetweenDatesDirect(startDate, endDate)
    }

    fun getLogsBetweenDates(startDate: String, endDate: String): Flow<List<PunchLog>> {
        return punchLogDao.getLogsBetweenDates(startDate, endDate)
    }

    suspend fun insertEvent(event: PunchEvent) {
        punchEventDao.insertEvent(event)
        updateDailyLogFromEvents(event.workerId, event.workerName, event.date)
    }

    suspend fun deleteEvent(event: PunchEvent) {
        punchEventDao.deleteEvent(event)
        updateDailyLogFromEvents(event.workerId, event.workerName, event.date)
    }

    suspend fun clearAllEvents() {
        punchEventDao.deleteAllEvents()
    }

    suspend fun updateDailyLogFromEvents(workerId: Int, workerName: String, date: String) {
        val events = punchEventDao.getEventsForWorkerAndDate(workerId, date)
        if (events.isEmpty()) {
            val existing = punchLogDao.getLogForDate(workerId, date)
            if (existing != null) {
                punchLogDao.deleteLog(existing)
            }
            return
        }

        // Aggregate
        val inEvents = events.filter { it.type == "IN" }
        val outEvents = events.filter { it.type == "OUT" }

        val earliestIn = inEvents.minByOrNull { it.timestamp }
        val latestOut = outEvents.maxByOrNull { it.timestamp }

        val punchInTime = earliestIn?.timestamp
        val punchOutTime = latestOut?.timestamp

        val isLate = earliestIn?.isLate ?: false
        val isEarlyOut = latestOut?.isEarlyOut ?: false
        val wifiSsid = earliestIn?.wifiSsid ?: latestOut?.wifiSsid

        val durationMinutes = if (punchInTime != null && punchOutTime != null && punchOutTime > punchInTime) {
            (punchOutTime - punchInTime) / (1000 * 60)
        } else {
            0L
        }

        val punchLog = PunchLog(
            workerId = workerId,
            workerName = workerName,
            date = date,
            punchInTime = punchInTime,
            punchOutTime = punchOutTime,
            isLate = isLate,
            isEarlyOut = isEarlyOut,
            durationMinutes = durationMinutes,
            wifiSsid = wifiSsid
        )

        val existingLog = punchLogDao.getLogForDate(workerId, date)
        if (existingLog != null) {
            punchLogDao.updateLog(punchLog.copy(id = existingLog.id))
        } else {
            punchLogDao.insertLog(punchLog)
        }
    }

    suspend fun preseedDatabaseIfEmpty() {
        val workers = workerDao.getAllWorkers().firstOrNull() ?: emptyList()
        if (workers.isEmpty()) {
            // Seed database with usernames, passwords, and correct Admin roles
            workerDao.insertWorker(Worker(id = 1, name = "Sulaiman (Saya)", username = "sulaiman", password = "123", isAdmin = true, email = "sulaimansabikan@gmail.com"))
            workerDao.insertWorker(Worker(id = 2, name = "Ahmad (Kakitangan)", username = "ahmad", password = "123", isAdmin = false, email = ""))
            workerDao.insertWorker(Worker(id = 3, name = "Siti (Kakitangan)", username = "siti", password = "123", isAdmin = false, email = ""))
        }

        // Seed some essential settings if missing
        if (settingDao.getSetting("wifi_ssid") == null) {
            settingDao.insertSetting(AppSetting("wifi_ssid", "Sulaiman_Home_WiFi"))
        }
        if (settingDao.getSetting("email_report") == null) {
            settingDao.insertSetting(AppSetting("email_report", "sulaimansabikan@gmail.com"))
        }
        if (settingDao.getSetting("google_sheets_url") == null) {
            settingDao.insertSetting(AppSetting("google_sheets_url", ""))
        }
        if (settingDao.getSetting("developer_bypass") == null) {
            settingDao.insertSetting(AppSetting("developer_bypass", "true"))
        }
        if (settingDao.getSetting("reminder_time") == null) {
            settingDao.insertSetting(AppSetting("reminder_time", "07:45"))
        }
        if (settingDao.getSetting("company_name") == null) {
            settingDao.insertSetting(AppSetting("company_name", "SULAIMAN INTEGRATED SERVICES"))
        }
        if (settingDao.getSetting("company_address") == null) {
            settingDao.insertSetting(AppSetting("company_address", "No 15, Jalan Perusahaan 2, Kawasan Perindustrian,\n43000 Kajang, Selangor Darul Ehsan"))
        }
        if (settingDao.getSetting("company_logo_icon") == null) {
            settingDao.insertSetting(AppSetting("company_logo_icon", "business"))
        }
    }

    // Punch in action
    suspend fun punchIn(workerId: Int, name: String, date: String, timestamp: Long, isLate: Boolean, ssid: String?): Boolean {
        val event = PunchEvent(
            workerId = workerId,
            workerName = name,
            date = date,
            timestamp = timestamp,
            type = "IN",
            isLate = isLate,
            wifiSsid = ssid
        )
        insertEvent(event)
        return true
    }

    // Punch out action
    suspend fun punchOut(workerId: Int, name: String, date: String, timestamp: Long, isEarlyOut: Boolean, ssid: String?): Boolean {
        val event = PunchEvent(
            workerId = workerId,
            workerName = name,
            date = date,
            timestamp = timestamp,
            type = "OUT",
            isEarlyOut = isEarlyOut,
            wifiSsid = ssid
        )
        insertEvent(event)
        return true
    }
}
