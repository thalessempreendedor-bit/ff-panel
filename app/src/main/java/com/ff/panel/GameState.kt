package com.ff.panel

data class Player(
    val uid: Int,
    var x: Float, var y: Float, var z: Float,
    var health: Int = 100,
    var team: Int = 0,
    var name: String = "",
    var isLocal: Boolean = false
)

object GameState {
    @Volatile var localUid: Int = -1
    @Volatile var players: Map<Int, Player> = emptyMap()
    @Volatile var lastPacketAt: Long = 0

    @Synchronized fun applySpawn(uid: Int, name: String) {
        val p = players.toMutableMap()
        p[uid] = Player(uid, 0f, 0f, 0f, name = name)
        players = p
    }

    @Synchronized fun applyPos(uid: Int, x: Float, y: Float, z: Float, hp: Int, team: Int) {
        val p = players.toMutableMap()
        val e = p[uid] ?: Player(uid, x, y, z)
        e.x = x; e.y = y; e.z = z; e.health = hp; e.team = team
        p[uid] = e; players = p
        lastPacketAt = System.currentTimeMillis()
    }

    @Synchronized fun applyDespawn(uid: Int) {
        val p = players.toMutableMap(); p.remove(uid); players = p
    }

    @Synchronized fun setLocal(uid: Int) {
        localUid = uid
        val p = players.toMutableMap(); p[uid]?.isLocal = true; players = p
    }

    @Synchronized fun clear() { players = emptyMap(); localUid = -1 }
}
