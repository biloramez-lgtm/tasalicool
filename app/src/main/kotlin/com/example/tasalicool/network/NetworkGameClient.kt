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

    /* ================= NETWORK ================= */

    private var socket: Socket? = null
    private var input: DataInputStream? = null
    private var output: DataOutputStream? = null

    private var scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val gson = Gson()
    private val isConnected = AtomicBoolean(false)

    var playerId: String = "P_${System.currentTimeMillis()}"

    /* ================= CALLBACKS ================= */

    var onGameStarted: (() -> Unit)? = null
    var onLobbyUpdated: ((String) -> Unit)? = null
    var onStateSynced: (() -> Unit)? = null

    /* ========================================================= */
    /* ========================= CONNECT ======================== */
    /* ========================================================= */

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
                socket?.tcpNoDelay = true

                input = DataInputStream(socket!!.inputStream)
                output = DataOutputStream(socket!!.outputStream)

                isConnected.set(true)

                withContext(Dispatchers.Main) {
                    onConnected()
                }

                sendMessage(
                    NetworkMessage.createJoin(
                        playerId = playerId,
                        name = "Player"
                    )
                )

                listen(onDisconnected)

            } catch (_: Exception) {

                isConnected.set(false)

                withContext(Dispatchers.Main) {
                    onDisconnected()
                }
            }
        }
    }

    /* ========================================================= */
    /* ========================== LISTEN ======================== */
    /* ========================================================= */

    private fun listen(onDisconnected: () -> Unit) {

        scope.launch {

            try {

                while (isActive && isConnected.get()) {

                    val json = input?.readUTF() ?: break
                    val message = NetworkMessage.fromJson(json)

                    when (message.action) {

                        GameAction.LOBBY_STATE -> {
                            message.payload?.let {
                                withContext(Dispatchers.Main) {
                                    onLobbyUpdated?.invoke(it)
                                }
                            }
                        }

                        GameAction.START_GAME -> {
                            withContext(Dispatchers.Main) {
                                onGameStarted?.invoke()
                            }
                        }

                        GameAction.SYNC_STATE -> {
                            message.payload?.let {
                                applyGameState(it)
                                withContext(Dispatchers.Main) {
                                    onStateSynced?.invoke()
                                }
                            }
                        }

                        GameAction.ERROR -> {
                            println("❌ Server error: ${message.payload}")
                        }

                        else -> {}
                    }
                }

            } catch (_: Exception) {
            } finally {

                disconnectInternal()

                withContext(Dispatchers.Main) {
                    onDisconnected()
                }
            }
        }
    }

    /* ========================================================= */
    /* ====================== APPLY GAME STATE ================= */
    /* ========================================================= */

    private fun applyGameState(stateJson: String) {

        val serverEngine =
            gson.fromJson(
                stateJson,
                Game400Engine::class.java
            )

        synchronized(gameEngine) {

            gameEngine.players.clear()
            gameEngine.players.addAll(serverEngine.players)

            gameEngine.currentTrick.clear()
            gameEngine.currentTrick.addAll(serverEngine.currentTrick)

            gameEngine.phase = serverEngine.phase
            gameEngine.trickNumber = serverEngine.trickNumber
        }
    }

    /* ========================================================= */
    /* =========================== READY ======================== */
    /* ========================================================= */

    fun sendReady() {
        if (!isConnected.get()) return

        sendMessage(
            NetworkMessage(
                playerId = playerId,
                action = GameAction.READY
            )
        )
    }

    /* ========================================================= */
    /* ======================== START GAME ===================== */
    /* ========================================================= */

    fun startGame() {
        if (!isConnected.get()) return

        sendMessage(
            NetworkMessage(
                playerId = playerId,
                action = GameAction.START_GAME
            )
        )
    }

    /* ========================================================= */
    /* ========================== PLAY CARD ==================== */
    /* ========================================================= */

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

    /* ========================================================= */
    /* =========================== BID ========================= */
    /* ========================================================= */

    fun placeBid(bid: Int) {

        if (!isConnected.get()) return

        val message = NetworkMessage.createPlaceBid(
            playerId = playerId,
            bidValue = bid
        )

        sendMessage(message)
    }

    /* ========================================================= */
    /* ======================= REQUEST SYNC ==================== */
    /* ========================================================= */

    fun requestSync() {
        sendMessage(
            NetworkMessage(
                playerId = playerId,
                action = GameAction.REQUEST_SYNC
            )
        )
    }

    /* ========================================================= */
    /* =========================== SEND ======================== */
    /* ========================================================= */

    private fun sendMessage(message: NetworkMessage) {

        if (!isConnected.get()) return

        scope.launch {
            try {

                val out = output ?: return@launch
                val json = NetworkMessage.toJson(message)

                out.writeUTF(json)
                out.flush()

            } catch (_: Exception) {
                disconnectInternal()
            }
        }
    }

    /* ========================================================= */
    /* ======================== DISCONNECT ===================== */
    /* ========================================================= */

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

        scope.cancel()
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    }
}
