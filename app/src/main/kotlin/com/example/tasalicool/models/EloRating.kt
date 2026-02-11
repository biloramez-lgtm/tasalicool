package com.example.tasalicool.models

class EloRating(
    private val kFactor: Int = 32
) {

    private var currentRating: Int = 1200

    fun calculateNewRating(
        playerRating: Int,
        opponentRating: Int,
        score: Double
    ): Int {

        val expectedScore =
            1.0 / (1 + Math.pow(10.0, (opponentRating - playerRating) / 400.0))

        return (playerRating + kFactor * (score - expectedScore)).toInt()
    }

    /* ✅ متوافق مع Game400Engine */
    fun update(aiWon: Boolean) {

        val score = if (aiWon) 1.0 else 0.0
        val opponentRating = 1200

        currentRating =
            calculateNewRating(currentRating, opponentRating, score)
    }

    fun getRating(): Int {
        return currentRating
    }
}
