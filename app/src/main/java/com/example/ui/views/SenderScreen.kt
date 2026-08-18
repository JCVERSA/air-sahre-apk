package com.example.ui.views

import android.graphics.Bitmap
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Gauge
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ChunkPayload
import com.example.model.LogEntry
import com.example.model.SenderConfig
import com.example.model.SessionHistoryItem
import com.example.model.TransferMeta
import com.example.ui.components.AppToast
import com.example.ui.components.ChunkMap
import com.example.ui.components.EventLogList
import com.example.ui.components.ToastType
import com.example.ui.theme.Cyan400
import com.example.ui.theme.Cyan900
import com.example.ui.theme.Emerald400
import com.example.ui.theme.Purple400
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import com.example.util.AirProtocol
import com.example.util.Base45
import com.example.util.CryptoUtil
import com.example.util.GzipUtil
import com.example.util.HistoryRepository
import com.example.util.QrCodeUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

data class FilePreviewInfo(
    val name: String,
    val size: Long,
    val type: String,
    val sha256: String,
    val compressedSize: Long,
    val wasCompressed: Boolean,
    val savingsPercent: Int,
    val estChunks: Int,
    val estDurationSec: Int,
    val rawBytes: ByteArray
)

val FPS_PRESETS = listOf(
    Pair("6 FPS", "High Reliability" to 6),
    Pair("12 FPS", "Balanced" to 12),
    Pair("16 FPS", "High Speed" to 16),
    Pair("24 FPS", "Turbo" to 24)
)

