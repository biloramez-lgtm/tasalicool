package com.example.tasalicool.models

data class Player(
    val id: String,
    val name: String,
    var score: Int = 0,
    var bid: Int = 0,
    var tricksWon: Int = 0,
    var isAI: Boolean = false,
    var eloRating: Int = 1000
)
