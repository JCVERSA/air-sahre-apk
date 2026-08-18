package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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

@Composable
fun DocumentationDialog(
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Slate950,
            border = androidx.compose.foundation.BorderStroke(1.dp, Slate800),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = Cyan400,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AirQR Protocol Specs",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Slate100
                            )
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Slate400
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                DocSection(
                    icon = Icons.Default.WifiOff,
                    title = "100% Air-Gapped Optical P2P",
                    description = "AirQR uses zero radio frequency (No Wi-Fi, No Bluetooth, No NFC, No Cellular). Files are converted to high-speed optical QR code streams emitted on the sender's display and decoded by the receiver's camera in real-time."
                )

                Spacer(modifier = Modifier.height(12.dp))

                DocSection(
                    icon = Icons.Default.Speed,
                    title = "AIR2 Protocol & Base45 Encoding",
                    description = "AIR2 utilizes RFC 9285 Base45 encoding combined with GZIP compression to activate the QR code alphanumeric mode. This reduces symbol matrix density by ~45%, enabling 15-25 frames per second optical throughput."
                )

                Spacer(modifier = Modifier.height(12.dp))

                DocSection(
                    icon = Icons.Default.Security,
                    title = "SHA-256 Cryptographic Integrity",
                    description = "Every file transfer calculates a SHA-256 hash prior to streaming. Upon receiving all chunks, the receiver independently reassembles the payload and verifies the cryptographic hash against the metadata header."
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Packet Wire Format:",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = Purple400,
                        fontWeight = FontWeight.Bold
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Slate900, RoundedCornerShape(8.dp))
                        .border(1.dp, Slate800, RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "AIR2:M:<id>:<size>:<chunks>:<chunkSize>:<sha256>:<name>:<type>:<comp>:<compSize>:<enc>",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Cyan400
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "AIR2:C:<id>:<index>:<total>:<enc>:<payload>",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Emerald400
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "AIR2:FB:<id>:<quality>:<failureRate>:<lighting>:<fps>:<chunkSize>:<ecc>:<missing>",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Purple400
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Cyan400, contentColor = Slate950)
                ) {
                    Text("Got It", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun DocSection(icon: ImageVector, title: String, description: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Cyan400,
            modifier = Modifier.size(18.dp).padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Slate100
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Slate400,
                    lineHeight = 16.sp
                )
            )
        }
    }
}
