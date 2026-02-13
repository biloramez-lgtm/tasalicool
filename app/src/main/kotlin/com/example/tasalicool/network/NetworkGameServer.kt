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
    private var lobbyUpdateListener: ((String) -> Unit)? = null

    fun setLobbyUpdateListener(listener: (String) -> Unit) {
        lobbyUpdateListener = listener
    }

    private var serverSocket: ServerSocket? = null
    private val clients = CopyOnWriteArrayList<ClientConnection>()
    private var scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val isRunning = AtomicBoolean(false)
    private val playerCounter = AtomicInteger(0)

    private val lobby = LobbyManager()
    private val networkPlayerMap = mutableMapOf<String, Player>()
    private val MAX_PLAYERS = 4

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
                gameEngine.isNetworkClient = false

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

    /* ================= LISTEN CLIENT ================= */

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

    /* ================= LOBBY ================= */

    private fun handleJoin(client: ClientConnection, message: NetworkMessage) {
        val name = message.playerName ?: "Player"
        lobby.addPlayer(client.playerId, name)
        broadcastLobby()
    }

    private fun handleReady(client: ClientConnection) {
        lobby.setReady(client.playerId, true)
        broadcastLobby()
    }

    /* ================= START GAME ================= */

    private fun handleStartGame(client: ClientConnection) {

        val host = lobby.getHost() ?: return
        if (host.networkId != client.playerId) return
        if (!lobby.areAllHumansReady()) return

        // 1️⃣ نكمل اللاعبين بـ AI
        fillWithAIPlayers()

        // 2️⃣ نحدث اللوبي ليظهر 4/4
        broadcastLobby()

        // 3️⃣ نؤخر بدء اللعبة قليلاً حتى يظهر التحديث
        scope.launch {

            delay(300)

            if (!lobby.startGame()) return@launch

            buildEnginePlayersFromLobby()
            gameEngine.startGame()

            broadcastStartGame()
            broadcastFullState()
        }
    }

    fun requestStartFromHost() {
        val host = lobby.getHost() ?: return
        val hostClient =
            clients.firstOrNull { it.playerId == host.networkId } ?: return

        handleStartGame(hostClient)
    }

    /* ================= FILL AI ================= */

    private fun fillWithAIPlayers() {

        val currentSize = lobby.getPlayers().size
        val missing = MAX_PLAYERS - currentSize

        repeat(missing) { index ->
            lobby.addAIPlayer("AI_${index + 1}")
        }
    }

    /* ================= BUILD ENGINE PLAYERS ================= */

    private fun buildEnginePlayersFromLobby() {

        gameEngine.players.clear()
        networkPlayerMap.clear()

        var teamIndex = 0

        lobby.getPlayers().forEach { lobbyPlayer ->

            val player = Player(
                name = lobbyPlayer.name,
                type = if (lobbyPlayer.isAI)
                    PlayerType.AI
                else
                    PlayerType.HUMAN,
                teamId = if (teamIndex % 2 == 0) 1 else 2
            )

            gameEngine.players.add(player)

            if (!lobbyPlayer.isAI) {
                networkPlayerMap[lobbyPlayer.networkId] = player
            }

            teamIndex++
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

    /* ================= BROADCAST ================= */

    private fun broadcastStartGame() {

        broadcast(
            NetworkMessage(
                action = GameAction.START_GAME,
                playerId = "SERVER"
            )
        )
    }

    private fun broadcastFullState() {

        val stateJson =
            NetworkMessage.getGson().toJson(gameEngine)

        broadcast(
            NetworkMessage.createStateSync(
                hostId = "SERVER",
                stateJson = stateJson,
                trick = gameEngine.trickNumber
            )
        )

        CoroutineScope(Dispatchers.Main).launch {
            onGameUpdated?.invoke()
        }
    }

    private fun broadcastLobby() {

        val lobbyJson = lobby.toJson()

        broadcast(
            NetworkMessage.createLobbyState(
                hostId = "SERVER",
                lobbyJson = lobbyJson
            )
        )

        lobbyUpdateListener?.invoke(lobbyJson)
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

    /* ================= DISCONNECT ================= */

    private fun removeClient(client: ClientConnection) {

        clients.remove(client)
        lobby.removePlayer(client.playerId)
        networkPlayerMap.remove(client.playerId)

        try { client.socket.close() } catch (_: Exception) {}

        CoroutineScope(Dispatchers.Main).launch {
            onClientDisconnected?.invoke(client.playerId)
        }

        broadcastLobby()
    }

    /* ================= STOP SERVER ================= */

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
}

/* ================= CLIENT ================= */

data class ClientConnection(
    val socket: Socket,
    val playerId: String
) {
    val input = DataInputStream(socket.inputStream)
    val output = DataOutputStream(socket.outputStream)
}
