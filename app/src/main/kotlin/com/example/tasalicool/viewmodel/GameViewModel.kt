package com.example.tasalicool.viewmodel

import androidx.lifecycle.ViewModel
import com.example.tasalicool.models.Game400Engine
import com.example.tasalicool.models.GamePhase
import com.example.tasalicool.ui.state.GameUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GameViewModel : ViewModel() {

    private val engine = Game400Engine()

    private val _uiState = MutableStateFlow(createInitialState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    init {
        engine.onGameUpdated = {
            updateUiState()
        }
        engine.startGame()
    }

    /* ===================================================== */
    /* ================= INITIAL STATE ===================== */
    /* ===================================================== */

    private fun createInitialState(): GameUiState {
        return GameUiState(
            phase = engine.phase,
            players = engine.players.toList(),
            currentPlayerIndex = engine.currentPlayerIndex,
            currentTrick = engine.currentTrick.toList(),
            winner = engine.winner,
            team1Score = calculateTeamScore(1),
            team2Score = calculateTeamScore(2),
            showBidDialog = engine.phase == GamePhase.BIDDING
        )
    }

    /* ===================================================== */
    /* ================= UPDATE UI STATE =================== */
    /* ===================================================== */

    private fun updateUiState() {

        _uiState.value = GameUiState(
            phase = engine.phase,
            players = engine.players.toList(),
            currentPlayerIndex = engine.currentPlayerIndex,
            currentTrick = engine.currentTrick.toList(),
            winner = engine.winner,
            team1Score = calculateTeamScore(1),
            team2Score = calculateTeamScore(2),
            showBidDialog = engine.phase == GamePhase.BIDDING
        )
    }

    /* ===================================================== */
    /* ================= SCORE CALCULATION ================= */
    /* ===================================================== */

    private fun calculateTeamScore(teamId: Int): Int {
        return engine.players
            .filter { it.teamId == teamId }
            .sumOf { it.score }
    }

    /* ===================================================== */
    /* ================= ENGINE ACCESS ===================== */
    /* ===================================================== */

    fun getEngine(): Game400Engine = engine

    /* ===================================================== */
    /* ================= SAFE ACTIONS ====================== */
    /* ===================================================== */

    fun playCard(cardIndex: Int) {
        val player = _uiState.value.currentPlayer ?: return
        val card = player.hand.getOrNull(cardIndex) ?: return
        engine.playCard(player, card)
    }

    fun placeBid(bid: Int) {
        val player = _uiState.value.currentPlayer ?: return
        engine.placeBid(player, bid)
    }

    fun restartGame() {
        engine.startGame()
        updateUiState()
    }
}
