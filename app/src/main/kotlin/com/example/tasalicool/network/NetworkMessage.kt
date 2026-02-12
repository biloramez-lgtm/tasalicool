package com.example.tasalicool.network

import com.google.gson.Gson
import java.io.Serializable
import java.util.UUID

data class NetworkMessage(

    val messageId: String = generateId(),

    val playerId: String,
    val playerName: String? = null,

    val gameType: String = "TASALI_400",

    val action: GameAction,

    // JSON payload (GameState / Card / Text / etc)
    val payload: String? = null,

    val targetPlayerId: String? = null,

    // اختياري لتفادي التعارض
    val roundNumber: Int? = null,
    val trickNumber: Int? = null,

    val isHost: Boolean = false,

    val timestamp: Long = System.currentTimeMillis()

) : Serializable {

    companion object {

        private val gsonInstance = Gson()

        /* ================= JSON ================= */

        fun toJson(message: NetworkMessage): String {
            return gsonInstance.toJson(message)
        }

        fun fromJson(json: String): NetworkMessage {
            return gsonInstance.fromJson(json, NetworkMessage::class.java)
        }

        fun getGson(): Gson = gsonInstance

        /* ================= SAFE VALIDATION ================= */

        fun isValidForTrick(
            message: NetworkMessage,
            currentTrick: Int
        ): Boolean {

            if (message.trickNumber == null) return true
            return message.trickNumber == currentTrick
        }

        /* ================= FACTORY HELPERS ================= */

        fun createStateSync(
            hostId: String,
            stateJson: String,
            trick: Int
        ): NetworkMessage {
            return NetworkMessage(
                playerId = hostId,
                action = GameAction.SYNC_STATE,
                payload = stateJson,
                trickNumber = trick,
                isHost = true
            )
        }

        fun createPlayCard(
            playerId: String,
            cardString: String,
            trick: Int
        ): NetworkMessage {
            return NetworkMessage(
                playerId = playerId,
                action = GameAction.PLAY_CARD,
                payload = cardString,
                trickNumber = trick
            )
        }

        fun createPlaceBid(
            playerId: String,
            bidValue: Int
        ): NetworkMessage {
            return NetworkMessage(
                playerId = playerId,
                action = GameAction.PLACE_BID,
                payload = bidValue.toString()
            )
        }

        fun createJoin(playerId: String, name: String): NetworkMessage {
            return NetworkMessage(
                playerId = playerId,
                playerName = name,
                action = GameAction.JOIN
            )
        }

        fun createLeave(playerId: String): NetworkMessage {
            return NetworkMessage(
                playerId = playerId,
                action = GameAction.LEAVE
            )
        }

        fun createPing(playerId: String): NetworkMessage {
            return NetworkMessage(
                playerId = playerId,
                action = GameAction.PING
            )
        }

        fun createError(playerId: String, message: String): NetworkMessage {
            return NetworkMessage(
                playerId = "SERVER",
                targetPlayerId = playerId,
                action = GameAction.ERROR,
                payload = message,
                isHost = true
            )
        }

        private fun generateId(): String {
            return UUID.randomUUID().toString()
        }
    }
}

/* ===================================================== */
/* ================= GAME ACTIONS ====================== */
/* ===================================================== */

enum class GameAction {

    /* ===== Lobby ===== */
    JOIN,
    LEAVE,
    READY,

    /* ===== Game Flow ===== */
    START_GAME,
    START_ROUND,

    PLAY_CARD,
    PLACE_BID,        // 🔥 أضفناها

    REQUEST_PLAY,

    /* ===== Sync ===== */
    SYNC_STATE,
    REQUEST_SYNC,

    /* ===== AI ===== */
    TRIGGER_AI_MOVE,

    /* ===== Score ===== */
    ROUND_RESULT,
    GAME_OVER,

    /* ===== Utility ===== */
    MESSAGE,
    PING,
    PONG,
    ERROR
}
