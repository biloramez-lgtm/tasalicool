package com.example.tasalicool.models

import java.io.Serializable

/* =====================================================
   أنواع الأوراق
   ===================================================== */

enum class Suit {
    HEARTS, DIAMONDS, CLUBS, SPADES
}

/* =====================================================
   رتب الأوراق
   ===================================================== */

enum class Rank(val displayName: String, val value: Int) {
    ACE("A", 14),        // في لعبة 400 الآص أعلى ورقة
    KING("K", 13),
    QUEEN("Q", 12),
    JACK("J", 11),
    TEN("10", 10),
    NINE("9", 9),
    EIGHT("8", 8),
    SEVEN("7", 7),
    SIX("6", 6),
    FIVE("5", 5),
    FOUR("4", 4),
    THREE("3", 3),
    TWO("2", 2)
}

/* =====================================================
   نموذج الورقة
   ===================================================== */

data class Card(
    val suit: Suit,
    val rank: Rank
) : Serializable {

    // الطرنيب الثابت = HEARTS
    fun isTrump(): Boolean = suit == Suit.HEARTS

    override fun toString(): String =
        "${rank.displayName}${suit.name.first()}"

    fun getResourceName(): String =
        "${rank.displayName.lowercase()}_of_${suit.name.lowercase()}"
}

/* =====================================================
   نموذج الدك
   ===================================================== */

data class Deck(
    val cards: MutableList<Card> = mutableListOf()
) {

    init {
        if (cards.isEmpty()) {
            reset()
        }
    }

    fun reset() {
        cards.clear()
        Suit.values().forEach { suit ->
            Rank.values().forEach { rank ->
                cards.add(Card(suit, rank))
            }
        }
        shuffle()
    }

    fun shuffle() {
        cards.shuffle()
    }

    fun drawCard(): Card? =
        if (cards.isNotEmpty()) cards.removeAt(0) else null

    fun drawCards(count: Int): List<Card> {
        val drawn = mutableListOf<Card>()
        repeat(count) {
            drawCard()?.let { drawn.add(it) }
        }
        return drawn
    }

    fun size(): Int = cards.size

    fun isEmpty(): Boolean = cards.isEmpty()
}

/* =====================================================
   نموذج اللاعب (معدل للعبة 400)
   ===================================================== */

data class Player(
    val id: String,
    val name: String,

    val hand: MutableList<Card> = mutableListOf(),

    // مجموع النقاط التراكمي
    var score: Int = 0,

    // الطلب الحالي
    var bid: Int = 0,

    // عدد الأكلات في الجولة
    var tricksWon: Int = 0,

    // الفريق (0 أو 1)
    var teamId: Int = 0,

    val isLocal: Boolean = false

) : Serializable {

    /* ===== إدارة الأوراق ===== */

    fun addCards(cards: List<Card>) {
        hand.addAll(cards)
        sortHand()
    }

    fun removeCard(card: Card): Boolean =
        hand.remove(card)

    fun clearHand() {
        hand.clear()
    }

    fun handSize(): Int =
        hand.size

    private fun sortHand() {
        hand.sortWith(compareBy<Card> { it.suit.ordinal }.thenByDescending { it.rank.value })
    }

    /* ===== إدارة الجولة ===== */

    fun resetForNewRound() {
        bid = 0
        tricksWon = 0
        hand.clear()
    }

    fun incrementTrick() {
        tricksWon++
    }

    /* ===== حساب النقاط ===== */

    fun applyRoundScore(): Int {

        var points = 0

        if (bid == 13) {
            // طلب 13 حالة خاصة
            points = if (tricksWon == 13) 400 else -52
        } else {
            if (tricksWon >= bid) {
                points = if (bid >= 7) bid * 2 else bid
            } else {
                points = if (bid >= 7) -(bid * 2) else -bid
            }
        }

        score += points
        return points
    }

    fun isPositiveScore(): Boolean = score > 0
}

/* =====================================================
   نموذج حالة اللعبة
   ===================================================== */

data class GameState(
    val players: List<Player>,
    var currentPlayerIndex: Int = 0,
    val deck: Deck = Deck(),

    // أوراق الأكلة الحالية
    val currentTrick: MutableList<Pair<Player, Card>> = mutableListOf(),

    var roundNumber: Int = 1,

    var gameInProgress: Boolean = true,
    var winner: Player? = null
) : Serializable {

    fun getCurrentPlayer(): Player =
        players[currentPlayerIndex]

    fun nextPlayer() {
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size
    }
}

/* =====================================================
   نوع اللعبة
   ===================================================== */

enum class GameType {
    GAME_400
}
