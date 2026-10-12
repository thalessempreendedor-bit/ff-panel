package com.ff.panel

import android.os.Handler
import android.os.Looper
import java.io.DataOutputStream
import kotlin.math.*

object Aimbot {
    private val handler = Handler(Looper.getMainLooper())
    private var running = false
    private val smooth = 0.30f
    private val fovPx = 320f

    fun start() { if (!running) { running = true; loop() } }
    fun stop() { running = false }

    fun tick(esp: ESPView) {
        val me = GameState.players[GameState.localUid] ?: return
        val cx = esp.width/2f; val cy = esp.height/2f
        var best: Pair<Float, Float>? = null; var bd = Float.MAX_VALUE
        for ((uid, p) in GameState.players) {
            if (uid == me.uid || p.health <= 0) continue
            if (p.team != 0 && p.team == me.team) continue
            val sp = esp.screenPos[uid] ?: continue
            val d = hypot(sp.first-cx, sp.second-cy)
            if (d < fovPx && d < bd) { bd = d; best = sp }
        }
        val t = best ?: return
        val dx = (t.first - cx) * smooth
        val dy = (t.second - cy) * smooth
        if (abs(dx) < 1.5f && abs(dy) < 1.5f) return
        swipe(dx, dy)
    }

    private fun loop() {
        if (!running) return
        handler.postDelayed({ loop() }, 16)
    }

    private fun swipe(dx: Float, dy: Float) {
        try {
            val su = Runtime.getRuntime().exec("su")
            val os = DataOutputStream(su.outputStream)
            val sx = 540; val sy = 960
            os.writeBytes("input swipe $sx $sy ${(sx+dx).toInt()} ${(sy+dy).toInt()} 12\n")
            os.writeBytes("exit\n"); os.flush(); su.waitFor()
        } catch (_: Exception) {}
    }
}
