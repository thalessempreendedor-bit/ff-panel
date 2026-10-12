package com.ff.panel

import android.content.Context
import android.graphics.*
import android.view.View
import kotlin.math.*

class ESPView(ctx: Context) : View(ctx) {
    private val boxP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 4f
    }
    private val fillP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val textP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textSize = 32f; typeface = Typeface.DEFAULT_BOLD
    }
    val screenPos = mutableMapOf<Int, Pair<Float, Float>>()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        screenPos.clear()
        val me = GameState.players[GameState.localUid] ?: return
        val sw = width.toFloat(); val sh = height.toFloat()
        for ((uid, p) in GameState.players) {
            if (uid == me.uid || p.health <= 0) continue
            if (p.team != 0 && p.team == me.team) continue
            val dx = p.x - me.x; val dy = p.y - me.y; val dz = p.z - me.z
            val dh = hypot(dx, dy); if (dh < 0.3f) continue
            val dist = sqrt(dh*dh + dz*dz)
            val ang = atan2(dy, dx)
            val sx = sw/2f + (ang / (PI.toFloat()/2f)) * sw
            val sy = sh/2f - (dz / dh) * (sh/3f)
            if (sx < 0 || sx > sw || sy < 0 || sy > sh) continue
            screenPos[uid] = sx to sy
            val bw = (1200f / dist).coerceIn(30f, 260f); val h = bw/2f
            boxP.color = if (p.team == me.team) Color.GREEN else Color.RED
            canvas.drawRect(sx-h, sy-h, sx+h, sy+h, boxP)
            fillP.color = Color.YELLOW
            canvas.drawCircle(sx, sy, 4f, fillP)
            val hy = sy + h + 6f
            fillP.color = Color.DKGRAY
            canvas.drawRect(sx-h, hy, sx+h, hy+8f, fillP)
            fillP.color = Color.GREEN
            canvas.drawRect(sx-h, hy, sx-h + bw*(p.health/100f), hy+8f, fillP)
            canvas.drawText("${p.name} ${dist.toInt()}m", sx-h, sy-h-10f, textP)
        }
    }
}
