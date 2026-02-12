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

    // إذا كانت الرسالة موجهة للاعب معين
    val targetPlayerId: String? = null,

    // رقم الجولة لمنع التعارض
    val roundNumber: Int? = null,

    // رقم التريك لمنع السباق
    val trickNumber: Int? = null,

    // هل المرسل هو الهوست؟
    val isHost: Boolean = false,

    // توقيت الإرسال
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

        /* ================= VALIDATION ================= */

        fun isValidForRound(
            message: NetworkMessage,
            currentRound: Int,
            currentTrick: Int
        ): Boolean {

            // إذا لا تحتوي أرقام جولات → نعتبرها عامة
            if (message.roundNumber == null) return true

            if (message.roundNumber != currentRound) return false

            if (message.trickNumber != null &&
                message.trickNumber != currentTrick
            ) return false

            return true
        }

        /* ================= FACTORY HELPERS ================= */

        fun createStateSync(
            hostId: String,
            stateJson: String,
            round: Int,
            trick: Int
        ): NetworkMessage {
            return NetworkMessage(
                playerId = hostId,
                action = GameAction.SYNC_STATE,
                payload = stateJson,
                roundNumber = round,
                trickNumber = trick,
                isHost = true
            )
        }

        fun createPlayCard(
            playerId: String,
            cardJson: String,
            round: Int,
            trick: Int
        ): NetworkMessage {
            return NetworkMessage(
                playerId = playerId,
                action = GameAction.PLAY_CARD,
                payload = cardJson,
                roundNumber = round,
                trickNumber = trick
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
