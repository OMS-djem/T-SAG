package com.example.data.model

data class GsmData(
    val operatorName: String = "Unknown Carrier",
    val networkType: String = "LTE / 4G",
    val mcc: String = "603", // Default Algeria / Mobile Country Code
    val mnc: String = "01",  // Mobile Network Code (e.g. Mobilis, Djezzy, Ooredoo)
    val lacTac: String = "1042", // Location Area Code / Tracking Area Code
    val cellId: String = "48192", // Cell Identifier (CID / ECI)
    val signalDbm: Int = -85,
    val signalLevel: String = "Good",
    val isRealSensor: Boolean = false
) {
    val summaryString: String
        get() = "$networkType | $operatorName | CID: $cellId | LAC/TAC: $lacTac | MCC-MNC: $mcc-$mnc | $signalDbm dBm"
}
