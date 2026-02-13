package com.example.tasalicool.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
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
import com.example.tasalicool.ui.components.FlipCardView

@Composable
fun Game400Screen(
    navController: NavHostController,
    viewModel: GameViewModel = viewModel()
) {

    val uiState by viewModel.uiState.collectAsState()
    val engine = viewModel.getEngine()

    if (uiState.players.size < 4) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val localPlayer = uiState.players[0]
    val leftPlayer = uiState.players[1]
    val topPlayer = uiState.players[2]
    val rightPlayer = uiState.players[3]
    val currentPlayer = uiState.players[uiState.currentPlayerIndex]

    var selectedCard by remember { mutableStateOf<Card?>(null) }

    fun highlight(player: Player): Modifier {
        return if (player == currentPlayer)
            Modifier.border(3.dp, Color.Green)
        else Modifier
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tarneeb 400") }
            )
        }
    ) { padding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFF0E3B2E))
                .padding(8.dp)
        ) {

            /* ================= الطاولة ================= */

            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(300.dp)
                    .background(
                        Color(0xFF1B5E20),
                        RoundedCornerShape(32.dp)
                    )
            )

            /* ================= السكور ================= */

            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
                    .background(
                        Color.Black.copy(alpha = 0.7f),
                        RoundedCornerShape(16.dp)
                    )
                    .padding(8.dp)
            ) {
                Text(
                    text = "Team A: ${uiState.teamAScore}",
                    color = Color.White
                )
                Text(
                    text = "Team B: ${uiState.teamBScore}",
                    color = Color.White
                )
            }

            when (uiState.phase) {

                /* ================= BIDDING ================= */

                GamePhase.BIDDING -> {

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        Text(
                            text = localPlayer.name,
                            color = Color.White,
                            modifier = highlight(localPlayer).padding(6.dp)
                        )

                        Spacer(Modifier.height(6.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(localPlayer.hand) { card ->
                                FlipCardView(
                                    card = card,
                                    isFaceUp = true
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                    }

                    if (currentPlayer == localPlayer) {

                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {

                            Text("كم أكلة تريد؟", color = Color.White)

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

                    Text(
                        text = "${topPlayer.name} (${topPlayer.hand.size})",
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .then(highlight(topPlayer))
                            .padding(8.dp)
                    )

                    Text(
                        text = "${leftPlayer.name} (${leftPlayer.hand.size})",
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .then(highlight(leftPlayer))
                            .padding(8.dp)
                    )

                    Text(
                        text = "${rightPlayer.name} (${rightPlayer.hand.size})",
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .then(highlight(rightPlayer))
                            .padding(8.dp)
                    )

                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        uiState.currentTrick.forEach { (_, card) ->
                            FlipCardView(
                                card = card,
                                isFaceUp = true
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        Text(
                            text = localPlayer.name,
                            color = Color.White,
                            modifier = highlight(localPlayer).padding(6.dp)
                        )

                        Spacer(Modifier.height(6.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(localPlayer.hand) { card ->

                                FlipCardView(
                                    card = card,
                                    isFaceUp = true,
                                    isSelected = card == selectedCard,
                                    enabled = currentPlayer == localPlayer,
                                    onClick = {
                                        if (currentPlayer == localPlayer) {
                                            selectedCard = card
                                        }
                                    }
                                )
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

                        Text("🏆 انتهت اللعبة", color = Color.Yellow)

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = "الفائز: ${uiState.winner?.name}",
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
}
