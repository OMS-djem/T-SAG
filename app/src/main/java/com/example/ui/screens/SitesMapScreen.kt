package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TelecomSiteEntity
import com.example.location.ProximityHelper
import com.example.location.ProximityInfo
import com.example.location.ProximityLevel
import com.example.ui.components.TelecomMapComponent
import com.example.ui.theme.AccessAccordedGreen
import com.example.ui.theme.AccessRefusedRed
import com.example.ui.theme.TelecomBlueAccent
import com.example.ui.theme.TelecomNavyPrimary
import com.example.ui.viewmodel.SiteAccessViewModel

@Composable
fun SitesMapScreen(
    viewModel: SiteAccessViewModel,
    onNavigateToAudit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val technicianGps by viewModel.gpsData.collectAsState()
    val allSites by viewModel.telecomSites.collectAsState()
    val selectedSite by viewModel.selectedSiteForMap.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedOperatorFilter by remember { mutableStateOf("ALL") }
    var isListExpanded by remember { mutableStateOf(false) }
    var showAddSiteDialog by remember { mutableStateOf(false) }

    // Filter sites according to search query and operator chip
    val filteredSites by remember(allSites, searchQuery, selectedOperatorFilter, technicianGps) {
        derivedStateOf {
            allSites
                .filter { site ->
                    val matchesSearch = searchQuery.isBlank() ||
                            site.siteId.contains(searchQuery, ignoreCase = true) ||
                            site.siteName.contains(searchQuery, ignoreCase = true) ||
                            site.operator.contains(searchQuery, ignoreCase = true) ||
                            site.address.contains(searchQuery, ignoreCase = true)

                    val matchesOperator = when (selectedOperatorFilter) {
                        "ALL" -> true
                        "ACCORDED" -> site.accessStatus.equals("ACCORDED", ignoreCase = true)
                        "REFUSED" -> site.accessStatus.equals("REFUSED", ignoreCase = true)
                        else -> site.operator.contains(selectedOperatorFilter, ignoreCase = true)
                    }

                    matchesSearch && matchesOperator
                }
                .sortedBy { site ->
                    // Sort nearest to technician first
                    val results = FloatArray(1)
                    android.location.Location.distanceBetween(
                        technicianGps.latitude,
                        technicianGps.longitude,
                        site.latitude,
                        site.longitude,
                        results
                    )
                    results[0]
                }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("sites_map_screen")
    ) {
        // Main Map Canvas
        TelecomMapComponent(
            technicianGps = technicianGps,
            sites = filteredSites,
            selectedSite = selectedSite,
            onSiteSelected = { site ->
                viewModel.selectSiteForMap(site)
                isListExpanded = false
            },
            modifier = Modifier.fillMaxSize()
        )

        // Top Overlay: Search & Operator Filter Chips
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .align(Alignment.TopCenter)
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                shadowElevation = 6.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    // Search Bar & Add Site button
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search site ID, name, operator...", fontSize = 13.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = TelecomBlueAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotBlank()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("map_search_input")
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = { showAddSiteDialog = true },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(TelecomNavyPrimary)
                                .testTag("map_add_site_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Telecom Site", tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Horizontal Filter Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val filters = listOf(
                            "ALL" to "All (${allSites.size})",
                            "Mobiserve" to "Mobiserve",
                            "Djezzy" to "Djezzy",
                            "Mobilis" to "Mobilis",
                            "Ooredoo" to "Ooredoo",
                            "ACCORDED" to "Accorded",
                            "REFUSED" to "Refused"
                        )

                        filters.forEach { (code, label) ->
                            val isSelected = selectedOperatorFilter == code
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedOperatorFilter = code },
                                label = { Text(label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = TelecomBlueAccent.copy(alpha = 0.2f),
                                    selectedLabelColor = TelecomNavyPrimary
                                )
                            )
                        }
                    }
                }
            }
        }

        // Bottom Overlay: Selected Site Card OR Proximity List Drawer
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            // Selected Site Bottom Card
            AnimatedVisibility(
                visible = selectedSite != null,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                selectedSite?.let { site ->
                    val proximity = ProximityHelper.calculateProximity(
                        technicianGps.latitude,
                        technicianGps.longitude,
                        site.latitude,
                        site.longitude
                    )

                    SelectedSiteCard(
                        site = site,
                        proximity = proximity,
                        onAudit = {
                            viewModel.selectSiteForAudit(site)
                            onNavigateToAudit()
                        },
                        onDirections = {
                            launchDirections(context, site.latitude, site.longitude, site.siteName)
                        },
                        onDismiss = {
                            viewModel.selectSiteForMap(null)
                        }
                    )
                }
            }

            // Proximity Sites List Drawer (Expandable)
            AnimatedVisibility(visible = selectedSite == null) {
                ProximitySitesDrawer(
                    sites = filteredSites,
                    technicianGps = technicianGps,
                    isExpanded = isListExpanded,
                    onToggleExpand = { isListExpanded = !isListExpanded },
                    onSiteSelected = { site ->
                        viewModel.selectSiteForMap(site)
                    }
                )
            }
        }
    }

    // Add New Telecom Site Dialog
    if (showAddSiteDialog) {
        AddSiteDialog(
            defaultLat = technicianGps.latitude.takeIf { it != 0.0 } ?: 36.753820,
            defaultLng = technicianGps.longitude.takeIf { it != 0.0 } ?: 3.058810,
            onDismiss = { showAddSiteDialog = false },
            onAddSite = { newSite ->
                viewModel.addNewTelecomSite(newSite)
                showAddSiteDialog = false
            }
        )
    }
}

