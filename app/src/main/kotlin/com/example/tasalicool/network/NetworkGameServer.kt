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
    private val gameEngine: Game400Engine
) {

    private var onClientConnected: ((String) -> Unit)? = null
    private var onClientDisconnected: ((String) -> Unit)? = null
    private var onGameUpdated: (() -> Unit)? = null

    private var serverSocket: ServerSocket? = null
    private val clients = CopyOnWriteArrayList<ClientConnection>()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val isRunning = AtomicBoolean(false)
    private val playerCounter = AtomicInteger(0)

    private val lobby = LobbyManager()
    private val networkPlayerMap = mutableMapOf<String, Player>()

    /* ================= START SERVER ================= */

    fun startServer(
        onClientConnected: ((String) -> Unit)? = null,
        onClientDisconnected: ((String) -> Unit)? = null,
        onGameUpdated: (() -> Unit)? = null
    ) {

        if (isRunning.get()) return

        this.onClientConnected = onClientConnected
        this.onClientDisconnected = onClientDisconnected
        this.onGameUpdated = onGameUpdated

        scope.launch {
            try {
                serverSocket = ServerSocket(port)
                isRunning.set(true)

                while (isActive && isRunning.get()) {

                    val socket = serverSocket?.accept() ?: continue
                    val networkId = "P${playerCounter.incrementAndGet()}"

                    val client = ClientConnection(socket, networkId)
                    clients.add(client)

                    this@NetworkGameServer.onClientConnected?.invoke(networkId)

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

    /* ================= START GAME ================= */

    private fun handleStartGame(client: ClientConnection) {

        val host = lobby.getHost() ?: return
        if (host.networkId != client.playerId) return
        if (!lobby.startGame()) return

        gameEngine.startGameFromLobby()

        mapNetworkPlayersToEngine()
        broadcastFullState()
    }

    /* ================= MAP ================= */

    private fun mapNetworkPlayersToEngine() {

        networkPlayerMap.clear()

        val humanPlayers =
            gameEngine.players.filter { it.type == PlayerType.HUMAN }

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

        if (gameEngine.phase != GamePhase.PLAYING) return

        val player = networkPlayerMap[client.playerId] ?: return
        if (gameEngine.getCurrentPlayer() != player) return

        val card = Card.fromString(message.payload ?: return) ?: return
        if (!gameEngine.playCard(player, card)) return

        processAITurns()
        broadcastFullState()
    }

    /* ================= AI ================= */

    private fun processAITurns() {

        while (
            gameEngine.phase == GamePhase.PLAYING &&
            gameEngine.isAITurn()
        ) {
            val ai = gameEngine.getCurrentPlayer()
            val card = AdvancedAI.chooseCard(ai, gameEngine)
            gameEngine.playCard(ai, card)
        }
    }

    /* ================= STATE ================= */

    private fun broadcastFullState() {

        val stateJson =
            NetworkMessage.getGson().toJson(gameEngine)

        val message =
            NetworkMessage.createStateSync(
                hostId = "SERVER",
                stateJson = stateJson,
                trick = gameEngine.trickNumber
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
