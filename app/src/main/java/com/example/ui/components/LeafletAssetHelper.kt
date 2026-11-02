package com.example.ui.components

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader

object LeafletAssetHelper {
    private var cachedLeafletJs: String? = null
    private var cachedLeafletCss: String? = null
    private var cachedMarkerClusterJs: String? = null
    private var cachedMarkerClusterCss: String? = null

    fun getLeafletJs(context: Context): String {
        if (cachedLeafletJs == null) {
            cachedLeafletJs = readAsset(context, "leaflet/leaflet.js")
        }
        return cachedLeafletJs ?: ""
    }

    fun getLeafletCss(context: Context): String {
        if (cachedLeafletCss == null) {
            cachedLeafletCss = readAsset(context, "leaflet/leaflet.css")
        }
        return cachedLeafletCss ?: ""
    }

    fun getMarkerClusterJs(context: Context): String {
        if (cachedMarkerClusterJs == null) {
            cachedMarkerClusterJs = readAsset(context, "leaflet/leaflet.markercluster.js")
        }
        return cachedMarkerClusterJs ?: ""
    }

    fun getMarkerClusterCss(context: Context): String {
        if (cachedMarkerClusterCss == null) {
            val base = readAsset(context, "leaflet/MarkerCluster.css")
            val defaultStyle = readAsset(context, "leaflet/MarkerCluster.Default.css")
            cachedMarkerClusterCss = "$base\n$defaultStyle"
        }
        return cachedMarkerClusterCss ?: ""
    }

    private fun readAsset(context: Context, filename: String): String {
        return try {
            context.assets.open(filename).use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { it.readText() }
            }
        } catch (e: Exception) {
            ""
        }
    }
}
