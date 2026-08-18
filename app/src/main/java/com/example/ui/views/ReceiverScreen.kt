package com.example.ui.views

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.net.Uri
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.model.LightingCondition
import com.example.model.LogEntry
import com.example.model.OpticalDiagnostics
import com.example.model.OpticalFeedbackPayload
import com.example.model.PacketType
import com.example.model.ReceiverConfig
import com.example.model.ReceiverState
import com.example.model.ReceiverStatus
import com.example.model.SessionHistoryItem
import com.example.model.TransferMeta
import com.example.ui.components.AdaptiveLinkHud
import com.example.ui.components.ChunkMap
import com.example.ui.components.EventLogList
import com.example.ui.components.QrFeedbackBeaconDialog
import com.example.ui.theme.Amber400
import com.example.ui.theme.Cyan400
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
import com.example.util.SoundFeedback
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.concurrent.Executors

@Composable
fun ReceiverScreen(
    historyRepo: HistoryRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val soundFeedback = remember { SoundFeedback(context) }

    DisposableEffect(Unit) {
        onDispose {
            soundFeedback.release()
        }
    }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    var receiverState by remember { mutableStateOf(ReceiverState()) }
    val receivedChunksMap = remember { mutableStateMapOf<Int, ByteArray>() }
    val logs = remember { mutableStateListOf<LogEntry>() }
    var isCameraActive by remember { mutableStateOf(true) }
    var showBeaconDialog by remember { mutableStateOf(false) }
    var speedKb by remember { mutableDoubleStateOf(0.0) }
    var lastScannedText by remember { mutableStateOf<String?>(null) }
    var lastFrameTimestamp by remember { mutableLongStateOf(0L) }
    var frameCount by remember { mutableIntStateOf(0) }

    fun addLog(level: String, msg: String) {
        logs.add(0, LogEntry(id = UUID.randomUUID().toString(), level = level, message = msg))
        if (logs.size > 50) logs.removeLast()
    }

    fun resetReceiver() {
        receivedChunksMap.clear()
        receiverState = ReceiverState()
        lastScannedText = null
        speedKb = 0.0
        addLog("info", "Receiver reset to idle state.")
    }

    // Assembly and SHA-256 verification
    fun verifyAndAssemble(meta: TransferMeta) {
        receiverState = receiverState.copy(status = ReceiverStatus.VERIFYING)
        addLog("info", "All ${meta.totalChunks} chunks received. Reconstructing payload...")

        scope.launch(Dispatchers.Default) {
            val bos = ByteArrayOutputStream()
            for (i in 0 until meta.totalChunks) {
                val chunk = receivedChunksMap[i]
                if (chunk != null) {
                    bos.write(chunk)
                }
            }
            val rawCombined = bos.toByteArray()
            val finalData = if (meta.compressed) {
                GzipUtil.decompress(rawCombined)
            } else {
                rawCombined
            }

            val computedSha256 = CryptoUtil.computeSHA256(finalData)
            val isValid = computedSha256.equals(meta.hash, ignoreCase = true)

            withContext(Dispatchers.Main) {
                if (isValid) {
                    soundFeedback.playSuccess()
                    receiverState = receiverState.copy(
                        status = ReceiverStatus.COMPLETED,
                        computedHash = computedSha256,
                        isHashValid = true,
                        reconstructedData = finalData
                    )
                    addLog("success", "SHA-256 Verification PASSED! ${meta.name} ready.")

                    val duration = ((System.currentTimeMillis() - (receiverState.startTime ?: System.currentTimeMillis())) / 1000.0).coerceAtLeast(0.1)
                    val avgSpeed = (meta.size / 1024.0) / duration

                    historyRepo.addItem(
                        SessionHistoryItem(
                            id = UUID.randomUUID().toString(),
                            transferId = meta.id,
                            fileName = meta.name,
                            fileSize = meta.size,
                            fileType = meta.type,
                            role = "received",
                            timestamp = System.currentTimeMillis(),
                            hash = computedSha256,
                            totalChunks = meta.totalChunks,
                            durationSeconds = duration,
                            averageSpeedKb = avgSpeed,
                            status = "success"
                        )
                    )
                } else {
                    soundFeedback.playError()
                    receiverState = receiverState.copy(
                        status = ReceiverStatus.ERROR,
                        computedHash = computedSha256,
                        isHashValid = false,
                        errorMessage = "SHA-256 hash mismatch! Data may be corrupted."
                    )
                    addLog("error", "SHA-256 Checksum FAILED! Expected ${meta.hash}, got $computedSha256")
                }
            }
        }
    }

    // Process incoming packet
    fun handleDecodedText(text: String) {
        if (text == lastScannedText && receiverState.status == ReceiverStatus.RECEIVING) {
            return // Skip duplicate frame
        }
        lastScannedText = text
        frameCount++

        val packet = AirProtocol.parsePacket(text) ?: return

        when (packet.type) {
            PacketType.META -> {
                packet.meta?.let { meta ->
                    if (receiverState.meta?.id != meta.id) {
                        receiverState = receiverState.copy(
                            status = ReceiverStatus.RECEIVING,
                            meta = meta,
                            totalChunks = meta.totalChunks,
                            transferId = meta.id,
                            startTime = System.currentTimeMillis()
                        )
                        addLog("info", "Detected optical stream: ${meta.name} (${CryptoUtil.formatBytes(meta.size)})")
                    }
                }
            }
            PacketType.CHUNK -> {
                packet.chunk?.let { chunk ->
                    if (receiverState.meta == null || receiverState.meta?.id == chunk.id) {
                        if (!receivedChunksMap.containsKey(chunk.index)) {
                            soundFeedback.playChunkTick()
                            try {
                                val chunkBytes = if (chunk.encoding == "base45") {
                                    Base45.decode(chunk.data)
                                } else {
                                    CryptoUtil.fromBase64(chunk.data)
                                }

                                receivedChunksMap[chunk.index] = chunkBytes
                                val total = chunk.total
                                val count = receivedChunksMap.size
                                val now = System.currentTimeMillis()

                                val elapsedSec = ((now - (receiverState.startTime ?: now)) / 1000.0).coerceAtLeast(0.1)
                                speedKb = (receivedChunksMap.values.sumOf { it.size.toLong() } / 1024.0) / elapsedSec

                                receiverState = receiverState.copy(
                                    status = ReceiverStatus.RECEIVING,
                                    totalChunks = total,
                                    receivedCount = count,
                                    currentScannedChunkIndex = chunk.index,
                                    lastChunkTime = now
                                )

                                if (count >= total && receiverState.meta != null) {
                                    verifyAndAssemble(receiverState.meta!!)
                                }
                            } catch (e: Exception) {
                                addLog("warn", "Failed to decode chunk #${chunk.index}: ${e.message}")
                            }
                        }
                    }
                }
            }
            PacketType.FEEDBACK -> {
                // Optical feedback packet received
            }
        }
    }

    // Share / Save completed file
    fun shareFile(data: ByteArray, name: String) {
        try {
            val cacheFile = File(context.cacheDir, name)
            FileOutputStream(cacheFile).use { it.write(data) }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", cacheFile)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "*/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share AirQR File"))
        } catch (e: Exception) {
            // fallback plain share or text
            try {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, String(data))
                }
                context.startActivity(Intent.createChooser(intent, "Share AirQR Content"))
            } catch (ex: Exception) {
                addLog("error", "Could not open share dialog: ${ex.message}")
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
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        tint = Emerald400,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Optical Receiver",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Slate100
                        )
                    )
                }
                val statusText = when (receiverState.status) {
                    ReceiverStatus.IDLE -> "Point camera at sender display"
                    ReceiverStatus.SCANNING -> "Scanning for optical stream..."
                    ReceiverStatus.RECEIVING -> "Receiving stream (${receivedChunksMap.size}/${receiverState.totalChunks})"
                    ReceiverStatus.VERIFYING -> "Verifying cryptographic SHA-256..."
                    ReceiverStatus.COMPLETED -> "Transfer completed & verified"
                    ReceiverStatus.ERROR -> "Transfer failed"
                }
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodySmall.copy(color = Slate400)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = { showBeaconDialog = true }) {
                    Icon(imageVector = Icons.Default.Sensors, contentDescription = "Beacon", tint = Purple400)
                }
                IconButton(onClick = { resetReceiver() }) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reset", tint = Cyan400)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Camera Viewfinder Box
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Slate950,
            border = BorderStroke(2.dp, if (receiverState.status == ReceiverStatus.RECEIVING) Cyan400 else Slate800),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .aspectRatio(1f)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (!hasCameraPermission) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = Cyan400,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Camera Permission Required",
                            style = MaterialTheme.typography.titleSmall.copy(color = Slate100)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "To decode air-gapped QR code streams",
                            style = MaterialTheme.typography.bodySmall.copy(color = Slate400)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                            colors = ButtonDefaults.buttonColors(containerColor = Cyan400, contentColor = Slate950)
                        ) {
                            Text("Grant Permission", fontWeight = FontWeight.Bold)
                        }
                    }
                } else if (isCameraActive) {
                    // CameraX View
                    AndroidView(
                        factory = { ctx ->
                            val previewView = PreviewView(ctx)
                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                            cameraProviderFuture.addListener({
                                val cameraProvider = cameraProviderFuture.get()
                                val preview = Preview.Builder().build().also {
                                    it.surfaceProvider = previewView.surfaceProvider
                                }

                                val imageAnalysis = ImageAnalysis.Builder()
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .build()

                                val multiFormatReader = MultiFormatReader()

                                imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                                    val buffer = imageProxy.planes[0].buffer
                                    val data = ByteArray(buffer.remaining())
                                    buffer.get(data)
                                    val width = imageProxy.width
                                    val height = imageProxy.height

                                    val source = PlanarYUVLuminanceSource(
                                        data, width, height, 0, 0, width, height, false
                                    )
                                    val bitmap = BinaryBitmap(HybridBinarizer(source))
                                    try {
                                        val result = multiFormatReader.decodeWithState(bitmap)
                                        result?.text?.let { raw ->
                                            scope.launch(Dispatchers.Main) {
                                                handleDecodedText(raw)
                                            }
                                        }
                                    } catch (e: Exception) {
                                        // Ignore QR not found in frame
                                    } finally {
                                        multiFormatReader.reset()
                                        imageProxy.close()
                                    }
                                }

                                try {
                                    cameraProvider.unbindAll()
                                    cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        CameraSelector.DEFAULT_BACK_CAMERA,
                                        preview,
                                        imageAnalysis
                                    )
                                } catch (e: Exception) {
                                    // Camera bind exception
                                }
                            }, ContextCompat.getMainExecutor(ctx))
                            previewView
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Target viewfinder reticle
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .align(Alignment.Center)
                            .border(2.dp, Cyan400.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                    )
                }

                // Status overlay
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
                            text = if (receiverState.totalChunks > 0) "${receivedChunksMap.size} / ${receiverState.totalChunks} CHUNKS" else "READY TO SCAN",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = if (receivedChunksMap.size == receiverState.totalChunks && receiverState.totalChunks > 0) Emerald400 else Cyan400
                        )
                        Text(
                            text = "%.1f KB/s".format(speedKb),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = Slate400
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Completion Card
        if (receiverState.status == ReceiverStatus.COMPLETED && receiverState.reconstructedData != null) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Slate900,
                border = BorderStroke(1.dp, Emerald400),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Emerald400,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Transfer Complete & Verified",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Emerald400
                                )
                            )
                            Text(
                                text = receiverState.meta?.name ?: "File",
                                style = MaterialTheme.typography.bodySmall.copy(color = Slate100)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "SHA-256: ${receiverState.computedHash?.take(16)}...",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Slate400
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                receiverState.reconstructedData?.let { data ->
                                    shareFile(data, receiverState.meta?.name ?: "airqr_file.bin")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Emerald400, contentColor = Slate950),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share / Save", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Chunk Matrix Map
        if (receiverState.totalChunks > 0) {
            val missingList = (0 until receiverState.totalChunks).filter { !receivedChunksMap.containsKey(it) }
            ChunkMap(
                totalChunks = receiverState.totalChunks,
                receivedIndices = receivedChunksMap.keys.toSet(),
                currentChunkIndex = receiverState.currentScannedChunkIndex,
                missingChunks = missingList
            )
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Optical Link Telemetry HUD
        AdaptiveLinkHud(
            diagnostics = receiverState.diagnostics.copy(
                suggestedFps = (frameCount.toDouble() / 5.0).toInt().coerceIn(1, 30),
                linkQualityScore = if (receivedChunksMap.size > 0) 95 else 70
            ),
            instantSpeedKb = speedKb
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Event Terminal Log
        EventLogList(logs = logs)
    }

    // Optical Feedback Beacon Dialog
    if (showBeaconDialog) {
        val missingList = (0 until receiverState.totalChunks).filter { !receivedChunksMap.containsKey(it) }
        val feedback = OpticalFeedbackPayload(
            transferId = receiverState.transferId ?: "AIRQR",
            qualityScore = if (missingList.isEmpty()) 100 else 85,
            failureRate = 5,
            lighting = LightingCondition.GOOD,
            recommendedFps = 14,
            recommendedChunkSize = 700,
            recommendedEcc = "M",
            missingChunks = missingList
        )

        QrFeedbackBeaconDialog(
            feedback = feedback,
            onDismiss = { showBeaconDialog = false }
        )
    }
}
