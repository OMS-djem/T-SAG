package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.GpsData
import com.example.ui.theme.AccessAccordedGreen
import com.example.ui.theme.TelecomBlueAccent
import com.example.ui.theme.TelecomNavyPrimary
import kotlinx.coroutines.delay
import java.util.Locale

enum class MapPreviewLayer(val id: String, val label: String) {
    HYBRID("hybrid", "Google Hybrid"),
    STREETS("streets", "Google Streets")
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun StaticMapPreviewCard(
    gps: GpsData,
    modifier: Modifier = Modifier,
    siteId: String = "",
    siteName: String = "",
    isRefreshingGps: Boolean = false,
    onRefreshGps: () -> Unit = {},
    onOpenMap: () -> Unit = {}
) {
    val context = LocalContext.current
    var selectedLayer by remember { mutableStateOf(MapPreviewLayer.HYBRID) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var isMapReady by remember { mutableStateOf(false) }

    // Coordinates fallback if 0.0 (e.g. before initial fix)
    val hasCoordinates = gps.latitude != 0.0 || gps.longitude != 0.0
    val effectiveLat = if (hasCoordinates) gps.latitude else 36.753820
    val effectiveLng = if (hasCoordinates) gps.longitude else 3.058810

    // GPS Acquisition Timeout tracking (8s without coordinates triggers error view)
    var gpsTimedOut by remember { mutableStateOf(false) }

    LaunchedEffect(isRefreshingGps, gps.latitude, gps.longitude) {
        if (!hasCoordinates) {
            gpsTimedOut = false
            delay(8000)
            if (!hasCoordinates && !isRefreshingGps) {
                gpsTimedOut = true
            }
        } else {
            gpsTimedOut = false
        }
    }

    val previewHtml = remember(effectiveLat, effectiveLng) {
        generatePreviewMapHtml(
            context = context,
            lat = effectiveLat,
            lng = effectiveLng,
            accuracy = if (gps.accuracy > 0) gps.accuracy else 15f
        )
    }

    LaunchedEffect(gps.latitude, gps.longitude, gps.accuracy, isMapReady) {
        if (isMapReady && hasCoordinates) {
            val latStr = String.format(Locale.US, "%.6f", gps.latitude)
            val lngStr = String.format(Locale.US, "%.6f", gps.longitude)
            val accStr = String.format(Locale.US, "%.1f", gps.accuracy)
            val js = "updateLocation($latStr, $lngStr, $accStr);"
            webViewInstance?.evaluateJavascript(js, null)
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("static_map_preview_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Title & Subtitle with status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(TelecomBlueAccent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Map,
                            contentDescription = "Map Preview",
                            tint = TelecomBlueAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Site Map & Radar Preview",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (hasCoordinates) {
                                "Google Hybrid & Streets Tiles • Leaflet"
                            } else if (gpsTimedOut) {
                                "GPS Fix Unavailable • Check Settings"
                            } else {
                                "Acquiring GPS Telemetry..."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (gpsTimedOut) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = {
                        if (hasCoordinates) {
                            val latStr = String.format(Locale.US, "%.6f", gps.latitude)
                            val lngStr = String.format(Locale.US, "%.6f", gps.longitude)
                            webViewInstance?.evaluateJavascript("centerMap($latStr, $lngStr);", null)
                        } else {
                            gpsTimedOut = false
                            onRefreshGps()
                        }
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("refresh_preview_map_button")
                ) {
                    if (isRefreshingGps) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(
                            imageVector = if (hasCoordinates) Icons.Default.MyLocation else Icons.Default.Refresh,
                            contentDescription = if (hasCoordinates) "Re-center Map" else "Scan GPS",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Map Layer Switcher
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MapPreviewLayer.entries.forEach { layer ->
                    val isSelected = selectedLayer == layer
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedLayer = layer
                            webViewInstance?.evaluateJavascript("setLayer('${layer.id}');", null)
                        },
                        label = {
                            Text(
                                text = layer.label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TelecomNavyPrimary,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("preview_layer_chip_${layer.id}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Embedded Map Viewport Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0F172A))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .testTag("preview_map_webview_container")
            ) {
                if (hasCoordinates) {
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
                                        android.util.Log.d("LeafletPreview", "${cm?.message()} at line ${cm?.lineNumber()}")
                                        return true
                                    }
                                }
                                webViewClient = object : WebViewClient() {
                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        isMapReady = true
                                        view?.evaluateJavascript("if (window.map) { window.map.invalidateSize(true); }", null)
                                        view?.postDelayed({
                                            view.evaluateJavascript("if (window.map) { window.map.invalidateSize(true); }", null)
                                        }, 250)
                                        val latStr = String.format(Locale.US, "%.6f", gps.latitude)
                                        val lngStr = String.format(Locale.US, "%.6f", gps.longitude)
                                        val accStr = String.format(Locale.US, "%.1f", gps.accuracy)
                                        view?.evaluateJavascript("updateLocation($latStr, $lngStr, $accStr);", null)
                                    }

                                    override fun onReceivedSslError(view: WebView?, handler: android.webkit.SslErrorHandler?, error: android.net.http.SslError?) {
                                        handler?.proceed()
                                    }
                                }
                                loadDataWithBaseURL(
                                    "https://maps.googleapis.com/",
                                    previewHtml,
                                    "text/html",
                                    "UTF-8",
                                    null
                                )
                            }
                        },
                        update = { view ->
                            if (isMapReady && hasCoordinates) {
                                val latStr = String.format(Locale.US, "%.6f", gps.latitude)
                                val lngStr = String.format(Locale.US, "%.6f", gps.longitude)
                                val accStr = String.format(Locale.US, "%.1f", gps.accuracy)
                                view.evaluateJavascript("if (window.updateLocation) { window.updateLocation($latStr, $lngStr, $accStr); }", null)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Top Left Overlay: Active Layer Badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color.Black.copy(alpha = 0.75f),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(AccessAccordedGreen)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = selectedLayer.label.uppercase(),
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    // Top Right Overlay: Zoom In / Zoom Out
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Black.copy(alpha = 0.75f),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { webViewInstance?.evaluateJavascript("setZoom(-1);", null) },
                                modifier = Modifier
                                    .size(32.dp)
                                    .testTag("preview_map_zoom_out")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Remove,
                                    contentDescription = "Zoom Out",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            IconButton(
                                onClick = { webViewInstance?.evaluateJavascript("setZoom(1);", null) },
                                modifier = Modifier
                                    .size(32.dp)
                                    .testTag("preview_map_zoom_in")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Zoom In",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // Bottom Overlay: GPS Coordinates Badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color.Black.copy(alpha = 0.75f),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${gps.formattedLatitude}, ${gps.formattedLongitude}",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Refreshing Badge when update is in-flight
                    if (isRefreshingGps) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = TelecomNavyPrimary.copy(alpha = 0.9f),
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    strokeWidth = 1.5.dp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Updating GPS...",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                } else if (gpsTimedOut) {
                    MapGpsErrorPlaceholder(
                        onRetry = {
                            gpsTimedOut = false
                            onRefreshGps()
                        },
                        modifier = Modifier.fillMaxSize(),
                        errorMessage = "No satellite fix received after scanning.",
                        detailedHelp = "Verify GPS toggle is enabled in Android settings and app has location permission."
                    )
                } else {
                    MapGpsLoadingPlaceholder(
                        modifier = Modifier.fillMaxSize(),
                        statusText = if (isRefreshingGps) "Refreshing Field Coordinates..." else "Acquiring Device GPS Fix...",
                        subText = "Connecting to Google Play Services Location Provider..."
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenMap,
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .testTag("preview_open_radar_button"),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Explore,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Interactive Radar", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                    onClick = {
                        if (hasCoordinates) {
                            try {
                                val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse(gps.mapsUrl))
                                mapIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                context.startActivity(mapIntent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "No map application available", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            onRefreshGps()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .testTag("preview_open_google_maps_button"),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = if (hasCoordinates) Icons.Default.OpenInNew else Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (hasCoordinates) "Google Maps" else "Acquire GPS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
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

private fun generatePreviewMapHtml(
    context: android.content.Context,
    lat: Double,
    lng: Double,
    accuracy: Float
): String {
    val latStr = String.format(Locale.US, "%.6f", lat)
    val lngStr = String.format(Locale.US, "%.6f", lng)
    val accStr = String.format(Locale.US, "%.1f", accuracy)
    val localCss = LeafletAssetManager.getInlinedCss(context)
    val localJs = LeafletAssetManager.getInlinedJs(context)

    val template = """
<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
    <title>Preview Map</title>
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
                zoom: 16,
                zoomControl: false,
                layers: [googleHybrid]
            });

            window.googleHybridLayer = googleHybrid;
            window.googleStreetsLayer = googleStreets;

            let techMarker = null;
            let accuracyCircle = null;

            window.setLayer = function(layerKey) {
                if (!window.map) return;
                if (layerKey === 'streets') {
                    if (window.map.hasLayer(window.googleHybridLayer)) window.map.removeLayer(window.googleHybridLayer);
                    if (!window.map.hasLayer(window.googleStreetsLayer)) window.map.addLayer(window.googleStreetsLayer);
                } else {
                    if (window.map.hasLayer(window.googleStreetsLayer)) window.map.removeLayer(window.googleStreetsLayer);
                    if (!window.map.hasLayer(window.googleHybridLayer)) window.map.addLayer(window.googleHybridLayer);
                }
            };

            window.updateLocation = function(lat, lng, acc) {
                if (!window.map || !lat || !lng) return;
                const pos = [lat, lng];
                if (!techMarker) {
                    const icon = L.divIcon({
                        className: 'tech-div-icon',
                        html: '<div class="tech-marker"></div>',
                        iconSize: [22, 22],
                        iconAnchor: [11, 11]
                    });
                    techMarker = L.marker(pos, { icon: icon, zIndexOffset: 1000 }).addTo(window.map);
                    accuracyCircle = L.circle(pos, {
                        radius: acc || 15,
                        color: '#3b82f6',
                        fillColor: '#3b82f6',
                        fillOpacity: 0.15,
                        weight: 1
                    }).addTo(window.map);
                } else {
                    techMarker.setLatLng(pos);
                    if (accuracyCircle) {
                        accuracyCircle.setLatLng(pos);
                        if (acc) accuracyCircle.setRadius(acc);
                    }
                }
                window.map.panTo(pos);
                window.map.invalidateSize(true);
            };

            window.centerMap = function(lat, lng) {
                if (window.map && lat && lng) {
                    window.map.flyTo([lat, lng], 16, { animate: true, duration: 0.8 });
                    window.map.invalidateSize(true);
                }
            };

            window.setZoom = function(delta) {
                if (!window.map) return;
                if (delta > 0) {
                    window.map.zoomIn();
                } else {
                    window.map.zoomOut();
                }
            };

            // Force recalculation of container dimensions
            window.map.invalidateSize(true);
            setTimeout(function() { if (window.map) window.map.invalidateSize(true); }, 150);
            setTimeout(function() { if (window.map) window.map.invalidateSize(true); }, 500);
            setTimeout(function() { if (window.map) window.map.invalidateSize(true); }, 1000);

            if (window.ResizeObserver) {
                new ResizeObserver(function() {
                    if (window.map) window.map.invalidateSize(true);
                }).observe(document.getElementById('map'));
            }

            window.updateLocation($latStr, $lngStr, $accStr);
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
