package com.bgy.farmmanager

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent

/** User-configured screen tap helper. It uses screen gestures only. */
class TapHelperService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private var index = 0
    private var loop: Runnable? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        scheduleCheck()
    }

    private fun scheduleCheck() {
        loop = object : Runnable {
            override fun run() {
                val prefs = getSharedPreferences("wheat_helper", MODE_PRIVATE)
                if (prefs.getBoolean("running", false)) tapNext()
                handler.postDelayed(this, 500)
            }
        }
        handler.post(loop!!)
    }

    private fun tapNext() {
        val prefs = getSharedPreferences("wheat_helper", MODE_PRIVATE)
        val points = prefs.getString("points", "")?.split(";")
            ?.mapNotNull { row ->
                val parts = row.split(",")
                if (parts.size != 2) null else {
                    val x = parts[0].toFloatOrNull()
                    val y = parts[1].toFloatOrNull()
                    if (x == null || y == null) null else Pair(x, y)
                }
            } ?: emptyList()
        if (points.isEmpty()) {
            prefs.edit().putBoolean("running", false).apply()
            return
        }
        val point = points[index % points.size]
        val path = Path().apply { moveTo(point.first, point.second) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 70))
            .build()
        dispatchGesture(gesture, null, null)
        index++
        val delay = prefs.getLong("delay_ms", 900L).coerceIn(350L, 10000L)
        prefs.edit().putLong("next_delay_ms", delay).apply()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {
        getSharedPreferences("wheat_helper", MODE_PRIVATE).edit().putBoolean("running", false).apply()
    }
    override fun onDestroy() {
        loop?.let { handler.removeCallbacks(it) }
        super.onDestroy()
    }
}
