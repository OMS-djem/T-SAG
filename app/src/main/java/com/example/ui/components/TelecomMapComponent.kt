package com.example.ui.components

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.GpsOff
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.CircularProgressIndicator
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
import com.example.ui.theme.TelecomBlueAccent
import com.example.ui.theme.TelecomNavyPrimary
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

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
    modifier: Modifier = Modifier,
    isRefreshingGps: Boolean = false,
    onRefreshGps: () -> Unit = {}
) {
    val context = LocalContext.current

    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var isMapLoaded by remember { mutableStateOf(false) }
    var currentLayer by remember { mutableStateOf(LeafletMapLayer.GOOGLE_HYBRID) }
    var showLayersMenu by remember { mutableStateOf(false) }

    // GPS Acquisition Timeout tracking
    var gpsTimedOut by remember { mutableStateOf(false) }

    LaunchedEffect(isRefreshingGps, technicianGps.latitude, technicianGps.longitude) {
        if (technicianGps.latitude == 0.0 && technicianGps.longitude == 0.0) {
            gpsTimedOut = false
            delay(8000)
            if (technicianGps.latitude == 0.0 && technicianGps.longitude == 0.0 && !isRefreshingGps) {
                gpsTimedOut = true
            }
        } else {
            gpsTimedOut = false
        }
    }

    val currentSitesRef = remember { mutableStateOf(sites) }
    currentSitesRef.value = sites

    val hasCoordinates = technicianGps.latitude != 0.0 || technicianGps.longitude != 0.0
    val initLat = if (hasCoordinates) technicianGps.latitude else 36.753820
    val initLng = if (hasCoordinates) technicianGps.longitude else 3.058810

    val leafletHtml = remember {
        generateLeafletHtml(
            context = context,
            initialLat = initLat,
            initialLng = initLng
        )
    }

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
        val js = "if (window.renderSites) { window.renderSites(${jsonArray.toString()}, $safeSelected); }"
        view.evaluateJavascript(js, null)
    }

    fun updateGpsInMap(view: WebView?, gps: GpsData) {
        if (view == null || !isMapLoaded) return
        if (gps.latitude != 0.0 && gps.longitude != 0.0) {
            val latStr = String.format(Locale.US, "%.6f", gps.latitude)
            val lngStr = String.format(Locale.US, "%.6f", gps.longitude)
            val accStr = String.format(Locale.US, "%.1f", gps.accuracy)
            val js = "if (window.updateTechnicianLocation) { window.updateTechnicianLocation($latStr, $lngStr, $accStr); }"
            view.evaluateJavascript(js, null)
        }
    }

    LaunchedEffect(sites, selectedSite?.siteId, isMapLoaded) {
        if (isMapLoaded) {
            updateSitesInMap(webViewInstance, sites, selectedSite?.siteId)
            if (selectedSite != null) {
                val latStr = String.format(Locale.US, "%.6f", selectedSite.latitude)
                val lngStr = String.format(Locale.US, "%.6f", selectedSite.longitude)
                val js = "if (window.selectSite) { window.selectSite(${JSONObject.quote(selectedSite.siteId)}, $latStr, $lngStr); }"
                webViewInstance?.evaluateJavascript(js, null)
            }
        }
    }

    LaunchedEffect(technicianGps.latitude, technicianGps.longitude, technicianGps.accuracy, isMapLoaded) {
        if (isMapLoaded) {
            updateGpsInMap(webViewInstance, technicianGps)
        }
    }

    // If both GPS is missing and no site data is available: Show Full-Screen Placeholder
    if (!hasCoordinates && sites.isEmpty()) {
        if (gpsTimedOut) {
            MapGpsErrorPlaceholder(
                onRetry = {
                    gpsTimedOut = false
                    onRefreshGps()
                },
                modifier = modifier,
                errorMessage = "Failed to acquire technician GPS coordinates.",
                detailedHelp = "Verify location is turned on in Android quick settings and app has location permission."
            )
        } else {
            MapGpsLoadingPlaceholder(
                modifier = modifier,
                statusText = "Acquiring Device GPS Coordinates...",
                subText = "Waiting for initial satellite fix to map nearby telecom sites..."
            )
        }
        return
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("telecom_map_container")
    ) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    webViewInstance = this
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        allowFileAccess = true
                        allowContentAccess = true
                        allowFileAccessFromFileURLs = true
                        allowUniversalAccessFromFileURLs = true
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        userAgentString = "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                        cacheMode = WebSettings.LOAD_DEFAULT
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        setSupportZoom(false)
                        builtInZoomControls = false
                        displayZoomControls = false
                    }
                    webChromeClient = object : WebChromeClient() {
                        override fun onConsoleMessage(cm: ConsoleMessage?): Boolean {
                            android.util.Log.d("LeafletFullMap", "${cm?.message()} at line ${cm?.lineNumber()}")
                            return true
                        }
                    }
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            isMapLoaded = true
                            view?.evaluateJavascript("if (window.map) { window.map.invalidateSize(true); }", null)
                            view?.postDelayed({
                                view.evaluateJavascript("if (window.map) { window.map.invalidateSize(true); }", null)
                            }, 250)
                            updateGpsInMap(view, technicianGps)
                            updateSitesInMap(view, currentSitesRef.value, selectedSite?.siteId)
                        }

                        override fun onReceivedSslError(view: WebView?, handler: android.webkit.SslErrorHandler?, error: android.net.http.SslError?) {
                            handler?.proceed()
                        }
                    }

                    addJavascriptInterface(
                        LeafletAndroidBridge(
                            onSiteSelected = onSiteSelected,
                            sitesSupplier = { currentSitesRef.value }
                        ),
                        "AndroidBridge"
                    )

                    loadDataWithBaseURL(
                        "https://maps.googleapis.com/",
                        leafletHtml,
                        "text/html",
                        "UTF-8",
                        null
                    )
                }
            },
            update = { view ->
                if (isMapLoaded) {
                    updateGpsInMap(view, technicianGps)
                    updateSitesInMap(view, currentSitesRef.value, selectedSite?.siteId)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Native HUD Controls
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
        ) {
            // Top Center: Floating GPS Acquisition / Error Telemetry Banner
            AnimatedVisibility(
                visible = !hasCoordinates || isRefreshingGps,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 110.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = TelecomNavyPrimary.copy(alpha = 0.92f),
                    shadowElevation = 6.dp,
                    modifier = Modifier.clickable { onRefreshGps() }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        if (isRefreshingGps || !gpsTimedOut) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = TelecomBlueAccent
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Acquiring GPS Telemetry...",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.GpsOff,
                                contentDescription = null,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "GPS Timeout • Tap to Retry",
                                color = Color(0xFFEF4444),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Top Right: Base Map Layer Switcher
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 110.dp)
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
                                webViewInstance?.evaluateJavascript("if (window.setBaseLayer) { window.setBaseLayer('${layer.layerKey}'); }", null)
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
                            .background(if (hasCoordinates) AccessAccordedGreen else Color(0xFFD97706))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = if (hasCoordinates) {
                                "GPS: %.5f°, %.5f°".format(technicianGps.latitude, technicianGps.longitude)
                            } else if (isRefreshingGps || !gpsTimedOut) {
                                "GPS: Acquiring fix..."
                            } else {
                                "GPS: Signal Unavailable"
                            },
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (hasCoordinates) {
                                "Accuracy: ±%.1fm • %s".format(technicianGps.accuracy, technicianGps.provider)
                            } else {
                                "Tap top banner to re-scan"
                            },
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Bottom Right: Zoom Controls, Center My Location, Fit All Sites
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.align(Alignment.BottomEnd)
            ) {
                SmallFloatingActionButton(
                    onClick = {
                        webViewInstance?.evaluateJavascript("if (window.fitAll) { window.fitAll(); }", null)
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
                        if (hasCoordinates) {
                            val latStr = String.format(Locale.US, "%.6f", technicianGps.latitude)
                            val lngStr = String.format(Locale.US, "%.6f", technicianGps.longitude)
                            webViewInstance?.evaluateJavascript("if (window.centerTechnician) { window.centerTechnician($latStr, $lngStr); }", null)
                        } else {
                            onRefreshGps()
                            webViewInstance?.evaluateJavascript("if (window.fitAll) { window.fitAll(); }", null)
                        }
                    },
                    containerColor = TelecomNavyPrimary,
                    contentColor = Color.White,
                    elevation = FloatingActionButtonDefaults.elevation(4.dp),
                    modifier = Modifier.testTag("map_center_gps_button")
                ) {
                    Icon(
                        imageVector = if (hasCoordinates) Icons.Default.MyLocation else Icons.Default.Refresh,
                        contentDescription = "My Location"
                    )
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
                            .clickable { webViewInstance?.evaluateJavascript("if (window.zoomIn) { window.zoomIn(); }", null) }
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
                            .clickable { webViewInstance?.evaluateJavascript("if (window.zoomOut) { window.zoomOut(); }", null) }
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

private fun generateLeafletHtml(
    context: android.content.Context,
    initialLat: Double,
    initialLng: Double
): String {
    val latStr = String.format(Locale.US, "%.6f", initialLat)
    val lngStr = String.format(Locale.US, "%.6f", initialLng)
    val localCss = LeafletAssetManager.getInlinedCss(context)
    val localJs = LeafletAssetManager.getInlinedJs(context)

    val template = """
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

    <!-- Inlined Offline Leaflet & MarkerCluster Engine -->
    <style>
/*__LOCAL_CSS__*/
    </style>
    <script>
/*__LOCAL_JS__*/
    </script>

    <!-- Leaflet JS -->
    <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/leaflet.markercluster/1.5.3/leaflet.markercluster.js"></script>

    <style>
        html, body {
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
        #map {
            position: absolute;
            top: 0;
            bottom: 0;
            left: 0;
            right: 0;
            width: 100%;
            height: 100%;
            background: #0f172a;
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
        function startMap() {
            if (typeof L === 'undefined') {
                setTimeout(startMap, 50);
                return;
            }

            // Base maps
            const googleHybrid = L.tileLayer('https://{s}.google.com/vt/lyrs=s,h&x={x}&y={y}&z={z}', { maxZoom: 20, subdomains: ['mt0', 'mt1', 'mt2', 'mt3'] });
            const googleStreets = L.tileLayer('https://{s}.google.com/vt/lyrs=m&x={x}&y={y}&z={z}', { maxZoom: 20, subdomains: ['mt0', 'mt1', 'mt2', 'mt3'] });

            window.map = L.map('map', {
                center: [$latStr, $lngStr],
                zoom: 14,
                zoomControl: false,
                layers: [googleHybrid]
            });

            window.googleHybrid = googleHybrid;
            window.googleStreets = googleStreets;

            const markerCluster = L.markerClusterGroup({
                spiderfyOnMaxZoom: true,
                showCoverageOnHover: false,
                zoomToBoundsOnClick: true,
                maxClusterRadius: 40
            });
            window.map.addLayer(markerCluster);
            window.markerCluster = markerCluster;

            let techMarker = null;
            let techAccuracyCircle = null;
            let siteMarkersMap = {};

            window.setBaseLayer = function(layerKey) {
                if (!window.map) return;
                if (layerKey === 'streets') {
                    if (window.map.hasLayer(window.googleHybrid)) window.map.removeLayer(window.googleHybrid);
                    if (!window.map.hasLayer(window.googleStreets)) window.map.addLayer(window.googleStreets);
                } else {
                    if (window.map.hasLayer(window.googleStreets)) window.map.removeLayer(window.googleStreets);
                    if (!window.map.hasLayer(window.googleHybrid)) window.map.addLayer(window.googleHybrid);
                }
            };

            window.updateTechnicianLocation = function(lat, lng, accuracy) {
                if (!window.map || !lat || !lng) return;
                const pos = [lat, lng];
                const isFirst = !techMarker;
                if (!techMarker) {
                    const icon = L.divIcon({
                        className: 'tech-div-icon',
                        html: '<div class="tech-marker"></div>',
                        iconSize: [22, 22],
                        iconAnchor: [11, 11]
                    });
                    techMarker = L.marker(pos, { icon: icon, zIndexOffset: 1000 }).addTo(window.map);
                    techAccuracyCircle = L.circle(pos, {
                        radius: accuracy || 15,
                        color: '#3b82f6',
                        fillColor: '#3b82f6',
                        fillOpacity: 0.15,
                        weight: 1
                    }).addTo(window.map);
                } else {
                    techMarker.setLatLng(pos);
                    if (techAccuracyCircle) {
                        techAccuracyCircle.setLatLng(pos);
                        if (accuracy) techAccuracyCircle.setRadius(accuracy);
                    }
                }
                if (isFirst) {
                    window.map.panTo(pos);
                    window.map.invalidateSize(true);
                }
            };

            window.centerTechnician = function(lat, lng) {
                if (window.map && lat && lng) {
                    window.map.flyTo([lat, lng], 16, { animate: true, duration: 0.8 });
                    window.map.invalidateSize(true);
                }
            };

            window.zoomIn = function() { if (window.map) window.map.zoomIn(); };
            window.zoomOut = function() { if (window.map) window.map.zoomOut(); };

            window.renderSites = function(sitesData, selectedSiteId) {
                if (!window.markerCluster) return;
                window.markerCluster.clearLayers();
                siteMarkersMap = {};

                sitesData.forEach(site => {
                    const status = (site.accessStatus || 'PENDING').toLowerCase();
                    const statusClass = status === 'accorded' ? 'accorded' : (status === 'refused' ? 'refused' : 'pending');
                    const isSelected = selectedSiteId && selectedSiteId.toLowerCase() === site.siteId.toLowerCase();
                    const selectedClass = isSelected ? ' selected' : '';

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

                    window.markerCluster.addLayer(marker);
                    siteMarkersMap[site.siteId.toLowerCase()] = marker;
                });

                if (sitesData.length > 0 && (!selectedSiteId || selectedSiteId === '')) {
                    window.fitAll();
                }
            };

            window.selectSite = function(siteId, lat, lng) {
                if (!siteId) return;
                const key = siteId.toLowerCase();
                const marker = siteMarkersMap[key];
                if (marker) {
                    window.markerCluster.zoomToShowLayer(marker, () => {
                        marker.openPopup();
                    });
                } else if (lat && lng && window.map) {
                    window.map.flyTo([lat, lng], 17, { animate: true, duration: 0.8 });
                }
            };

            window.fitAll = function() {
                try {
                    if (!window.map) return;
                    let group = L.featureGroup();
                    if (window.markerCluster && window.markerCluster.getLayers().length > 0) {
                        group.addLayer(window.markerCluster);
                    }
                    if (techMarker) {
                        group.addLayer(techMarker);
                    }
                    const bounds = group.getBounds();
                    if (bounds && bounds.isValid()) {
                        window.map.fitBounds(bounds, { padding: [50, 50], maxZoom: 16 });
                    }
                } catch (e) {
                    console.error(e);
                }
            };

            window.triggerAndroidSelection = function(siteId) {
                if (window.AndroidBridge && window.AndroidBridge.onSiteSelected) {
                    window.AndroidBridge.onSiteSelected(siteId);
                }
            };

            window.map.invalidateSize(true);
            setTimeout(function() { if (window.map) window.map.invalidateSize(true); }, 150);
            setTimeout(function() { if (window.map) window.map.invalidateSize(true); }, 500);
            setTimeout(function() { if (window.map) window.map.invalidateSize(true); }, 1200);

            if (window.ResizeObserver) {
                new ResizeObserver(function() {
                    if (window.map) window.map.invalidateSize(true);
                }).observe(document.getElementById('map'));
            }
        }

        if (document.readyState === 'complete' || document.readyState === 'interactive') {
            startMap();
        } else {
            window.addEventListener('DOMContentLoaded', startMap);
            window.addEventListener('load', startMap);
        }
    </script>
</body>
</html>
    """.trimIndent()

    return template
        .replace("/*__LOCAL_CSS__*/", localCss)
        .replace("/*__LOCAL_JS__*/", localJs)
}
