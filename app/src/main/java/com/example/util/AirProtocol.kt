package com.example.util

import com.example.model.ChunkPayload
import com.example.model.LightingCondition
import com.example.model.OpticalFeedbackPayload
import com.example.model.Packet
import com.example.model.PacketType
import com.example.model.TransferMeta

object AirProtocol {
    private const val PREFIX_V1 = "AIR1:"
    private const val PREFIX_V2 = "AIR2:"

    fun encodeMetaPacket(meta: TransferMeta): String {
        val b64Name = CryptoUtil.toBase64(CryptoUtil.urlEncode(meta.name).toByteArray())
        val b64Type = CryptoUtil.toBase64(CryptoUtil.urlEncode(meta.type.ifEmpty { "application/octet-stream" }).toByteArray())
        val compFlag = if (meta.compressed) "1" else "0"
        val compSize = meta.compressedSize
        val encFlag = if (meta.encoding == "base45") "45" else "64"

        return "${PREFIX_V2}M:${meta.id}:${meta.size}:${meta.totalChunks}:${meta.chunkSize}:${meta.hash}:$b64Name:$b64Type:$compFlag:$compSize:$encFlag"
    }

    fun encodeChunkPacket(chunk: ChunkPayload): String {
        val encFlag = if (chunk.encoding == "base45") "45" else "64"
        return "${PREFIX_V2}C:${chunk.id}:${chunk.index}:${chunk.total}:$encFlag:${chunk.data}"
    }

    fun encodeFeedbackPacket(feedback: OpticalFeedbackPayload): String {
        val missingStr = feedback.missingChunks.take(50).joinToString(",")
        return "${PREFIX_V2}FB:${feedback.transferId}:${feedback.qualityScore}:${feedback.failureRate}:${feedback.lighting.name}:${feedback.recommendedFps}:${feedback.recommendedChunkSize}:${feedback.recommendedEcc}:$missingStr"
    }

    fun parsePacket(rawText: String?): Packet? {
        if (rawText.isNullOrBlank()) return null
        val text = rawText.trim()

        // Protocol V2
        if (text.startsWith(PREFIX_V2)) {
            val content = text.substring(PREFIX_V2.length)
            val parts = content.split(":")
            if (parts.isEmpty()) return null

            val type = parts[0]

            // FEEDBACK
            if (type == "FB" && parts.size >= 8) {
                return try {
                    val transferId = parts[1]
                    val qualityScore = parts[2].toIntOrNull() ?: 50
                    val failureRate = parts[3].toIntOrNull() ?: 0
                    val lighting = try {
                        LightingCondition.valueOf(parts[4])
                    } catch (e: Exception) {
                        LightingCondition.GOOD
                    }
                    val recommendedFps = parts[5].toIntOrNull() ?: 14
                    val recommendedChunkSize = parts[6].toIntOrNull() ?: 700
                    val recommendedEcc = parts[7]
                    val missingChunks = if (parts.size >= 9 && parts[8].isNotBlank()) {
                        parts[8].split(",").mapNotNull { it.toIntOrNull() }
                    } else {
                        emptyList()
                    }

                    Packet(
                        version = 2,
                        type = PacketType.FEEDBACK,
                        feedback = OpticalFeedbackPayload(
                            transferId = transferId,
                            qualityScore = qualityScore,
                            failureRate = failureRate,
                            lighting = lighting,
                            recommendedFps = recommendedFps,
                            recommendedChunkSize = recommendedChunkSize,
                            recommendedEcc = recommendedEcc,
                            missingChunks = missingChunks
                        )
                    )
                } catch (e: Exception) {
                    null
                }
            }

            // META
            if (type == "M" && parts.size >= 8) {
                return try {
                    val id = parts[1]
                    val size = parts[2].toLongOrNull() ?: return null
                    val totalChunks = parts[3].toIntOrNull() ?: return null
                    val chunkSize = parts[4].toIntOrNull() ?: return null
                    val hash = parts[5]
                    val nameDecoded = String(CryptoUtil.fromBase64(parts[6]))
                    val name = CryptoUtil.urlDecode(nameDecoded)
                    val mimeDecoded = String(CryptoUtil.fromBase64(parts[7]))
                    val mimeType = CryptoUtil.urlDecode(mimeDecoded)
                    val compressed = parts.getOrNull(8) == "1"
                    val compressedSize = parts.getOrNull(9)?.toLongOrNull() ?: size
                    val encoding = if (parts.getOrNull(10) == "45") "base45" else "base64"

                    Packet(
                        version = 2,
                        type = PacketType.META,
                        meta = TransferMeta(
                            id = id,
                            name = name,
                            size = size,
                            type = mimeType,
                            totalChunks = totalChunks,
                            chunkSize = chunkSize,
                            hash = hash,
                            compressed = compressed,
                            compressedSize = compressedSize,
                            encoding = encoding,
                            timestamp = System.currentTimeMillis()
                        )
                    )
                } catch (e: Exception) {
                    null
                }
            }

            // CHUNK
            if (type == "C" && parts.size >= 5) {
                return try {
                    val id = parts[1]
                    val index = parts[2].toIntOrNull() ?: return null
                    val total = parts[3].toIntOrNull() ?: return null
                    val encoding = if (parts[4] == "45") "base45" else "base64"
                    // Join everything after the 4th index in case the data contains colons
                    val data = parts.subList(5, parts.size).joinToString(":")

                    Packet(
                        version = 2,
                        type = PacketType.CHUNK,
                        chunk = ChunkPayload(
                            id = id,
                            index = index,
                            total = total,
                            data = data,
                            encoding = encoding
                        )
                    )
                } catch (e: Exception) {
                    null
                }
            }
        }

        // Protocol V1 legacy fallback
        if (text.startsWith(PREFIX_V1)) {
            val content = text.substring(PREFIX_V1.length)
            val parts = content.split(":")
            if (parts.isEmpty()) return null

            val type = parts[0]
            if (type == "M" && parts.size >= 8) {
                return try {
                    val id = parts[1]
                    val size = parts[2].toLongOrNull() ?: return null
                    val totalChunks = parts[3].toIntOrNull() ?: return null
                    val chunkSize = parts[4].toIntOrNull() ?: return null
                    val hash = parts[5]
                    val name = CryptoUtil.urlDecode(String(CryptoUtil.fromBase64(parts[6])))
                    val mimeType = CryptoUtil.urlDecode(String(CryptoUtil.fromBase64(parts[7])))

                    Packet(
                        version = 1,
                        type = PacketType.META,
                        meta = TransferMeta(
                            id = id,
                            name = name,
                            size = size,
                            type = mimeType,
                            totalChunks = totalChunks,
                            chunkSize = chunkSize,
                            hash = hash,
                            encoding = "base64",
                            timestamp = System.currentTimeMillis()
                        )
                    )
                } catch (e: Exception) {
                    null
                }
            }

            if (type == "C" && parts.size >= 5) {
                return try {
                    val id = parts[1]
                    val index = parts[2].toIntOrNull() ?: return null
                    val total = parts[3].toIntOrNull() ?: return null
                    val data = parts.subList(4, parts.size).joinToString(":")

                    Packet(
                        version = 1,
                        type = PacketType.CHUNK,
                        chunk = ChunkPayload(
                            id = id,
                            index = index,
                            total = total,
                            data = data,
                            encoding = "base64"
                        )
                    )
                } catch (e: Exception) {
                    null
                }
            }
        }

        return null
    }
}
