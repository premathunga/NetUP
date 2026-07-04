package com.pingoptimizer.pro

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class PingOptimizerApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            val boosterChannel = NotificationChannel(
                CHANNEL_BOOSTER,
                "Network Booster",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows when Game Booster VPN tunnel is active"
            }
            manager.createNotificationChannel(boosterChannel)
        }
    }

    companion object {
        const val CHANNEL_BOOSTER = "network_booster_channel"
    }
}
