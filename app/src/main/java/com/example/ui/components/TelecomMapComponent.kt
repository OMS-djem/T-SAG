package com.example.ui.components

import android.content.Context
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.GpsData
import com.example.data.model.TelecomSiteEntity
import com.example.location.ProximityHelper
import com.example.ui.theme.AccessAccordedGreen
import com.example.ui.theme.AccessRefusedRed
import com.example.ui.theme.TelecomBlueAccent
import com.example.ui.theme.TelecomNavyPrimary
import kotlin.math.PI
import kotlin.math.asinh
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sinh
import kotlin.math.tan

enum class MapTileSource(val label: String, val baseUrl: String, val maxZoom: Int) {
    VOYAGER(
        "Carto Voyager (Clean)",
        "https://a.basemaps.cartocdn.com/rastertiles/voyager",
        19
    ),
    OSM_STANDARD(
        "OpenStreetMap",
        "https://tile.openstreetmap.org",
        19
    ),
    DARK_NOC(
        "NOC Dark Telemetry",
        "https://a.basemaps.cartocdn.com/dark_all",
        19
    )
}

/**
 * Web Mercator projection helpers for OpenStreetMap Slippy Tiles.
 */
object MercatorProjection {
    const val TILE_SIZE = 256

    fun lonToPixelX(lon: Double, zoom: Float): Double {
        val n = Math.pow(2.0, zoom.toDouble())
        return ((lon + 180.0) / 360.0 * n) * TILE_SIZE
    }

    fun latToPixelY(lat: Double, zoom: Float): Double {
        val latRad = Math.toRadians(lat.coerceIn(-85.0511, 85.0511))
        val n = Math.pow(2.0, zoom.toDouble())
        return ((1.0 - asinh(tan(latRad)) / PI) / 2.0 * n) * TILE_SIZE
    }

    fun pixelXToLon(px: Double, zoom: Float): Double {
        val n = Math.pow(2.0, zoom.toDouble())
        return (px / TILE_SIZE / n) * 360.0 - 180.0
    }

    fun pixelYToLat(py: Double, zoom: Float): Double {
        val n = Math.pow(2.0, zoom.toDouble())
        val latRad = atan(sinh(PI * (1.0 - 2.0 * py / TILE_SIZE / n)))
        return Math.toDegrees(latRad)
    }
}

