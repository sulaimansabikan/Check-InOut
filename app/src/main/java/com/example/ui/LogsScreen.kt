package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.PunchLog
import com.example.service.ServerTimeService
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val workers by viewModel.workers.collectAsStateWithLifecycle()
    val allLogs by viewModel.allLogs.collectAsStateWithLifecycle()
    
    var filterWorkerId by remember { mutableStateOf<Int?>(null) }
    var isAddManualOpen by remember { mutableStateOf(false) }

    // Manual Log Input Form States
    var selectedWorkerForManual by remember { mutableStateOf(1) }
    
    val todayDateStr = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }
    var manualDate by remember { mutableStateOf(todayDateStr) }
    var manualInTime by remember { mutableStateOf("07:55") }
    var manualOutTime by remember { mutableStateOf("17:05") }

    val loggedInUser by viewModel.loggedInUser.collectAsStateWithLifecycle()

    val keyboardController = LocalSoftwareKeyboardController.current

    // Combined filtered logs - Workers can ONLY see their own logs!
    val filteredLogs = remember(allLogs, filterWorkerId, loggedInUser) {
        val visibleLogs = if (loggedInUser?.isAdmin == true) {
            allLogs
        } else {
            allLogs.filter { it.workerId == loggedInUser?.id }
        }

        if (filterWorkerId == null || loggedInUser?.isAdmin == false) {
            visibleLogs
        } else {
            visibleLogs.filter { it.workerId == filterWorkerId }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Log screen header
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column {
                Text(
                    text = if (loggedInUser?.isAdmin == true) "REKOD SEJARAH (SEMUA)" else "REKOD SEJARAH ANDA",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Slate500,
                    letterSpacing = 1.2.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Log Kehadiran",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate900,
                    letterSpacing = (-0.5).sp
                )
            }

            // Button to trigger manual log insertion panel - ADMIN ONLY
            if (loggedInUser?.isAdmin == true) {
                Button(
                    onClick = { isAddManualOpen = !isAddManualOpen },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isAddManualOpen) Slate500 else Blue600
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("toggle_manual_log_form"),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = if (isAddManualOpen) Icons.Default.Close else Icons.Default.Add,
                        contentDescription = "Form manual",
                        modifier = Modifier.size(15.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isAddManualOpen) "Tutup Form" else "Tambah Rekod", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        // --- SUB SECTION: COLLAPSIBLE MANUAL PUNCH ADD FORM ---
        AnimatedVisibility(
            visible = isAddManualOpen,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, CardBorderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "TAMBAH REKOD MANUAL (OVERRIDE)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate800,
                        letterSpacing = 0.5.sp
                    )

                    // Worker selection dropdown row for manual log
                    Column {
                        Text("Pilih Pekerja:", fontSize = 10.sp, color = Slate400, fontWeight = FontWeight.Bold)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(vertical = 6.dp)
                        ) {
                            workers.forEach { worker ->
                                val isChosen = worker.id == selectedWorkerForManual
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (isChosen) Blue50 else Slate50,
                                            RoundedCornerShape(10.dp)
                                        )
                                        .border(
                                            1.dp,
                                            if (isChosen) Blue600.copy(alpha = 0.3f) else Slate200,
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable { selectedWorkerForManual = worker.id }
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = worker.name,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isChosen) Blue600 else Slate700
                                    )
                                }
                            }
                        }
                    }

                    // Input Fields: Date, In Time, Out Time
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = manualDate,
                            onValueChange = { manualDate = it },
                            label = { Text("Tarikh (YYYY-MM-DD)", fontSize = 10.sp) },
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
                                .weight(1.3f)
                                .testTag("manual_date_input"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next)
                        )

                        OutlinedTextField(
                            value = manualInTime,
                            onValueChange = { manualInTime = it },
                            label = { Text("In (HH:MM)", fontSize = 10.sp) },
                            placeholder = { Text("08:00") },
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
                                .weight(1f)
                                .testTag("manual_in_input"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next)
                        )

                        OutlinedTextField(
                            value = manualOutTime,
                            onValueChange = { manualOutTime = it },
                            label = { Text("Out (HH:MM)", fontSize = 10.sp) },
                            placeholder = { Text("17:00") },
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
                                .weight(1f)
                                .testTag("manual_out_input"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() })
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "*Nota: Waktu lewat (>08:00 AM) dan pulang awal (<17:00 PM) akan dikira secara automatik berdasarkan tarikh yang disimpan.",
                            fontSize = 9.sp,
                            color = Slate400,
                            modifier = Modifier.weight(1f),
                            lineHeight = 12.sp
                        )

                        Button(
                            onClick = {
                                viewModel.addManualWorkerLog(
                                    selectedWorkerForManual,
                                    manualDate,
                                    manualInTime,
                                    manualOutTime
                                )
                                keyboardController?.hide()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Slate900),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("save_manual_log_button"),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = "Simpan", modifier = Modifier.size(15.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Simpan", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }

        // --- FILTER CHIPS SECTION ---
        if (loggedInUser?.isAdmin == true) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.FilterList, contentDescription = "Tapis", tint = Slate400, modifier = Modifier.size(18.dp))
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = filterWorkerId == null,
                            onClick = { filterWorkerId = null },
                            label = { 
                                Text(
                                    "Semua Pekerja",
                                    fontWeight = if (filterWorkerId == null) FontWeight.Bold else FontWeight.Medium,
                                    color = if (filterWorkerId == null) Blue600 else Slate700
                                ) 
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Blue50,
                                containerColor = Color.Transparent
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = filterWorkerId == null,
                                borderColor = Slate200,
                                selectedBorderColor = Blue600.copy(alpha = 0.3f),
                                borderWidth = 1.dp,
                                selectedBorderWidth = 1.dp
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("filter_all_chip")
                        )
                    }

                    items(workers) { worker ->
                        val isSelected = filterWorkerId == worker.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { filterWorkerId = worker.id },
                            label = { 
                                Text(
                                    worker.name,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Blue600 else Slate700
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
                            modifier = Modifier.testTag("filter_worker_chip_${worker.id}")
                        )
                    }
                }
            }
        }

        // --- DYNAMIC DATA LISTING OR EMPTY STATE PANEL ---
        if (filteredLogs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.White, RoundedCornerShape(24.dp))
                    .border(1.dp, CardBorderColor, RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Inbox,
                        contentDescription = "Empty lists",
                        tint = Slate400,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Tiada Rekod Kehadiran",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Slate800
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Mulakan punch atau tekan butang 'Tambah Rekod' di atas untuk melompatkan entri manual bagi menguji pelaporan.",
                        textAlign = TextAlign.Center,
                        fontSize = 11.sp,
                        color = Slate500,
                        lineHeight = 15.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredLogs) { log ->
                    LogItemRow(log = log)
                }
            }
        }
    }
}

