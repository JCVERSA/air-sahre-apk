package com.example.ui.views

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.DocumentationDialog
import com.example.ui.components.HistoryDialog
import com.example.ui.theme.Cyan400
import com.example.ui.theme.Cyan900
import com.example.ui.theme.Emerald400
import com.example.ui.theme.Purple400
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import com.example.util.HistoryRepository

enum class AirMode {
    SENDER,
    RECEIVER,
    LOOPBACK
}

@Composable
fun MainScreen() {
    val context = LocalContext.current
    val historyRepo = remember { HistoryRepository(context) }

    var currentMode by remember { mutableStateOf(AirMode.SENDER) }
    var showDocsDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var historyItems by remember { mutableStateOf(historyRepo.getHistory()) }

    Scaffold(
        containerColor = Slate950,
        topBar = {
            Surface(
                color = Slate950,
                border = BorderStroke(1.dp, Slate800),
                modifier = Modifier.fillMaxWidth().statusBarsPadding()
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Cyan900)
                                    .border(1.dp, Cyan400, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WifiOff,
                                    contentDescription = "AirQR",
                                    tint = Cyan400,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "AirQR",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Slate100,
                                        fontSize = 18.sp
                                    )
                                )
                                Text(
                                    text = "Visual Optical Air-Gapped Transfer",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 11.sp,
                                        color = Cyan400
                                    )
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(onClick = {
                                historyItems = historyRepo.getHistory()
                                showHistoryDialog = true
                            }) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = "History",
                                    tint = Slate400
                                )
                            }
                            IconButton(onClick = { showDocsDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.HelpOutline,
                                    contentDescription = "Documentation",
                                    tint = Cyan400
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Mode Switcher Tabs
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Slate900, RoundedCornerShape(10.dp))
                            .border(1.dp, Slate800, RoundedCornerShape(10.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ModeTabButton(
                            title = "Transmitter",
                            icon = Icons.Default.Upload,
                            isSelected = currentMode == AirMode.SENDER,
                            activeColor = Cyan400,
                            modifier = Modifier.weight(1f),
                            onClick = { currentMode = AirMode.SENDER }
                        )
                        ModeTabButton(
                            title = "Receiver",
                            icon = Icons.Default.QrCodeScanner,
                            isSelected = currentMode == AirMode.RECEIVER,
                            activeColor = Emerald400,
                            modifier = Modifier.weight(1f),
                            onClick = { currentMode = AirMode.RECEIVER }
                        )
                        ModeTabButton(
                            title = "Loopback",
                            icon = Icons.Default.Sync,
                            isSelected = currentMode == AirMode.LOOPBACK,
                            activeColor = Purple400,
                            modifier = Modifier.weight(1f),
                            onClick = { currentMode = AirMode.LOOPBACK }
                        )
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                color = Slate950,
                border = BorderStroke(1.dp, Slate800),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.WifiOff, contentDescription = null, tint = Cyan400, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "100% Offline Optical P2P",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = Slate400
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = Emerald400, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "SHA-256 Verified",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = Slate400
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Slate950)
        ) {
            when (currentMode) {
                AirMode.SENDER -> SenderScreen(historyRepo = historyRepo)
                AirMode.RECEIVER -> ReceiverScreen(historyRepo = historyRepo)
                AirMode.LOOPBACK -> LoopbackScreen(historyRepo = historyRepo)
            }
        }
    }

    if (showDocsDialog) {
        DocumentationDialog(onDismiss = { showDocsDialog = false })
    }

    if (showHistoryDialog) {
        HistoryDialog(
            historyItems = historyItems,
            onClearHistory = {
                historyRepo.clearHistory()
                historyItems = emptyList()
            },
            onDismiss = { showHistoryDialog = false }
        )
    }
}

@Composable
private fun ModeTabButton(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    activeColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) Slate800 else androidx.compose.ui.graphics.Color.Transparent,
        border = if (isSelected) BorderStroke(1.dp, activeColor.copy(alpha = 0.5f)) else null,
        modifier = modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) activeColor else Slate400,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 12.sp,
                    color = if (isSelected) Slate100 else Slate400
                )
            )
        }
    }
}
