package com.example.tasalicool.network

import com.google.gson.Gson
import java.io.Serializable

data class NetworkMessage(
    val playerId: String,
    val gameType: String,
    val action: NetworkActions,
    val payload: Map<String, String>? = null,
    val timestamp: Long = System.currentTimeMillis()
) : Serializable {

    companion object {
        private val gson = Gson()

        fun toJson(message: NetworkMessage): String {
            return gson.toJson(message)
        }

        fun fromJson(json: String): NetworkMessage {
            return gson.fromJson(json, NetworkMessage::class.java)
        }
    }
}
