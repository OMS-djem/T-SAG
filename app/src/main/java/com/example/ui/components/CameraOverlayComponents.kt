package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.model.GpsData
import com.example.data.model.GsmData
import com.example.data.model.SiteAccessStatus
import com.example.data.model.SitePhoto
import com.example.ui.theme.TelecomBlueAccent
import com.example.ui.theme.TelecomGreen
import com.example.ui.theme.TelecomNavyPrimary
import com.example.ui.theme.TelecomRed
import java.io.File
import java.util.Locale

@Composable
fun CameraTopBar(
    liveTime: String,
    isTorchEnabled: Boolean,
    onClose: () -> Unit,
    onToggleTorch: () -> Unit,
    onFlipCamera: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.Black.copy(alpha = 0.65f),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier.testTag("camera_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to Audit",
                    tint = Color.White
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "CAMERAX TELECOM HUD",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = TelecomBlueAccent,
                    letterSpacing = 1.sp
                )
                Text(
                    text = liveTime.ifBlank { "Syncing clock..." },
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onToggleTorch,
                    modifier = Modifier.testTag("camera_torch_button")
                ) {
                    Icon(
                        imageVector = if (isTorchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = "Toggle Torch",
                        tint = if (isTorchEnabled) Color(0xFFFBBF24) else Color.White
                    )
                }

                IconButton(
                    onClick = onFlipCamera,
                    modifier = Modifier.testTag("camera_switch_lens_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FlipCameraAndroid,
                        contentDescription = "Flip Camera",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun CameraTagRow(
    tags: List<String>,
    selectedTag: String,
    onSelectTag: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.Black.copy(alpha = 0.45f),
        modifier = modifier.fillMaxWidth()
    ) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(tags) { tag ->
                val isSelected = selectedTag == tag
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelectTag(tag) },
                    label = {
                        Text(
                            text = tag,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = TelecomNavyPrimary,
                        selectedLabelColor = Color.White,
                        containerColor = Color.White.copy(alpha = 0.15f),
                        labelColor = Color.White
                    )
                )
            }
        }
    }
}

@Composable
fun CameraTelemetryHudCard(
    siteId: String,
    operator: String,
    activeTag: String,
    status: SiteAccessStatus,
    gpsData: GpsData,
    gsmData: GsmData,
    liveTime: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("telemetry_hud_card"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xDD0A1423))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MOBISERVE HOLDING • SITE ACCESS AUDIT",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = TelecomBlueAccent,
                    fontSize = 10.sp
                )

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (status == SiteAccessStatus.ACCORDED) TelecomGreen else TelecomRed
                ) {
                    Text(
                        text = if (status == SiteAccessStatus.ACCORDED) "ACCORDED ✓" else "NO ACCESS ✕",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "SITE: ${siteId.ifBlank { "SITE-UNKNOWN" }}  |  OP: ${operator.ifBlank { "N/A" }}  |  TAG: $activeTag",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.GpsFixed,
                    contentDescription = null,
                    tint = Color(0xFF34D399),
                    modifier = Modifier.size(11.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "GPS: ${gpsData.coordinatesString} (±${String.format(Locale.US, "%.1f", gpsData.accuracy)}m)  |  ALT: ${String.format(Locale.US, "%.0f", gpsData.altitude)}m",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.5.sp,
                    color = Color(0xFF34D399),
                    fontWeight = FontWeight.Medium
                )
            }

            Text(
                text = "GSM: ${gsmData.networkType}  |  CID: ${gsmData.cellId}  |  LAC: ${gsmData.lacTac}  |  ${gsmData.signalDbm} dBm",
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                color = Color(0xFFCBD5E1),
                modifier = Modifier.padding(top = 2.dp)
            )

            Text(
                text = "TIMESTAMP: $liveTime",
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                color = Color(0xFF94A3B8),
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
fun CameraBottomControlsBar(
    photosCount: Int,
    lastPhoto: SitePhoto?,
    isProcessing: Boolean,
    onTakePhoto: () -> Unit,
    onOpenPreview: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.Black.copy(alpha = 0.85f),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.5.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .background(Color.DarkGray)
                    .clickable { onOpenPreview() }
                    .testTag("camera_thumbnail_preview"),
                contentAlignment = Alignment.Center
            ) {
                if (lastPhoto != null) {
                    AsyncImage(
                        model = File(lastPhoto.filePath),
                        contentDescription = "Last Stamped Photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .background(TelecomNavyPrimary, RoundedCornerShape(bottomStart = 6.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "$photosCount",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = "No photos yet",
                        tint = Color.LightGray,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .border(4.dp, Color.White, CircleShape)
                    .clickable(enabled = !isProcessing) { onTakePhoto() }
                    .testTag("shutter_button"),
                contentAlignment = Alignment.Center
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(36.dp),
                        color = Color.White,
                        strokeWidth = 3.dp
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                }
            }

            Button(
                onClick = onDone,
                colors = ButtonDefaults.buttonColors(containerColor = TelecomNavyPrimary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("camera_done_button")
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Done", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun CameraPhotoPreviewDialog(
    photo: SitePhoto,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("stamped_photo_dialog"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Stamped Evidence Photo",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Tag: ${photo.tag} • Lat: ${String.format(Locale.US, "%.4f", photo.latitude)}, Lon: ${String.format(Locale.US, "%.4f", photo.longitude)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close Preview")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black)
                ) {
                    AsyncImage(
                        model = File(photo.filePath),
                        contentDescription = "Geo-stamped site photo preview",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "✓ GPS coordinates, GSM cell telemetry, and timestamp are burned into this image.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TelecomGreen,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = TelecomNavyPrimary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close Preview")
                }
            }
        }
    }
}
