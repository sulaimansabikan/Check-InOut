package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Worker
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val workers by viewModel.workers.collectAsStateWithLifecycle()
    val designatedSsid by viewModel.designatedSsid.collectAsStateWithLifecycle()
    val emailDestination by viewModel.emailDestination.collectAsStateWithLifecycle()
    val isDeveloperBypass by viewModel.isDeveloperBypass.collectAsStateWithLifecycle()
    val reminderTimeSetting by viewModel.reminderTimeSetting.collectAsStateWithLifecycle()
    val companyNameSetting by viewModel.companyName.collectAsStateWithLifecycle()
    val companyAddressSetting by viewModel.companyAddress.collectAsStateWithLifecycle()
    val companyLogoIconSetting by viewModel.companyLogoIcon.collectAsStateWithLifecycle()

    // Form inputs states
    var wifiInput by remember { mutableStateOf(designatedSsid) }
    var emailInput by remember { mutableStateOf(emailDestination) }
    var reminderInput by remember { mutableStateOf(reminderTimeSetting) }
    var companyNameInput by remember { mutableStateOf(companyNameSetting) }
    var companyAddressInput by remember { mutableStateOf(companyAddressSetting) }
    var companyLogoIconInput by remember { mutableStateOf(companyLogoIconSetting) }

    // Sync inputs when DB updates
    LaunchedEffect(designatedSsid) { wifiInput = designatedSsid }
    LaunchedEffect(emailDestination) { emailInput = emailDestination }
    LaunchedEffect(reminderTimeSetting) { reminderInput = reminderTimeSetting }
    LaunchedEffect(companyNameSetting) { companyNameInput = companyNameSetting }
    LaunchedEffect(companyAddressSetting) { companyAddressInput = companyAddressSetting }
    LaunchedEffect(companyLogoIconSetting) { companyLogoIconInput = companyLogoIconSetting }

    // PDF month range selector list (Mon-Sat stats ready)
    val listMonths = listOf(
        "2026-06" to "Jun 2026",
        "2026-05" to "Mei 2026",
        "2026-04" to "April 2026",
        "2026-03" to "Mac 2026"
    )
    var selectedMonthVal by remember { mutableStateOf("2026-06") }

    // State for Editing an existing worker
    var editingWorker by remember { mutableStateOf<Worker?>(null) }
    var wName by remember { mutableStateOf("") }
    var wUsername by remember { mutableStateOf("") }
    var wPassword by remember { mutableStateOf("") }
    var wEmail by remember { mutableStateOf("") }
    var wIsAdmin by remember { mutableStateOf(false) }

    // State for Registering a new worker
    var isAddNewWorkerOpen by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newUsername by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var newEmail by remember { mutableStateOf("") }
    var newIsAdmin by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(AppBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        
        // Settings page header
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            Text(
                text = "PANEL PENTADBIR (ADMIN)",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Orange500,
                letterSpacing = 1.2.sp
            )
            Text(
                text = "Tetapan & Laporan PDF",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = Blue700
            )
        }

        // --- SECTION 1: ATTENDANCE PDF SUMMARY GENERATOR ---
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, CardBorderColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = "PDF icon",
                        tint = Blue600,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "PENJANAAN LAPORAN BULANAN (PDF)",
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        color = Slate800,
                        letterSpacing = 0.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Muat turun laporan prestasi kerja dan kehadiran bulanan pekerja (Isnin hingga Sabtu) yang diformat khusus dalam PDF dan dihantar terus ke e-mel pengurus.",
                    fontSize = 11.sp,
                    color = Slate500,
                    lineHeight = 15.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text("Pilih Bulan Larian Laporan:", fontSize = 10.sp, color = Slate400, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listMonths.forEach { (yearMonth, label) ->
                        val isSelected = yearMonth == selectedMonthVal
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isSelected) Blue50 else Slate50,
                                    RoundedCornerShape(10.dp)
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) Blue600.copy(alpha = 0.3f) else Slate200,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { selectedMonthVal = yearMonth }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Blue600 else Slate700
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { viewModel.generateAndSendPDF(context, selectedMonthVal) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("generate_pdf_report_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Blue600)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Email, contentDescription = "Send PDF Icon", tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "HANTAR PDF KE EMAIL ($emailDestination)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // --- SECTION 2: WIFI ACCESSIBILITY RESTRICTION CONFIG ---
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, CardBorderColor)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Wifi, contentDescription = "WiFi", tint = Blue600, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "KAWALAN SEKATAN WiFi RUMAH",
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        color = Slate800,
                        letterSpacing = 0.5.sp
                    )
                }

                Text(
                    text = "Aplikasi ini membataskan daftar masuk dan keluar pekerja melainkan apabila tersambung ke nama SSID Wi-Fi yang anda tetapkan.",
                    fontSize = 11.sp,
                    color = Slate500,
                    lineHeight = 15.sp
                )

                OutlinedTextField(
                    value = wifiInput,
                    onValueChange = { wifiInput = it },
                    label = { Text("Nama Wi-Fi Rumah (SSID) Sasaran", fontSize = 11.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Slate900,
                        unfocusedTextColor = Slate900,
                        focusedBorderColor = Blue600,
                        unfocusedBorderColor = Slate200,
                        unfocusedContainerColor = Slate50,
                        focusedContainerColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("designated_wifi_input"),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            viewModel.saveDesignatedSsid(wifiInput)
                            keyboardController?.hide()
                        }
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { 
                            viewModel.saveDesignatedSsid(wifiInput)
                            keyboardController?.hide()
                        },
                        modifier = Modifier.testTag("save_wifi_setting_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Slate900)
                    ) {
                        Text("Kemaskini Nama WiFi", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    // DEV WIFI SIMULATION BYPASS
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Ujian Pintas", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Slate400)
                        Switch(
                            checked = isDeveloperBypass == "true",
                            onCheckedChange = { viewModel.saveDeveloperBypass(it) },
                            modifier = Modifier.testTag("developer_bypass_switch"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Orange500,
                                uncheckedThumbColor = Slate400,
                                uncheckedTrackColor = Slate100
                            )
                        )
                    }
                }
            }
        }

        // --- SECTION: PROFIL & LOGO SYARIKAT ---
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, CardBorderColor)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Business, contentDescription = "Syarikat", tint = Blue600, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "PROFIL & LOGO SYARIKAT",
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        color = Slate800,
                        letterSpacing = 0.5.sp
                    )
                }

                Text(
                    text = "Kemaskini nama, alamat syarikat dan ikon korporat yang dipaparkan pada skrin utama dan laporan PDF.",
                    fontSize = 11.sp,
                    color = Slate500,
                    lineHeight = 15.sp
                )

                // Company Name Input
                OutlinedTextField(
                    value = companyNameInput,
                    onValueChange = { companyNameInput = it },
                    label = { Text("Nama Syarikat / Organisasi", fontSize = 11.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Slate900,
                        unfocusedTextColor = Slate900,
                        focusedBorderColor = Blue600,
                        unfocusedBorderColor = Slate200,
                        unfocusedContainerColor = Slate50,
                        focusedContainerColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("company_name_setting_input")
                )

                // Company Address Input
                OutlinedTextField(
                    value = companyAddressInput,
                    onValueChange = { companyAddressInput = it },
                    label = { Text("Alamat Syarikat", fontSize = 11.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Slate900,
                        unfocusedTextColor = Slate900,
                        focusedBorderColor = Blue600,
                        unfocusedBorderColor = Slate200,
                        unfocusedContainerColor = Slate50,
                        focusedContainerColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .testTag("company_address_setting_input")
                )

                // Logo Symbol Selector
                Text("Pilih Lambang/Ikon Syarikat:", fontSize = 10.sp, color = Slate400, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "business" to "Pejabat",
                        "home" to "Kediaman",
                        "store" to "Kedai",
                        "build" to "Bengkel"
                    ).forEach { (key, label) ->
                        val isSelected = key == companyLogoIconInput
                        val iconVector = when(key) {
                            "home" -> Icons.Filled.Home
                            "store" -> Icons.Filled.Store
                            "build" -> Icons.Filled.Build
                            else -> Icons.Filled.Business
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(if (isSelected) Blue50 else Slate50, RoundedCornerShape(10.dp))
                                .border(1.dp, if (isSelected) Blue600.copy(alpha = 0.3f) else Slate200, RoundedCornerShape(10.dp))
                                .clickable { companyLogoIconInput = key }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(
                                    imageVector = iconVector,
                                    contentDescription = label,
                                    tint = if (isSelected) Blue600 else Slate400,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = label,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Blue600 else Slate500
                                )
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        viewModel.saveCompanyName(companyNameInput)
                        viewModel.saveCompanyAddress(companyAddressInput)
                        viewModel.saveCompanyLogoIcon(companyLogoIconInput)
                        keyboardController?.hide()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_company_profile_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Slate900)
                ) {
                    Icon(Icons.Default.Save, contentDescription = "Save Profile", modifier = Modifier.size(15.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Simpan Info Syarikat", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        // --- SECTION 3: SYSTEM PRESETS CONFIG ---
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, CardBorderColor)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Settings, contentDescription = "Presets", tint = Blue600, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "KONFIGURASI PRESET AUTOMATIK",
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        color = Slate800,
                        letterSpacing = 0.5.sp
                    )
                }

                // Email Destination Config
                OutlinedTextField(
                    value = emailInput,
                    onValueChange = { emailInput = it },
                    label = { Text("E-mel Penerima Laporan Bulanan (PDF)", fontSize = 11.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Slate900,
                        unfocusedTextColor = Slate900,
                        focusedBorderColor = Blue600,
                        unfocusedBorderColor = Slate200,
                        unfocusedContainerColor = Slate50,
                        focusedContainerColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("recipient_email_input"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            viewModel.saveEmailDestination(emailInput)
                            keyboardController?.hide()
                        }
                    )
                )

                // Alarms / Notifications Reminder Clock Config
                OutlinedTextField(
                    value = reminderInput,
                    onValueChange = { reminderInput = it },
                    label = { Text("Waktu Notifikasi Peringatan Harian (HH:MM)", fontSize = 11.sp) },
                    placeholder = { Text("07:45") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Slate900,
                        unfocusedTextColor = Slate900,
                        focusedBorderColor = Blue600,
                        unfocusedBorderColor = Slate200,
                        unfocusedContainerColor = Slate50,
                        focusedContainerColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reminder_time_input"),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            viewModel.saveReminderTime(reminderInput)
                            keyboardController?.hide()
                        }
                    )
                )

                Button(
                    onClick = {
                        viewModel.saveEmailDestination(emailInput)
                        viewModel.saveReminderTime(reminderInput)
                        keyboardController?.hide()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_presets_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Slate900)
                ) {
                    Icon(Icons.Default.Save, contentDescription = "Save", modifier = Modifier.size(15.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Simpan Konfigurasi Preset", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        // --- SECTION 4: EDIT & MANAGE STAFFS PROFILES ---
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, CardBorderColor)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Group, contentDescription = "Staff", tint = Blue600, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SENARAI PROFIL KERJA",
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            color = Slate800,
                            letterSpacing = 0.5.sp
                        )
                    }

                    // Button to add workers
                    Text(
                        text = if (isAddNewWorkerOpen) "Tutup Form" else "+ Tambah Pekerja",
                        color = Orange500,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier
                            .clickable { isAddNewWorkerOpen = !isAddNewWorkerOpen }
                            .padding(4.dp)
                    )
                }

                Text(
                    text = "Uruskan senarai kelayakan profil kakitangan, nama pengguna (username) dan kata laluan unik bagi mendaftar masuk kerja.",
                    fontSize = 11.sp,
                    color = Slate500,
                    lineHeight = 15.sp
                )

                // Render workers list
                workers.forEach { worker ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Slate50, RoundedCornerShape(12.dp))
                            .border(1.dp, Slate100, RoundedCornerShape(12.dp))
                            .clickable {
                                editingWorker = worker
                                wName = worker.name
                                wUsername = worker.username
                                wPassword = worker.password
                                wEmail = worker.email
                                wIsAdmin = worker.isAdmin
                            }
                            .padding(14.dp)
                            .testTag("edit_worker_row_${worker.id}"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = worker.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Slate900
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Username: ${worker.username}",
                                    fontSize = 11.sp,
                                    color = Blue600,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Password: ${worker.password}",
                                    fontSize = 11.sp,
                                    color = Slate500
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (worker.isAdmin) Color(0xFFFFF7ED) else Color(0xFFEFF6FF),
                                            RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (worker.isAdmin) "Admin" else "Normal Staff",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (worker.isAdmin) Orange600 else Blue600
                                    )
                                }
                                if (worker.email.isNotBlank()) {
                                    Text(text = "E-mel: ${worker.email}", fontSize = 10.sp, color = Slate400)
                                }
                            }
                        }

                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Profile",
                            tint = Slate400,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // --- SUB SECTION: COLLAPSIBLE WORKER EDITOR FORM ---
        AnimatedVisibility(
            visible = editingWorker != null,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            editingWorker?.let { worker ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Blue600.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "KEMASKINI PROFIL KAKITANGAN: ID #${worker.id}",
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            color = Blue600,
                            letterSpacing = 0.5.sp
                        )

                        OutlinedTextField(
                            value = wName,
                            onValueChange = { wName = it },
                            label = { Text("Nama Penuh Pekerja", fontSize = 11.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Slate900,
                                unfocusedTextColor = Slate900,
                                focusedBorderColor = Blue600,
                                unfocusedBorderColor = Slate200,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("edit_w_name")
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = wUsername,
                                onValueChange = { wUsername = it },
                                label = { Text("Nama Pengguna (Login)", fontSize = 11.sp) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Slate900,
                                    unfocusedTextColor = Slate900,
                                    focusedBorderColor = Blue600,
                                    unfocusedBorderColor = Slate200,
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("edit_w_username")
                            )

                            OutlinedTextField(
                                value = wPassword,
                                onValueChange = { wPassword = it },
                                label = { Text("Kata Laluan (Password)", fontSize = 11.sp) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Slate900,
                                    unfocusedTextColor = Slate900,
                                    focusedBorderColor = Blue600,
                                    unfocusedBorderColor = Slate200,
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("edit_w_password")
                            )
                        }

                        OutlinedTextField(
                            value = wEmail,
                            onValueChange = { wEmail = it },
                            label = { Text("E-mel Pekerja (Pilihan)", fontSize = 11.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Slate900,
                                unfocusedTextColor = Slate900,
                                focusedBorderColor = Blue600,
                                unfocusedBorderColor = Slate200,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("edit_w_email")
                        )

                        // Set Admin status
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = wIsAdmin,
                                onCheckedChange = { wIsAdmin = it },
                                colors = CheckboxDefaults.colors(checkedColor = Blue600)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                "Mempunyai Akses Admin (Boleh urus settings & hantar PDF)",
                                fontSize = 11.sp,
                                color = Slate800,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { editingWorker = null },
                                colors = ButtonDefaults.buttonColors(containerColor = Slate100),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Text("Batal", color = Slate700, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    viewModel.updateWorkerWithCredentials(
                                        worker.id,
                                        wName,
                                        wUsername,
                                        wPassword,
                                        wEmail,
                                        wIsAdmin
                                    )
                                    editingWorker = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Blue600),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("save_edit_w_button")
                            ) {
                                Text("Simpan Profil", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        // --- SUB SECTION: COLLAPSIBLE REGISTER NEW WORKER FORM ---
        AnimatedVisibility(
            visible = isAddNewWorkerOpen,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Orange500.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "DAFTAR PEKERJA BAHARU",
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        color = Orange600,
                        letterSpacing = 0.5.sp
                    )

                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Nama Penuh Pekerja", fontSize = 11.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Slate900,
                            unfocusedTextColor = Slate900,
                            focusedBorderColor = Orange500,
                            unfocusedBorderColor = Slate200,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_w_name")
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = newUsername,
                            onValueChange = { newUsername = it },
                            label = { Text("Username", fontSize = 11.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Slate900,
                                unfocusedTextColor = Slate900,
                                focusedBorderColor = Orange500,
                                unfocusedBorderColor = Slate200,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("new_w_username")
                        )

                        OutlinedTextField(
                            value = newPassword,
                            onValueChange = { newPassword = it },
                            label = { Text("Kata Laluan", fontSize = 11.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Slate900,
                                unfocusedTextColor = Slate900,
                                focusedBorderColor = Orange500,
                                unfocusedBorderColor = Slate200,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("new_w_password")
                        )
                    }

                    OutlinedTextField(
                        value = newEmail,
                        onValueChange = { newEmail = it },
                        label = { Text("E-mel Pekerja (Pilihan)", fontSize = 11.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Slate900,
                            unfocusedTextColor = Slate900,
                            focusedBorderColor = Orange500,
                            unfocusedBorderColor = Slate200,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_w_email")
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = newIsAdmin,
                            onCheckedChange = { newIsAdmin = it },
                            colors = CheckboxDefaults.colors(checkedColor = Orange500)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            "Berikan Kuasa Admin",
                            fontSize = 11.sp,
                            color = Slate800,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                isAddNewWorkerOpen = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Slate100),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text("Batal", color = Slate700, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                viewModel.addNewWorker(
                                    name = newName,
                                    email = newEmail,
                                    username = newUsername,
                                    password = newPassword,
                                    isAdmin = newIsAdmin
                                )
                                // Reset inputs
                                newName = ""
                                newUsername = ""
                                newPassword = ""
                                newEmail = ""
                                newIsAdmin = false
                                isAddNewWorkerOpen = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Orange500),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("save_new_w_button")
                        ) {
                            Text("Daftar Pekerja", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }

        // --- SECTION 5: DANGER RESET AREA ---
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
            border = BorderStroke(1.dp, Color(0xFFFCA5A5).copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "KAWASAN AMARAN PENTADBIR (DANGER ZONE)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = Color(0xFF991B1B),
                    letterSpacing = 0.5.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Padamkan semua rekod kehadiran di dalam pangkalan data.",
                        fontSize = 11.sp,
                        color = Color(0xFF7F1D1D),
                        modifier = Modifier.weight(2f),
                        lineHeight = 15.sp
                    )

                    Button(
                        onClick = { viewModel.clearAllLogs() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF991B1B)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("clear_logs_button"),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text("Padam Semua", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}
