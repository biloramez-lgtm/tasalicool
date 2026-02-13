package com.example.tasalicool.network

import com.example.tasalicool.models.*
import com.google.gson.Gson
import kotlinx.coroutines.*
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean

class NetworkGameClient(
    private val gameEngine: Game400Engine
) {

    private var socket: Socket? = null
    private var input: DataInputStream? = null
    private var output: DataOutputStream? = null

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val gson = Gson()

    private val isConnected = AtomicBoolean(false)

    var playerId: String = "P_${System.currentTimeMillis()}"

    /* ================= CONNECT ================= */

    fun connect(
        hostIp: String,
        port: Int = 5000,
        onConnected: () -> Unit = {},
        onDisconnected: () -> Unit = {}
    ) {
        if (isConnected.get()) return

        scope.launch {
            try {
                socket = Socket(hostIp, port)
                input = DataInputStream(socket!!.inputStream)
                output = DataOutputStream(socket!!.outputStream)

                isConnected.set(true)
                onConnected()

                sendMessage(
                    NetworkMessage.createJoin(
                        playerId = playerId,
                        name = "Player"
                    )
                )

                listen(onDisconnected)

            } catch (_: Exception) {
                isConnected.set(false)
                onDisconnected()
            }
        }
    }

    /* ================= LISTEN ================= */

    private fun listen(onDisconnected: () -> Unit) {
        scope.launch {
            try {
                while (isActive && isConnected.get()) {

                    val json = input?.readUTF() ?: break
                    val message = NetworkMessage.fromJson(json)

                    when (message.action) {

                        GameAction.SYNC_STATE -> {
                            message.payload?.let {
                                applyGameState(it)
                            }
                        }

                        GameAction.ERROR -> {
                            println("❌ Server error: ${message.payload}")
                        }

                        GameAction.PONG -> {
                            // اتصال سليم
                        }

                        else -> {}
                    }
                }

            } catch (_: Exception) {
            } finally {
                disconnectInternal()
                onDisconnected()
            }
        }
    }

    /* ================= APPLY STATE ================= */

    private fun applyGameState(stateJson: String) {

        val serverEngine =
            gson.fromJson(
                stateJson,
                Game400Engine::class.java
            )

        // مزامنة مباشرة آمنة
        gameEngine.players.clear()
        gameEngine.players.addAll(serverEngine.players)

        gameEngine.currentTrick.clear()
        gameEngine.currentTrick.addAll(serverEngine.currentTrick)

        gameEngine.startNewRound() // لضمان تحديث الحالة داخلياً
    }

    /* ================= PLAY CARD ================= */

    fun playCard(card: Card) {

        if (!isConnected.get()) return

        if (gameEngine.getCurrentPlayer().id != playerId) return

        val message = NetworkMessage.createPlayCard(
            playerId = playerId,
            cardString = card.toString(),
            trick = gameEngine.trickNumber
        )

        sendMessage(message)
    }

    /* ================= PLACE BID ================= */

    fun placeBid(bid: Int) {

        if (!isConnected.get()) return

        val message = NetworkMessage.createPlaceBid(
            playerId = playerId,
            bidValue = bid
        )

        sendMessage(message)
    }

    /* ================= REQUEST SYNC ================= */

    fun requestSync() {
        sendMessage(
            NetworkMessage(
                playerId = playerId,
                action = GameAction.REQUEST_SYNC
            )
        )
    }

    /* ================= SEND ================= */

    private fun sendMessage(message: NetworkMessage) {

        if (!isConnected.get()) return

        scope.launch {
            try {
                val json = NetworkMessage.toJson(message)
                output?.writeUTF(json)
                output?.flush()
            } catch (_: Exception) {
                disconnectInternal()
            }
        }
    }

    /* ================= DISCONNECT ================= */

    fun disconnect() {
        if (!isConnected.get()) return

        sendMessage(
            NetworkMessage.createLeave(playerId)
        )

        disconnectInternal()
    }

    private fun disconnectInternal() {

        isConnected.set(false)

        try { socket?.close() } catch (_: Exception) {}

        socket = null
        input = null
        output = null
    }
}