@Composable
private fun SelectedSiteCard(
    site: TelecomSiteEntity,
    proximity: ProximityInfo,
    onAudit: () -> Unit,
    onDirections: () -> Unit,
    onDismiss: () -> Unit
) {
    val statusColor = when (site.accessStatus.uppercase()) {
        "ACCORDED" -> AccessAccordedGreen
        "REFUSED" -> AccessRefusedRed
        else -> Color(0xFFD97706)
    }

    val proximityColor = when (proximity.proximityLevel) {
        ProximityLevel.IMMEDIATE -> AccessAccordedGreen
        ProximityLevel.NEARBY -> TelecomBlueAccent
        ProximityLevel.COMMUTE -> Color(0xFF4F46E5)
        ProximityLevel.REGIONAL -> Color(0xFF64748B)
    }

    Card(
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("selected_site_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Site ID, Status Badge, Close
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = site.siteId,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = statusColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = site.accessStatus.uppercase(),
                        color = statusColor,
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close Site Details")
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${site.operator} • ${site.siteName}",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (site.address.isNotBlank()) {
                Text(
                    text = site.address,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Proximity Badge Banner
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = proximityColor.copy(alpha = 0.10f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.NearMe,
                        contentDescription = "Proximity",
                        tint = proximityColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${proximity.formattedDistance} away",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = proximityColor
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "• ${proximity.cardinalDirection}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "${proximity.proximityLevel.label} • ${proximity.etaDescription}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Stored GPS Coordinates & Site Type
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "GPS: %.6f, %.6f".format(site.latitude, site.longitude),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = site.siteType,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = TelecomNavyPrimary
                )
            }

            if (site.accessInstructions.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = site.accessInstructions,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons: Audit Site & Get Directions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onAudit,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TelecomNavyPrimary),
                    modifier = Modifier
                        .weight(1.3f)
                        .testTag("action_audit_selected_site")
                ) {
                    Icon(Icons.Default.CellTower, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Audit This Site", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onDirections,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("action_directions_site")
                ) {
                    Icon(Icons.Default.Directions, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Directions")
                }
            }
        }
    }
}

