package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.GpsData
import com.example.data.model.TelecomSiteEntity
import com.example.ui.theme.AccessAccordedGreen
import com.example.ui.theme.TelecomNavyPrimary
import org.json.JSONArray
import org.json.JSONObject

enum class LeafletMapLayer(val label: String, val layerKey: String) {
    GOOGLE_HYBRID("Google Hybrid (Satellite + Roads)", "hybrid"),
    GOOGLE_STREETS("Google Streets (Standard)", "streets")
}

private class LeafletAndroidBridge(
    private val onSiteSelected: (TelecomSiteEntity) -> Unit,
    private val sitesSupplier: () -> List<TelecomSiteEntity>
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun onSiteSelected(siteId: String) {
        mainHandler.post {
            val matching = sitesSupplier().find { it.siteId.equals(siteId, ignoreCase = true) }
            if (matching != null) {
                onSiteSelected(matching)
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun TelecomMapComponent(
    technicianGps: GpsData,
    sites: List<TelecomSiteEntity>,
    selectedSite: TelecomSiteEntity?,
    onSiteSelected: (TelecomSiteEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var isMapLoaded by remember { mutableStateOf(false) }
    var currentLayer by remember { mutableStateOf(LeafletMapLayer.GOOGLE_HYBRID) }
    var showLayersMenu by remember { mutableStateOf(false) }

    // Keep sites in a ref for bridge callback
    val currentSitesRef = remember { mutableStateOf(sites) }
    currentSitesRef.value = sites

    // Generate HTML content embedding Leaflet 1.9.4, MarkerCluster 1.5.3, and Google Tiles
    val leafletHtml = remember {
        generateLeafletHtml(
            initialLat = if (technicianGps.latitude != 0.0) technicianGps.latitude else 36.753820,
            initialLng = if (technicianGps.longitude != 0.0) technicianGps.longitude else 3.058810
        )
    }

    // Function to push sites to Leaflet
    fun updateSitesInMap(view: WebView?, siteList: List<TelecomSiteEntity>, selId: String?) {
        if (view == null || !isMapLoaded) return
        val jsonArray = JSONArray()
        siteList.forEach { site ->
            val obj = JSONObject().apply {
                put("siteId", site.siteId)
                put("siteName", site.siteName)
                put("operator", site.operator)
                put("latitude", site.latitude)
                put("longitude", site.longitude)
                put("accessStatus", site.accessStatus)
                put("address", site.address)
                put("siteType", site.siteType)
            }
            jsonArray.put(obj)
        }
        val safeSelected = JSONObject.quote(selId ?: "")
        val js = "renderSites(${jsonArray.toString()}, $safeSelected);"
        view.evaluateJavascript(js, null)
    }

    // Function to push GPS location
    fun updateGpsInMap(view: WebView?, gps: GpsData) {
        if (view == null || !isMapLoaded) return
        if (gps.latitude != 0.0 && gps.longitude != 0.0) {
            val js = "updateTechnicianLocation(${gps.latitude}, ${gps.longitude}, ${gps.accuracy});"
            view.evaluateJavascript(js, null)
        }
    }

    // Whenever sites or selectedSite changes, update Leaflet markers
    LaunchedEffect(sites, selectedSite?.siteId, isMapLoaded) {
        if (isMapLoaded) {
            updateSitesInMap(webViewInstance, sites, selectedSite?.siteId)
            if (selectedSite != null) {
                val js = "selectSite(${JSONObject.quote(selectedSite.siteId)}, ${selectedSite.latitude}, ${selectedSite.longitude});"
                webViewInstance?.evaluateJavascript(js, null)
            }
        }
    }

    // Whenever technician GPS updates, update Leaflet beacon
    LaunchedEffect(technicianGps.latitude, technicianGps.longitude, technicianGps.accuracy, isMapLoaded) {
        if (isMapLoaded) {
            updateGpsInMap(webViewInstance, technicianGps)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("telecom_map_container")
    ) {
        // Leaflet Interactive WebView Map
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    webViewInstance = this
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        cacheMode = WebSettings.LOAD_DEFAULT
                        setSupportZoom(false)
                        builtInZoomControls = false
                        displayZoomControls = false
                    }
                    webChromeClient = WebChromeClient()
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            isMapLoaded = true
                            updateGpsInMap(view, technicianGps)
                            updateSitesInMap(view, currentSitesRef.value, selectedSite?.siteId)
                        }
                    }

                    // Bridge for clicking markers & selecting sites
                    addJavascriptInterface(
                        LeafletAndroidBridge(
                            onSiteSelected = onSiteSelected,
                            sitesSupplier = { currentSitesRef.value }
                        ),
                        "AndroidBridge"
                    )

                    loadDataWithBaseURL(
                        "https://leafletjs.com/",
                        leafletHtml,
                        "text/html",
                        "UTF-8",
                        null
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Native HUD Controls: Layer Switcher, Center Location, Fit All, Zoom
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
        ) {
            // Top Right: Base Map Layer Switcher (Google Hybrid / Google Streets)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 110.dp) // Below search bar overlay
            ) {
                SmallFloatingActionButton(
                    onClick = { showLayersMenu = true },
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = TelecomNavyPrimary,
                    elevation = FloatingActionButtonDefaults.elevation(4.dp),
                    modifier = Modifier.testTag("map_layer_button")
                ) {
                    Icon(Icons.Default.Layers, contentDescription = "Map Style")
                }

                DropdownMenu(
                    expanded = showLayersMenu,
                    onDismissRequest = { showLayersMenu = false }
                ) {
                    LeafletMapLayer.entries.forEach { layer ->
                        val isSelected = layer == currentLayer
                        DropdownMenuItem(
                            leadingIcon = if (isSelected) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = TelecomNavyPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            } else null,
                            text = {
                                Text(
                                    text = layer.label,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp
                                )
                            },
                            onClick = {
                                currentLayer = layer
                                showLayersMenu = false
                                webViewInstance?.evaluateJavascript("setBaseLayer('${layer.layerKey}');", null)
                            }
                        )
                    }
                }
            }

            // Bottom Left: GPS Fix & Accuracy HUD
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                shadowElevation = 4.dp,
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
                    onClick = {
                        webViewInstance?.evaluateJavascript("fitAll();", null)
                    },
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = TelecomNavyPrimary,
                    elevation = FloatingActionButtonDefaults.elevation(4.dp),
                    modifier = Modifier.testTag("map_fit_all_button")
                ) {
                    Icon(Icons.Default.CenterFocusStrong, contentDescription = "Fit All Sites")
                }

                SmallFloatingActionButton(
                    onClick = {
                        if (technicianGps.latitude != 0.0 && technicianGps.longitude != 0.0) {
                            webViewInstance?.evaluateJavascript(
                                "centerTechnician(${technicianGps.latitude}, ${technicianGps.longitude});",
                                null
                            )
                        }
                    },
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
                            .clickable { webViewInstance?.evaluateJavascript("zoomIn();", null) }
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
                            .clickable { webViewInstance?.evaluateJavascript("zoomOut();", null) }
                            .testTag("map_zoom_out_button")
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Zoom Out", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webViewInstance?.destroy()
        }
    }
}

/**
 * Generates the self-contained HTML page using Leaflet CSS, MarkerCluster CSS,
 * Google Hybrid & Google Streets tile layers as requested by the user.
 */
private fun generateLeafletHtml(initialLat: Double, initialLng: Double): String {
    return """
<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
    <title>Telecom Sites Radar Map</title>
    <!-- Leaflet CSS -->
    <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/leaflet.markercluster/1.5.3/MarkerCluster.css" />
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/leaflet.markercluster/1.5.3/MarkerCluster.Default.css" />
    
    <!-- Leaflet JS & MarkerCluster JS -->
    <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/leaflet.markercluster/1.5.3/leaflet.markercluster.js"></script>

    <style>
        html, body, #map {
            width: 100%;
            height: 100%;
            margin: 0;
            padding: 0;
            background: #0f172a;
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
            overflow: hidden;
            -webkit-user-select: none;
            user-select: none;
        }
        .tower-marker {
            display: flex;
            align-items: center;
            justify-content: center;
            width: 32px;
            height: 32px;
            border-radius: 50%;
            border: 2px solid #ffffff;
            box-shadow: 0 4px 10px rgba(0,0,0,0.5);
            color: #ffffff;
            transition: transform 0.2s ease;
        }
        .tower-marker.accorded { background: #16a34a; }
        .tower-marker.refused { background: #dc2626; }
        .tower-marker.pending { background: #d97706; }
        .tower-marker.selected {
            transform: scale(1.3);
            border: 3px solid #3b82f6;
            box-shadow: 0 0 16px #2563eb;
        }
        .tech-marker {
            width: 22px;
            height: 22px;
            border-radius: 50%;
            background: #2563eb;
            border: 3px solid #ffffff;
            box-shadow: 0 0 12px #2563eb;
            animation: pulse-ring 2s infinite;
        }
        @keyframes pulse-ring {
            0% { box-shadow: 0 0 0 0 rgba(37, 99, 235, 0.7); }
            70% { box-shadow: 0 0 0 16px rgba(37, 99, 235, 0); }
            100% { box-shadow: 0 0 0 0 rgba(37, 99, 235, 0); }
        }
        .popup-container {
            font-size: 12px;
            line-height: 1.4;
            min-width: 180px;
        }
        .popup-site-id {
            font-weight: 800;
            font-size: 14px;
            color: #0f172a;
            font-family: monospace;
        }
        .popup-site-name {
            font-weight: 600;
            color: #1e3a8a;
            margin-bottom: 2px;
        }
        .popup-status-badge {
            display: inline-block;
            padding: 2px 7px;
            border-radius: 4px;
            font-weight: 800;
            font-size: 10px;
            margin: 4px 0;
            color: #ffffff;
            text-transform: uppercase;
        }
        .popup-status-badge.accorded { background: #16a34a; }
        .popup-status-badge.refused { background: #dc2626; }
        .popup-status-badge.pending { background: #d97706; }
        .popup-select-btn {
            display: block;
            width: 100%;
            margin-top: 8px;
            padding: 7px 10px;
            background: #0f2042;
            color: #ffffff;
            border: none;
            border-radius: 6px;
            font-weight: 700;
            font-size: 11px;
            cursor: pointer;
            text-align: center;
        }
    </style>
</head>
<body>
    <div id="map"></div>
    <script>
        // Base maps
        const googleHybrid = L.tileLayer('https://{s}.google.com/vt/lyrs=s,h&x={x}&y={y}&z={z}', {
            maxZoom: 20,
            subdomains: ['mt0', 'mt1', 'mt2', 'mt3'],
            attribution: 'Google Maps Hybrid'
        });
        const googleStreets = L.tileLayer('https://{s}.google.com/vt/lyrs=m&x={x}&y={y}&z={z}', {
            maxZoom: 20,
            subdomains: ['mt0', 'mt1', 'mt2', 'mt3'],
            attribution: 'Google Maps Streets'
        });

        // Initialize Map
        const map = L.map('map', {
            center: [$initialLat, $initialLng],
            zoom: 14,
            zoomControl: false,
            layers: [googleHybrid]
        });

        // MarkerCluster Group
        const markerCluster = L.markerClusterGroup({
            spiderfyOnMaxZoom: true,
            showCoverageOnHover: false,
            zoomToBoundsOnClick: true,
            maxClusterRadius: 40
        });
        map.addLayer(markerCluster);

        let techMarker = null;
        let techAccuracyCircle = null;
        let siteMarkersMap = {};

        function setBaseLayer(layerKey) {
            if (layerKey === 'streets') {
                if (map.hasLayer(googleHybrid)) map.removeLayer(googleHybrid);
                if (!map.hasLayer(googleStreets)) map.addLayer(googleStreets);
            } else {
                if (map.hasLayer(googleStreets)) map.removeLayer(googleStreets);
                if (!map.hasLayer(googleHybrid)) map.addLayer(googleHybrid);
            }
        }

        function updateTechnicianLocation(lat, lng, accuracy) {
            if (!lat || !lng) return;
            const pos = [lat, lng];
            if (!techMarker) {
                const icon = L.divIcon({
                    className: 'tech-div-icon',
                    html: '<div class="tech-marker"></div>',
                    iconSize: [22, 22],
                    iconAnchor: [11, 11]
                });
                techMarker = L.marker(pos, { icon: icon, zIndexOffset: 1000 }).addTo(map);
                techAccuracyCircle = L.circle(pos, {
                    radius: accuracy || 15,
                    color: '#3b82f6',
                    fillColor: '#3b82f6',
                    fillOpacity: 0.15,
                    weight: 1
                }).addTo(map);
            } else {
                techMarker.setLatLng(pos);
                if (techAccuracyCircle) {
                    techAccuracyCircle.setLatLng(pos);
                    if (accuracy) techAccuracyCircle.setRadius(accuracy);
                }
            }
        }

        function centerTechnician(lat, lng) {
            if (lat && lng) {
                map.flyTo([lat, lng], 16, { animate: true, duration: 0.8 });
            }
        }

        function zoomIn() { map.zoomIn(); }
        function zoomOut() { map.zoomOut(); }

        function renderSites(sitesData, selectedSiteId) {
            markerCluster.clearLayers();
            siteMarkersMap = {};

            sitesData.forEach(site => {
                const status = (site.accessStatus || 'PENDING').toLowerCase();
                const statusClass = status === 'accorded' ? 'accorded' : (status === 'refused' ? 'refused' : 'pending');
                const isSelected = selectedSiteId && selectedSiteId.toLowerCase() === site.siteId.toLowerCase();
                const selectedClass = isSelected ? ' selected' : '';

                // Tower Icon SVG
                const towerSvg = '<svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor">' +
                    '<path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z"/>' +
                    '</svg>';

                const icon = L.divIcon({
                    className: 'site-div-icon',
                    html: '<div class="tower-marker ' + statusClass + selectedClass + '">' + towerSvg + '</div>',
                    iconSize: [32, 32],
                    iconAnchor: [16, 16]
                });

                const marker = L.marker([site.latitude, site.longitude], { icon: icon });

                const popupHtml = '<div class="popup-container">' +
                    '<div class="popup-site-id">' + site.siteId + '</div>' +
                    '<div class="popup-site-name">' + site.operator + ' • ' + site.siteName + '</div>' +
                    '<div>' + (site.address || site.siteType || '') + '</div>' +
                    '<span class="popup-status-badge ' + statusClass + '">' + (site.accessStatus || 'PENDING') + '</span>' +
                    '<button class="popup-select-btn" onclick="triggerAndroidSelection(\'' + site.siteId + '\')">Select & View Details</button>' +
                    '</div>';

                marker.bindPopup(popupHtml);
                marker.on('click', () => {
                    triggerAndroidSelection(site.siteId);
                });

                markerCluster.addLayer(marker);
                siteMarkersMap[site.siteId.toLowerCase()] = marker;
            });
        }

        function selectSite(siteId, lat, lng) {
            if (!siteId) return;
            const key = siteId.toLowerCase();
            const marker = siteMarkersMap[key];
            if (marker) {
                markerCluster.zoomToShowLayer(marker, () => {
                    marker.openPopup();
                });
            } else if (lat && lng) {
                map.flyTo([lat, lng], 17, { animate: true, duration: 0.8 });
            }
        }

        function fitAll() {
            try {
                const bounds = markerCluster.getBounds();
                if (bounds && bounds.isValid()) {
                    map.fitBounds(bounds, { padding: [50, 50], maxZoom: 16 });
                }
            } catch (e) {
                console.error(e);
            }
        }

        function triggerAndroidSelection(siteId) {
            if (window.AndroidBridge && window.AndroidBridge.onSiteSelected) {
                window.AndroidBridge.onSiteSelected(siteId);
            }
        }
    </script>
</body>
</html>
    """.trimIndent()
}
