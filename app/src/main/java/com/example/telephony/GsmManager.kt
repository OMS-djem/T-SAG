package com.example.telephony

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.telephony.CellInfo
import android.telephony.CellInfoGsm
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.CellInfoWcdma
import android.telephony.TelephonyManager
import com.example.data.model.GsmData

class GsmManager(private val context: Context) {

    private val telephonyManager =
        context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager

    @SuppressLint("MissingPermission")
    fun readCurrentCellInfo(): GsmData {
        if (telephonyManager == null) {
            return fallbackGsmData("Telephony Unavailable")
        }

        try {
            val opName = telephonyManager.networkOperatorName.ifBlank {
                telephonyManager.simOperatorName.ifBlank { "Mobiserve Carrier" }
            }
            val rawOp = telephonyManager.networkOperator ?: ""
            var parsedMcc = if (rawOp.length >= 3) rawOp.substring(0, 3) else "603"
            var parsedMnc = if (rawOp.length >= 5) rawOp.substring(3) else "01"

            val cellInfos: List<CellInfo>? = try {
                telephonyManager.allCellInfo
            } catch (e: SecurityException) {
                null
            } catch (e: Exception) {
                null
            }

            if (!cellInfos.isNullOrEmpty()) {
                for (info in cellInfos) {
                    if (info.isRegistered) {
                        return parseRegisteredCellInfo(info, opName, parsedMcc, parsedMnc)
                    }
                }
                // If none explicitly marked registered, parse first
                val first = cellInfos.first()
                return parseRegisteredCellInfo(first, opName, parsedMcc, parsedMnc)
            }

            // Fallback with network operator info if cellInfo was empty (common on emulators)
            return GsmData(
                operatorName = if (opName.isNotBlank() && opName != "Android") opName else "Mobiserve Telecom",
                networkType = detectNetworkTypeName(telephonyManager),
                mcc = parsedMcc,
                mnc = parsedMnc,
                lacTac = "1042",
                cellId = "48192",
                signalDbm = -82,
                signalLevel = "Good",
                isRealSensor = false
            )
        } catch (e: Exception) {
            return fallbackGsmData(e.localizedMessage ?: "Error reading GSM")
        }
    }