@Composable
fun SenderScreen(
    historyRepo: HistoryRepository,
    batterySaver: Boolean = false,
    onToggleBatterySaver: (() -> Unit)? = null,
    onNotify: (AppToast) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var config by remember { mutableStateOf(SenderConfig()) }
    var meta by remember { mutableStateOf<TransferMeta?>(null) }
    var pendingPreview by remember { mutableStateOf<FilePreviewInfo?>(null) }
    var qrBitmaps by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var packets by remember { mutableStateOf<List<String>>(emptyList()) }
    var currentFrameIndex by remember { mutableIntStateOf(0) }
    var isPlaying by remember { mutableStateOf(true) }
    var isProcessing by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    val logs = remember { mutableStateListOf<LogEntry>() }

    val effectiveFps = if (batterySaver) minOf(config.fps, 6) else config.fps

    fun addLog(level: String, msg: String) {
        logs.add(0, LogEntry(id = UUID.randomUUID().toString(), level = level, message = msg))
        if (logs.size > 50) logs.removeLast()
    }

    fun prepareStream(preview: FilePreviewInfo) {
        isProcessing = true
        scope.launch(Dispatchers.Default) {
            val (dataToChunk, wasCompressed) = if (config.useCompression) {
                GzipUtil.compress(preview.rawBytes)
            } else {
                Pair(preview.rawBytes, false)
            }

            val savings = if (wasCompressed) {
                (((preview.size - dataToChunk.size).toDouble() / preview.size.toDouble()) * 100).toInt()
            } else 0

            if (wasCompressed) {
                addLog("info", "GZIP Compressed: ${CryptoUtil.formatBytes(preview.size)} -> ${CryptoUtil.formatBytes(dataToChunk.size.toLong())} (-$savings% savings)")
            } else {
                addLog("info", "Encoding payload: ${preview.name} (${CryptoUtil.formatBytes(preview.size)})")
            }

            val chunkSize = config.chunkSize
            val rawChunks = mutableListOf<ByteArray>()
            var offset = 0
            while (offset < dataToChunk.size) {
                val end = minOf(offset + chunkSize, dataToChunk.size)
                rawChunks.add(dataToChunk.copyOfRange(offset, end))
                offset = end
            }

            val transferId = CryptoUtil.generateTransferId()
            val transferMeta = TransferMeta(
                id = transferId,
                name = preview.name,
                size = preview.size,
                type = preview.type,
                totalChunks = rawChunks.size,
                chunkSize = chunkSize,
                hash = preview.sha256,
                compressed = wasCompressed,
                compressedSize = dataToChunk.size.toLong(),
                encoding = config.encodingMode
            )

            val packetList = mutableListOf<String>()
            val metaStr = AirProtocol.encodeMetaPacket(transferMeta)
            packetList.add(metaStr)

            for (i in rawChunks.indices) {
                val chunkData = if (config.encodingMode == "base45") {
                    Base45.encode(rawChunks[i])
                } else {
                    CryptoUtil.toBase64(rawChunks[i])
                }

                val chunkPayload = ChunkPayload(
                    id = transferId,
                    index = i,
                    total = rawChunks.size,
                    data = chunkData,
                    encoding = config.encodingMode
                )

                if (i % config.metaFrequency == 0) {
                    packetList.add(metaStr)
                }
                packetList.add(AirProtocol.encodeChunkPacket(chunkPayload))
            }
            packetList.add(metaStr)

            val bitmaps = packetList.map { pkt ->
                QrCodeUtil.generateQrBitmap(
                    content = pkt,
                    size = config.qrSize,
                    eccLevel = config.errorCorrection,
                    invert = config.invertColor
                )
            }

            withContext(Dispatchers.Main) {
                meta = transferMeta
                packets = packetList
                qrBitmaps = bitmaps
                currentFrameIndex = 0
                isPlaying = true
                isProcessing = false
                addLog("success", "Stream active: ${rawChunks.size} chunks (${packetList.size} frames)")

                onNotify(
                    AppToast(
                        type = ToastType.SUCCESS,
                        title = "Optical Stream Active",
                        message = "Transmitting \"${preview.name}\" (${rawChunks.size} chunks at $effectiveFps FPS${if (wasCompressed) ", -$savings% compressed" else ""})."
                    )
                )

                val duration = (rawChunks.size.toDouble() / effectiveFps.toDouble()).coerceAtLeast(0.1)
                val avgSpeed = (preview.size / 1024.0) / duration

                historyRepo.addItem(
                    SessionHistoryItem(
                        id = UUID.randomUUID().toString(),
                        transferId = transferId,
                        fileName = preview.name,
                        fileSize = preview.size,
                        fileType = preview.type,
                        role = "sent",
                        timestamp = System.currentTimeMillis(),
                        hash = preview.sha256,
                        totalChunks = rawChunks.size,
                        durationSeconds = duration,
                        averageSpeedKb = avgSpeed,
                        status = "success"
                    )
                )
            }
        }
    }

    fun inspectFile(name: String, size: Long, type: String, bytes: ByteArray, autoStart: Boolean = false) {
        val sha256 = CryptoUtil.computeSHA256(bytes)
        val (compressed, wasCompressed) = if (config.useCompression) GzipUtil.compress(bytes) else Pair(bytes, false)
        val effectiveSize = if (wasCompressed) compressed.size.toLong() else size
        val savings = if (wasCompressed) (((size - compressed.size).toDouble() / size.toDouble()) * 100).toInt() else 0
        val estChunks = (effectiveSize / config.chunkSize).toInt().coerceAtLeast(1)
        val estDuration = (estChunks / effectiveFps).coerceAtLeast(1)

        val preview = FilePreviewInfo(
            name = name,
            size = size,
            type = type,
            sha256 = sha256,
            compressedSize = effectiveSize,
            wasCompressed = wasCompressed,
            savingsPercent = savings,
            estChunks = estChunks,
            estDurationSec = estDuration,
            rawBytes = bytes
        )
        pendingPreview = preview

        if (autoStart) {
            prepareStream(preview)
        } else {
            onNotify(
                AppToast(
                    type = ToastType.INFO,
                    title = "File Metadata & Compression Ready",
                    message = "Preview details for \"$name\" before initiating optical stream."
                )
            )
        }
    }

    // Default sample file initialization
    LaunchedEffect(Unit) {
        if (packets.isEmpty()) {
            val sampleText = "AirQR Optical Protocol V2 Payload\n=================================\nThis is an air-gapped file transfer beamed across screens via high-density Base45 optical QR codes.\nLightweight GZIP/Deflate Compression + RFC 9285 Base45 + SHA-256 Checksum.\n100% offline visual communication.\n" +
                    "Repeating text for compression: \n" +
                    "Secure air-gapped transfer without Wi-Fi or Bluetooth. ".repeat(8)
            val bytes = sampleText.toByteArray(Charsets.UTF_8)
            inspectFile("airqr_memo_report.txt", bytes.size.toLong(), "text/plain", bytes, autoStart = true)
        }
    }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch(Dispatchers.IO) {
                var fileName = "air_file.bin"
                var fileSize = 0L
                val mimeType = context.contentResolver.getType(it) ?: "application/octet-stream"

                context.contentResolver.query(it, null, null, null, null)?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        if (nameIdx >= 0) fileName = cursor.getString(nameIdx)
                        if (sizeIdx >= 0) fileSize = cursor.getLong(sizeIdx)
                    }
                }

                val bytes = context.contentResolver.openInputStream(it)?.use { input ->
                    input.readBytes()
                } ?: ByteArray(0)

                withContext(Dispatchers.Main) {
                    inspectFile(fileName, if (fileSize > 0) fileSize else bytes.size.toLong(), mimeType, bytes, false)
                }
            }
        }
    }

    // Playback loop with effectiveFps
    LaunchedEffect(isPlaying, qrBitmaps.size, effectiveFps) {
        if (isPlaying && qrBitmaps.isNotEmpty()) {
            val delayMs = (1000L / effectiveFps.coerceIn(1, 30))
            while (isActive && isPlaying) {
                delay(delayMs)
                currentFrameIndex = (currentFrameIndex + 1) % qrBitmaps.size
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Radio,
                        contentDescription = null,
                        tint = Cyan400,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Optical Transmitter",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Slate100
                        )
                    )
                }
                Text(
                    text = "Beam animated Base45 QR codes to receiver",
                    style = MaterialTheme.typography.bodySmall.copy(color = Slate400)
                )
            }

            Button(
                onClick = { filePicker.launch("*/*") },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Cyan400,
                    contentColor = Slate950
                )
            ) {
                Icon(imageVector = Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Select File", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Battery Saver Active Indicator Banner
        if (batterySaver) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Emerald400.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, Emerald400.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Eco, contentDescription = null, tint = Emerald400, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Battery Saver: Capped at $effectiveFps FPS & Display Dimmed",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, color = Emerald400)
                        )
                    }
                    if (onToggleBatterySaver != null) {
                        Text(
                            text = "Turn Off",
                            color = Slate300,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.clickable { onToggleBatterySaver() }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // QR Code Display Stage (with battery saver brightness reduction)
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (batterySaver) Color(0xFFD4D4D8) else Color.White,
            border = BorderStroke(3.dp, if (batterySaver) Emerald400 else Cyan400),
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .aspectRatio(1f)
                .alpha(if (batterySaver) 0.88f else 1.0f)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(color = Cyan400)
                } else if (qrBitmaps.isNotEmpty() && currentFrameIndex < qrBitmaps.size) {
                    Image(
                        bitmap = qrBitmaps[currentFrameIndex].asImageBitmap(),
                        contentDescription = "AirQR Optical Stream Frame",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                    )

                    // Overlay indicator
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(Slate950.copy(alpha = 0.85f))
                            .padding(vertical = 4.dp, horizontal = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val isMeta = packets.getOrNull(currentFrameIndex)?.startsWith("AIR2:M") == true
                            Text(
                                text = if (isMeta) "META PACKET" else "FRAME #${currentFrameIndex + 1}/${packets.size}",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = if (isMeta) Purple400 else Cyan400
                            )
                            Text(
                                text = "$effectiveFps FPS ${if (batterySaver) "(SAVER)" else ""} • BASE45",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = Slate400
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Speed (FPS) Presets Selector
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Slate900,
            border = BorderStroke(1.dp, Slate800),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Gauge, contentDescription = null, tint = Cyan400, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Stream Speed (FPS)",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, color = Slate300)
                        )
                    }
                    Text(
                        text = "$effectiveFps FPS",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Cyan400
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FPS_PRESETS.forEach { (label, pair) ->
                        val (desc, targetFps) = pair
                        val isSelected = config.fps == targetFps && !batterySaver
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) Cyan400 else Slate950,
                            border = BorderStroke(1.dp, if (isSelected) Cyan400 else Slate800),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    config = config.copy(fps = targetFps)
                                    onNotify(
                                        AppToast(
                                            type = ToastType.INFO,
                                            title = "Speed Adjusted",
                                            message = "Transmitter set to $label ($desc)."
                                        )
                                    )
                                }
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = label,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = if (isSelected) Slate950 else Slate100
                                )
                                Text(
                                    text = desc,
                                    fontSize = 8.sp,
                                    color = if (isSelected) Slate950.copy(alpha = 0.8f) else Slate400
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Playback Bar
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Slate900,
            border = BorderStroke(1.dp, Slate800),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (packets.isNotEmpty()) {
                    Slider(
                        value = currentFrameIndex.toFloat(),
                        onValueChange = { currentFrameIndex = it.toInt().coerceIn(0, packets.size - 1) },
                        valueRange = 0f..(packets.size - 1).toFloat(),
                        colors = SliderDefaults.colors(thumbColor = Cyan400, activeTrackColor = Cyan400)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(onClick = {
                            if (packets.isNotEmpty()) {
                                currentFrameIndex = (currentFrameIndex - 1 + packets.size) % packets.size
                            }
                        }) {
                            Icon(imageVector = Icons.Default.ChevronLeft, contentDescription = "Prev", tint = Slate300)
                        }

                        Button(
                            onClick = { isPlaying = !isPlaying },
                            colors = ButtonDefaults.buttonColors(containerColor = Cyan400, contentColor = Slate950)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isPlaying) "Pause" else "Play", fontWeight = FontWeight.Bold)
                        }

                        IconButton(onClick = {
                            if (packets.isNotEmpty()) {
                                currentFrameIndex = (currentFrameIndex + 1) % packets.size
                            }
                        }) {
                            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Next", tint = Slate300)
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (onToggleBatterySaver != null) {
                            IconButton(onClick = onToggleBatterySaver) {
                                Icon(
                                    imageVector = Icons.Default.Eco,
                                    contentDescription = "Battery Saver",
                                    tint = if (batterySaver) Emerald400 else Slate400
                                )
                            }
                        }

                        IconButton(onClick = { showSettings = !showSettings }) {
                            Icon(imageVector = Icons.Default.Tune, contentDescription = "Settings", tint = Slate400)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // File Metadata Preview & Compression Stats Card
        pendingPreview?.let { prev ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Slate900,
                border = BorderStroke(1.dp, Cyan400.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Description, contentDescription = null, tint = Cyan400, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "File Metadata Preview",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Slate100)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Cyan400.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, Cyan400.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = prev.type.split("/").lastOrNull()?.uppercase() ?: "FILE",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = Cyan400,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Slate950,
                        border = BorderStroke(1.dp, Slate800),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("File Name", color = Slate400, fontSize = 11.sp)
                            Text(prev.name, color = Slate100, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // GZIP Compression Metrics
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Slate950,
                        border = BorderStroke(1.dp, Slate800),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.FolderZip, contentDescription = null, tint = Purple400, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text("GZIP Compression", color = Slate100, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                                    Text(
                                        if (prev.wasCompressed) "${CryptoUtil.formatBytes(prev.size)} -> ${CryptoUtil.formatBytes(prev.compressedSize)}" else "Uncompressed binary",
                                        color = Slate400,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                            if (prev.wasCompressed) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Purple400.copy(alpha = 0.2f),
                                    border = BorderStroke(1.dp, Purple400.copy(alpha = 0.5f))
                                ) {
                                    Text(
                                        "-${prev.savingsPercent}% Size",
                                        color = Purple400,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Slate950,
                            border = BorderStroke(1.dp, Slate800),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("Transfer Chunks", color = Slate400, fontSize = 10.sp)
                                Text("${prev.estChunks} chunks", color = Slate100, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Slate950,
                            border = BorderStroke(1.dp, Slate800),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("Est. Duration", color = Slate400, fontSize = 10.sp)
                                Text("~${prev.estDurationSec}s @ $effectiveFps FPS", color = Purple400, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Slate950,
                        border = BorderStroke(1.dp, Slate800),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("SHA-256 Checksum", color = Slate400, fontSize = 10.sp)
                            Text(prev.sha256, color = Emerald400, fontFamily = FontFamily.Monospace, fontSize = 9.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { prepareStream(prev) },
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald400, contentColor = Slate950),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (meta?.id != null) "Regenerate & Start Transmission" else "Initiate Optical Transmission", fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Chunk Map
        meta?.let {
            ChunkMap(
                totalChunks = it.totalChunks,
                receivedIndices = (0 until it.totalChunks).toSet(),
                currentChunkIndex = currentFrameIndex % it.totalChunks
            )
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Fine Tuning Drawer
        if (showSettings) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Slate900,
                border = BorderStroke(1.dp, Slate800),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Compression Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("GZIP/ZLIB Compression", fontWeight = FontWeight.SemiBold, color = Slate100, fontSize = 12.sp)
                            Text("Compress text & data before optical segmenting", color = Slate400, fontSize = 10.sp)
                        }
                        Switch(
                            checked = config.useCompression,
                            onCheckedChange = {
                                config = config.copy(useCompression = it)
                                pendingPreview?.let { prev -> inspectFile(prev.name, prev.size, prev.type, prev.rawBytes, false) }
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Cyan400, checkedTrackColor = Cyan900)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Fine-Tune FPS", style = MaterialTheme.typography.bodySmall.copy(color = Slate400))
                    Slider(
                        value = config.fps.toFloat(),
                        onValueChange = { config = config.copy(fps = it.toInt()) },
                        valueRange = 1f..30f,
                        colors = SliderDefaults.colors(thumbColor = Cyan400, activeTrackColor = Cyan400)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text("Chunk Size (${config.chunkSize} bytes)", style = MaterialTheme.typography.bodySmall.copy(color = Slate400))
                    Slider(
                        value = config.chunkSize.toFloat(),
                        onValueChange = {
                            config = config.copy(chunkSize = it.toInt())
                            pendingPreview?.let { prev -> prepareStream(prev) }
                        },
                        valueRange = 150f..1200f,
                        steps = 21,
                        colors = SliderDefaults.colors(thumbColor = Cyan400, activeTrackColor = Cyan400)
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Event Terminal Logs
        EventLogList(logs = logs)
    }
}