@Composable
fun LogItemRow(log: PunchLog) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("log_row_${log.id}"),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, CardBorderColor),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Worker name & Log date Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "User logo",
                        tint = Slate500,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = log.workerName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Slate800
                    )
                }

                Text(
                    text = log.date,
                    fontSize = 11.sp,
                    color = Slate400,
                    fontWeight = FontWeight.Bold
                )
            }

            HorizontalDivider(color = CardBorderColor)

            // Timings breakdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Punch in time
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Masuk (Punch In)", fontSize = 9.sp, color = Slate400, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = "In Arrow",
                            tint = Emerald600,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = log.punchInTime?.let {
                                ServerTimeService.formatToMalaysiaTime(it, "hh:mm a")
                            } ?: " TIADA",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (log.punchInTime != null) Slate800 else Slate400
                        )
                    }
                }

                // Punch out time
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Keluar (Punch Out)", fontSize = 9.sp, color = Slate400, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = "Out Arrow",
                            tint = Color(0xFFD35400),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = log.punchOutTime?.let {
                                ServerTimeService.formatToMalaysiaTime(it, "hh:mm a")
                            } ?: " TIADA",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (log.punchOutTime != null) Slate800 else Slate400
                        )
                    }
                }

                // Total hours computed
                val totalHours = log.durationMinutes / 60.0
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(text = "Jumlah Tempoh", fontSize = 9.sp, color = Slate400, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (log.punchInTime != null && log.punchOutTime != null) {
                            String.format("%.1f Jam", totalHours)
                        } else {
                            "-"
                        },
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        color = Blue600
                    )
                }
            }

            // Warnings row & Wifi connection
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Wifi connection name used
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Wifi,
                        contentDescription = "Wifi icon",
                        tint = Slate400,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = log.wifiSsid?.let { "WiFi: $it" } ?: "WiFi: Tiada sambungan",
                        fontSize = 10.sp,
                        color = Slate400
                    )
                }

                // Warn Badges for Lewat (Late) and Pulang Awal (Early Out)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (log.isLate) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFFEE2E2), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                "Lewat Masuk",
                                color = Color(0xFF991B1B),
                                fontWeight = FontWeight.Bold,
                                fontSize = 8.sp
                            )
                        }
                    }

                    if (log.isEarlyOut) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFFEF3C7), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                "Pulang Awal",
                                color = Color(0xFF92400E),
                                fontWeight = FontWeight.Bold,
                                fontSize = 8.sp
                            )
                        }
                    }
                    
                    if (log.punchInTime != null && log.punchOutTime == null) {
                        Box(
                            modifier = Modifier
                                .background(Emerald100, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                "Aktif (Bekerja)",
                                color = Emerald700,
                                fontWeight = FontWeight.Bold,
                                fontSize = 8.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
