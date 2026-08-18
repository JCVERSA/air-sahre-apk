package com.example.util

import android.util.Base64
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Locale
import java.util.UUID
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

object CryptoUtil {
    fun computeSHA256(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(data)
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    fun toBase64(bytes: ByteArray): String {
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    fun fromBase64(str: String): ByteArray {
        return Base64.decode(str, Base64.DEFAULT)
    }

    fun urlEncode(str: String): String {
        return URLEncoder.encode(str, StandardCharsets.UTF_8.toString())
    }

    fun urlDecode(str: String): String {
        return URLDecoder.decode(str, StandardCharsets.UTF_8.toString())
    }

    fun generateTransferId(): String {
        return UUID.randomUUID().toString().replace("-", "").take(6).uppercase(Locale.US)
    }

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, 3)
        return "%.1f %s".format(Locale.US, bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
    }
}

object GzipUtil {
    fun compress(data: ByteArray): Pair<ByteArray, Boolean> {
        if (data.size < 256) {
            return Pair(data, false)
        }
        try {
            val bos = ByteArrayOutputStream(data.size)
            GZIPOutputStream(bos).use { it.write(data) }
            val compressed = bos.toByteArray()
            if (compressed.size < data.size * 0.95) {
                return Pair(compressed, true)
            }
        } catch (e: Exception) {
            // fallback to raw
        }
        return Pair(data, false)
    }

    fun decompress(data: ByteArray): ByteArray {
        return try {
            val bis = ByteArrayInputStream(data)
            GZIPInputStream(bis).use { it.readBytes() }
        } catch (e: Exception) {
            data
        }
    }
}
