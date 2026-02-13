package com.example.tasalicool.network

import com.example.tasalicool.models.*
import com.example.tasalicool.game.AdvancedAI
import kotlinx.coroutines.*
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class NetworkGameServer(
    private val port: Int = 5000,
    private val onClientConnected: ((String) -> Unit)? = null,
    private val onClientDisconnected: ((String) -> Unit)? = null,
    private val onGameUpdated: (() -> Unit)? = null
) {

    private var serverSocket: ServerSocket? = null
    private val clients = CopyOnWriteArrayList<ClientConnection>()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val isRunning = AtomicBoolean(false)
    private val playerCounter = AtomicInteger(0)

    private val lobby = LobbyManager()
    private var gameEngine: Game400Engine? = null
    private val networkPlayerMap = mutableMapOf<String, Player>()

    /* ================= START SERVER ================= */

    fun startServer() {

        if (isRunning.get()) return

        scope.launch {
            try {
                serverSocket = ServerSocket(port)
                isRunning.set(true)

                while (isActive && isRunning.get()) {

                    val socket = serverSocket?.accept() ?: continue
                    val networkId = "P${playerCounter.incrementAndGet()}"

                    val client = ClientConnection(socket, networkId)
                    clients.add(client)

                    onClientConnected?.invoke(networkId)

                    listenToClient(client)
                }

            } catch (_: Exception) {}
        }
    }

    /* ================= LISTEN ================= */

    private fun listenToClient(client: ClientConnection) {

        scope.launch {
            try {
                while (isActive && isRunning.get()) {

                    val json = client.input.readUTF()
                    val message = NetworkMessage.fromJson(json)

                    when (message.action) {

                        GameAction.JOIN -> handleJoin(client, message)

                        GameAction.READY -> {
                            lobby.setReady(client.playerId, true)
                            broadcastLobby()
                        }

                        GameAction.START_GAME -> handleStartGame(client)

                        GameAction.PLAY_CARD -> handlePlayCard(client, message)

                        GameAction.REQUEST_SYNC -> sendFullStateTo(client)

                        GameAction.LEAVE -> removeClient(client)

                        else -> {}
                    }
                }

            } catch (_: Exception) {
                removeClient(client)
            }
        }
    }

    /* ================= JOIN ================= */

    private fun handleJoin(
        client: ClientConnection,
        message: NetworkMessage
    ) {

        val name = message.playerName ?: "Player"

        val lobbyPlayer = lobby.addPlayer(client.playerId, name)

        if (lobbyPlayer == null) {
            sendToClient(
                client,
                NetworkMessage.createError(
                    client.playerId,
                    "Game already started. Waiting next round."
                )
            )
            return
        }

        broadcastLobby()
    }

    /* ================= START GAME ================= */

    private fun handleStartGame(client: ClientConnection) {

        val host = lobby.getHost() ?: return
        if (host.networkId != client.playerId) return
        if (!lobby.startGame()) return

        gameEngine = lobby.createGameEngine()
        gameEngine?.startGameFromLobby()

        mapNetworkPlayersToEngine()
        broadcastFullState()
    }

    /* ================= MAP PLAYERS ================= */

    private fun mapNetworkPlayersToEngine() {

        val engine = gameEngine ?: return

        networkPlayerMap.clear()

        val humanPlayers =
            engine.players.filter { it.type == PlayerType.HUMAN }

        lobby.getPlayers().forEachIndexed { index, lobbyPlayer ->
            if (index < humanPlayers.size) {
                networkPlayerMap[lobbyPlayer.networkId] =
                    humanPlayers[index]
            }
        }
    }

    /* ================= PLAY CARD ================= */

    private fun handlePlayCard(
        client: ClientConnection,
        message: NetworkMessage
    ) {

        val engine = gameEngine ?: return
        if (engine.phase != GamePhase.PLAYING) return

        val player = networkPlayerMap[client.playerId] ?: return
        if (engine.getCurrentPlayer() != player) return

        val card = Card.fromString(message.payload ?: return) ?: return

        if (!engine.playCard(player, card)) return

        processAITurns()
        broadcastFullState()

        if (engine.currentTrick.isEmpty() &&
            engine.lastTrickWinner != null
        ) {
            scope.launch {
                delay(1500)
                engine.clearTrickAfterDelay()
                lobby.replaceAIWithWaitingPlayers(engine)
                broadcastFullState()
            }
        }
    }

    /* ================= AI ================= */

    private fun processAITurns() {

        val engine = gameEngine ?: return

        while (
            engine.phase == GamePhase.PLAYING &&
            engine.isAITurn()
        ) {
            val ai = engine.getCurrentPlayer()
            val card = AdvancedAI.chooseCard(ai, engine)
            engine.playCard(ai, card)
        }
    }

    /* ================= LOBBY ================= */

    private fun broadcastLobby() {

        val lobbyJson = lobby.toJson()

        val message =
            NetworkMessage.createLobbyState(
                hostId = "SERVER",
                lobbyJson = lobbyJson
            )

        clients.forEach {
            sendToClient(it, message)
        }
    }

    /* ================= STATE ================= */

    private fun broadcastFullState() {

        val engine = gameEngine ?: return

        val stateJson =
            NetworkMessage.getGson().toJson(engine)

        val message =
            NetworkMessage.createStateSync(
                hostId = "SERVER",
                stateJson = stateJson,
                trick = engine.trickNumber
            )

        clients.forEach {
            sendToClient(it, message)
        }

        onGameUpdated?.invoke()
    }

    private fun sendFullStateTo(client: ClientConnection) {
        broadcastFullState()
    }

    /* ================= REMOVE ================= */

    private fun removeClient(client: ClientConnection) {

        clients.remove(client)
        lobby.removePlayer(client.playerId)
        networkPlayerMap.remove(client.playerId)

        onClientDisconnected?.invoke(client.playerId)

        try { client.socket.close() } catch (_: Exception) {}

        broadcastLobby()
    }

    /* ================= SEND ================= */

    private fun sendToClient(
        client: ClientConnection,
        message: NetworkMessage
    ) {
        try {
            val json = NetworkMessage.toJson(message)
            client.output.writeUTF(json)
            client.output.flush()
        } catch (_: Exception) {}
    }

    /* ================= STOP ================= */

    fun stopServer() {

        isRunning.set(false)
        scope.cancel()

        clients.forEach {
            try { it.socket.close() } catch (_: Exception) {}
        }

        try { serverSocket?.close() } catch (_: Exception) {}
    }
}

/* ================= CLIENT ================= */

data class ClientConnection(
    val socket: Socket,
    val playerId: String
) {
    val input = DataInputStream(socket.inputStream)
    val output = DataOutputStream(socket.outputStream)
}
