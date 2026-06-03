package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.service.ServerTimeService
import com.example.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val serverTimeMs by viewModel.serverTimeMs.collectAsStateWithLifecycle()
    val isInternetConnected by viewModel.isConnectedToInternet.collectAsStateWithLifecycle()
    val isDeveloperBypass by viewModel.isDeveloperBypass.collectAsStateWithLifecycle()
    val simulatedSsid by viewModel.simulatedSsid.collectAsStateWithLifecycle()
    val detectedSsid by viewModel.detectedSsid.collectAsStateWithLifecycle()
    val designatedSsid by viewModel.designatedSsid.collectAsStateWithLifecycle()
    
    val loggedInUser by viewModel.loggedInUser.collectAsStateWithLifecycle()
    val selectedWorkerId by viewModel.selectedWorkerId.collectAsStateWithLifecycle()
    val workers by viewModel.workers.collectAsStateWithLifecycle()
    val allLogs by viewModel.allLogs.collectAsStateWithLifecycle()
    val currentWorker by viewModel.currentWorker.collectAsStateWithLifecycle()

    val scrollState = rememberScrollState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val currentActiveSsid = if (isDeveloperBypass == "true") simulatedSsid else detectedSsid
    val canPunch = viewModel.canPunchCard()

    val todayStr = remember(serverTimeMs) {
        ServerTimeService.formatToMalaysiaTime(serverTimeMs, "yyyy-MM-dd")
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppBackground)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        
        // 1. App Header Greeting & Logout Action
        val headerDate = remember(serverTimeMs) {
            ServerTimeService.formatToMalaysiaTime(serverTimeMs, "EEEE, d MMMM YYYY")
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = headerDate.uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Orange500,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Laman Utama",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = Blue700
                )
            }
            
            // Log Out Button
            Button(
                onClick = { viewModel.logout() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFFDC2626)
                ),
                border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("logout_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Logout,
                        contentDescription = "Log Keluar",
                        modifier = Modifier.size(16.dp)
                    )
                    Text("Keluar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // 2. Profile Selector Row: Admins see all, Workers see locked single user info
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, CardBorderColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (loggedInUser?.isAdmin == true) {
                    Text(
                        text = "ADMIN: URUS KAUNTER PENTADBIR",
                        fontWeight = FontWeight.Black,
                        fontSize = 10.sp,
                        color = Orange500,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(workers) { worker ->
                            val isSelected = worker.id == selectedWorkerId
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.selectWorker(worker.id) },
                                label = { 
                                    Text(
                                        text = worker.name,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Blue600 else Slate700
                                    ) 
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.Person,
                                        contentDescription = "Pilihan Profil",
                                        tint = if (isSelected) Blue600 else Slate400,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Blue50,
                                    containerColor = Color.Transparent
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = Slate200,
                                    selectedBorderColor = Blue600.copy(alpha = 0.3f),
                                    borderWidth = 1.dp,
                                    selectedBorderWidth = 1.dp
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("worker_chip_${worker.id}")
                            )
                        }
                    }
                } else {
                    // Lock for normal workers
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Blue50),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Person, "Profil", tint = Blue600)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("DAFTAR SEBAGAI", fontSize = 9.sp, color = Slate400, fontWeight = FontWeight.Bold)
                                Text(
                                    text = loggedInUser?.name ?: "Kakitangan",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate900
                                )
                            }
                        }
                        
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFF0FDF4), RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFFBBF7D0), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Profil Disahkan", fontSize = 10.sp, color = Color(0xFF15803D), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 3. Server-Synced Digital Wall Clock Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, CardBorderColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp, horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "JAM PELAYAN DISINKRONISASI",
                    color = Slate400,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                
                Spacer(modifier = Modifier.height(2.dp))
                
                Text(
                    text = ServerTimeService.formatToMalaysiaTime(serverTimeMs, "HH:mm:ss"),
                    color = Blue700,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Light,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(2.dp))
                
                val isCheckedInToday = remember(allLogs, selectedWorkerId, todayStr) {
                    allLogs.any { it.workerId == selectedWorkerId && it.date == todayStr && it.punchInTime != null }
                }
                val isCheckedOutToday = remember(allLogs, selectedWorkerId, todayStr) {
                    allLogs.any { it.workerId == selectedWorkerId && it.date == todayStr && it.punchOutTime != null }
                }

                val statusText = when {
                    !canPunch -> "Sila Sambung WiFi Rumah"
                    isCheckedOutToday -> "Telah Keluar Selesai Kerja"
                    isCheckedInToday -> "Sudah Masuk, Sedia untuk keluar"
                    else -> "Sedia Punch Masuk Kerja"
                }
                val statusColor = when {
                    !canPunch -> Color(0xFFDC2626)
                    isCheckedOutToday -> Slate500
                    isCheckedInToday -> Orange500
                    else -> Color(0xFF059669)
                }

                Text(
                    text = statusText,
                    color = statusColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 4. WiFi Restriction Banner
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (canPunch) Color(0xFFF0FDF4) else Color(0xFFFEF2F2)
            ),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(
                1.dp,
                if (canPunch) Color(0xFFBBF7D0) else Color(0xFFFCA5A5)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (canPunch) Icons.Default.LockOpen else Icons.Default.Lock,
                            contentDescription = "Status WiFi",
                            tint = if (canPunch) Color(0xFF15803D) else Color(0xFF991B1B),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (canPunch) "WiFi RUMAH DISAHKAN" else "SEKATAN WiFi RUMAH: DIKUNCI",
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            color = if (canPunch) Color(0xFF15803D) else Color(0xFF991B1B)
                        )
                    }

                    // Network state Dot
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(
                                    if (isInternetConnected) Color(0xFF22C55E) else Color(0xFFEF4444),
                                    CircleShape
                                )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isInternetConnected) "Online" else "Offline",
                            fontSize = 10.sp,
                            color = Slate500,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                
                Text(
                    text = if (canPunch) "Anda berada di dalam jangkauan WiFi rumah. Kebenaran punch card aktif."
                           else "Aktiviti mendaftar masuk/keluar disekat. Sambung peranti anda ke WiFi rumah yang dibenarkan sahaja.",
                    fontSize = 11.sp,
                    color = Slate600,
                    lineHeight = 15.sp
                )

                Spacer(modifier = Modifier.height(10.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = "WiFi Ditetapkan:", fontSize = 9.sp, color = Slate400, fontWeight = FontWeight.Bold)
                        Text(text = designatedSsid, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Slate800)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = "WiFi Semasa:", fontSize = 9.sp, color = Slate400, fontWeight = FontWeight.Bold)
                        Text(
                            text = if (currentActiveSsid.isNotBlank()) currentActiveSsid else "Tiada Sambungan",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (canPunch) Color(0xFF15803D) else Color(0xFF991B1B)
                        )
                    }
                }

                // Simulated Input Widget if in Developer Bypass simulation Mode
                if (isDeveloperBypass == "true") {
                    Spacer(modifier = Modifier.height(12.dp))
                    Column(
                        modifier = Modifier
                            .background(Color.White, RoundedCornerShape(12.dp))
                            .border(1.dp, Slate100, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "🛠️ PENGUJI: TUKAR WiFi SIMULASI",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Orange500,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = simulatedSsid,
                            onValueChange = { viewModel.updateSimulatedSsid(it) },
                            label = { Text("Nama Wi-Fi Simulasi semasa", fontSize = 10.sp) },
                            singleLine = true,
                            trailingIcon = {
                                if (simulatedSsid == designatedSsid) {
                                    Icon(Icons.Default.Check, contentDescription = "Sama", tint = Color(0xFF22C55E))
                                } else {
                                    Icon(Icons.Default.Warning, contentDescription = "Salah", tint = Color(0xFFEF4444))
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("wifi_simulator_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Slate900,
                                unfocusedTextColor = Slate900,
                                focusedBorderColor = Blue600,
                                unfocusedBorderColor = Slate200,
                                unfocusedContainerColor = Slate50,
                                focusedContainerColor = Color.White
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                                onDone = { focusManager.clearFocus() }
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tip: Jadikan nama WiFi Simulasi sama dengan WiFi Ditetapkan agar lulus sekatan rangkaian.",
                            fontSize = 9.sp,
                            color = Slate400,
                            lineHeight = 12.sp
                        )
                    }
                }
            }
        }

        // 5. Primary Punch In & Out Actions
        currentWorker?.let { worker ->
            Text(
                text = "KAWALAN PUNCH KAD - ${worker.name.uppercase()}",
                fontWeight = FontWeight.Black,
                fontSize = 11.sp,
                color = Slate500,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 4.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Punch In Button
                Button(
                    onClick = { viewModel.punchInActiveWorker() },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("punch_in_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Blue600,
                        contentColor = Color.White,
                        disabledContainerColor = Slate200,
                        disabledContentColor = Slate400
                    ),
                    shape = RoundedCornerShape(12.dp),
                    enabled = canPunch
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Fingerprint, contentDescription = "In")
                        Text("Daftar Masuk", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }

                // Punch Out Button
                Button(
                    onClick = { viewModel.punchOutActiveWorker() },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("punch_out_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Orange500,
                        contentColor = Color.White,
                        disabledContainerColor = Slate200,
                        disabledContentColor = Slate400
                    ),
                    shape = RoundedCornerShape(12.dp),
                    enabled = canPunch
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Out")
                        Text("Daftar Keluar", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }

            // Summary Work Statistics (Today Total & Weekly)
            val todayLog = remember(allLogs, worker.id, todayStr) {
                allLogs.find { it.workerId == worker.id && it.date == todayStr }
            }
            val todayHours = todayLog?.let { it.durationMinutes / 60.0 } ?: 0.0

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, CardBorderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "STATISTIK JAM BEKERJA",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = Orange500,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Today statistic
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .background(Blue50, RoundedCornerShape(12.dp))
                                .border(1.dp, Blue100, RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Text("Jam Belajar/Kerja Hari Ini", fontSize = 10.sp, color = Slate500, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = String.format(Locale.getDefault(), "%.1f Jam", todayHours),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = Blue700
                            )
                        }

                        // Weekly Total hours
                        val weeklyHoursMap = viewModel.getWeeklyHours(worker.id, allLogs)
                        val weeklyTotal = weeklyHoursMap.values.sum()

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .background(Orange5.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .border(1.dp, Orange50.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Text("Jumlah Jam Seminggu", fontSize = 10.sp, color = Slate500, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = String.format(Locale.getDefault(), "%.1f Jam", weeklyTotal),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = Orange600
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Divider(color = Slate100)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "PRESTASI MINIMUM SEMINGGU (ISNIN HINGGA SABTU)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate400,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Mon-Sat Schedule listing
                    val daysList = listOf("Isnin", "Selasa", "Rabu", "Khamis", "Jumaat", "Sabtu")
                    val weeklyHoursMap = viewModel.getWeeklyHours(worker.id, allLogs)

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        daysList.forEach { dayName ->
                            val hoursVal = weeklyHoursMap[dayName] ?: 0.0
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = null,
                                        tint = Slate400,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = dayName, fontSize = 12.sp, color = Slate750, fontWeight = FontWeight.Medium)
                                }
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (hoursVal > 0) Color(0xFFF0FDF4) else Slate50,
                                            RoundedCornerShape(6.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = String.format(Locale.getDefault(), "%.1f j", hoursVal),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (hoursVal > 0) Color(0xFF15803D) else Slate400
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 6. Admin Only Reminders Command Section
        if (loggedInUser?.isAdmin == true) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, CardBorderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = Orange500)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "NOTIFIKASI PERINGATAN (ADMIN)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = Slate800,
                            letterSpacing = 0.5.sp
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Text(
                        text = "Hantar peringatan serta merta kepada kakitangan yang masih belum mendaftar masuk (punch-in) sebelum jam 8.00 pagi hari ini.",
                        fontSize = 11.sp,
                        color = Slate600,
                        lineHeight = 15.sp,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { viewModel.triggerInstantCheckInReminders() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("send_reminder_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Blue600,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Hantar Peringatan", modifier = Modifier.size(16.dp))
                            Text("Hantar Peringatan Kehadiran", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

val Slate750 = Color(0xFF2E394A)
val Orange5 = Color(0xFFFFECE0)
