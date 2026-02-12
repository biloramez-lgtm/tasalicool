package com.example.tasalicool.models

import java.io.Serializable

data class GameState(

    val players: List<Player> = emptyList(),

    val currentPlayerIndex: Int = 0,
    val currentTrick: List<Pair<Player, Card>> = emptyList()

    val trickNumber: Int = 0,
    val roundNumber: Int = 1,

    val gameInProgress: Boolean = false,
    val winnerId: String? = null

) : Serializable {

    /* ===================================================== */
    /* ================= HELPERS =========================== */
    /* ===================================================== */

    fun getCurrentPlayer(): Player? =
        players.getOrNull(currentPlayerIndex)

    fun isRoundFinished(): Boolean =
        players.all { it.hand.isEmpty() }

    fun isGameOver(): Boolean =
        winnerId != null

    fun getTeamScores(): Map<Int, Int> {
        return players
            .groupBy { it.teamId }
            .mapValues { entry ->
                entry.value.sumOf { it.score }
            }
    }
}
