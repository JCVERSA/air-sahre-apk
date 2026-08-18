package com.example.util

/**
 * Base45 Encoding/Decoding (RFC 9285) implementation.
 * Encodes arbitrary binary data into QR Code Alphanumeric character set:
 * '0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ $%*+-./:'
 * This allows QR codes to run in Alphanumeric mode rather than 8-bit Byte mode,
 * reducing QR matrix density significantly for optimal optical scanning throughput.
 */
object Base45 {
    private const val CHARSET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ $%*+-./:"
    private val LOOKUP = IntArray(256) { -1 }

    init {
        for (i in CHARSET.indices) {
            LOOKUP[CHARSET[i].code] = i
        }
    }

    fun encode(bytes: ByteArray): String {
        val sb = StringBuilder()
        val len = bytes.size
        var i = 0

        while (i < len) {
            if (i + 1 < len) {
                // 2 bytes -> 3 Base45 characters
                val b0 = bytes[i].toInt() and 0xFF
                val b1 = bytes[i + 1].toInt() and 0xFF
                val value = (b0 shl 8) or b1
                val c = value / (45 * 45)
                val rem = value % (45 * 45)
                val d = rem / 45
                val e = rem % 45
                sb.append(CHARSET[e])
                sb.append(CHARSET[d])
                sb.append(CHARSET[c])
                i += 2
            } else {
                // 1 byte -> 2 Base45 characters
                val value = bytes[i].toInt() and 0xFF
                val d = value / 45
                val e = value % 45
                sb.append(CHARSET[e])
                sb.append(CHARSET[d])
                i += 1
            }
        }

        return sb.toString()
    }

    fun decode(str: String): ByteArray {
        val len = str.length
        val out = mutableListOf<Byte>()
        var i = 0

        while (i < len) {
            if (i + 2 < len) {
                val c0 = str[i].code
                val c1 = str[i + 1].code
                val c2 = str[i + 2].code
                val e = if (c0 in 0..255) LOOKUP[c0] else -1
                val d = if (c1 in 0..255) LOOKUP[c1] else -1
                val c = if (c2 in 0..255) LOOKUP[c2] else -1

                if (e == -1 || d == -1 || c == -1) {
                    throw IllegalArgumentException("Invalid Base45 character in input")
                }

                val value = e + d * 45 + c * 45 * 45
                out.add(((value shr 8) and 0xFF).toByte())
                out.add((value and 0xFF).toByte())
                i += 3
            } else if (i + 1 < len) {
                val c0 = str[i].code
                val c1 = str[i + 1].code
                val e = if (c0 in 0..255) LOOKUP[c0] else -1
                val d = if (c1 in 0..255) LOOKUP[c1] else -1

                if (e == -1 || d == -1) {
                    throw IllegalArgumentException("Invalid Base45 character in input")
                }

                val value = e + d * 45
                out.add((value and 0xFF).toByte())
                i += 2
            } else {
                throw IllegalArgumentException("Invalid Base45 string length")
            }
        }

        return out.toByteArray()
    }
}
