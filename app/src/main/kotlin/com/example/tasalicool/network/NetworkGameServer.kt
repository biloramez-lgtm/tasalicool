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

    /* ================= CALLBACKS ================= */

    private var onClientConnected: ((String) -> Unit)? = null
    private var onClientDisconnected: ((String) -> Unit)? = null
    private var onGameUpdated: (() -> Unit)? = null

    /* ================= NETWORK ================= */

    private var serverSocket: ServerSocket? = null
    private val clients = CopyOnWriteArrayList<ClientConnection>()
    private var scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val isRunning = AtomicBoolean(false)
    private val playerCounter = AtomicInteger(0)

    /* ================= GAME ================= */

    private val lobby = LobbyManager()
    private val networkPlayerMap = mutableMapOf<String, Player>()

    /* ========================================================= */
    /* ======================= START SERVER ==================== */
    /* ========================================================= */

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
                    socket.tcpNoDelay = true

                    val networkId = "P${playerCounter.incrementAndGet()}"
                    val client = ClientConnection(socket, networkId)

                    clients.add(client)

                    withContext(Dispatchers.Main) {
                        onClientConnected?.invoke(networkId)
                    }

                    listenToClient(client)
                }

            } catch (_: Exception) {
                stopServer()
            }
        }
    }

    /* ========================================================= */
    /* ====================== LISTEN CLIENT ==================== */
    /* ========================================================= */

    private fun listenToClient(client: ClientConnection) {

        scope.launch {

            try {

                while (isActive && isRunning.get()) {

                    val json = client.input.readUTF()
                    val message = NetworkMessage.fromJson(json)

                    when (message.action) {

                        GameAction.JOIN -> handleJoin(client, message)
                        GameAction.READY -> handleReady(client)
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

    /* ========================================================= */
    /* ======================== LOBBY ========================== */
    /* ========================================================= */

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
                    "Game already started."
                )
            )
            return
        }

        broadcastLobby()
    }

    private fun handleReady(client: ClientConnection) {
        lobby.setReady(client.playerId, true)
        broadcastLobby()
    }

    /* ========================================================= */
    /* ====================== START GAME ======================= */
    /* ========================================================= */

    private fun handleStartGame(client: ClientConnection) {

        val host = lobby.getHost() ?: return

        if (host.networkId != client.playerId) return

        // 🔥 لازم يكون العدد 4
        if (lobby.getPlayers().size > 4) return

        // 🔥 كمّل AI إذا أقل من 4
        fillWithAIPlayers()

        // 🔥 تأكد أن الكل Ready
        if (!lobby.canStartGame()) return

        if (!lobby.startGame()) return

        gameEngine.startGame()
        mapNetworkPlayersToEngine()

        broadcastStartGame()
        broadcastFullState()
    }

    /* ========================================================= */
    /* ======================== AI FILL ======================== */
    /* ========================================================= */

    private fun fillWithAIPlayers() {

        val currentSize = lobby.getPlayers().size
        val missing = 4 - currentSize

        repeat(missing) { index ->
            lobby.addAIPlayer("AI_${index + 1}")
        }
    }

    private fun broadcastStartGame() {

        val message = NetworkMessage(
            action = GameAction.START_GAME,
            playerId = "SERVER"
        )

        broadcast(message)
    }

    /* ========================================================= */
    /* ======================= GAME LOGIC ====================== */
    /* ========================================================= */

    private fun mapNetworkPlayersToEngine() {

        networkPlayerMap.clear()

        val humanPlayers =
            gameEngine.players.filter { it.type == PlayerType.HUMAN }

        lobby.getPlayers().forEachIndexed { index, lobbyPlayer ->

            if (!lobbyPlayer.isAI && index < humanPlayers.size) {

                networkPlayerMap[lobbyPlayer.networkId] =
                    humanPlayers[index]
            }
        }
    }

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

    /* ========================================================= */
    /* ======================= BROADCAST ======================= */
    /* ========================================================= */

    private fun broadcastFullState() {

        val stateJson =
            NetworkMessage.getGson().toJson(gameEngine)

        val message =
            NetworkMessage.createStateSync(
                hostId = "SERVER",
                stateJson = stateJson,
                trick = gameEngine.trickNumber
            )

        broadcast(message)

        withContextSafeMain {
            onGameUpdated?.invoke()
        }
    }

    private fun broadcastLobby() {

        val lobbyJson = lobby.toJson()

        val message =
            NetworkMessage.createLobbyState(
                hostId = "SERVER",
                lobbyJson = lobbyJson
            )

        broadcast(message)
    }

    private fun broadcast(message: NetworkMessage) {
        clients.forEach { sendToClient(it, message) }
    }

    private fun sendFullStateTo(client: ClientConnection) {
        sendToClient(
            client,
            NetworkMessage.createStateSync(
                hostId = "SERVER",
                stateJson = NetworkMessage.getGson().toJson(gameEngine),
                trick = gameEngine.trickNumber
            )
        )
    }

    private fun sendToClient(
        client: ClientConnection,
        message: NetworkMessage
    ) {

        try {
            val json = NetworkMessage.toJson(message)
            client.output.writeUTF(json)
            client.output.flush()
        } catch (_: Exception) {
            removeClient(client)
        }
    }

    /* ========================================================= */
    /* ======================= DISCONNECT ====================== */
    /* ========================================================= */

    private fun removeClient(client: ClientConnection) {

        clients.remove(client)
        lobby.removePlayer(client.playerId)
        networkPlayerMap.remove(client.playerId)

        try { client.socket.close() } catch (_: Exception) {}

        withContextSafeMain {
            onClientDisconnected?.invoke(client.playerId)
        }

        broadcastLobby()
    }

    /* ========================================================= */
    /* ======================= STOP SERVER ===================== */
    /* ========================================================= */

    fun stopServer() {

        isRunning.set(false)

        try { serverSocket?.close() } catch (_: Exception) {}

        clients.forEach {
            try { it.socket.close() } catch (_: Exception) {}
        }

        clients.clear()
        scope.cancel()
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    }

    private fun withContextSafeMain(block: () -> Unit) {
        CoroutineScope(Dispatchers.Main).launch {
            block()
        }
    }
}

/* ========================================================= */
/* ================= CLIENT CONNECTION ===================== */
/* ========================================================= */

data class ClientConnection(
    val socket: Socket,
    val playerId: String
) {
    val input = DataInputStream(socket.inputStream)
    val output = DataOutputStream(socket.outputStream)
}
