package com.pingoptimizer.pro.utils

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import android.telephony.TelephonyManager

data class WifiInfoSnapshot(
    val connected: Boolean,
    val ssid: String,
    val rssiDbm: Int,
    val linkSpeedMbps: Int,
    val frequencyMhz: Int,
    val qualityLabel: String
)

data class CellularInfoSnapshot(
    val networkType: String,
    val signalLevel: Int, // 0..4
    val qualityLabel: String
)

object NetworkSignalUtils {

    fun readWifi(context: Context): WifiInfoSnapshot {
        val wifiManager = context.applicationContext
            .getSystemService(Context.WIFI_SERVICE) as WifiManager
        val info = wifiManager.connectionInfo
        val rssi = info?.rssi ?: -100
        val connected = info != null && info.networkId != -1
        val ssid = info?.ssid?.replace("\"", "") ?: "-"
        val linkSpeed = info?.linkSpeed ?: 0
        val frequency = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            info?.frequency ?: 0
        } else 0

        return WifiInfoSnapshot(
            connected = connected,
            ssid = ssid,
            rssiDbm = rssi,
            linkSpeedMbps = linkSpeed,
            frequencyMhz = frequency,
            qualityLabel = rssiQualityLabel(rssi)
        )
    }

    private fun rssiQualityLabel(rssi: Int): String = when {
        rssi >= -50 -> "විශිෂ්ට (Excellent)"
        rssi >= -60 -> "හොඳයි (Good)"
        rssi >= -70 -> "සාමාන්‍ය (Fair)"
        rssi >= -80 -> "දුර්වලයි (Weak)"
        else -> "ඉතා දුර්වලයි (Very Weak)"
    }

    fun readCellular(context: Context): CellularInfoSnapshot {
        val tm = context.applicationContext
            .getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        val networkType = try {
            networkTypeName(tm.dataNetworkType)
        } catch (e: SecurityException) {
            "Unknown"
        }

        // SignalStrength based level requires READ_PHONE_STATE + a listener/callback
        // on older APIs; we surface a best-effort level here and let the UI degrade
        // gracefully if permission isn't granted.
        val level = try {
            tm.signalStrength?.level ?: -1
        } catch (e: SecurityException) {
            -1
        }

        val label = when (level) {
            4 -> "විශිෂ්ට (Excellent)"
            3 -> "හොඳයි (Good)"
            2 -> "සාමාන්‍ය (Fair)"
            1 -> "දුර්වලයි (Weak)"
            0 -> "ඉතා දුර්වලයි (Very Weak)"
            else -> "නොදනී (Unknown)"
        }

        return CellularInfoSnapshot(networkType, level, label)
    }

    private fun networkTypeName(type: Int): String = when (type) {
        TelephonyManager.NETWORK_TYPE_LTE -> "4G LTE"
        TelephonyManager.NETWORK_TYPE_NR -> "5G"
        TelephonyManager.NETWORK_TYPE_HSPA,
        TelephonyManager.NETWORK_TYPE_HSPAP -> "3G (HSPA)"
        TelephonyManager.NETWORK_TYPE_UMTS -> "3G"
        TelephonyManager.NETWORK_TYPE_EDGE,
        TelephonyManager.NETWORK_TYPE_GPRS -> "2G"
        else -> "Unknown"
    }

    /** Simple advisor: recommends Wi-Fi vs Mobile Data for gaming based on current readings. */
    fun recommendBestNetwork(wifi: WifiInfoSnapshot, cellular: CellularInfoSnapshot): String {
        return when {
            wifi.connected && wifi.rssiDbm >= -65 -> "Wi-Fi පාවිච්චි කරන්න - සංඥාව ශක්තිමත්."
            cellular.signalLevel >= 3 -> "Mobile Data (${cellular.networkType}) පාවිච්චි කරන්න - වඩා ස්ථාවරයි."
            wifi.connected -> "Wi-Fi සම්බන්ධයි ඒත් signal එක දුර්වලයි - router එකට ළං වෙන්න."
            else -> "දෙකම දුර්වලයි - වෙනත් location එකකට ගෙන් උත්සාහ කරන්න."
        }
    }
}
