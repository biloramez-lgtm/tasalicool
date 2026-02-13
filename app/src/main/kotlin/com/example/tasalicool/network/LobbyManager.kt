package com.example.tasalicool.network

import com.example.tasalicool.models.*
import com.example.tasalicool.game.GameMode
import java.util.concurrent.CopyOnWriteArrayList

class LobbyManager {

    data class LobbyPlayer(
        val networkId: String,
        var name: String,
        var isReady: Boolean = false,
        var isHost: Boolean = false,
        var isAI: Boolean = false
    )

    private val players = CopyOnWriteArrayList<LobbyPlayer>()
    private val waitingPlayers = CopyOnWriteArrayList<LobbyPlayer>()

    var gameStarted = false
        private set

    private val MAX_PLAYERS = 4

    /* ================= ADD HUMAN ================= */

    fun addPlayer(networkId: String, name: String): LobbyPlayer? {

        if (gameStarted) {
            val waiting = LobbyPlayer(networkId, name)
            waitingPlayers.add(waiting)
            return null
        }

        if (players.size >= MAX_PLAYERS) return null

        val isFirst = players.isEmpty()

        val player = LobbyPlayer(
            networkId = networkId,
            name = name,
            isHost = isFirst,
            isAI = false
        )

        players.add(player)
        return player
    }

    /* ================= ADD AI ================= */

    fun addAIPlayer(name: String) {

        if (players.size >= MAX_PLAYERS) return

        val ai = LobbyPlayer(
            networkId = "AI_${players.size + 1}",
            name = name,
            isReady = true,
            isAI = true
        )

        players.add(ai)
    }

    /* ================= REMOVE ================= */

    fun removePlayer(networkId: String) {

        players.removeIf { it.networkId == networkId }
        waitingPlayers.removeIf { it.networkId == networkId }

        // إعادة تعيين Host
        if (players.isNotEmpty() && players.none { it.isHost }) {
            players.first().isHost = true
        }
    }

    /* ================= READY ================= */

    fun setReady(networkId: String, ready: Boolean) {
        players.find { it.networkId == networkId }?.isReady = ready
    }

    fun areAllHumansReady(): Boolean {
        return players
            .filter { !it.isAI }
            .all { it.isReady }
    }

    fun getHost(): LobbyPlayer? {
        return players.firstOrNull { it.isHost }
    }

    fun getPlayers(): List<LobbyPlayer> {
        return players.toList()
    }

    fun getHumanCount(): Int {
        return players.count { !it.isAI }
    }

    fun getTotalCount(): Int {
        return players.size
    }

    /* ================= START GAME ================= */

    fun canStartGame(): Boolean {

        if (gameStarted) return false
        if (players.isEmpty()) return false
        if (players.size > MAX_PLAYERS) return false

        // لازم يكون العدد النهائي 4
        if (players.size != MAX_PLAYERS) return false

        if (!areAllHumansReady()) return false

        return true
    }

    fun startGame(): Boolean {

        if (!canStartGame()) return false

        gameStarted = true
        return true
    }

    /* ================= CREATE ENGINE ================= */

    fun createGameEngine(): Game400Engine {

        val humanCount = getHumanCount()

        return Game400Engine(
            gameMode = GameMode.WIFI_MULTIPLAYER,
            humanCount = humanCount
        )
    }

    /* ================= RESET AFTER GAME ================= */

    fun resetLobby() {

        gameStarted = false

        players.removeIf { it.isAI }

        players.forEach {
            it.isReady = false
            it.isHost = false
        }

        if (players.isNotEmpty()) {
            players.first().isHost = true
        }
    }

    /* ================= SERIALIZE ================= */

    fun toJson(): String {

        return NetworkMessage.getGson().toJson(
            mapOf(
                "players" to players,
                "started" to gameStarted,
                "humanCount" to getHumanCount()
            )
        )
    }
}