@Composable
private fun ProximitySitesDrawer(
    sites: List<TelecomSiteEntity>,
    technicianGps: com.example.data.model.GpsData,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onSiteSelected: (TelecomSiteEntity) -> Unit
) {
    Card(
        shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("proximity_sites_drawer")
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            // Drag Handle & Drawer Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpand)
                    .padding(vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.NearMe,
                    contentDescription = null,
                    tint = TelecomBlueAccent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Nearby Telecom Sites (${sites.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                    contentDescription = if (isExpanded) "Collapse" else "Expand"
                )
            }

            // Sites list
            val displayHeight = if (isExpanded) 260.dp else 90.dp
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = displayHeight)
            ) {
                items(sites, key = { it.siteId }) { site ->
                    val proximity = ProximityHelper.calculateProximity(
                        technicianGps.latitude,
                        technicianGps.longitude,
                        site.latitude,
                        site.longitude
                    )
                    val statusColor = when (site.accessStatus.uppercase()) {
                        "ACCORDED" -> AccessAccordedGreen
                        "REFUSED" -> AccessRefusedRed
                        else -> Color(0xFFD97706)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSiteSelected(site) }
                            .padding(vertical = 8.dp, horizontal = 4.dp)
                            .testTag("proximity_item_${site.siteId}")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = site.siteId,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "• ${site.operator}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = site.siteName,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Proximity distance chip
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = TelecomNavyPrimary.copy(alpha = 0.08f),
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Text(
                                text = "${proximity.formattedDistance} ${proximity.cardinalDirection.substringBefore(" ")}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TelecomNavyPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddSiteDialog(
    defaultLat: Double,
    defaultLng: Double,
    onDismiss: () -> Unit,
    onAddSite: (TelecomSiteEntity) -> Unit
) {
    var siteId by remember { mutableStateOf("MOB-DZ-") }
    var siteName by remember { mutableStateOf("") }
    var operator by remember { mutableStateOf("Mobiserve") }
    var siteType by remember { mutableStateOf("Rooftop Tower") }
    var latText by remember { mutableStateOf("%.6f".format(defaultLat)) }
    var lngText by remember { mutableStateOf("%.6f".format(defaultLng)) }
    var address by remember { mutableStateOf("") }
    var accessInstructions by remember { mutableStateOf("Security gate clearance required") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Telecom Site to Map", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = siteId,
                    onValueChange = { siteId = it },
                    label = { Text("Site ID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = siteName,
                    onValueChange = { siteName = it },
                    label = { Text("Site Name / Station") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = operator,
                    onValueChange = { operator = it },
                    label = { Text("Operator") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = latText,
                        onValueChange = { latText = it },
                        label = { Text("Latitude") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = lngText,
                        onValueChange = { lngText = it },
                        label = { Text("Longitude") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address / Location") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val lat = latText.toDoubleOrNull() ?: defaultLat
                    val lng = lngText.toDoubleOrNull() ?: defaultLng
                    onAddSite(
                        TelecomSiteEntity(
                            siteId = siteId.trim(),
                            siteName = siteName.ifBlank { "Telecom Tower" },
                            operator = operator,
                            latitude = lat,
                            longitude = lng,
                            address = address,
                            siteType = siteType,
                            accessStatus = "PENDING",
                            accessInstructions = accessInstructions
                        )
                    )
                },
                enabled = siteId.isNotBlank()
            ) {
                Text("Add Site")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun launchDirections(context: Context, latitude: Double, longitude: Double, label: String) {
    try {
        val gmmIntentUri = Uri.parse("google.navigation:q=$latitude,$longitude&mode=d")
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
            setPackage("com.google.android.apps.maps")
        }
        if (mapIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(mapIntent)
            return
        }
    } catch (_: Exception) {}

    // Fallback to universal web/app intent
    try {
        val browserUri = Uri.parse("https://maps.google.com/?q=$latitude,$longitude")
        val browserIntent = Intent(Intent.ACTION_VIEW, browserUri)
        context.startActivity(browserIntent)
    } catch (_: Exception) {}
}
