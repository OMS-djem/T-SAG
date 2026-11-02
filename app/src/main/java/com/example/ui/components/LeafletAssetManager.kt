package com.example.ui.components

import android.content.Context
import android.util.Log

object LeafletAssetManager {
    @Volatile
    private var cachedCss: String? = null

    @Volatile
    private var cachedJs: String? = null

    fun getInlinedCss(context: Context): String {
        cachedCss?.let { return it }
        return synchronized(this) {
            cachedCss ?: try {
                val lCss = context.assets.open("leaflet/leaflet.css").bufferedReader().use { it.readText() }
                val cCss = context.assets.open("leaflet/MarkerCluster.css").bufferedReader().use { it.readText() }
                val dCss = context.assets.open("leaflet/MarkerCluster.Default.css").bufferedReader().use { it.readText() }
                val combined = "$lCss\n$cCss\n$dCss"
                cachedCss = combined
                combined
            } catch (e: Exception) {
                Log.e("LeafletAssetManager", "Failed to load Leaflet CSS assets", e)
                ""
            }
        }
    }

    fun getInlinedJs(context: Context): String {
        cachedJs?.let { return it }
        return synchronized(this) {
            cachedJs ?: try {
                val lJs = context.assets.open("leaflet/leaflet.js").bufferedReader().use { it.readText() }
                val cJs = context.assets.open("leaflet/leaflet.markercluster.js").bufferedReader().use { it.readText() }
                val combined = "$lJs\n$cJs"
                cachedJs = combined
                combined
            } catch (e: Exception) {
                Log.e("LeafletAssetManager", "Failed to load Leaflet JS assets", e)
                ""
            }
        }
    }
}
