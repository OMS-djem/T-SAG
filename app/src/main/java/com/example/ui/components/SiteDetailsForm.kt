package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SiteDetailsForm(
    siteId: String,
    onSiteIdChanged: (String) -> Unit,
    siteName: String,
    onSiteNameChanged: (String) -> Unit,
    operator: String,
    onOperatorChanged: (String) -> Unit,
    technicianName: String,
    onTechnicianNameChanged: (String) -> Unit,
    technicianPhone: String,
    onTechnicianPhoneChanged: (String) -> Unit,
    notes: String,
    onNotesChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val operatorPresets = listOf("Mobiserve", "Djezzy", "Mobilis", "Ooredoo", "Orange", "Vodafone")

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("site_details_form"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Site & Field Information",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Telecom site identification and technician details",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = siteId,
                    onValueChange = onSiteIdChanged,
                    label = { Text("Site ID") },
                    placeholder = { Text("e.g. MOB-DZ-1044") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("site_id_input"),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = siteName,
                    onValueChange = onSiteNameChanged,
                    label = { Text("Site Name / Location") },
                    placeholder = { Text("e.g. Hydra Central") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1.2f)
                        .testTag("site_name_input"),
                    shape = RoundedCornerShape(10.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Operator / Client:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(operatorPresets) { op ->
                    FilterChip(
                        selected = operator.contains(op, ignoreCase = true),
                        onClick = { onOperatorChanged(op) },
                        label = { Text(op, fontSize = 11.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = technicianName,
                    onValueChange = onTechnicianNameChanged,
                    label = { Text("Technician") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("technician_name_input"),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = technicianPhone,
                    onValueChange = onTechnicianPhoneChanged,
                    label = { Text("Phone") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("technician_phone_input"),
                    shape = RoundedCornerShape(10.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = notes,
                onValueChange = onNotesChanged,
                label = { Text("Field Observations & Site Conditions") },
                placeholder = { Text("Access condition, gate status, locks, safety check, work description...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("notes_input"),
                shape = RoundedCornerShape(10.dp),
                maxLines = 3
            )
        }
    }
}
