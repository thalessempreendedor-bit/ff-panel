package com.ff.panel

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    private val VPN_REQ = 1001

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 96, 48, 48)
        }
        val status = TextView(this).apply {
            text = "Concede overlay e liga a VPN."
            textSize = 16f
        }
        layout.addView(status)
        layout.addView(Button(this).apply {
            text = "1. Permissao de overlay"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
            }
        })
        layout.addView(Button(this).apply {
            text = "2. Iniciar overlay"
            setOnClickListener {
                startForegroundService(Intent(this@MainActivity, OverlayService::class.java))
            }
        })
        layout.addView(Button(this).apply {
            text = "3. Ligar captura VPN"
            setOnClickListener {
                val prep = VpnService.prepare(this@MainActivity)
                if (prep != null) startActivityForResult(prep, VPN_REQ)
                else onActivityResult(VPN_REQ, RESULT_OK, null)
            }
        })
        setContentView(layout)
    }

    override fun onActivityResult(req: Int, res: Int, d: Intent?) {
        super.onActivityResult(req, res, d)
        if (req == VPN_REQ && res == RESULT_OK)
            startService(Intent(this, FFVpnService::class.java))
    }
}