@Composable
fun TelecomMapComponent(
    technicianGps: GpsData,
    sites: List<TelecomSiteEntity>,
    selectedSite: TelecomSiteEntity?,
    onSiteSelected: (TelecomSiteEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    // Map Viewport state
    var centerLat by remember { mutableStateOf(technicianGps.latitude.takeIf { it != 0.0 } ?: 36.753820) }
    var centerLng by remember { mutableStateOf(technicianGps.longitude.takeIf { it != 0.0 } ?: 3.058810) }
    var zoomLevel by remember { mutableFloatStateOf(14.5f) }
    var tileSource by remember { mutableStateOf(MapTileSource.VOYAGER) }
    var showLayersMenu by remember { mutableStateOf(false) }

    // Synchronize to technician GPS when location becomes available initially
    LaunchedEffect(technicianGps.latitude, technicianGps.longitude) {
        if (technicianGps.latitude != 0.0 && technicianGps.longitude != 0.0) {
            if (centerLat == 36.753820 && centerLng == 3.058810 && zoomLevel == 14.5f) {
                centerLat = technicianGps.latitude
                centerLng = technicianGps.longitude
            }
        }
    }

    // When an external site selection changes, center map on that site
    LaunchedEffect(selectedSite?.siteId) {
        selectedSite?.let {
            centerLat = it.latitude
            centerLng = it.longitude
            if (zoomLevel < 15f) zoomLevel = 15.5f
        }
    }

    // Pulse animation for technician GPS beacon
    val infiniteTransition = rememberInfiniteTransition(label = "RadarPulse")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 12f,
        targetValue = 44f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseRadius"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseAlpha"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .testTag("telecom_map_container")
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoomFactor, _ ->
                    // Apply zoom change
                    val newZoom = (zoomLevel + (zoomFactor - 1.0f) * 1.5f).coerceIn(4f, 18.5f)
                    zoomLevel = newZoom

                    // Apply pan in pixels converted back to geographical center
                    val currentPxX = MercatorProjection.lonToPixelX(centerLng, newZoom)
                    val currentPxY = MercatorProjection.latToPixelY(centerLat, newZoom)
                    val newPxX = currentPxX - pan.x
                    val newPxY = currentPxY - pan.y
                    centerLng = MercatorProjection.pixelXToLon(newPxX, newZoom).coerceIn(-180.0, 180.0)
                    centerLat = MercatorProjection.pixelYToLat(newPxY, newZoom).coerceIn(-85.0, 85.0)
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        zoomLevel = (zoomLevel + 1.0f).coerceAtMost(18.5f)
                    }
                )
            }
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()
        val centerX = widthPx / 2f
        val centerY = heightPx / 2f

        val intZoom = zoomLevel.toInt().coerceIn(0, 19)
        val zoomScale = Math.pow(2.0, (zoomLevel - intZoom).toDouble()).toFloat()

        // Center pixel coordinate at current integer zoom level
        val centerIntPxX = MercatorProjection.lonToPixelX(centerLng, intZoom.toFloat())
        val centerIntPxY = MercatorProjection.latToPixelY(centerLat, intZoom.toFloat())

        // Calculate tile range to cover screen
        val halfW = (widthPx / 2f) / zoomScale
        val halfH = (heightPx / 2f) / zoomScale
        val minX = ((centerIntPxX - halfW) / MercatorProjection.TILE_SIZE).toInt() - 1
        val maxX = ((centerIntPxX + halfW) / MercatorProjection.TILE_SIZE).toInt() + 1
        val minY = ((centerIntPxY - halfH) / MercatorProjection.TILE_SIZE).toInt() - 1
        val maxY = ((centerIntPxY + halfH) / MercatorProjection.TILE_SIZE).toInt() + 1
        val maxTileIndex = (1 shl intZoom) - 1

        // Base Layer: GIS Coordinate Grid & Fallback Background
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(color = if (tileSource == MapTileSource.DARK_NOC) Color(0xFF0F172A) else Color(0xFFF1F5F9))

            // Geographic grid lines (every 100 pixels)
            val gridStep = 80.dp.toPx()
            var x = centerX % gridStep
            while (x < size.width) {
                drawLine(
                    color = if (tileSource == MapTileSource.DARK_NOC) Color(0xFF1E293B) else Color(0xFFE2E8F0),
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 1f
                )
                x += gridStep
            }
            var y = centerY % gridStep
            while (y < size.height) {
                drawLine(
                    color = if (tileSource == MapTileSource.DARK_NOC) Color(0xFF1E293B) else Color(0xFFE2E8F0),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f
                )
                y += gridStep
            }
        }

        // Layer 1: Slippy Map Tiles (OpenStreetMap / CartoDB)
        Box(modifier = Modifier.fillMaxSize()) {
            for (tx in minX..maxX) {
                val wrappedTx = (tx % (maxTileIndex + 1) + (maxTileIndex + 1)) % (maxTileIndex + 1)
                for (ty in minY..maxY) {
                    if (ty in 0..maxTileIndex) {
                        val tileLeftInt = tx * MercatorProjection.TILE_SIZE
                        val tileTopInt = ty * MercatorProjection.TILE_SIZE
                        val screenTileX = centerX + ((tileLeftInt - centerIntPxX) * zoomScale).toFloat()
                        val screenTileY = centerY + ((tileTopInt - centerIntPxY) * zoomScale).toFloat()
                        val tileDisplaySize = (MercatorProjection.TILE_SIZE * zoomScale).roundToInt()

                        val tileUrl = "${tileSource.baseUrl}/$intZoom/$wrappedTx/$ty.png"

                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(tileUrl)
                                .addHeader("User-Agent", "MobiserveTelecomSiteAccessApp/1.0 (Android; Telecom Field Operations)")
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.FillBounds,
                            modifier = Modifier
                                .offset { IntOffset(screenTileX.roundToInt(), screenTileY.roundToInt()) }
                                .size(with(density) { tileDisplaySize.toDp() })
                        )
                    }
                }
            }
        }

        // Layer 2: Distance Vector & Bearing Lines on Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val techScreenX = centerX + (MercatorProjection.lonToPixelX(technicianGps.longitude, zoomLevel) -
                    MercatorProjection.lonToPixelX(centerLng, zoomLevel)).toFloat()
            val techScreenY = centerY + (MercatorProjection.latToPixelY(technicianGps.latitude, zoomLevel) -
                    MercatorProjection.latToPixelY(centerLat, zoomLevel)).toFloat()

            // If a site is selected, draw proximity connector line
            selectedSite?.let { site ->
                val siteScreenX = centerX + (MercatorProjection.lonToPixelX(site.longitude, zoomLevel) -
                        MercatorProjection.lonToPixelX(centerLng, zoomLevel)).toFloat()
                val siteScreenY = centerY + (MercatorProjection.latToPixelY(site.latitude, zoomLevel) -
                        MercatorProjection.latToPixelY(centerLat, zoomLevel)).toFloat()

                // Dashed proximity connection line
                drawLine(
                    color = TelecomBlueAccent,
                    start = Offset(techScreenX, techScreenY),
                    end = Offset(siteScreenX, siteScreenY),
                    strokeWidth = 3.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 10f), 0f)
                )

                // Highlight circle around selected site
                drawCircle(
                    color = TelecomBlueAccent.copy(alpha = 0.25f),
                    radius = 32.dp.toPx(),
                    center = Offset(siteScreenX, siteScreenY)
                )
            }

            // Radar pulse ring around technician's location
            drawCircle(
                color = TelecomBlueAccent.copy(alpha = pulseAlpha),
                radius = pulseRadius * density.density,
                center = Offset(techScreenX, techScreenY),
                style = Stroke(width = 2.dp.toPx())
            )
        }

        // Layer 3: Site Markers
        sites.forEach { site ->
            val sitePxX = MercatorProjection.lonToPixelX(site.longitude, zoomLevel)
            val sitePxY = MercatorProjection.latToPixelY(site.latitude, zoomLevel)
            val centerPxX = MercatorProjection.lonToPixelX(centerLng, zoomLevel)
            val centerPxY = MercatorProjection.latToPixelY(centerLat, zoomLevel)

            val screenX = centerX + (sitePxX - centerPxX).toFloat()
            val screenY = centerY + (sitePxY - centerPxY).toFloat()

            // Check if marker is within visible viewport + padding
            if (screenX >= -120f && screenX <= widthPx + 120f && screenY >= -120f && screenY <= heightPx + 120f) {
                val isSelected = selectedSite?.siteId == site.siteId
                val proximity = ProximityHelper.calculateProximity(
                    technicianGps.latitude,
                    technicianGps.longitude,
                    site.latitude,
                    site.longitude
                )

                SiteMarkerComposable(
                    site = site,
                    proximity = proximity,
                    isSelected = isSelected,
                    onClick = { onSiteSelected(site) },
                    modifier = Modifier.offset {
                        // Offset by marker anchor point (bottom center of pin)
                        IntOffset(screenX.roundToInt(), screenY.roundToInt())
                    }
                )
            }
        }

        // Layer 4: Technician GPS Marker
        val techPxX = MercatorProjection.lonToPixelX(technicianGps.longitude, zoomLevel)
        val techPxY = MercatorProjection.latToPixelY(technicianGps.latitude, zoomLevel)
        val centerPxX = MercatorProjection.lonToPixelX(centerLng, zoomLevel)
        val centerPxY = MercatorProjection.latToPixelY(centerLat, zoomLevel)
        val techScreenX = centerX + (techPxX - centerPxX).toFloat()
        val techScreenY = centerY + (techPxY - centerPxY).toFloat()

        TechnicianMarkerComposable(
            gps = technicianGps,
            modifier = Modifier.offset {
                IntOffset(techScreenX.roundToInt(), techScreenY.roundToInt())
            }
        )

        // Map HUD Controls: Layer Menu, Zoom, My Location, Fit All
        MapControlsOverlay(
            currentTileSource = tileSource,
            onTileSourceChanged = { tileSource = it },
            onZoomIn = { zoomLevel = (zoomLevel + 1f).coerceAtMost(18.5f) },
            onZoomOut = { zoomLevel = (zoomLevel - 1f).coerceAtLeast(4f) },
            onCenterTechnician = {
                centerLat = technicianGps.latitude.takeIf { it != 0.0 } ?: 36.753820
                centerLng = technicianGps.longitude.takeIf { it != 0.0 } ?: 3.058810
                zoomLevel = 15.5f
            },
            onFitAllSites = {
                if (sites.isNotEmpty()) {
                    var minLat = sites.minOf { it.latitude }
                    var maxLat = sites.maxOf { it.latitude }
                    var minLng = sites.minOf { it.longitude }
                    var maxLng = sites.maxOf { it.longitude }

                    if (technicianGps.latitude != 0.0) {
                        minLat = min(minLat, technicianGps.latitude)
                        maxLat = max(maxLat, technicianGps.latitude)
                        minLng = min(minLng, technicianGps.longitude)
                        maxLng = max(maxLng, technicianGps.longitude)
                    }

                    centerLat = (minLat + maxLat) / 2.0
                    centerLng = (minLng + maxLng) / 2.0
                    val latDelta = max(maxLat - minLat, 0.01)
                    val lngDelta = max(maxLng - minLng, 0.01)
                    val maxDelta = max(latDelta, lngDelta)

                    zoomLevel = when {
                        maxDelta < 0.05 -> 14f
                        maxDelta < 0.2 -> 12f
                        maxDelta < 1.0 -> 10f
                        maxDelta < 4.0 -> 7.5f
                        else -> 5.5f
                    }
                }
            },
            technicianGps = technicianGps,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun SiteMarkerComposable(
    site: TelecomSiteEntity,
    proximity: com.example.location.ProximityInfo,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColor = when (site.accessStatus.uppercase()) {
        "ACCORDED" -> AccessAccordedGreen
        "REFUSED" -> AccessRefusedRed
        else -> Color(0xFFD97706)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .offset(x = (-36).dp, y = (-54).dp)
            .clickable(onClick = onClick)
            .testTag("marker_${site.siteId}")
    ) {
        // Tag label with Site ID and distance
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = if (isSelected) TelecomNavyPrimary else MaterialTheme.colorScheme.surface,
            shadowElevation = if (isSelected) 8.dp else 3.dp,
            border = androidx.compose.foundation.BorderStroke(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) TelecomBlueAccent else statusColor
            ),
            modifier = Modifier.padding(bottom = 2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = site.siteId,
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "• ${proximity.formattedDistance}",
                    color = if (isSelected) TelecomBlueAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                    fontSize = 10.sp
                )
            }
        }

        // Tower Pin Icon
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(if (isSelected) 36.dp else 30.dp)
                .shadow(6.dp, CircleShape)
                .clip(CircleShape)
                .background(if (isSelected) statusColor else statusColor.copy(alpha = 0.95f))
                .border(2.dp, Color.White, CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.CellTower,
                contentDescription = site.siteName,
                tint = Color.White,
                modifier = Modifier.size(if (isSelected) 20.dp else 16.dp)
            )
        }

        // Pin Pointer Triangle
        Canvas(modifier = Modifier.size(8.dp, 6.dp)) {
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(0f, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width / 2f, size.height)
                close()
            }
            drawPath(path, color = statusColor)
        }
    }
}

