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

    private val gson = Gson()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val isConnected = AtomicBoolean(false)

    var playerId: String = "Player_${System.currentTimeMillis()}"

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

                // إرسال JOIN
                sendMessage(
                    NetworkMessage(
                        playerId = playerId,
                        gameType = "GAME400",
                        action = GameAction.JOIN,
                        data = null
                    )
                )

                listen(onDisconnected)

            } catch (e: Exception) {
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
                    val message =
                        gson.fromJson(json, NetworkMessage::class.java)

                    when (message.action) {

                        GameAction.UPDATE_GAME_STATE -> {
                            message.data?.let { applyGameState(it) }
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
            gson.fromJson(stateJson, Game400Engine::class.java)

        // تحديث القيم الأساسية
        gameEngine.currentPlayerIndex =
            serverEngine.currentPlayerIndex

        gameEngine.trickNumber =
            serverEngine.trickNumber

        gameEngine.roundActive =
            serverEngine.roundActive

        gameEngine.gameWinner =
            serverEngine.gameWinner

        // تحديث اللاعبين
        serverEngine.players.forEach { serverPlayer ->

            val localPlayer =
                gameEngine.players.find { it.id == serverPlayer.id }

            if (localPlayer != null) {
                localPlayer.updateFromNetwork(serverPlayer)
            }
        }

        gameEngine.currentTrick.clear()
        gameEngine.currentTrick.addAll(serverEngine.currentTrick)
    }

    /* ================= PLAY CARD ================= */

    fun playCard(card: Card) {

        sendMessage(
            NetworkMessage(
                playerId = playerId,
                gameType = "GAME400",
                action = GameAction.PLAY_CARD,
                data = card.toString()
            )
        )
    }

    /* ================= SEND ================= */

    private fun sendMessage(message: NetworkMessage) {
        if (!isConnected.get()) return

        scope.launch {
            try {
                val json = gson.toJson(message)
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
            NetworkMessage(
                playerId = playerId,
                gameType = "GAME400",
                action = GameAction.LEAVE,
                data = null
            )
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
