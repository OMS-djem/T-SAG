package com.example.ui.components

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SiteAccessStatus
import com.example.ui.theme.AccessAccordedGreen
import com.example.ui.theme.AccessAccordedGreenBg
import com.example.ui.theme.AccessRefusedRed
import com.example.ui.theme.AccessRefusedRedBg
import com.example.ui.theme.TelecomNavyPrimary

data class PredefinedRefusalReason(
    val title: String,
    val subtitle: String,
    val fullStatement: String,
    val icon: ImageVector
)

val STANDARD_PRIMARY_REFUSAL_REASONS = listOf(
    PredefinedRefusalReason(
        title = "Guard Refused Entry",
        subtitle = "Security guard denied entry without written authorization or regional escort",
        fullStatement = "Site security guard refused entry; requested physical clearance permit and authorized NOC escort.",
        icon = Icons.Default.Security
    ),
    PredefinedRefusalReason(
        title = "Key / Padlock Missing or Changed",
        subtitle = "Lockbox empty, padlock replaced by third-party, or master key doesn't fit",
        fullStatement = "Site gate padlock replaced or master key missing from key lockbox; gate chained shut.",
        icon = Icons.Default.Key
    ),
    PredefinedRefusalReason(
        title = "Work Permit / Clearance Expired",
        subtitle = "Operator work permit, mission order, or regional security pass outdated",
        fullStatement = "Access authorization / regional work permit clearance expired or not issued for current work order.",
        icon = Icons.Default.Description
    ),
    PredefinedRefusalReason(
        title = "Landlord / Syndicate Dispute",
        subtitle = "Building owner or syndicate blocked rooftop/compound access due to lease dispute",
        fullStatement = "Building owner / syndicate refused access; demanded lease co-location rent annex documentation.",
        icon = Icons.Default.Apartment
    ),
    PredefinedRefusalReason(
        title = "Safety & Hazard Risk",
        subtitle = "Live electrical danger, severe storm damage, hornets/bees, or structural hazard",
        fullStatement = "Severe safety risk on-site; electrical short / structural hazard detected, making entrance unsafe.",
        icon = Icons.Default.Warning
    ),
    PredefinedRefusalReason(
        title = "Access Road Blocked / Flooded",
        subtitle = "Track flooded, bridge collapsed, or snow/landslide blocking vehicle access",
        fullStatement = "Site access track completely impassable due to flooding / landslide / heavy obstacle.",
        icon = Icons.Default.DirectionsCar
    ),
    PredefinedRefusalReason(
        title = "Escort Contact Absent",
        subtitle = "Key holder, military escort, or site coordinator unreachable/absent",
        fullStatement = "Designated site key holder and military/civil escort absent and unreachable via phone.",
        icon = Icons.Default.PersonOff
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusSelectorCard(
    currentStatus: SiteAccessStatus,
    onStatusSelected: (SiteAccessStatus) -> Unit,
    refusalReason: String,
    onRefusalReasonChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("mobiserve_custom_reasons", Context.MODE_PRIVATE) }

    var customReasons by remember {
        mutableStateOf<List<String>>(
            prefs.getStringSet("reasons", emptySet<String>())?.filterNotNull()?.sorted() ?: emptyList()
        )
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var newReasonText by remember { mutableStateOf("") }
    var isDropdownExpanded by remember { mutableStateOf(false) }

    fun addCustomReason(text: String) {
        val trimmed = text.trim()
        if (trimmed.isNotBlank()) {
            val updated = (customReasons + trimmed).distinct().sorted()
            customReasons = updated
            prefs.edit().putStringSet("reasons", updated.toSet()).apply()
            onRefusalReasonChanged(trimmed)
            newReasonText = ""
            showAddDialog = false
        }
    }

    fun removeCustomReason(text: String) {
        val updated = customReasons.filter { it != text }
        customReasons = updated
        prefs.edit().putStringSet("reasons", updated.toSet()).apply()
        if (refusalReason.equals(text, ignoreCase = true)) {
            onRefusalReasonChanged("")
        }
    }

    // Determine the matched predefined reason or custom reason title
    val matchedStandard = STANDARD_PRIMARY_REFUSAL_REASONS.firstOrNull {
        refusalReason.equals(it.title, ignoreCase = true) ||
        refusalReason.equals(it.fullStatement, ignoreCase = true)
    }

    val matchedCustom = customReasons.firstOrNull {
        refusalReason.equals(it, ignoreCase = true)
    }

    val displaySelectionText = when {
        matchedStandard != null -> matchedStandard.title
        matchedCustom != null -> matchedCustom
        refusalReason.isNotBlank() -> "Custom: ${if (refusalReason.length > 32) refusalReason.take(30) + "..." else refusalReason}"
        else -> "Select Primary Refusal Reason..."
    }

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
                text = "Select access outcome to prepare corresponding email template and logs",
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.WarningAmber,
                            contentDescription = null,
                            tint = AccessRefusedRed,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Primary Reason of Refused Access *",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = AccessRefusedRed
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        TextButton(
                            onClick = {
                                newReasonText = ""
                                showAddDialog = true
                            },
                            modifier = Modifier.testTag("action_add_custom_reason")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Custom Reason",
                                tint = TelecomNavyPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "+ Custom",
                                color = TelecomNavyPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Text(
                        text = "Select the primary cause of denial. This will be prominently highlighted in NOC alert emails and audit reports.",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    // 1. PRIMARY REASON DROPDOWN SELECTOR
                    ExposedDropdownMenuBox(
                        expanded = isDropdownExpanded,
                        onExpandedChange = { isDropdownExpanded = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("primary_refusal_reason_dropdown_box")
                    ) {
                        OutlinedTextField(
                            value = displaySelectionText,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Select Primary Refusal Reason *") },
                            leadingIcon = {
                                Icon(
                                    imageVector = matchedStandard?.icon ?: Icons.Default.WarningAmber,
                                    contentDescription = null,
                                    tint = AccessRefusedRed,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded)
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                                .testTag("primary_refusal_reason_dropdown_input")
                        )

                        ExposedDropdownMenu(
                            expanded = isDropdownExpanded,
                            onDismissRequest = { isDropdownExpanded = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Standard Telecommunication Reasons
                            STANDARD_PRIMARY_REFUSAL_REASONS.forEach { reason ->
                                DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(
                                            imageVector = reason.icon,
                                            contentDescription = null,
                                            tint = AccessRefusedRed,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    },
                                    text = {
                                        Column {
                                            Text(
                                                text = reason.title,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = reason.subtitle,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                lineHeight = 14.sp
                                            )
                                        }
                                    },
                                    onClick = {
                                        onRefusalReasonChanged(reason.fullStatement)
                                        isDropdownExpanded = false
                                    },
                                    modifier = Modifier.testTag("dropdown_item_${reason.title.replace(" ", "_")}")
                                )
                            }

                            // Saved Custom Reasons Section (if any exist)
                            if (customReasons.isNotEmpty()) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "SAVED CUSTOM REASONS",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        color = TelecomNavyPrimary,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                                        letterSpacing = 1.sp
                                    )
                                }

                                customReasons.forEach { custom ->
                                    DropdownMenuItem(
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.EditNote,
                                                contentDescription = null,
                                                tint = TelecomNavyPrimary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        },
                                        text = {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = custom,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                IconButton(
                                                    onClick = { removeCustomReason(custom) },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Close,
                                                        contentDescription = "Delete custom reason",
                                                        tint = Color.Gray,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            onRefusalReasonChanged(custom)
                                            isDropdownExpanded = false
                                        }
                                    )
                                }
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            // Action to Add New Custom Reason
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = TelecomNavyPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                text = {
                                    Text(
                                        text = "+ Enter New Custom Reason...",
                                        fontWeight = FontWeight.Bold,
                                        color = TelecomNavyPrimary,
                                        fontSize = 13.sp
                                    )
                                },
                                onClick = {
                                    isDropdownExpanded = false
                                    newReasonText = ""
                                    showAddDialog = true
                                },
                                modifier = Modifier.testTag("dropdown_item_add_custom")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 2. EDITABLE / DETAILED REFUSAL REASON TEXT FIELD
                    OutlinedTextField(
                        value = refusalReason,
                        onValueChange = onRefusalReasonChanged,
                        label = { Text("Specific Denial Reason / Contact Attempted") },
                        placeholder = { Text("e.g. Guard Rachid stated authorization letter from HQ was not received...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("refusal_reason_input"),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = false,
                        maxLines = 4,
                        supportingText = {
                            // If user typed a custom reason not in standard or custom list, offer 1-tap save
                            val trimmed = refusalReason.trim()
                            val isAlreadySaved = STANDARD_PRIMARY_REFUSAL_REASONS.any {
                                it.title.equals(trimmed, ignoreCase = true) || it.fullStatement.equals(trimmed, ignoreCase = true)
                            } || customReasons.any { it.equals(trimmed, ignoreCase = true) }

                            if (trimmed.isNotBlank() && !isAlreadySaved) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clickable { addCustomReason(trimmed) }
                                        .padding(top = 2.dp)
                                        .testTag("action_save_typed_reason")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.BookmarkAdd,
                                        contentDescription = null,
                                        tint = TelecomNavyPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Save as reusable custom reason",
                                        color = TelecomNavyPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    // Add Custom Denial Reason Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = AccessRefusedRed,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Custom Denial Reason", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column {
                    Text(
                        text = "Enter a primary refusal reason. It will be added to your custom reason list, selectable from the dropdown, and included in escalation emails.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newReasonText,
                        onValueChange = { newReasonText = it },
                        placeholder = { Text("e.g. Access road flooded, Generator out of fuel...") },
                        label = { Text("Custom Refusal Reason *") },
                        singleLine = false,
                        maxLines = 3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_reason_dialog_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { addCustomReason(newReasonText) },
                    enabled = newReasonText.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = AccessRefusedRed),
                    modifier = Modifier.testTag("confirm_add_custom_reason_button")
                ) {
                    Text("Add & Select")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
