package com.example.tasalicool.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.tasalicool.R
import com.example.tasalicool.models.*

@Composable
fun Game400Screen(
    navController: NavHostController,
    gameEngine: Game400Engine
) {

    val engine = gameEngine

    LaunchedEffect(Unit) {
        engine.startGame()
    }

    if (engine.players.size < 4) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val localPlayer = engine.players[0]
    val currentPlayer = engine.getCurrentPlayer()

    fun playerModifier(player: Player): Modifier {
        return if (player == currentPlayer) {
            Modifier.border(3.dp, Color.Green)
        } else {
            Modifier
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0E3B2E))
    ) {

        when (engine.phase) {

            /* ================= BIDDING ================= */

            GamePhase.BIDDING -> {

                if (currentPlayer == localPlayer) {

                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        Text(
                            text = stringResource(R.string.how_many_tricks),
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(Modifier.height(16.dp))

                        LazyRow {
                            items((2..13).toList()) { bid ->
                                Button(
                                    onClick = {
                                        engine.placeBid(localPlayer, bid)
                                    },
                                    modifier = Modifier.padding(4.dp)
                                ) {
                                    Text(bid.toString())
                                }
                            }
                        }
                    }
                } else {
                    Box(Modifier.align(Alignment.Center)) {
                        Text(
                            text = stringResource(
                                R.string.waiting_player,
                                currentPlayer.name
                            ),
                            color = Color.White
                        )
                    }
                }
            }

            /* ================= PLAYING ================= */

            GamePhase.PLAYING -> {

                val leftPlayer = engine.players[1]
                val topPlayer = engine.players[2]
                val rightPlayer = engine.players[3]

                // ===== Player Names with Highlight =====

                Text(
                    text = topPlayer.name,
                    color = Color.White,
                    fontWeight = if (topPlayer == currentPlayer) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .then(playerModifier(topPlayer))
                        .padding(6.dp)
                )

                Text(
                    text = leftPlayer.name,
                    color = Color.White,
                    fontWeight = if (leftPlayer == currentPlayer) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .then(playerModifier(leftPlayer))
                        .padding(6.dp)
                )

                Text(
                    text = rightPlayer.name,
                    color = Color.White,
                    fontWeight = if (rightPlayer == currentPlayer) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .then(playerModifier(rightPlayer))
                        .padding(6.dp)
                )

                Text(
                    text = localPlayer.name,
                    color = Color.White,
                    fontWeight = if (localPlayer == currentPlayer) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .then(playerModifier(localPlayer))
                        .padding(6.dp)
                )

                // ===== Center Trick =====

                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    engine.currentTrick.forEach { (player, card) ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White
                            ),
                            modifier = Modifier.padding(4.dp)
                        ) {
                            Text(
                                text = "${card.rank} ${card.suit}",
                                modifier = Modifier.padding(12.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // ===== Player Hand =====

                LazyRow(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 60.dp)
                ) {
                    items(localPlayer.hand) { card ->

                        val isMyTurn = currentPlayer == localPlayer

                        Card(
                            modifier = Modifier
                                .padding(6.dp)
                                .alpha(if (isMyTurn) 1f else 0.4f)
                                .clickable(enabled = isMyTurn) {
                                    engine.playCard(localPlayer, card)
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White
                            )
                        ) {
                            Text(
                                text = "${card.rank} ${card.suit}",
                                modifier = Modifier.padding(14.dp),
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }
                }
            }

            /* ================= GAME OVER ================= */

            GamePhase.GAME_OVER -> {
                Box(Modifier.align(Alignment.Center)) {
                    Text(
                        text = stringResource(
                            R.string.winner_text,
                            engine.winner?.name ?: ""
                        ),
                        color = Color.Yellow,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }

            else -> {}
        }
    }
}
