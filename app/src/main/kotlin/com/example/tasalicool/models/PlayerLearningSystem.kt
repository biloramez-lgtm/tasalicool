package com.example.tasalicool.models

import kotlin.math.max
import kotlin.math.min

class PlayerLearningSystem {

    private var skillLevel: Int = 1
    private val moveHistory = mutableListOf<Card>()

    private val maxSkillLevel = 10
    private val minSkillLevel = 1

    /* ===================================================== */
    /* ================= MOVE TRACKING ===================== */
    /* ===================================================== */

    fun recordPlayerMove(card: Card) {
        moveHistory.add(card)
    }

    /* ===================================================== */
    /* ================= ROUND ANALYSIS ==================== */
    /* ===================================================== */

    fun endRoundAnalysis(): AIDifficulty {

        if (moveHistory.isEmpty()) {
            skillLevel = max(minSkillLevel, skillLevel - 1)
            return mapSkillToDifficulty()
        }

        val highCardsPlayed = moveHistory.count { it.strength() > 10 }
        val totalMoves = moveHistory.size

        val performanceScore = when {
            totalMoves >= 13 && highCardsPlayed > 5 -> 90
            totalMoves >= 8 -> 70
            totalMoves >= 4 -> 50
            else -> 25
        }

        skillLevel = updateSkillLevel(skillLevel, performanceScore)

        moveHistory.clear()

        return mapSkillToDifficulty()
    }

    /* ===================================================== */
    /* ================= SKILL UPDATE ====================== */
    /* ===================================================== */

    private fun updateSkillLevel(currentLevel: Int, performanceScore: Int): Int {

        val newLevel = when {
            performanceScore >= 85 -> currentLevel + 1
            performanceScore <= 30 -> currentLevel - 1
            else -> currentLevel
        }

        return min(maxSkillLevel, max(minSkillLevel, newLevel))
    }

    /* ===================================================== */
    /* ================= DIFFICULTY MAP ==================== */
    /* ===================================================== */

    private fun mapSkillToDifficulty(): AIDifficulty {
        return when (skillLevel) {
            in 1..3 -> AIDifficulty.EASY
            in 4..6 -> AIDifficulty.NORMAL
            in 7..8 -> AIDifficulty.HARD
            else -> AIDifficulty.ELITE
        }
    }

    fun getSkillLevel(): Int = skillLevel

    fun resetLearning() {
        skillLevel = 1
        moveHistory.clear()
    }
}
