package com.example.telephony

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.example.data.model.GsmData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Android Background Service to monitor and collect cellular network information
 * including RSRP signal strength, network type (LTE/5G), Cell ID (CID/ECI), LAC/TAC,
 * and carrier metrics for inclusion in site audit logs.
 */
class CellularMonitoringService : Service() {

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var pollingJob: Job? = null
    private lateinit var gsmManager: GsmManager

    inner class LocalBinder : Binder() {
        fun getService(): CellularMonitoringService = this@CellularMonitoringService
        fun getLatestGsmData(): GsmData = _cellularData.value
        fun refreshTelemetryNow(): GsmData = readAndPublishTelemetry()
    }

    override fun onCreate() {
        super.onCreate()
        gsmManager = GsmManager(applicationContext)
        Log.i(TAG, "CellularMonitoringService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START
        when (action) {
            ACTION_STOP -> {
                Log.i(TAG, "Stopping cellular telemetry monitoring")
                stopPolling()
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_REFRESH_NOW -> {
                serviceScope.launch {
                    readAndPublishTelemetry()
                }
            }
            ACTION_START -> {
                startPolling()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder {
        startPolling()
        return binder
    }

    private fun startPolling() {
        if (pollingJob?.isActive == true) return

        pollingJob = serviceScope.launch {
            Log.i(TAG, "Cellular monitoring loop started")
            while (isActive) {
                try {
                    readAndPublishTelemetry()
                } catch (e: Exception) {
                    Log.w(TAG, "Error collecting cellular telemetry: ${e.message}")
                }
                // Poll every 12 seconds for fresh RSRP, Cell ID and LTE/5G state
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    private fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    fun readAndPublishTelemetry(): GsmData {
        val data = try {
            gsmManager.readCurrentCellInfo()
        } catch (e: Exception) {
            Log.e(TAG, "Failed reading cell info: ${e.message}")
            _cellularData.value
        }
        _cellularData.value = data
        return data
    }

    override fun onDestroy() {
        super.onDestroy()
        stopPolling()
        serviceScope.cancel()
        Log.i(TAG, "CellularMonitoringService destroyed")
    }

    companion object {
        private const val TAG = "CellularMonitoringSvc"
        private const val POLL_INTERVAL_MS = 12_000L

        const val ACTION_START = "com.example.telephony.ACTION_START_MONITORING"
        const val ACTION_STOP = "com.example.telephony.ACTION_STOP_MONITORING"
        const val ACTION_REFRESH_NOW = "com.example.telephony.ACTION_REFRESH_NOW"

        private val _cellularData = MutableStateFlow(
            GsmData(
                operatorName = "Mobiserve Telecom",
                networkType = "LTE 4G",
                mcc = "603",
                mnc = "01",
                lacTac = "1042",
                cellId = "48192",
                signalDbm = -85,
                rsrpDbm = -85,
                signalLevel = "Good (-85 dBm)",
                isRealSensor = false
            )
        )
        val cellularData: StateFlow<GsmData> = _cellularData.asStateFlow()

        fun start(context: Context) {
            val intent = Intent(context, CellularMonitoringService::class.java).apply {
                action = ACTION_START
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                Log.w(TAG, "Cannot startService directly: ${e.message}")
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, CellularMonitoringService::class.java).apply {
                action = ACTION_STOP
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                Log.w(TAG, "Cannot stopService directly: ${e.message}")
            }
        }

        fun updateTelemetry(data: GsmData) {
            _cellularData.value = data
        }

        fun getLatestGsmData(): GsmData = _cellularData.value
    }
}