@Composable
private fun TechnicianMarkerComposable(
    gps: GpsData,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .offset(x = (-32).dp, y = (-42).dp)
            .testTag("marker_technician_gps")
    ) {
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = TelecomNavyPrimary,
            shadowElevation = 4.dp,
            modifier = Modifier.padding(bottom = 2.dp)
        ) {
            Text(
                text = "You (Technician)",
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(26.dp)
                .shadow(4.dp, CircleShape)
                .clip(CircleShape)
                .background(TelecomBlueAccent)
                .border(2.5.dp, Color.White, CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.MyLocation,
                contentDescription = "Technician Live Location",
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun MapControlsOverlay(
    currentTileSource: MapTileSource,
    onTileSourceChanged: (MapTileSource) -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onCenterTechnician: () -> Unit,
    onFitAllSites: () -> Unit,
    technicianGps: GpsData,
    modifier: Modifier = Modifier
) {
    var showLayersMenu by remember { mutableStateOf(false) }

    Box(modifier = modifier.padding(14.dp)) {
        // Top Right: Layer Switcher
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
        ) {
            SmallFloatingActionButton(
                onClick = { showLayersMenu = true },
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                elevation = FloatingActionButtonDefaults.elevation(4.dp),
                modifier = Modifier.testTag("map_layer_button")
            ) {
                Icon(Icons.Default.Layers, contentDescription = "Map Style")
            }

            DropdownMenu(
                expanded = showLayersMenu,
                onDismissRequest = { showLayersMenu = false }
            ) {
                MapTileSource.values().forEach { source ->
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = source.label,
                                    fontWeight = if (source == currentTileSource) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        },
                        onClick = {
                            onTileSourceChanged(source)
                            showLayersMenu = false
                        }
                    )
                }
            }
        }

        // Bottom Left: GPS Fix & Accuracy HUD
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            shadowElevation = 3.dp,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .testTag("map_gps_hud")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(AccessAccordedGreen)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = "GPS: %.5f°, %.5f°".format(technicianGps.latitude, technicianGps.longitude),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Accuracy: ±%.1fm • %s".format(technicianGps.accuracy, technicianGps.provider),
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Bottom Right: Zoom +, Zoom -, Center My Location, Fit All Sites
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.align(Alignment.BottomEnd)
        ) {
            SmallFloatingActionButton(
                onClick = onFitAllSites,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = TelecomNavyPrimary,
                elevation = FloatingActionButtonDefaults.elevation(4.dp),
                modifier = Modifier.testTag("map_fit_all_button")
            ) {
                Icon(Icons.Default.CenterFocusStrong, contentDescription = "Fit All Sites")
            }

            SmallFloatingActionButton(
                onClick = onCenterTechnician,
                containerColor = TelecomNavyPrimary,
                contentColor = Color.White,
                elevation = FloatingActionButtonDefaults.elevation(4.dp),
                modifier = Modifier.testTag("map_center_gps_button")
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "My Location")
            }

            Column(
                modifier = Modifier
                    .shadow(4.dp, RoundedCornerShape(8.dp))
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clickable(onClick = onZoomIn)
                        .testTag("map_zoom_in_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Zoom In", tint = MaterialTheme.colorScheme.onSurface)
                }
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .size(width = 40.dp, height = 1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clickable(onClick = onZoomOut)
                        .testTag("map_zoom_out_button")
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Zoom Out", tint = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}
