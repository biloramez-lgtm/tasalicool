package com.example.tasalicool.network

import com.google.gson.Gson
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.ServerSocket
import java.net.Socket

class NetworkGameServer(val port: Int = 5000) {
    
    private var serverSocket: ServerSocket? = null
    private val clients = mutableListOf<ClientConnection>()
    private val gson = Gson()

    fun startServer() {
        serverSocket = ServerSocket(port)
        println("Server started on port $port")
    }

    fun stopServer() {
        serverSocket?.close()
        clients.forEach { it.socket.close() }
    }

    fun acceptConnections() {
        Thread {
            while (true) {
                try {
                    val socket = serverSocket?.accept()
                    if (socket != null) {
                        val client = ClientConnection(socket)
                        clients.add(client)
                        println("Client connected: ${socket.inetAddress}")
                    }
                } catch (e: Exception) {
                    println("Error accepting connection: ${e.message}")
                }
            }
        }.start()
    }

    fun broadcastMessage(message: NetworkMessage) {
        val json = gson.toJson(message)
        clients.forEach { client ->
            try {
                client.output?.writeUTF(json)
                client.output?.flush()
            } catch (e: Exception) {
                println("Error sending message: ${e.message}")
            }
        }
    }
}

class NetworkGameClient(val host: String, val port: Int = 5000) {
    
    private var socket: Socket? = null
    private var input: DataInputStream? = null
    private var output: DataOutputStream? = null
    private val gson = Gson()

    fun connect() {
        socket = Socket(host, port)
        input = DataInputStream(socket?.inputStream)
        output = DataOutputStream(socket?.outputStream)
        println("Connected to server at $host:$port")
    }

    fun disconnect() {
        socket?.close()
    }

    fun sendMessage(message: NetworkMessage) {
        try {
            val json = gson.toJson(message)
            output?.writeUTF(json)
            output?.flush()
        } catch (e: Exception) {
            println("Error sending message: ${e.message}")
        }
    }

    fun receiveMessage(): NetworkMessage? {
        return try {
            val json = input?.readUTF()
            if (json != null) {
                gson.fromJson(json, NetworkMessage::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            println("Error receiving message: ${e.message}")
            null
        }
    }
}

// نموذج الرسالة عبر الشبكة
data class NetworkMessage(
    val playerId: String,
    val gameType: String,
    val action: String,
    val payload: Map<String, String> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis()
)

// فئة الاتصال بالعميل
data class ClientConnection(
    val socket: Socket,
    val input: DataInputStream? = DataInputStream(socket.inputStream),
    val output: DataOutputStream? = DataOutputStream(socket.outputStream),
    val playerId: String = socket.inetAddress.hostAddress
)

// أنواع الإجراءات عبر الشبكة
object NetworkActions {
    const val PLAYER_JOINED = "PLAYER_JOINED"
    const val PLAYER_LEFT = "PLAYER_LEFT"
    const val GAME_STARTED = "GAME_STARTED"
    const val GAME_STATE_UPDATE = "GAME_STATE_UPDATE"
    const val CARD_PLAYED = "CARD_PLAYED"
    const val TURN_CHANGED = "TURN_CHANGED"
    const val GAME_ENDED = "GAME_ENDED"
}
