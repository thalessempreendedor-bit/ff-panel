package com.ff.panel

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.WindowManager

class OverlayService : Service() {
    private lateinit var wm: WindowManager
    private lateinit var esp: ESPView

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        esp = ESPView(this)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT)
        wm.addView(esp, params)
        ParserBridge.onUpdate = {
            esp.postInvalidateOnAnimation()
            Aimbot.tick(esp)
        }
        Aimbot.start()
    }

    override fun onDestroy() {
        ParserBridge.onUpdate = null
        Aimbot.stop()
        wm.removeView(esp)
        super.onDestroy()
    }

    override fun onBind(i: Intent?): IBinder? = null
}
