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
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Palette
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
import androidx.compose.runtime.LaunchedEffect
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
import com.example.ui.components.AppToast
import com.example.ui.components.CalibrationDialog
import com.example.ui.components.DocumentationDialog
import com.example.ui.components.HistoryDialog
import com.example.ui.components.ThemeSelectorDialog
import com.example.ui.components.ToastHost
import com.example.ui.components.ToastType
import com.example.ui.theme.AirThemePalette
import com.example.ui.theme.Cyan400
import com.example.ui.theme.Emerald400
import com.example.ui.theme.Purple400
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import com.example.util.AndroidBatteryInfo
import com.example.util.BatteryUtil
import com.example.util.HistoryRepository
import kotlinx.coroutines.delay

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
    var currentPalette by remember { mutableStateOf(AirThemePalette.CYBER_SLATE) }

    var showDocsDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showCalibrationDialog by remember { mutableStateOf(false) }

    var historyItems by remember { mutableStateOf(historyRepo.getHistory()) }
    var currentToast by remember { mutableStateOf<AppToast?>(null) }

    var batteryInfo by remember { mutableStateOf(BatteryUtil.getBatteryInfo(context)) }
    var batterySaver by remember { mutableStateOf(false) }

    // Periodic battery level check
    LaunchedEffect(Unit) {
        while (true) {
            val info = BatteryUtil.getBatteryInfo(context)
            batteryInfo = info
            if (info.isLow && !batterySaver) {
                batterySaver = true
                currentToast = AppToast(
                    type = ToastType.INFO,
                    title = "Battery Saver Activated",
                    message = "Battery low (${info.levelPercent}%). Display brightness reduced & refresh rate capped to 6 FPS."
                )
            }
            delay(10000L)
        }
    }

    fun toggleBatterySaver() {
        val next = !batterySaver
        batterySaver = next
        currentToast = AppToast(
            type = ToastType.INFO,
            title = if (next) "Battery Saver Active" else "Battery Saver Off",
            message = if (next) "Refresh rate capped at 6 FPS and display dimmed to save battery." else "Restored standard refresh rate and brightness."
        )
    }

    Scaffold(
        containerColor = currentPalette.background,
        topBar = {
            Surface(
                color = currentPalette.surface,
                border = BorderStroke(1.dp, currentPalette.border),
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
                                    .background(currentPalette.surface)
                                    .border(1.dp, currentPalette.primaryAccent, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WifiOff,
                                    contentDescription = "AirQR",
                                    tint = currentPalette.primaryAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "AirQR",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = currentPalette.textPrimary,
                                        fontSize = 18.sp
                                    )
                                )
                                Text(
                                    text = "Visual Optical Air-Gapped Transfer",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 11.sp,
                                        color = currentPalette.primaryAccent
                                    )
                                )
                            }
                        }

                        // Top Right Actions
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Battery Badge
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = currentPalette.surface,
                                border = BorderStroke(1.dp, if (batterySaver) Emerald400 else currentPalette.border),
                                modifier = Modifier.clickable { toggleBatterySaver() }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (batteryInfo.isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryFull,
                                        contentDescription = null,
                                        tint = if (batterySaver) Emerald400 else currentPalette.primaryAccent,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${batteryInfo.levelPercent}%",
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        color = if (batterySaver) Emerald400 else currentPalette.textSecondary
                                    )
                                }
                            }

                            // Calibration Button
                            IconButton(onClick = { showCalibrationDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.Assessment,
                                    contentDescription = "Auto Calibrate",
                                    tint = currentPalette.primaryAccent
                                )
                            }

                            // Theme Selector
                            IconButton(onClick = { showThemeDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = "Themes",
                                    tint = currentPalette.textSecondary
                                )
                            }

                            IconButton(onClick = {
                                historyItems = historyRepo.getHistory()
                                showHistoryDialog = true
                            }) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = "History",
                                    tint = currentPalette.textSecondary
                                )
                            }

                            IconButton(onClick = { showDocsDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.HelpOutline,
                                    contentDescription = "Documentation",
                                    tint = currentPalette.primaryAccent
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Mode Switcher Tabs
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(currentPalette.surface, RoundedCornerShape(10.dp))
                            .border(1.dp, currentPalette.border, RoundedCornerShape(10.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ModeTabButton(
                            title = "Transmitter",
                            icon = Icons.Default.Upload,
                            isSelected = currentMode == AirMode.SENDER,
                            activeColor = currentPalette.primaryAccent,
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
                color = currentPalette.surface,
                border = BorderStroke(1.dp, currentPalette.border),
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
                        Icon(imageVector = Icons.Default.WifiOff, contentDescription = null, tint = currentPalette.primaryAccent, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "100% Offline Optical P2P",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = currentPalette.textSecondary
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Palette, contentDescription = null, tint = currentPalette.primaryAccent, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = currentPalette.title,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = currentPalette.textSecondary
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = Emerald400, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "SHA-256 Verified",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = currentPalette.textSecondary
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
                .background(currentPalette.background)
        ) {
            when (currentMode) {
                AirMode.SENDER -> SenderScreen(
                    historyRepo = historyRepo,
                    batterySaver = batterySaver,
                    onToggleBatterySaver = { toggleBatterySaver() },
                    onNotify = { currentToast = it }
                )
                AirMode.RECEIVER -> ReceiverScreen(
                    historyRepo = historyRepo,
                    onNotify = { currentToast = it }
                )
                AirMode.LOOPBACK -> LoopbackScreen(
                    historyRepo = historyRepo
                )
            }

            // Toast overlay
            ToastHost(
                toast = currentToast,
                onDismiss = { currentToast = null }
            )
        }
    }

    if (showThemeDialog) {
        ThemeSelectorDialog(
            currentPalette = currentPalette,
            onSelectPalette = {
                currentPalette = it
                currentToast = AppToast(
                    type = ToastType.INFO,
                    title = "Theme Updated",
                    message = "Switched to ${it.title} high-contrast optical palette."
                )
            },
            onDismiss = { showThemeDialog = false }
        )
    }

    if (showCalibrationDialog) {
        CalibrationDialog(
            onApplyProfile = { fps, chunk, ecc ->
                currentToast = AppToast(
                    type = ToastType.SUCCESS,
                    title = "Calibration Profile Applied",
                    message = "Optimized settings: $fps FPS, $chunk bytes/chunk, Level $ecc error correction."
                )
            },
            onDismiss = { showCalibrationDialog = false }
        )
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
