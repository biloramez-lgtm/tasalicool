package com.example.tasalicool.network

import kotlinx.coroutines.*
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

class NetworkGameServer(private val port: Int = 5000) {

    private var serverSocket: ServerSocket? = null
    private val clients = CopyOnWriteArrayList<ClientConnection>()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val playerCounter = AtomicInteger(1)

    /* ===================================================== */
    /* ================= START SERVER ====================== */
    /* ===================================================== */

    fun startServer(
        onClientConnected: (String) -> Unit = {},
        onClientDisconnected: (String) -> Unit = {},
        onMessageReceived: (NetworkMessage) -> Unit = {}
    ) {
        scope.launch {
            try {
                serverSocket = ServerSocket(port)
                println("🔥 Server started on port $port")

                while (isActive) {
                    val socket = serverSocket?.accept() ?: continue

                    val playerId = "Player_${playerCounter.getAndIncrement()}"
                    val client = ClientConnection(socket, playerId)

                    clients.add(client)

                    println("✅ Client connected: $playerId")
                    onClientConnected(playerId)

                    // إرسال رسالة JOIN للجميع
                    broadcastMessage(
                        NetworkMessage(
                            playerId = playerId,
                            gameType = "TASALI",
                            action = GameAction.JOIN
                        )
                    )

                    listenToClient(client, onClientDisconnected, onMessageReceived)
                }

            } catch (e: Exception) {
                println("❌ Server error: ${e.message}")
            }
        }
    }

    /* ===================================================== */
    /* ================= LISTEN TO CLIENT ================== */
    /* ===================================================== */

    private fun listenToClient(
        client: ClientConnection,
        onClientDisconnected: (String) -> Unit,
        onMessageReceived: (NetworkMessage) -> Unit
    ) {
        scope.launch {
            try {
                while (isActive) {
                    val json = client.input.readUTF()
                    val message = NetworkMessage.fromJson(json)

                    onMessageReceived(message)

                    when (message.action) {

                        GameAction.PLAY_CARD,
                        GameAction.DEAL_CARDS,
                        GameAction.START_GAME,
                        GameAction.UPDATE_GAME_STATE,
                        GameAction.MESSAGE -> {

                            broadcastMessage(
                                message,
                                excludePlayer = client.playerId
                            )
                        }

                        GameAction.LEAVE -> {
                            removeClient(client, onClientDisconnected)
                        }

                        else -> {}
                    }
                }

            } catch (e: Exception) {
                removeClient(client, onClientDisconnected)
            }
        }
    }

    /* ===================================================== */
    /* ================= BROADCAST ========================= */
    /* ===================================================== */

    fun broadcastMessage(
        message: NetworkMessage,
        excludePlayer: String? = null
    ) {
        val json = NetworkMessage.toJson(message)

        clients.forEach { client ->
            if (client.playerId == excludePlayer) return@forEach

            try {
                client.output.writeUTF(json)
                client.output.flush()
            } catch (e: Exception) {
                removeClient(client) {}
            }
        }
    }

    /* ===================================================== */
    /* ================= SEND TO ONE PLAYER ================= */
    /* ===================================================== */

    fun sendToPlayer(playerId: String, message: NetworkMessage) {
        val json = NetworkMessage.toJson(message)

        clients.find { it.playerId == playerId }?.let { client ->
            try {
                client.output.writeUTF(json)
                client.output.flush()
            } catch (e: Exception) {
                removeClient(client) {}
            }
        }
    }

    /* ===================================================== */
    /* ================= REMOVE CLIENT ===================== */
    /* ===================================================== */

    private fun removeClient(
        client: ClientConnection,
        onClientDisconnected: (String) -> Unit
    ) {
        clients.remove(client)

        try { client.socket.close() } catch (_: Exception) {}

        println("🚪 Client disconnected: ${client.playerId}")

        broadcastMessage(
            NetworkMessage(
                playerId = client.playerId,
                gameType = "TASALI",
                action = GameAction.LEAVE
            )
        )

        onClientDisconnected(client.playerId)
    }

    /* ===================================================== */
    /* ================= STOP SERVER ======================= */
    /* ===================================================== */

    fun stopServer() {
        scope.cancel()

        clients.forEach {
            try { it.socket.close() } catch (_: Exception) {}
        }

        try { serverSocket?.close() } catch (_: Exception) {}

        println("🛑 Server stopped")
    }
}

/* ====================================================== */
/* ================= CLIENT CONNECTION ================== */
/* ====================================================== */

data class ClientConnection(
    val socket: Socket,
    val playerId: String
) {
    val input: DataInputStream = DataInputStream(socket.inputStream)
    val output: DataOutputStream = DataOutputStream(socket.outputStream)
}
