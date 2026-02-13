package com.example.tasalicool.network

import com.example.tasalicool.models.*
import com.example.tasalicool.game.GameMode
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

class LobbyManager {

    data class LobbyPlayer(
        val networkId: String,
        var name: String,
        var isReady: Boolean = false,
        var isHost: Boolean = false
    )

    private val players = ConcurrentHashMap<String, LobbyPlayer>()
    private val waitingPlayers = CopyOnWriteArrayList<LobbyPlayer>()

    var gameStarted = false
        private set

    private val MAX_PLAYERS = 4

    /* ================= ADD PLAYER ================= */

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
            isHost = isFirst
        )

        players[networkId] = player
        return player
    }

    /* ================= REMOVE ================= */

    fun removePlayer(networkId: String) {

        players.remove(networkId)
        waitingPlayers.removeIf { it.networkId == networkId }

        // إعادة تعيين Host إذا خرج
        if (players.isNotEmpty() && players.values.none { it.isHost }) {
            players.values.first().isHost = true
        }
    }

    /* ================= READY ================= */

    fun setReady(networkId: String, ready: Boolean) {
        players[networkId]?.isReady = ready
    }

    fun areAllHumansReady(): Boolean {
        return players.isNotEmpty() &&
                players.values.all { it.isReady }
    }

    fun getHost(): LobbyPlayer? {
        return players.values.firstOrNull { it.isHost }
    }

    fun getPlayers(): List<LobbyPlayer> {
        return players.values.toList()
    }

    fun getWaitingPlayers(): List<LobbyPlayer> {
        return waitingPlayers.toList()
    }

    fun getHumanCount(): Int {
        return players.size
    }

    fun getRequiredAI(): Int {
        return MAX_PLAYERS - players.size
    }

    /* ================= START GAME ================= */

    fun canStartGame(): Boolean {

        if (gameStarted) return false
        if (players.isEmpty()) return false
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

        val humanCount = players.size

        return Game400Engine(
            gameMode = GameMode.WIFI_MULTIPLAYER,
            humanCount = humanCount
        )
    }

    /* ================= REPLACE AI WITH WAITING ================= */

    fun replaceAIWithWaitingPlayers(engine: Game400Engine) {

        if (waitingPlayers.isEmpty()) return

        val aiPlayers =
            engine.players.filter { it.type == PlayerType.AI }

        if (aiPlayers.isEmpty()) return

        val iterator = waitingPlayers.iterator()

        for (ai in aiPlayers) {

            if (!iterator.hasNext()) break

            val waiting = iterator.next()
            val index = engine.players.indexOf(ai)

            engine.players[index] = Player(
                id = waiting.networkId,
                name = waiting.name,
                type = PlayerType.HUMAN,
                teamId = ai.teamId
            )

            iterator.remove()
        }
    }

    /* ================= RESET AFTER GAME ================= */

    fun resetLobby() {

        gameStarted = false

        players.values.forEach {
            it.isReady = false
        }
    }

    /* ================= SERIALIZE ================= */

    fun toJson(): String {

        return NetworkMessage.getGson().toJson(
            mapOf(
                "players" to players.values,
                "waiting" to waitingPlayers,
                "started" to gameStarted,
                "requiredAI" to getRequiredAI()
            )
        )
    }
}
