# AirQR — High-Speed Optical Air-Gapped File Transport (Android)

**AirQR** is a native Android application built with Kotlin and Jetpack Compose for ultra-fast, 100% offline, optical air-gapped file transfers between devices using animated high-density QR code streams and CameraX scanning with SHA-256 integrity verification.

---

## ⚡ Architecture & Features

1. **Base45 Alphanumeric QR Matrix Encoding (RFC 9285):**
   - Encodes binary data into QR Code alphanumeric mode, reducing matrix density by ~45% compared to raw byte mode for high-FPS optical transmission.

2. **Turbo Streaming Compression (Gzip):**
   - Automatically compresses payloads before chunking to minimize transfer time.

3. **Pre-rendered BitMatrix Memory Buffering:**
   - Pre-renders QR frames for zero-stutter playback up to 30 FPS.

4. **CameraX Optical Scanning:**
   - Real-time viewfinder with region-of-interest QR decoding and adaptive optical telemetry.

5. **Loopback Mode:**
   - On-device self-test mode to test transmitter and receiver optical pipelines side-by-side.

6. **Cryptographic SHA-256 Verification:**
   - Verifies the reconstructed file byte-for-byte upon completion and offers instant sharing/saving.

7. **Optical Feedback Beacon:**
   - Generates live reverse feedback QR codes (`AIR2:FB`) for sender speed & chunk prioritization.

---

## 📡 Protocol Specification (AIR2)

- **Metadata Packet (`M`):**
  `AIR2:M:<transferId>:<fileSize>:<totalChunks>:<chunkSize>:<sha256Hex>:<b64FileName>:<b64MimeType>:<compressedFlag>:<compressedSize>:<encodingFlag>`

- **Data Chunk Packet (`C`):**
  `AIR2:C:<transferId>:<chunkIndex>:<totalChunks>:<encodingFlag>:<base45OrBase64Payload>`

- **Feedback Beacon Packet (`FB`):**
  `AIR2:FB:<transferId>:<qualityScore>:<failureRate>:<lighting>:<recommendedFps>:<recommendedChunkSize>:<recommendedEcc>:<missingChunks>`

---

## 💻 Android Tech Stack
- **Language:** Kotlin 2.0.21
- **UI Framework:** Jetpack Compose (Material 3)
- **Camera:** AndroidX CameraX (Core, Camera2, Lifecycle, View)
- **QR Engine:** ZXing Core
- **Cryptography & Compression:** Java `MessageDigest` (SHA-256) & `GZIPInputStream`/`GZIPOutputStream`
- **State Management:** Compose Architecture & Coroutines
