package com.example.tasalicool.network

import com.google.gson.Gson
import java.io.Serializable

data class NetworkMessage(
    val playerId: String,
    val gameType: String,
    val action: GameAction,
    val data: String? = null,
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

enum class GameAction {
    JOIN,
    LEAVE,
    START_GAME,
    DEAL_CARDS,
    PLAY_CARD,
    UPDATE_GAME_STATE,
    SYNC_STATE,
    MESSAGE
}
