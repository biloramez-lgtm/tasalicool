package com.example.cardgames.models

import java.io.Serializable

// قواعد لعبة 400
object Game400Rules {
    const val TARGET_SCORE = 400
    const val CARDS_PER_HAND = 7
    const val MAX_PLAYERS = 4
}

// نموذج جولة اللعبة
data class Game400Round(
    val players: List<Player>,
    val deck: Deck = Deck(),
    val discardPile: MutableList<Card> = mutableListOf(),
    val currentPlayerIndex: Int = 0,
    var roundInProgress: Boolean = true
) : Serializable {

    fun initialize() {
        // توزيع الأوراق
        players.forEach { player ->
            player.addCards(deck.drawCards(Game400Rules.CARDS_PER_HAND))
        }
        // الورقة الأولى في pile الرمي
        deck.drawCard()?.let { discardPile.add(it) }
    }

    fun getCurrentPlayer(): Player = players[currentPlayerIndex]

    fun getNextPlayerIndex(): Int = (currentPlayerIndex + 1) % players.size

    fun canPlay(card: Card): Boolean {
        val lastCard = discardPile.lastOrNull() ?: return true
        return card.rank == lastCard.rank || card.suit == lastCard.suit
    }

    fun playCard(card: Card): Boolean {
        val player = getCurrentPlayer()
        return if (player.removeCard(card)) {
            discardPile.add(card)
            true
        } else {
            false
        }
    }

    fun drawFromDeck(player: Player) {
        deck.drawCard()?.let { player.addCards(listOf(it)) }
    }

    fun nextTurn() {
        val currentIndex = currentPlayerIndex
        // تحديث الفهرس في نفس السياق
    }

    fun calculateRoundScores(): Map<Player, Int> {
        val scores = mutableMapOf<Player, Int>()
        players.forEach { player ->
            var score = 0
            player.hand.forEach { card ->
                score += when (card.rank) {
                    Rank.ACE -> 1
                    Rank.TEN, Rank.JACK, Rank.QUEEN, Rank.KING -> 10
                    else -> card.rank.value
                }
            }
            scores[player] = score
        }
        return scores
    }

    fun isRoundOver(): Boolean {
        return players.any { it.hand.isEmpty() } || deck.isEmpty()
    }
}

// حالة لعبة 400
data class Game400State(
    val players: List<Player>,
    val round: Game400Round,
    val scores: Map<String, Int> = emptyMap(),
    val gameOver: Boolean = false,
    val winner: Player? = null
) : Serializable {

    fun isGameOver(): Boolean {
        return players.any { (scores[it.id] ?: 0) >= Game400Rules.TARGET_SCORE }
    }
}
