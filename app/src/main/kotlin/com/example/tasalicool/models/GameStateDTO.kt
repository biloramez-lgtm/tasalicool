package com.example.tasalicool.models

data class GameStateDTO(
    val players: List<Player>,
    val phase: GamePhase,
    val currentPlayerIndex: Int,
    val trickNumber: Int
)
