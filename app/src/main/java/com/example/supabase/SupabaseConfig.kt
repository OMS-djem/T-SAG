package com.example.supabase

import android.content.Context
import com.example.BuildConfig

object SupabaseConfig {

    private const val PREFS_NAME = "supabase_config"
    private const val KEY_URL = "supabase_url"
    private const val KEY_ANON = "supabase_anon_key"

    fun getUrl(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val custom = prefs.getString(KEY_URL, null)?.trim()
        if (!custom.isNullOrBlank()) {
            return custom.removeSuffix("/")
        }
        val fromBuildConfig = try {
            BuildConfig.SUPABASE_URL.trim()
        } catch (_: Exception) {
            ""
        }
        return if (fromBuildConfig.isNotBlank() && fromBuildConfig.startsWith("http")) {
            fromBuildConfig.removeSuffix("/")
        } else {
            "https://tsag-telecom-site.supabase.co"
        }
    }

    fun getAnonKey(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val custom = prefs.getString(KEY_ANON, null)?.trim()
        if (!custom.isNullOrBlank()) {
            return custom
        }
        val fromBuildConfig = try {
            BuildConfig.SUPABASE_ANON_KEY.trim()
        } catch (_: Exception) {
            ""
        }
        return fromBuildConfig.ifBlank {
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.dummy_anon_key_for_secrets_build_template"
        }
    }

    fun saveConfig(context: Context, url: String, anonKey: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_URL, url.trim().removeSuffix("/"))
            .putString(KEY_ANON, anonKey.trim())
            .apply()
    }

    fun isConfigured(context: Context): Boolean {
        val url = getUrl(context)
        val key = getAnonKey(context)
        return url.startsWith("https://") && key.isNotBlank() && !key.contains("dummy_anon_key")
    }
}
