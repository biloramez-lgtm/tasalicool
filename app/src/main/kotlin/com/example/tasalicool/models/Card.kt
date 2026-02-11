package com.example.tasalicool.models

import java.io.Serializable

// أنواع الأوراق
enum class Suit {
    HEARTS, DIAMONDS, CLUBS, SPADES
}

// رتب الأوراق
enum class Rank(val displayName: String, val value: Int) {
    ACE("A", 1),
    TWO("2", 2),
    THREE("3", 3),
    FOUR("4", 4),
    FIVE("5", 5),
    SIX("6", 6),
    SEVEN("7", 7),
    EIGHT("8", 8),
    NINE("9", 9),
    TEN("10", 10),
    JACK("J", 11),
    QUEEN("Q", 12),
    KING("K", 13)
}

// نموذج الورقة
data class Card(
    val suit: Suit,
    val rank: Rank
) : Serializable {

    override fun toString(): String =
        "${rank.displayName}${suit.name.first()}"

    fun getResourceName(): String =
        "${rank.displayName.lowercase()}_of_${suit.name.lowercase()}"
}

// نموذج الدك
data class Deck(
    val cards: MutableList<Card> = mutableListOf()
) {

    init {
        if (cards.isEmpty()) {
            Suit.values().forEach { suit ->
                Rank.values().forEach { rank ->
                    cards.add(Card(suit, rank))
                }
            }
            shuffle()
        }
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

    // ✅ أضفناها لدعم إعادة خلط discard pile
    fun addCards(newCards: List<Card>) {
        cards.addAll(newCards)
    }

    fun size(): Int = cards.size

    fun isEmpty(): Boolean = cards.isEmpty()
}

// نموذج اللاعب
data class Player(
    val id: String,
    val name: String,
    val hand: MutableList<Card> = mutableListOf(),
    var score: Int = 0,
    val isLocal: Boolean = true
) : Serializable {

    fun addCards(cards: List<Card>) {
        hand.addAll(cards)
    }

    fun removeCard(card: Card): Boolean =
        hand.remove(card)

    fun hasCard(card: Card): Boolean =
        hand.contains(card)

    fun handSize(): Int =
        hand.size

    fun clearHand() {
        hand.clear()
    }
}

// نموذج حالة اللعبة العامة
data class GameState(
    val gameType: GameType,
    val players: List<Player>,
    var currentPlayerIndex: Int = 0,
    val deck: Deck = Deck(),
    val discardPile: MutableList<Card> = mutableListOf(),
    var gameInProgress: Boolean = true,
    var winner: Player? = null
) : Serializable

// أنواع الألعاب
enum class GameType {
    GAME_400,
    SOLITAIRE,
    HAND,
    MULTIPLAYER
}