    private fun parseRegisteredCellInfo(
        info: CellInfo,
        fallbackOp: String,
        fallbackMcc: String,
        fallbackMnc: String
    ): GsmData {
        when (info) {
            is CellInfoLte -> {
                val id = info.cellIdentity
                val mcc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    id.mccString ?: fallbackMcc
                } else {
                    id.mcc.takeIf { it != Int.MAX_VALUE }?.toString() ?: fallbackMcc
                }
                val mnc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    id.mncString ?: fallbackMnc
                } else {
                    id.mnc.takeIf { it != Int.MAX_VALUE }?.toString() ?: fallbackMnc
                }
                val tac = id.tac.takeIf { it != Int.MAX_VALUE && it != -1 }?.toString() ?: "1042"
                val ci = id.ci.takeIf { it != Int.MAX_VALUE && it != -1 }?.toString() ?: "48192"
                val dbm = info.cellSignalStrength.dbm.takeIf { it != Int.MAX_VALUE } ?: -85
                return GsmData(
                    operatorName = fallbackOp.ifBlank { "LTE Network" },
                    networkType = "LTE 4G",
                    mcc = mcc,
                    mnc = mnc,
                    lacTac = tac,
                    cellId = ci,
                    signalDbm = dbm,
                    signalLevel = getSignalLevelDescription(dbm),
                    isRealSensor = true
                )
            }
            is CellInfoGsm -> {
                val id = info.cellIdentity
                val mcc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    id.mccString ?: fallbackMcc
                } else {
                    id.mcc.takeIf { it != Int.MAX_VALUE }?.toString() ?: fallbackMcc
                }
                val mnc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    id.mncString ?: fallbackMnc
                } else {
                    id.mnc.takeIf { it != Int.MAX_VALUE }?.toString() ?: fallbackMnc
                }
                val lac = id.lac.takeIf { it != Int.MAX_VALUE && it != -1 }?.toString() ?: "1042"
                val cid = id.cid.takeIf { it != Int.MAX_VALUE && it != -1 }?.toString() ?: "48192"
                val dbm = info.cellSignalStrength.dbm.takeIf { it != Int.MAX_VALUE } ?: -80
                return GsmData(
                    operatorName = fallbackOp.ifBlank { "GSM 2G Network" },
                    networkType = "GSM 2G",
                    mcc = mcc,
                    mnc = mnc,
                    lacTac = lac,
                    cellId = cid,
                    signalDbm = dbm,
                    signalLevel = getSignalLevelDescription(dbm),
                    isRealSensor = true
                )
            }
            is CellInfoWcdma -> {
                val id = info.cellIdentity
                val mcc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    id.mccString ?: fallbackMcc
                } else {
                    id.mcc.takeIf { it != Int.MAX_VALUE }?.toString() ?: fallbackMcc
                }
                val mnc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    id.mncString ?: fallbackMnc
                } else {
                    id.mnc.takeIf { it != Int.MAX_VALUE }?.toString() ?: fallbackMnc
                }
                val lac = id.lac.takeIf { it != Int.MAX_VALUE && it != -1 }?.toString() ?: "1042"
                val cid = id.cid.takeIf { it != Int.MAX_VALUE && it != -1 }?.toString() ?: "48192"
                val dbm = info.cellSignalStrength.dbm.takeIf { it != Int.MAX_VALUE } ?: -85
                return GsmData(
                    operatorName = fallbackOp.ifBlank { "3G UMTS Network" },
                    networkType = "3G WCDMA",
                    mcc = mcc,
                    mnc = mnc,
                    lacTac = lac,
                    cellId = cid,
                    signalDbm = dbm,
                    signalLevel = getSignalLevelDescription(dbm),
                    isRealSensor = true
                )
            }
            else -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && info is CellInfoNr) {
                    val id = info.cellIdentity as? android.telephony.CellIdentityNr
                    val tac = id?.tac?.takeIf { it != Int.MAX_VALUE && it != -1 }?.toString() ?: "1042"
                    val nci = id?.nci?.takeIf { it != Long.MAX_VALUE && it != -1L }?.toString() ?: "48192"
                    val mcc = id?.mccString ?: fallbackMcc
                    val mnc = id?.mncString ?: fallbackMnc
                    val dbm = info.cellSignalStrength.dbm.takeIf { it != Int.MAX_VALUE } ?: -78
                    return GsmData(
                        operatorName = fallbackOp.ifBlank { "5G NR Network" },
                        networkType = "5G NR",
                        mcc = mcc,
                        mnc = mnc,
                        lacTac = tac,
                        cellId = nci,
                        signalDbm = dbm,
                        signalLevel = getSignalLevelDescription(dbm),
                        isRealSensor = true
                    )
                }
                return fallbackGsmData(fallbackOp)
            }
        }
    }

    private fun detectNetworkTypeName(tm: TelephonyManager): String {
        return try {
            when (tm.networkType) {
                TelephonyManager.NETWORK_TYPE_LTE -> "LTE 4G"
                TelephonyManager.NETWORK_TYPE_NR -> "5G NR"
                TelephonyManager.NETWORK_TYPE_HSDPA,
                TelephonyManager.NETWORK_TYPE_HSPA,
                TelephonyManager.NETWORK_TYPE_HSPAP,
                TelephonyManager.NETWORK_TYPE_UMTS -> "3G HSPA+"
                TelephonyManager.NETWORK_TYPE_EDGE,
                TelephonyManager.NETWORK_TYPE_GPRS -> "GSM 2G"
                else -> "LTE / GSM"
            }
        } catch (e: SecurityException) {
            "LTE / GSM"
        }
    }

    private fun getSignalLevelDescription(dbm: Int): String {
        return when {
            dbm >= -75 -> "Excellent (${dbm} dBm)"
            dbm >= -88 -> "Good (${dbm} dBm)"
            dbm >= -100 -> "Moderate (${dbm} dBm)"
            dbm >= -115 -> "Weak (${dbm} dBm)"
            else -> "Poor / Borderline (${dbm} dBm)"
        }
    }

    private fun fallbackGsmData(info: String): GsmData {
        return GsmData(
            operatorName = "Mobiserve Telecom",
            networkType = "LTE 4G",
            mcc = "603",
            mnc = "01",
            lacTac = "1042",
            cellId = "48192",
            signalDbm = -85,
            signalLevel = "Good (-85 dBm)",
            isRealSensor = false
        )
    }
}
