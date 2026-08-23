package com.example.myapplication

import android.content.SharedPreferences
import android.os.SystemClock

object SessionTimer {
    var startTime: Long = SystemClock.elapsedRealtime()
    var maxMinutes: Int = 60

    fun elapsedMinutes(): Int = ((SystemClock.elapsedRealtime() - startTime) / 60000).toInt()

    fun isOverLimit(): Boolean = elapsedMinutes() > maxMinutes

    fun reset() {
        startTime = SystemClock.elapsedRealtime()
    }

    fun load(prefs: SharedPreferences) {
        maxMinutes = prefs.getInt("watch_max_minutes", 60)
    }

    fun save(prefs: SharedPreferences) {
        prefs.edit().putInt("watch_max_minutes", maxMinutes).apply()
    }
}
