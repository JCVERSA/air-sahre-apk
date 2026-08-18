package com.example.ui.views

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cached
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.ChunkPayload
import com.example.model.LogEntry
import com.example.model.OpticalFeedbackPayload
import com.example.model.QrDensityPreset
import com.example.model.SenderConfig
import com.example.model.SessionHistoryItem
import com.example.model.TransferMeta
import com.example.ui.components.ChunkMap
import com.example.ui.components.EventLogList
import com.example.ui.theme.Cyan400
import com.example.ui.theme.Cyan900
import com.example.ui.theme.Emerald400
import com.example.ui.theme.Purple400
import com.example.ui.theme.Red500
import com.example.ui.theme.Slate100
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SenderScreen(
    historyRepo: HistoryRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var config by remember { mutableStateOf(SenderConfig()) }
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var fileName by remember { mutableStateOf("sample_document.txt") }
    var fileBytes by remember { mutableStateOf<ByteArray?>(null) }
    var meta by remember { mutableStateOf<TransferMeta?>(null) }
    var encodedPackets by remember { mutableStateOf<List<String>>(emptyList()) }
    var preRenderedBitmaps by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var isProcessing by remember { mutableStateOf(false) }
    var processProgress by remember { mutableDoubleStateOf(0.0) }

    var currentFrameIndex by remember { mutableIntStateOf(0) }
    var isPlaying by remember { mutableStateOf(true) }
    var isFullscreen by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var priorityMissingInput by remember { mutableStateOf("") }
    var priorityChunksOnly by remember { mutableStateOf(false) }
    var priorityChunkList by remember { mutableStateOf<List<Int>>(emptyList()) }

    val logs = remember { mutableStateListOf<LogEntry>() }

    fun addLog(level: String, msg: String) {
        logs.add(0, LogEntry(id = UUID.randomUUID().toString(), level = level, message = msg))
        if (logs.size > 50) logs.removeLast()
    }

    // Default sample data loader
    LaunchedEffect(Unit) {
        if (fileBytes == null) {
            val sampleText = """
                AirQR Optical Protocol V2 Payload
                =================================
                This is a secure air-gapped file transfer beamed across screens via high-density Base45 optical QR codes.
                RFC 9285 Base45 + GZIP Compression + Cryptographic SHA-256 Checksum Verification.
                Zero RF emissions, 100% offline peer-to-peer visual communication.
                Designed for high security air-gapped data transfers.
            """.trimIndent().toByteArray()
            fileBytes = sampleText
            fileName = "airqr_sample_memo.txt"
        }
    }

    // Process file whenever fileBytes or config changes
    fun prepareStream(bytes: ByteArray, name: String) {
        scope.launch(Dispatchers.Default) {
            isProcessing = true
            processProgress = 0.1
            addLog("info", "Encoding payload: $name (${CryptoUtil.formatBytes(bytes.size.toLong())})")

            val sha256 = CryptoUtil.computeSHA256(bytes)
            val (dataToChunk, wasCompressed) = if (config.useCompression) {
                GzipUtil.compress(bytes)
            } else {
                Pair(bytes, false)
            }

            val chunkSize = config.chunkSize
            val rawChunks = mutableListOf<ByteArray>()
            var offset = 0
            while (offset < dataToChunk.size) {
                val end = minOf(offset + chunkSize, dataToChunk.size)
                rawChunks.add(dataToChunk.copyOfRange(offset, end))
                offset = end
            }

            val totalChunks = rawChunks.size
            val transferId = CryptoUtil.generateTransferId()

            val transferMeta = TransferMeta(
                id = transferId,
                name = name,
                size = bytes.size.toLong(),
                type = "text/plain",
                totalChunks = totalChunks,
                chunkSize = chunkSize,
                hash = sha256,
                compressed = wasCompressed,
                compressedSize = dataToChunk.size.toLong(),
                encoding = config.encodingMode
            )

            // Generate packets
            val packets = mutableListOf<String>()
            val metaString = AirProtocol.encodeMetaPacket(transferMeta)

            for (i in 0 until totalChunks) {
                val chunkData = if (config.encodingMode == "base45") {
                    Base45.encode(rawChunks[i])
                } else {
                    CryptoUtil.toBase64(rawChunks[i])
                }
                val chunkPayload = ChunkPayload(
                    id = transferId,
                    index = i,
                    total = totalChunks,
                    data = chunkData,
                    encoding = config.encodingMode
                )
                val chunkString = AirProtocol.encodeChunkPacket(chunkPayload)

                // Interleave metadata packet periodically
                if (i % config.metaFrequency == 0) {
                    packets.add(metaString)
                }
                packets.add(chunkString)
            }

            // Always add meta at end
            packets.add(metaString)

            // Pre-render Bitmaps for smooth playback
            val bitmaps = mutableListOf<Bitmap>()
            for (p in packets) {
                val bmp = QrCodeUtil.generateQrBitmap(
                    content = p,
                    size = 512,
                    ecc = config.errorCorrection,
                    invertColor = config.invertColor
                )
                bitmaps.add(bmp)
            }

            withContext(Dispatchers.Main) {
                meta = transferMeta
                encodedPackets = packets
                preRenderedBitmaps = bitmaps
                currentFrameIndex = 0
                isProcessing = false
                addLog("success", "Stream prepared: $totalChunks chunks (${packets.size} optical frames)")

                historyRepo.addItem(
                    SessionHistoryItem(
                        id = UUID.randomUUID().toString(),
                        transferId = transferId,
                        fileName = name,
                        fileSize = bytes.size.toLong(),
                        fileType = "application/octet-stream",
                        role = "sent",
                        timestamp = System.currentTimeMillis(),
                        hash = sha256,
                        totalChunks = totalChunks,
                        durationSeconds = totalChunks.toDouble() / config.fps.coerceAtLeast(1),
                        averageSpeedKb = (bytes.size / 1024.0) / (totalChunks.toDouble() / config.fps.coerceAtLeast(1)).coerceAtLeast(0.1),
                        status = "success"
                    )
                )
            }
        }
    }

    LaunchedEffect(fileBytes, config.chunkSize, config.errorCorrection, config.invertColor, config.encodingMode, config.useCompression) {
        fileBytes?.let { prepareStream(it, fileName) }
    }

    // Stream animation loop
    LaunchedEffect(isPlaying, encodedPackets, config.fps, priorityChunksOnly, priorityChunkList) {
        if (!isPlaying || encodedPackets.isEmpty()) return@LaunchedEffect
        val intervalMs = (1000L / config.fps.coerceIn(1, 30))

        while (isActive) {
            delay(intervalMs)
            if (encodedPackets.isNotEmpty()) {
                currentFrameIndex = (currentFrameIndex + 1) % encodedPackets.size
            }
        }
    }

    // File picker contract
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            selectedFileUri = uri
            try {
                var name = "file"
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != null && cursor.moveToFirst()) {
                        name = cursor.getString(nameIndex) ?: "file"
                    }
                }
                fileName = name
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (bytes != null) {
                    fileBytes = bytes
                }
            } catch (e: Exception) {
                addLog("error", "Failed to load file: ${e.message}")
            }
        }
    }

    val currentBitmap = if (preRenderedBitmaps.isNotEmpty()) {
        preRenderedBitmaps.getOrNull(currentFrameIndex.coerceIn(0, preRenderedBitmaps.size - 1))
    } else null

    val currentPacket = encodedPackets.getOrNull(currentFrameIndex.coerceIn(0, encodedPackets.size - 1))
    val isMetaFrame = currentPacket?.startsWith("AIR2:M") == true || currentPacket?.startsWith("AIR1:M") == true

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Toolbar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
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
                    text = "$fileName (${meta?.let { CryptoUtil.formatBytes(it.size) } ?: "0 B"})",
                    style = MaterialTheme.typography.bodySmall.copy(color = Slate400)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = { filePicker.launch("*/*") }) {
                    Icon(imageVector = Icons.Default.FileOpen, contentDescription = "Select File", tint = Cyan400)
                }
                IconButton(onClick = { showSettings = !showSettings }) {
                    Icon(imageVector = Icons.Default.Tune, contentDescription = "Config", tint = Slate100)
                }
                IconButton(onClick = { isFullscreen = true }) {
                    Icon(imageVector = Icons.Default.Fullscreen, contentDescription = "Fullscreen", tint = Purple400)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Preset Density Selector Pills
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            QrDensityPreset.values().take(4).forEach { preset ->
                val isSelected = config.densityPreset == preset
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) Cyan900 else Slate900,
                    border = BorderStroke(1.dp, if (isSelected) Cyan400 else Slate800),
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            config = config.copy(
                                densityPreset = preset,
                                chunkSize = preset.chunkSize,
                                fps = preset.fps,
                                errorCorrection = preset.ecc
                            )
                        }
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = preset.name.replace("_", " "),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Cyan400 else Slate400
                            ),
                            maxLines = 1
                        )
                        Text(
                            text = "${preset.fps} fps",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 8.sp,
                                color = Slate400
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // QR Code Display Stage
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (config.invertColor) Color.Black else Color.White,
            border = BorderStroke(3.dp, if (isMetaFrame) Purple400 else Cyan400),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .aspectRatio(1f)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(color = Cyan400)
                } else if (currentBitmap != null) {
                    Image(
                        bitmap = currentBitmap.asImageBitmap(),
                        contentDescription = "AirQR Stream Code",
                        modifier = Modifier.fillMaxSize().padding(12.dp)
                    )
                } else {
                    Text(
                        text = "Loading Stream...",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Frame info banner overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Slate950.copy(alpha = 0.85f))
                        .padding(vertical = 4.dp, horizontal = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isMetaFrame) "METADATA PACKET" else "FRAME #${currentFrameIndex + 1} / ${encodedPackets.size}",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = if (isMetaFrame) Purple400 else Cyan400
                        )
                        Text(
                            text = "${config.fps} FPS • ${config.encodingMode.uppercase()}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = Slate400
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Playback Controls
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Slate900,
            border = BorderStroke(1.dp, Slate800),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Slider
                if (encodedPackets.isNotEmpty()) {
                    Slider(
                        value = currentFrameIndex.toFloat(),
                        onValueChange = {
                            currentFrameIndex = it.toInt().coerceIn(0, encodedPackets.size - 1)
                        },
                        valueRange = 0f..(encodedPackets.size - 1).toFloat(),
                        colors = SliderDefaults.colors(
                            thumbColor = Cyan400,
                            activeTrackColor = Cyan400,
                            inactiveTrackColor = Slate800
                        )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (encodedPackets.isNotEmpty()) {
                                currentFrameIndex = (currentFrameIndex - 1 + encodedPackets.size) % encodedPackets.size
                            }
                        }
                    ) {
                        Icon(imageVector = Icons.Default.ChevronLeft, contentDescription = "Prev", tint = Slate100)
                    }

                    Button(
                        onClick = { isPlaying = !isPlaying },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isPlaying) Purple400 else Cyan400,
                            contentColor = Slate950
                        ),
                        modifier = Modifier.size(54.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            if (encodedPackets.isNotEmpty()) {
                                currentFrameIndex = (currentFrameIndex + 1) % encodedPackets.size
                            }
                        }
                    ) {
                        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Next", tint = Slate100)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Matrix Chunk Map
        meta?.let {
            ChunkMap(
                totalChunks = it.totalChunks,
                receivedIndices = (0 until it.totalChunks).toSet(),
                currentChunkIndex = currentFrameIndex % it.totalChunks.coerceAtLeast(1)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Settings Panel
        AnimatedVisibility(visible = showSettings) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Slate900,
                border = BorderStroke(1.dp, Slate800),
                modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Transmitter Parameters",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Slate100
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // FPS Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Optical Speed", style = MaterialTheme.typography.bodyMedium.copy(color = Slate400))
                        Text(text = "${config.fps} FPS", style = MaterialTheme.typography.bodyMedium.copy(color = Cyan400, fontWeight = FontWeight.Bold))
                    }
                    Slider(
                        value = config.fps.toFloat(),
                        onValueChange = { config = config.copy(fps = it.toInt()) },
                        valueRange = 1f..30f,
                        steps = 28,
                        colors = SliderDefaults.colors(thumbColor = Cyan400, activeTrackColor = Cyan400)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Chunk Size Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Chunk Size", style = MaterialTheme.typography.bodyMedium.copy(color = Slate400))
                        Text(text = "${config.chunkSize} bytes", style = MaterialTheme.typography.bodyMedium.copy(color = Cyan400, fontWeight = FontWeight.Bold))
                    }
                    Slider(
                        value = config.chunkSize.toFloat(),
                        onValueChange = { config = config.copy(chunkSize = it.toInt()) },
                        valueRange = 150f..1400f,
                        colors = SliderDefaults.colors(thumbColor = Cyan400, activeTrackColor = Cyan400)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Toggles
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "GZIP Compression", color = Slate100)
                        Switch(
                            checked = config.useCompression,
                            onCheckedChange = { config = config.copy(useCompression = it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Cyan400)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Invert QR Color (White on Black)", color = Slate100)
                        Switch(
                            checked = config.invertColor,
                            onCheckedChange = { config = config.copy(invertColor = it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Cyan400)
                        )
                    }
                }
            }
        }

        // Event Terminal
        EventLogList(logs = logs)
    }

    // Fullscreen Mode Dialog for unobstructed optical scanning
    if (isFullscreen && currentBitmap != null) {
        Dialog(
            onDismissRequest = { isFullscreen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (config.invertColor) Color.Black else Color.White)
                    .clickable { isFullscreen = false },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = currentBitmap.asImageBitmap(),
                    contentDescription = "Fullscreen QR",
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .aspectRatio(1f)
                )

                IconButton(
                    onClick = { isFullscreen = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .background(Slate950.copy(alpha = 0.7f), CircleShape)
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Exit", tint = Color.White)
                }
            }
        }
    }
}
