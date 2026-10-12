package com.ff.panel

import java.nio.ByteBuffer
import java.nio.ByteOrder

object PacketParser {
    private val MAGIC = byteArrayOf(0x46, 0x46, 0x01, 0x00)
    val FF_PORTS = 10000..10099

    fun parse(data: ByteArray, len: Int): Boolean {
        if (len < 8) return false
        if (!data.copyOfRange(0, 4).contentEquals(MAGIC)) return false
        val buf = ByteBuffer.wrap(data, 0, len).order(ByteOrder.LITTLE_ENDIAN)
        buf.position(4)
        val length = buf.short.toInt() and 0xFFFF
        val type = buf.short.toInt() and 0xFFFF
        val payload = ByteArray((length - 4).coerceAtLeast(0))
        if (buf.remaining() >= payload.size) buf.get(payload)
        return when (type) {
            0x0A -> pos(payload)
            0x0B -> hp(payload)
            0x0C -> spawn(payload)
            0x0D -> despawn(payload)
            0x10 -> local(payload)
            else -> false
        }
    }

    private fun pos(p: ByteArray): Boolean {
        if (p.size < 18) return false
        val b = ByteBuffer.wrap(p).order(ByteOrder.LITTLE_ENDIAN)
        val uid = b.int; val x = b.float; val y = b.float; val z = b.float
        val hp = b.get().toInt() and 0xFF
        val team = b.get().toInt() and 0xFF
        GameState.applyPos(uid, x, y, z, hp, team)
        return true
    }

    private fun hp(p: ByteArray): Boolean {
        if (p.size < 5) return false
        val b = ByteBuffer.wrap(p).order(ByteOrder.LITTLE_ENDIAN)
        val uid = b.int; val hp = b.get().toInt() and 0xFF
        val c = GameState.players[uid] ?: return false
        GameState.applyPos(uid, c.x, c.y, c.z, hp, c.team)
        return true
    }

    private fun spawn(p: ByteArray): Boolean {
        if (p.size < 8) return false
        val b = ByteBuffer.wrap(p).order(ByteOrder.LITTLE_ENDIAN)
        val uid = b.int
        val nl = b.short.toInt() and 0xFFFF
        val name = if (nl > 0 && p.size >= 8 + nl) String(p, 8, nl, Charsets.UTF_8) else ""
        GameState.applySpawn(uid, name)
        return true
    }

    private fun despawn(p: ByteArray): Boolean {
        if (p.size < 4) return false
        GameState.applyDespawn(ByteBuffer.wrap(p).order(ByteOrder.LITTLE_ENDIAN).int)
        return true
    }

    private fun local(p: ByteArray): Boolean {
        if (p.size < 4) return false
        GameState.setLocal(ByteBuffer.wrap(p).order(ByteOrder.LITTLE_ENDIAN).int)
        return true
    }
}
