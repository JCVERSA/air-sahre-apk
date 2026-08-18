package com.example.model

enum class PacketType {
    META,
    CHUNK,
    FEEDBACK
}

enum class LightingCondition {
    EXCELLENT,
    GOOD,
    LOW_LIGHT,
    HIGH_GLARE,
    BLUR
}

enum class QrDensityPreset(val label: String, val chunkSize: Int, val fps: Int, val ecc: String) {
    ULTRA_LOW("Ultra-Low (Max Distance)", 200, 6, "H"),
    LOW("Low Density (Phone-to-Phone)", 380, 10, "Q"),
    MEDIUM("Medium (Standard AirQR)", 700, 14, "M"),
    HIGH("High Density (HD Screens)", 950, 18, "M"),
    ULTRA_HIGH("Ultra-High (4K Monitor)", 1250, 22, "L"),
    CUSTOM("Custom", 700, 14, "M")
}

data class TransferMeta(
    val id: String,
    val name: String,
    val size: Long,
    val type: String,
    val totalChunks: Int,
    val chunkSize: Int,
    val hash: String,
    val timestamp: Long = System.currentTimeMillis(),
    val compressed: Boolean = false,
    val compressedSize: Long = size,
    val encoding: String = "base45"
)

data class ChunkPayload(
    val id: String,
    val index: Int,
    val total: Int,
    val data: String,
    val encoding: String = "base45"
)

data class OpticalFeedbackPayload(
    val transferId: String,
    val qualityScore: Int,
    val failureRate: Int,
    val lighting: LightingCondition,
    val recommendedFps: Int,
    val recommendedChunkSize: Int,
    val recommendedEcc: String,
    val missingChunks: List<Int>
)

data class Packet(
    val version: Int,
    val type: PacketType,
    val meta: TransferMeta? = null,
    val chunk: ChunkPayload? = null,
    val feedback: OpticalFeedbackPayload? = null
)

data class SenderConfig(
    val chunkSize: Int = 700,
    val fps: Int = 14,
    val errorCorrection: String = "M",
    val invertColor: Boolean = false,
    val metaFrequency: Int = 8,
    val useCompression: Boolean = true,
    val encodingMode: String = "base45",
    val densityPreset: QrDensityPreset = QrDensityPreset.MEDIUM,
    val adaptiveRateControl: Boolean = true,
    val minFps: Int = 3,
    val maxFps: Int = 22,
    val minChunkSize: Int = 180,
    val maxChunkSize: Int = 1250
)

data class ReceiverConfig(
    val resolution: String = "720p",
    val audioFeedback: Boolean = true,
    val autoDownload: Boolean = false,
    val roiCropEnabled: Boolean = true,
    val adaptiveFeedbackBeacon: Boolean = true
)

data class OpticalDiagnostics(
    val luminance: Int = 128,
    val contrast: Int = 70,
    val lightingCondition: LightingCondition = LightingCondition.GOOD,
    val failureRate: Int = 0,
    val successRate: Int = 100,
    val linkQualityScore: Int = 95,
    val consecutiveFailures: Int = 0,
    val suggestedFps: Int = 14,
    val suggestedChunkSize: Int = 700,
    val suggestedEcc: String = "M",
    val recommendedAction: String = "Waiting for optical stream detection..."
)

enum class ReceiverStatus {
    IDLE,
    SCANNING,
    RECEIVING,
    VERIFYING,
    COMPLETED,
    ERROR
}

data class ReceiverState(
    val status: ReceiverStatus = ReceiverStatus.IDLE,
    val transferId: String? = null,
    val meta: TransferMeta? = null,
    val receivedChunks: Map<Int, ByteArray> = emptyMap(),
    val totalChunks: Int = 0,
    val receivedCount: Int = 0,
    val startTime: Long? = null,
    val lastChunkTime: Long? = null,
    val bytesReceived: Long = 0,
    val computedHash: String? = null,
    val isHashValid: Boolean? = null,
    val reconstructedData: ByteArray? = null,
    val errorMessage: String? = null,
    val fpsDetected: Double = 0.0,
    val scanLatencyMs: Long = 0,
    val uniqueFrameRate: Double = 0.0,
    val diagnostics: OpticalDiagnostics = OpticalDiagnostics(),
    val currentScannedChunkIndex: Int? = null
)

data class SessionHistoryItem(
    val id: String,
    val transferId: String,
    val fileName: String,
    val fileSize: Long,
    val fileType: String,
    val role: String, // "sent" or "received"
    val timestamp: Long,
    val hash: String,
    val totalChunks: Int,
    val durationSeconds: Double,
    val averageSpeedKb: Double,
    val status: String // "success" or "failed"
)

data class LogEntry(
    val id: String,
    val timestamp: Long = System.currentTimeMillis(),
    val level: String, // "info", "success", "warn", "error"
    val message: String,
    val details: String? = null
)
