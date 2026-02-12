package com.example.tasalicool.models

import com.example.tasalicool.game.AdvancedAI
import com.example.tasalicool.game.GameMode
import java.io.Serializable

enum class GamePhase {
    BIDDING,
    PLAYING,
    ROUND_END,
    GAME_OVER
}

class Game400Engine(
    var gameMode: GameMode = GameMode.SINGLE_PLAYER,
    humanCount: Int = 1,
    val players: MutableList<Player> = initializePlayers(gameMode, humanCount)
) : Serializable {

    private val deck = Deck()

    var phase = GamePhase.BIDDING
        private set

    var currentPlayerIndex = 0
        private set

    var dealerIndex = -1
        private set

    var trickNumber = 0
        private set

    val currentTrick = mutableListOf<Pair<Player, Card>>()

    var lastTrickWinner: Player? = null
        private set

    var winner: Player? = null
        private set

    init {
        startNewRound()
    }

    /* ================= ROUND ================= */

    fun startNewRound() {

        deck.reset()

        // دائماً نعيد ذاكرة AI
        AdvancedAI.resetMemory()

        dealerIndex = (dealerIndex + 1) % players.size

        players.forEach {
            it.resetForNewRound()
            it.addCards(deck.drawCards(13))
        }

        trickNumber = 0
        currentTrick.clear()
        lastTrickWinner = null

        // يبدأ اللاعب يمين الموزع
        currentPlayerIndex = (dealerIndex + 1) % players.size
        phase = GamePhase.BIDDING
    }

    /* ================= BIDDING ================= */

    fun placeBid(player: Player, bid: Int): Boolean {

        if (phase != GamePhase.BIDDING) return false
        if (player != getCurrentPlayer()) return false

        val minBid = minimumBidFor(player)
        if (bid < minBid || bid > 13) return false

        player.bid = bid
        nextPlayer()

        if (players.all { it.bid > 0 }) {
            phase = GamePhase.PLAYING
            currentPlayerIndex = (dealerIndex + 1) % players.size
        }

        return true
    }

    private fun minimumBidFor(player: Player): Int {
        return when {
            player.score < 30 -> 2
            player.score < 40 -> 3
            else -> 4
        }
    }

    /* ================= PLAY ================= */

    fun playCard(player: Player, card: Card): Boolean {

        if (phase != GamePhase.PLAYING) return false
        if (player != getCurrentPlayer()) return false
        if (!isValidPlay(player, card)) return false

        player.removeCard(card)
        currentTrick.add(player to card)

        // تسجيل للذكاء فقط إذا اللاعب AI
        if (player.type == PlayerType.AI) {
            AdvancedAI.rememberCard(player, card)
        }

        if (currentTrick.size == players.size) {
            finishTrick()
        } else {
            nextPlayer()
        }

        return true
    }

    private fun finishTrick() {

        val trickWinner = determineTrickWinner()
        trickWinner.incrementTrick()

        lastTrickWinner = trickWinner
        currentPlayerIndex = players.indexOf(trickWinner)

        trickNumber++
    }

    fun clearTrickAfterDelay() {

        if (currentTrick.isEmpty()) return

        currentTrick.clear()

        if (trickNumber >= 13) {
            finishRound()
        }
    }

    private fun determineTrickWinner(): Player {

        val leadSuit = currentTrick.first().second.suit

        val trumpCards =
            currentTrick.filter { it.second.suit == Suit.HEARTS }

        return if (trumpCards.isNotEmpty()) {
            trumpCards.maxBy { it.second.rank.value }.first
        } else {
            currentTrick
                .filter { it.second.suit == leadSuit }
                .maxBy { it.second.rank.value }
                .first
        }
    }

    private fun isValidPlay(player: Player, card: Card): Boolean {

        if (currentTrick.isEmpty()) return true

        val leadSuit = currentTrick.first().second.suit
        val hasSuit = player.hand.any { it.suit == leadSuit }

        return if (hasSuit) card.suit == leadSuit else true
    }

    /* ================= AI CHECK ================= */

    fun isAITurn(): Boolean {
        return getCurrentPlayer().type == PlayerType.AI
    }

    /* ================= SCORING ================= */

    private fun finishRound() {

        players.forEach { player ->
            if (player.bid == 13 && player.tricksWon == 13) {
                winner = player
                phase = GamePhase.GAME_OVER
                return
            }
        }

        players.forEach { it.applyRoundScore() }

        checkGameWinner()

        if (phase != GamePhase.GAME_OVER) {
            phase = GamePhase.ROUND_END
        }
    }

    private fun checkGameWinner() {

        players.forEach { player ->
            val partner = getPartner(player)

            if (player.score >= 41 && partner.score > 0) {
                winner = player
                phase = GamePhase.GAME_OVER
                return
            }
        }
    }

    /* ================= HELPERS ================= */

    fun getCurrentPlayer(): Player =
        players[currentPlayerIndex]

    private fun nextPlayer() {
        currentPlayerIndex =
            (currentPlayerIndex + 1) % players.size
    }

    private fun getPartner(player: Player): Player {
        val index = players.indexOf(player)
        return players[(index + 2) % players.size]
    }

    companion object {

        fun initializePlayers(
            mode: GameMode,
            humanCount: Int = 1
        ): MutableList<Player> {

            val players = mutableListOf<Player>()
            val totalPlayers = 4

            val humans = when (mode) {
                GameMode.SINGLE_PLAYER -> 1
                GameMode.LOCAL_MULTIPLAYER -> humanCount
                GameMode.WIFI_MULTIPLAYER -> humanCount
            }

            val aiCount = totalPlayers - humans

            var index = 0

            // إضافة البشر
            for (i in 1..humans) {
                players.add(
                    Player(
                        name = "Player $i",
                        type = PlayerType.HUMAN,
                        teamId = if (index % 2 == 0) 1 else 2
                    )
                )
                index++
            }

            // إضافة AI لتعويض النقص
            for (i in 1..aiCount) {
                players.add(
                    Player(
                        name = "AI $i",
                        type = PlayerType.AI,
                        teamId = if (index % 2 == 0) 1 else 2
                    )
                )
                index++
            }

            return players
        }
    }
}
