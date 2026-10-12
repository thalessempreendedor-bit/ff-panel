package com.ff.panel

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import kotlin.concurrent.thread

class FFVpnService : VpnService() {
    private var tun: ParcelFileDescriptor? = null
    @Volatile private var running = true

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundNotif()
        if (tun != null) return START_STICKY
        val b = Builder()
            .setSession("FF Panel")
            .addAddress("10.0.0.2", 32)
            .addRoute("0.0.0.0", 0)
            .addDnsServer("1.1.1.1")
        try { b.addDisallowedApplication(packageName) } catch (_: Exception) {}
        try { b.addAllowedApplication("com.dts.freefireth") } catch (_: Exception) {}
        try { b.addAllowedApplication("com.dts.freefiremax") } catch (_: Exception) {}
        tun = b.establish()
        thread(name = "vpn-reader") { tunLoop() }
        return START_STICKY
    }

    private fun tunLoop() {
        val pfd = tun ?: return
        val input = FileInputStream(pfd.fileDescriptor)
        val output = FileOutputStream(pfd.fileDescriptor)
        val buffer = ByteArray(32767)
        val relay = mutableMapOf<String, DatagramSocket>()

        while (running) {
            val n = try { input.read(buffer) } catch (e: Exception) { break }
            if (n <= 0) continue
            val version = (buffer[0].toInt() and 0xF0) shr 4
            if (version != 4) continue
            val ihl = (buffer[0].toInt() and 0x0F) * 4
            val proto = buffer[9].toInt() and 0xFF
            if (proto != 17) continue
            val srcIp = byteArrayOf(buffer[12], buffer[13], buffer[14], buffer[15])
            val dstIp = byteArrayOf(buffer[16], buffer[17], buffer[18], buffer[19])
            val srcPort = ((buffer[ihl].toInt() and 0xFF) shl 8) or (buffer[ihl+1].toInt() and 0xFF)
            val dstPort = ((buffer[ihl+2].toInt() and 0xFF) shl 8) or (buffer[ihl+3].toInt() and 0xFF)
            val off = ihl + 8
            val plen = n - off
            if (dstPort !in PacketParser.FF_PORTS && srcPort !in PacketParser.FF_PORTS) continue
            PacketParser.parse(buffer.copyOfRange(off, off + plen), plen)
            ParserBridge.notifyUpdate()

            val key = "${dstIp.joinToString(".")}:$dstPort"
            val sock = relay.getOrPut(key) {
                DatagramSocket().also { protect(it); it.soTimeout = 2000 }
            }
            try {
                sock.send(DatagramPacket(buffer, off, plen,
                    InetSocketAddress(InetAddress.getByAddress(dstIp), dstPort)))
            } catch (_: Exception) {}
            try {
                val resp = DatagramPacket(buffer, buffer.size)
                sock.receive(resp)
                val rlen = resp.length
                PacketParser.parse(buffer.copyOfRange(0, rlen), rlen)
                ParserBridge.notifyUpdate()
                val out = ByteBuffer.allocate(ihl + 8 + rlen)
                System.arraycopy(buffer, 0, out.array(), 0, ihl)
                out.array()[12]=dstIp[0]; out.array()[13]=dstIp[1]
                out.array()[14]=dstIp[2]; out.array()[15]=dstIp[3]
                out.array()[16]=srcIp[0]; out.array()[17]=srcIp[1]
                out.array()[18]=srcIp[2]; out.array()[19]=srcIp[3]
                out.array()[ihl]   = ((dstPort shr 8) and 0xFF).toByte()
                out.array()[ihl+1] = (dstPort and 0xFF).toByte()
                out.array()[ihl+2] = ((srcPort shr 8) and 0xFF).toByte()
                out.array()[ihl+3] = (srcPort and 0xFF).toByte()
                val ulen = 8 + rlen
                out.array()[ihl+4] = ((ulen shr 8) and 0xFF).toByte()
                out.array()[ihl+5] = (ulen and 0xFF).toByte()
                System.arraycopy(resp.data, 0, out.array(), ihl + 8, rlen)
                output.write(out.array(), 0, ihl + 8 + rlen)
            } catch (_: Exception) {}
        }
    }

    private fun startForegroundNotif() {
        val ch = NotificationChannel("ffp", "FF Panel", NotificationManager.IMPORTANCE_LOW)
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(ch)
        val n = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(this, "ffp")
                else Notification.Builder(this)
        startForeground(1, n.setContentTitle("FF Panel ativo")
            .setSmallIcon(android.R.drawable.ic_menu_view).build())
    }

    override fun onDestroy() {
        running = false; tun?.close(); super.onDestroy()
    }
}
