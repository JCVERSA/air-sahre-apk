package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.Cyan400
import com.example.ui.theme.Emerald400
import com.example.ui.theme.Purple400
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import com.example.util.QrCodeUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AndroidCalibrationProfile(
    val avgLatencyMs: Long,
    val maxTheoreticalFps: Int,
    val recommendedFps: Int,
    val recommendedChunkSize: Int,
    val recommendedEcc: String,
    val tierName: String
)

@Composable
fun CalibrationDialog(
    onApplyProfile: (fps: Int, chunkSize: Int, ecc: String) -> Unit,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var isTesting by remember { mutableStateOf(false) }
    var progress by remember { mutableIntStateOf(0) }
    var instantLatency by remember { mutableStateOf(0L) }
    var peakFps by remember { mutableIntStateOf(0) }
    var resultProfile by remember { mutableStateOf<AndroidCalibrationProfile?>(null) }

    fun runBenchmark() {
        isTesting = true
        progress = 0
        resultProfile = null

        scope.launch(Dispatchers.Default) {
            val testPayloads = listOf(
                "AIR2:M:BENCHMARK_1:file.bin:2048:10:700:M:BASE45:0123456789AB",
                "AIR2:C:BENCHMARK_1:0:10:base45:25%*+ABXYZ99928174981273981273891273981273",
                "AIR2:C:BENCHMARK_1:1:10:base45:88%*+CDXYZ77728174981273981273891273981273",
                "AIR2:C:BENCHMARK_1:2:10:base45:99%*+EFXYZ55528174981273981273891273981273"
            )

            val latencies = mutableListOf<Long>()
            val totalIterations = 20

            for (i in 0 until totalIterations) {
                val payload = testPayloads[i % testPayloads.size]
                val t0 = System.currentTimeMillis()

                val bitmap = QrCodeUtil.generateQrBitmap(payload, 360, "M", false)
                QrCodeUtil.decodeQrBitmap(bitmap)

                val t1 = System.currentTimeMillis()
                val delta = maxOf(1L, t1 - t0)
                latencies.add(delta)

                val fpsNow = (1000L / delta).toInt()

                withContext(Dispatchers.Main) {
                    instantLatency = delta
                    peakFps = maxOf(peakFps, fpsNow)
                    progress = (((i + 1).toFloat() / totalIterations.toFloat()) * 100).toInt()
                }
                delay(30L)
            }

            val avgLatency = latencies.average().toLong()
            val maxFps = (1000L / maxOf(1L, avgLatency)).toInt().coerceIn(4, 30)

            val (recFps, recChunk, recEcc, tier) = when {
                avgLatency <= 25 -> listOf(22, 900, "L", "Turbo Ultra")
                avgLatency <= 45 -> listOf(16, 800, "M", "High-Performance")
                avgLatency <= 80 -> listOf(12, 650, "M", "Standard (Mid-Range)")
                else -> listOf(8, 450, "Q", "Entry (Low-End)")
            }

            val profile = AndroidCalibrationProfile(
                avgLatencyMs = avgLatency,
                maxTheoreticalFps = maxFps,
                recommendedFps = recFps as Int,
                recommendedChunkSize = recChunk as Int,
                recommendedEcc = recEcc as String,
                tierName = tier as String
            )

            withContext(Dispatchers.Main) {
                resultProfile = profile
                isTesting = false
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Slate900),
            border = BorderStroke(1.dp, Slate800),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Assessment, contentDescription = null, tint = Cyan400, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Optical Speed Calibration",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Slate100)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Slate400)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (!isTesting && resultProfile == null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Slate950,
                        border = BorderStroke(1.dp, Slate800),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Speed, contentDescription = null, tint = Cyan400, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Auto Hardware Performance Benchmark",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Slate100
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Analyzes your device's camera decode latency and frame throughput to suggest the optimal FPS and chunk size that prevents dropped frames.",
                                fontSize = 11.sp,
                                color = Slate400,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { runBenchmark() },
                        colors = ButtonDefaults.buttonColors(containerColor = Cyan400, contentColor = Slate950),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Start Calibration Test", fontWeight = FontWeight.Bold)
                    }
                } else if (isTesting) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(80.dp)) {
                            CircularProgressIndicator(
                                progress = { progress / 100f },
                                color = Cyan400,
                                modifier = Modifier.size(80.dp)
                            )
                            Text(
                                text = "$progress%",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Cyan400
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Stress Testing QR Matrix Decoder...", fontWeight = FontWeight.Bold, color = Slate100, fontSize = 13.sp)
                        Text("Simulating burst Base45 frame streams", color = Slate400, fontSize = 11.sp)

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Slate950,
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Decode Latency", fontSize = 10.sp, color = Slate400)
                                    Text("${instantLatency}ms", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = Cyan400, fontSize = 13.sp)
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Slate950,
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Throughput", fontSize = 10.sp, color = Slate400)
                                    Text("$peakFps FPS", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = Emerald400, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                } else if (resultProfile != null) {
                    val p = resultProfile!!
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Emerald400.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, Emerald400.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("DEVICE TIER DETECTED", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Emerald400)
                                    Text(p.tierName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Slate100)
                                    Text("Avg Decode: ${p.avgLatencyMs}ms • Max FPS: ~${p.maxTheoreticalFps}", fontSize = 10.sp, color = Slate400)
                                }
                                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Emerald400, modifier = Modifier.size(28.dp))
                            }
                        }

                        Text("Recommended Optical Profile", fontWeight = FontWeight.Bold, color = Slate100, fontSize = 12.sp)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(shape = RoundedCornerShape(8.dp), color = Slate950, border = BorderStroke(1.dp, Slate800), modifier = Modifier.weight(1f)) {
                                Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Optimal Speed", fontSize = 9.sp, color = Slate400)
                                    Text("${p.recommendedFps} FPS", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = Cyan400, fontSize = 13.sp)
                                }
                            }
                            Surface(shape = RoundedCornerShape(8.dp), color = Slate950, border = BorderStroke(1.dp, Slate800), modifier = Modifier.weight(1f)) {
                                Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Chunk Size", fontSize = 9.sp, color = Slate400)
                                    Text("${p.recommendedChunkSize} B", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = Purple400, fontSize = 13.sp)
                                }
                            }
                            Surface(shape = RoundedCornerShape(8.dp), color = Slate950, border = BorderStroke(1.dp, Slate800), modifier = Modifier.weight(1f)) {
                                Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("ECC Level", fontSize = 9.sp, color = Slate400)
                                    Text("Level ${p.recommendedEcc}", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = Emerald400, fontSize = 13.sp)
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { runBenchmark() },
                                colors = ButtonDefaults.buttonColors(containerColor = Slate950, contentColor = Slate300),
                                border = BorderStroke(1.dp, Slate800),
                                modifier = Modifier.weight(0.4f)
                            ) {
                                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Re-Test", fontSize = 11.sp)
                            }

                            Button(
                                onClick = {
                                    onApplyProfile(p.recommendedFps, p.recommendedChunkSize, p.recommendedEcc)
                                    onDismiss()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Emerald400, contentColor = Slate950),
                                modifier = Modifier.weight(0.6f)
                            ) {
                                Text("Apply Profile", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
