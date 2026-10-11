package com.pigeonstudy.pigeon

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager

class PigeonApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Store.init(this)
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel("session", "Study session", NotificationManager.IMPORTANCE_LOW)
        )
    }
}
