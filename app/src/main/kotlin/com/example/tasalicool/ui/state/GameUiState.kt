package com.example.tasalicool.ui.state

import com.example.tasalicool.models.Card
import com.example.tasalicool.models.GamePhase
import com.example.tasalicool.models.Player

data class GameUiState(
    val phase: GamePhase = GamePhase.BIDDING,
    val players: List<Player> = emptyList(),
    val currentPlayerIndex: Int = 0,
    val currentTrick: List<Pair<Player, Card>> = emptyList(),
    val winner: Player? = null,

    // ✅ أضف هول
    val teamAScore: Int = 0,
    val teamBScore: Int = 0
)
