package com.example

import android.app.Application
import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class TelecomApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        ensureFirebaseInitialized(this)
    }

    companion object {
        fun ensureFirebaseInitialized(context: Context) {
            val appContext = context.applicationContext ?: context
            try {
                if (FirebaseApp.getApps(appContext).isEmpty()) {
                    val defaultApp = FirebaseApp.initializeApp(appContext)
                    if (defaultApp == null) {
                        FirebaseApp.initializeApp(
                            appContext,
                            FirebaseOptions.Builder()
                                .setApplicationId("1:228203708271:android:b6c49ab9c20d2d61ef729a")
                                .setProjectId("gen-lang-client-0924150504")
                                .setApiKey("AIzaSyApgpwHgESkMnwfJ60JfNswD_U-gZ_M8Ww")
                                .setStorageBucket("gen-lang-client-0924150504.firebasestorage.app")
                                .build()
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("TelecomApp", "Firebase initialization fallback error: ${e.message}", e)
                try {
                    if (FirebaseApp.getApps(appContext).isEmpty()) {
                        FirebaseApp.initializeApp(
                            appContext,
                            FirebaseOptions.Builder()
                                .setApplicationId("1:228203708271:android:b6c49ab9c20d2d61ef729a")
                                .setProjectId("gen-lang-client-0924150504")
                                .setApiKey("AIzaSyApgpwHgESkMnwfJ60JfNswD_U-gZ_M8Ww")
                                .build()
                        )
                    }
                } catch (ignored: Exception) {
                    Log.e("TelecomApp", "Could not initialize Firebase: ${ignored.message}")
                }
            }
        }
    }
}
