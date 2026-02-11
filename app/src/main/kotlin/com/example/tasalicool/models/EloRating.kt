package com.example.tasalicool.models

class EloRating(
    private val kFactor: Int = 32
) {

    fun calculateNewRating(
        playerRating: Int,
        opponentRating: Int,
        score: Double
    ): Int {

        val expectedScore =
            1.0 / (1 + Math.pow(10.0, (opponentRating - playerRating) / 400.0))

        return (playerRating + kFactor * (score - expectedScore)).toInt()
    }
}
