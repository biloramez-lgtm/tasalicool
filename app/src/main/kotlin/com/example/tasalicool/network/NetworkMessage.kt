package com.example.tasalicool.network

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import java.io.Serializable
import java.util.UUID

data class NetworkMessage(

    val messageId: String = UUID.randomUUID().toString(),

    val playerId: String,
    val gameType: String,

    val action: GameAction,

    /**
     * البيانات الفعلية (GameState / Player / Card / String ...)
     */
    val payload: String? = null,

    val timestamp: Long = System.currentTimeMillis(),

    /**
     * هل هذه رسالة رد تأكيد
     */
    val isAck: Boolean = false

) : Serializable {

    companion object {

        private val gson = Gson()

        /* ================= JSON ================= */

        fun toJson(message: NetworkMessage): String {
            return gson.toJson(message)
        }

        fun fromJson(json: String): NetworkMessage? {
            return try {
                gson.fromJson(json, NetworkMessage::class.java)
            } catch (e: JsonSyntaxException) {
                null
            }
        }

        /* ================= Payload Helpers ================= */

        fun <T> encodePayload(data: T): String {
            return gson.toJson(data)
        }

        inline fun <reified T> decodePayload(payload: String?): T? {
            return try {
                if (payload == null) null
                else gson.fromJson(payload, T::class.java)
            } catch (e: Exception) {
                null
            }
        }
    }
}

/* ======================================================= */
/* ================= GAME ACTIONS ======================== */
/* ======================================================= */

enum class GameAction {

    /* ===== CONNECTION ===== */

    JOIN,
    LEAVE,
    HEARTBEAT,
    ACK,

    /* ===== GAME FLOW ===== */

    START_GAME,
    DEAL_CARDS,
    PLAY_CARD,
    END_TRICK,
    END_ROUND,

    /* ===== SYNC ===== */

    UPDATE_GAME_STATE,
    SYNC_STATE,
    RESYNC_REQUEST,

    /* ===== CHAT ===== */

    MESSAGE
}
