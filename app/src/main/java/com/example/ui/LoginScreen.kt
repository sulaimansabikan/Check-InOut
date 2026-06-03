package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val isConnected by viewModel.isConnectedToInternet.collectAsStateWithLifecycle()
    val isBypassEnabled by viewModel.isDeveloperBypass.collectAsStateWithLifecycle()
    val simulatedSsid by viewModel.simulatedSsid.collectAsStateWithLifecycle()
    val detectedSsid by viewModel.detectedSsid.collectAsStateWithLifecycle()
    val designatedSsid by viewModel.designatedSsid.collectAsStateWithLifecycle()
    val companyName by viewModel.companyName.collectAsStateWithLifecycle()
    val companyAddress by viewModel.companyAddress.collectAsStateWithLifecycle()
    val companyLogoIcon by viewModel.companyLogoIcon.collectAsStateWithLifecycle()

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val scrollState = rememberScrollState()

    val currentSsid = if (isBypassEnabled == "true") simulatedSsid else detectedSsid
    val canLogin = isConnected && (currentSsid == designatedSsid)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppBackground)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .verticalScroll(scrollState)
                .widthIn(max = 480.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            
            // 1. COMPANY LOGO (Featuring Blue, White, and Orange colors corporate design)
            val logoVector = when (companyLogoIcon) {
                "home" -> Icons.Filled.Home
                "store" -> Icons.Filled.Store
                "build" -> Icons.Filled.Build
                else -> Icons.Filled.Business
            }
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(Blue50)
                    .border(3.dp, Orange500, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(Blue600),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = logoVector,
                        contentDescription = "Syarikat Logo Icon",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                        )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. COMPANY NAME & ADDRESS
            Text(
                text = companyName,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = Blue700,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("company_name_label")
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = companyAddress,
                fontSize = 11.sp,
                color = Slate500,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp,
                modifier = Modifier.testTag("company_address_label")
            )

            Spacer(modifier = Modifier.height(24.dp))

            // WiFi constraint status banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (canLogin) Color(0xFFF0FDF4) else Color(0xFFFEF2F2)
                ),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (canLogin) Icons.Filled.Wifi else Icons.Filled.WifiOff,
                        contentDescription = "Rangkaian Status",
                        tint = if (canLogin) Color(0xFF16A34A) else Color(0xFFDC2626),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (canLogin) "WiFi Rangkaian Sah" else "WiFi Luar Jangkauan",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (canLogin) Color(0xFF15803D) else Color(0xFF991B1B)
                        )
                        Text(
                            text = if (canLogin) "Sambungan: $currentSsid (Sedia untuk Punch Card)" else "Log masuk dibenarkan, tetapi sila sambung ke WiFi: $designatedSsid untuk boleh punch card.",
                            fontSize = 11.sp,
                            color = if (canLogin) Color(0xFF166534) else Color(0xFF7F1D1D)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 3. LOGIN FORM
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "SILA LOG MASUK",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate800,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = username,
                        onValueChange = { 
                            username = it
                            errorMessage = null
                        },
                        label = { Text("Nama Pengguna (Username)") },
                        leadingIcon = { Icon(Icons.Filled.Person, "Username icon", tint = Slate400) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_username_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Slate900,
                            unfocusedTextColor = Slate900,
                            focusedBorderColor = Blue600,
                            unfocusedBorderColor = Slate200,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            cursorColor = Blue600,
                            focusedLabelColor = Blue600,
                            unfocusedLabelColor = Slate500
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { 
                            password = it
                            errorMessage = null
                        },
                        label = { Text("Kata Laluan (Password)") },
                        leadingIcon = { Icon(Icons.Filled.Lock, "Password icon", tint = Slate400) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_password_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Slate900,
                            unfocusedTextColor = Slate900,
                            focusedBorderColor = Blue600,
                            unfocusedBorderColor = Slate200,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            cursorColor = Blue600,
                            focusedLabelColor = Blue600,
                            unfocusedLabelColor = Slate500
                        )
                    )

                    errorMessage?.let { msg ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = msg,
                            color = Color(0xFFDC2626),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            val err = viewModel.loginUser(username.trim(), password)
                            if (err != null) {
                                errorMessage = err
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("login_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Blue600,
                            contentColor = Color.White
                        )
                    ) {
                        Text("Log Masuk Kerja", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 4. TESTER/SIMULATOR SUB-CARD (If bypass is turned on for evaluation)
            if (isBypassEnabled == "true") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Orange50),
                    border = BorderStroke(1.dp, Color(0xFFFEE2E2))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Info,
                                contentDescription = "Simulasi Wifi",
                                tint = Orange600,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Panel Simulasi Wi-Fi (Untuk Penguji)",
                                color = Orange600,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = { viewModel.updateSimulatedSsid(designatedSsid) },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Blue500
                                )
                            ) {
                                Text("Sambung WiFi Rumah", fontSize = 10.sp, color = Color.White)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { viewModel.updateSimulatedSsid("Rangkaian_Luar_WiFi") },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.LightGray
                                )
                            ) {
                                Text("Sambung WiFi Luar", fontSize = 10.sp, color = Color.Black)
                            }
                        }
                    }
                }
            }
        }
    }
}
