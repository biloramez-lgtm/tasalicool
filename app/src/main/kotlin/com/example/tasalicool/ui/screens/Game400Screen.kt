package com.example.tasalicool.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.tasalicool.models.Card
import com.example.tasalicool.models.GamePhase
import com.example.tasalicool.models.Player
import com.example.tasalicool.viewmodel.GameViewModel

@Composable
fun Game400Screen(
    navController: NavHostController,
    viewModel: GameViewModel = viewModel()
) {

    val engine = viewModel.engine
    viewModel.refresh.value

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
    val leftPlayer = engine.players[1]
    val topPlayer = engine.players[2]
    val rightPlayer = engine.players[3]
    val currentPlayer = engine.getCurrentPlayer()

    var selectedCard by remember { mutableStateOf<Card?>(null) }

    fun highlight(player: Player): Modifier {
        return if (player == currentPlayer)
            Modifier.border(3.dp, Color.Green)
        else Modifier
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0E3B2E))
            .padding(8.dp)
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
                            text = "كم أكلة تريد؟",
                            color = Color.White
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

                    Text(
                        text = "بانتظار ${currentPlayer.name}...",
                        color = Color.White,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            /* ================= PLAYING ================= */

            GamePhase.PLAYING -> {

                // ===== TOP PLAYER =====
                Text(
                    text = "${topPlayer.name} (${topPlayer.hand.size})",
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .then(highlight(topPlayer))
                        .padding(8.dp)
                )

                // ===== LEFT PLAYER =====
                Text(
                    text = "${leftPlayer.name} (${leftPlayer.hand.size})",
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .then(highlight(leftPlayer))
                        .padding(8.dp)
                )

                // ===== RIGHT PLAYER =====
                Text(
                    text = "${rightPlayer.name} (${rightPlayer.hand.size})",
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .then(highlight(rightPlayer))
                        .padding(8.dp)
                )

                // ===== CURRENT TRICK =====
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    engine.currentTrick.forEach { (_, card) ->
                        Card(
                            modifier = Modifier.size(50.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White
                            )
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "${card.rank} ${card.suit}",
                                    color = Color.Black
                                )
                            }
                        }
                    }
                }

                // ===== LOCAL PLAYER =====
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Text(
                        text = localPlayer.name,
                        color = Color.White,
                        modifier = highlight(localPlayer)
                            .padding(6.dp)
                    )

                    Spacer(Modifier.height(6.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(localPlayer.hand) { card ->

                            Card(
                                modifier = Modifier
                                    .size(60.dp)
                                    .border(
                                        width = if (card == selectedCard) 3.dp else 1.dp,
                                        color = if (card == selectedCard)
                                            Color.Yellow
                                        else Color.Black
                                    ),
                                onClick = {
                                    if (currentPlayer == localPlayer) {
                                        selectedCard = card
                                    }
                                }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("${card.rank} ${card.suit}")
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Button(
                        onClick = {
                            selectedCard?.let {
                                engine.playCard(localPlayer, it)
                                selectedCard = null
                            }
                        },
                        enabled =
                            selectedCard != null &&
                                    currentPlayer == localPlayer
                    ) {
                        Text("لعب الورقة")
                    }

                    Spacer(Modifier.height(8.dp))
                }
            }

            /* ================= GAME OVER ================= */

            GamePhase.GAME_OVER -> {

                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Text(
                        text = "🏆 انتهت اللعبة",
                        color = Color.Yellow
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = "الفائز: ${engine.winner?.name}",
                        color = Color.White
                    )

                    Spacer(Modifier.height(16.dp))

                    Button(onClick = {
                        navController.popBackStack()
                    }) {
                        Text("رجوع")
                    }
                }
            }

            else -> {}
        }
    }
}
