package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SiteAccessStatus
import com.example.ui.theme.AccessAccordedGreen
import com.example.ui.theme.AccessAccordedGreenBg
import com.example.ui.theme.AccessRefusedRed
import com.example.ui.theme.AccessRefusedRedBg

@Composable
fun StatusSelectorCard(
    currentStatus: SiteAccessStatus,
    onStatusSelected: (SiteAccessStatus) -> Unit,
    refusalReason: String,
    onRefusalReasonChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val commonRefusalReasons = listOf(
        "Guard Refused Entry",
        "Key / Padlock Missing or Changed",
        "Owner / Landlord Dispute",
        "Work Permit Clearance Expired",
        "Site Inaccessible / Gate Chained",
        "Power / Hazard Safety Risk"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("status_selector_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Site Access Decision",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Select access outcome to prepare corresponding email template",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Access Accorded Option
                val isAccorded = currentStatus == SiteAccessStatus.ACCORDED
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onStatusSelected(SiteAccessStatus.ACCORDED) }
                        .testTag("select_status_accorded"),
                    shape = RoundedCornerShape(12.dp),
                    color = if (isAccorded) AccessAccordedGreenBg else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    border = BorderStroke(
                        width = if (isAccorded) 2.dp else 1.dp,
                        color = if (isAccorded) AccessAccordedGreen else Color.LightGray.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 14.dp, horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(if (isAccorded) AccessAccordedGreen else Color.Gray.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Access Accorded",
                                tint = if (isAccorded) Color.White else Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "ACCORDED",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (isAccorded) AccessAccordedGreen else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Access Granted",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = if (isAccorded) AccessAccordedGreen.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // No Access Option
                val isRefused = currentStatus == SiteAccessStatus.REFUSED
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onStatusSelected(SiteAccessStatus.REFUSED) }
                        .testTag("select_status_refused"),
                    shape = RoundedCornerShape(12.dp),
                    color = if (isRefused) AccessRefusedRedBg else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    border = BorderStroke(
                        width = if (isRefused) 2.dp else 1.dp,
                        color = if (isRefused) AccessRefusedRed else Color.LightGray.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 14.dp, horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(if (isRefused) AccessRefusedRed else Color.Gray.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cancel,
                                contentDescription = "No Access",
                                tint = if (isRefused) Color.White else Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "NO ACCESS",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (isRefused) AccessRefusedRed else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Denied / Blocked",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = if (isRefused) AccessRefusedRed.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Expanded Refusal Details
            AnimatedVisibility(
                visible = currentStatus == SiteAccessStatus.REFUSED,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.WarningAmber,
                            contentDescription = null,
                            tint = AccessRefusedRed,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Primary Reason for Refusal (Included in Escalation Email)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = AccessRefusedRed
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(commonRefusalReasons) { reason ->
                            val selected = refusalReason.equals(reason, ignoreCase = true)
                            FilterChip(
                                selected = selected,
                                onClick = { onRefusalReasonChanged(reason) },
                                label = { Text(reason, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AccessRefusedRed.copy(alpha = 0.15f),
                                    selectedLabelColor = AccessRefusedRed
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = refusalReason,
                        onValueChange = onRefusalReasonChanged,
                        label = { Text("Specific Denial Reason / Contact Attempted") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("refusal_reason_input"),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = false,
                        maxLines = 3
                    )
                }
            }
        }
    }
}
