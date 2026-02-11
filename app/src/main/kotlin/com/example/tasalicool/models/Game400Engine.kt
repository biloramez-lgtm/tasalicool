package com.example.tasalicool.models

import java.io.Serializable
import kotlin.math.max
import kotlin.math.min

object Game400Constants {
    const val CARDS_PER_PLAYER = 13
    const val WIN_SCORE = 41
    val TRUMP_SUIT = Suit.HEARTS
}

class Game400Engine(
    val players: List<Player>
) : Serializable {

    val deck = Deck()

    var currentPlayerIndex = 0
    var trickNumber = 0

    val currentTrick: MutableList<Pair<Player, Card>> = mutableListOf()

    var roundActive = false
    var gameWinner: Player? = null

    /* ================================
       🧠 ذاكرة احترافية
       ================================ */

    private val playedCards = mutableListOf<Card>()

    // اللاعب → الأنواع التي لا يملكها
    private val knownVoidSuits: MutableMap<Player, MutableSet<Suit>> =
        mutableMapOf()

    private fun initializeMemory() {
        playedCards.clear()
        knownVoidSuits.clear()
        players.forEach {
            knownVoidSuits[it] = mutableSetOf()
        }
    }

    private fun registerPlayedCard(player: Player, card: Card) {

        playedCards.add(card)

        // إذا لم يلعب من النوع المطلوب → لا يملك هذا النوع
        if (currentTrick.isNotEmpty()) {

            val leadSuit = currentTrick.first().second.suit

            if (card.suit != leadSuit &&
                player.hand.none { it.suit == leadSuit }
            ) {
                knownVoidSuits[player]?.add(leadSuit)
            }
        }
    }

    private fun playerHasNoSuit(player: Player, suit: Suit): Boolean {
        return knownVoidSuits[player]?.contains(suit) == true
    }

    /* ================================
       بدء جولة جديدة
       ================================ */

    fun startNewRound() {

        deck.reset()
        initializeMemory()

        players.forEach {
            it.resetForNewRound()
            it.addCards(deck.drawCards(Game400Constants.CARDS_PER_PLAYER))
        }

        calculateAdvancedAIBids()

        trickNumber = 0
        currentPlayerIndex = 0
        roundActive = true
        currentTrick.clear()
    }

    fun getCurrentPlayer(): Player = players[currentPlayerIndex]

    fun nextPlayer() {
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size
    }

    /* ================================
       🤖 Bid احترافي
       ================================ */

    private fun calculateAdvancedAIBids() {
        players.forEach { player ->
            if (!player.isLocal) {
                player.bid = calculateProfessionalBid(player)
            }
        }
    }

    private fun calculateProfessionalBid(player: Player): Int {

        var score = 0.0

        val trumpCount =
            player.hand.count { it.suit == Game400Constants.TRUMP_SUIT }

        val highCards =
            player.hand.count { it.rank.value >= 11 }

        score += trumpCount * 2.8
        score += highCards * 1.4

        var bid = (score / 3).toInt()
        bid = max(2, bid)
        bid = min(13, bid)

        return bid
    }

    /* ================================
       اللعب
       ================================ */

    fun playCard(player: Player, card: Card): Boolean {

        if (!roundActive) return false
        if (player != getCurrentPlayer()) return false
        if (!isValidPlay(player, card)) return false

        player.removeCard(card)

        // 🔥 نسجل قبل الإضافة
        registerPlayedCard(player, card)

        currentTrick.add(player to card)

        if (currentTrick.size == 4) {
            finishTrick()
        } else {
            nextPlayer()
        }

        return true
    }

    private fun isValidPlay(player: Player, card: Card): Boolean {

        if (currentTrick.isEmpty()) return true

        val leadSuit = currentTrick.first().second.suit
        val hasLeadSuit = player.hand.any { it.suit == leadSuit }

        return if (hasLeadSuit) card.suit == leadSuit else true
    }

    /* ================================
       🤖 AI تحليلي احترافي
       ================================ */

    fun playAITurnIfNeeded() {

        val current = getCurrentPlayer()

        if (!current.isLocal && roundActive) {

            val card = chooseProfessionalCard(current)
            playCard(current, card)

            playAITurnIfNeeded()
        }
    }

    private fun chooseProfessionalCard(player: Player): Card {

        val validCards = player.hand.filter { isValidPlay(player, it) }
        val leadSuit = currentTrick.firstOrNull()?.second?.suit

        val partner = players.first {
            it.teamId == player.teamId && it != player
        }

        // ==========================
        // إذا يبدأ الأكلة
        // ==========================

        if (leadSuit == null) {

            // إذا خصم لا يملك نوع معين → اضربه بهذا النوع
            Suit.values().forEach { suit ->
                val enemyVoid = players
                    .filter { it.teamId != player.teamId }
                    .any { playerHasNoSuit(it, suit) }

                if (enemyVoid) {
                    val attackCard =
                        validCards
                            .filter { it.suit == suit }
                            .maxByOrNull { it.rank.value }

                    if (attackCard != null)
                        return attackCard
                }
            }

            return validCards.maxBy { it.rank.value }
        }

        // ==========================
        // إذا هناك نوع مطلوب
        // ==========================

        val sameSuit = validCards.filter { it.suit == leadSuit }

        if (sameSuit.isNotEmpty()) {

            val highestOnTable =
                currentTrick
                    .filter { it.second.suit == leadSuit }
                    .maxBy { it.second.rank.value }

            val currentWinner = highestOnTable.first

            // 🔥 إذا الشريك غالب → لا تحرق ورقة قوية
            if (currentWinner.teamId == player.teamId) {
                return sameSuit.minBy { it.rank.value }
            }

            val winningCard =
                sameSuit
                    .filter {
                        it.rank.value >
                                highestOnTable.second.rank.value
                    }
                    .minByOrNull { it.rank.value }

            return winningCard ?: sameSuit.minBy { it.rank.value }
        }

        // ==========================
        // لا يملك النوع → يفحص طرنيب
        // ==========================

        val trumps =
            validCards.filter {
                it.suit == Game400Constants.TRUMP_SUIT
            }

        if (trumps.isNotEmpty()) {

            val highestTrumpOnTable =
                currentTrick
                    .filter {
                        it.second.suit ==
                                Game400Constants.TRUMP_SUIT
                    }
                    .maxByOrNull { it.second.rank.value }

            // إذا الشريك غالب → لا تضرب
            if (highestTrumpOnTable != null &&
                highestTrumpOnTable.first.teamId ==
                player.teamId
            ) {
                return validCards.minBy { it.rank.value }
            }

            val winningTrump =
                trumps
                    .filter {
                        highestTrumpOnTable == null ||
                                it.rank.value >
                                highestTrumpOnTable.second.rank.value
                    }
                    .minByOrNull { it.rank.value }

            return winningTrump ?: validCards.minBy { it.rank.value }
        }

        return validCards.minBy { it.rank.value }
    }

    /* ================================
       إنهاء الأكلة
       ================================ */

    private fun finishTrick() {

        val winnerPair = determineTrickWinner()
        winnerPair.first.incrementTrick()

        currentPlayerIndex = players.indexOf(winnerPair.first)

        currentTrick.clear()
        trickNumber++

        if (trickNumber == 13) {
            finishRound()
        }
    }

    private fun determineTrickWinner(): Pair<Player, Card> {

        val leadSuit = currentTrick.first().second.suit

        val trumpCards =
            currentTrick.filter {
                it.second.suit ==
                        Game400Constants.TRUMP_SUIT
            }

        return if (trumpCards.isNotEmpty()) {
            trumpCards.maxBy { it.second.rank.value }
        } else {
            currentTrick
                .filter { it.second.suit == leadSuit }
                .maxBy { it.second.rank.value }
        }
    }

    private fun finishRound() {

        players.forEach { it.applyRoundScore() }

        checkGameWinner()
        roundActive = false
    }

    private fun checkGameWinner() {

        players.forEach { player ->
            if (player.score >= Game400Constants.WIN_SCORE) {
                gameWinner = player
            }
        }
    }

    fun isGameOver(): Boolean = gameWinner != null
}
