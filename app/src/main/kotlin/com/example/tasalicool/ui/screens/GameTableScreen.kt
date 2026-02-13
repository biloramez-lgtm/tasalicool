package com.example.tasalicool.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.tasalicool.models.Card
import com.example.tasalicool.models.Game400Engine
import com.example.tasalicool.models.GamePhase
import com.example.tasalicool.models.Player
import com.example.tasalicool.ui.state.GameUiState

@Composable
fun GameTableScreen(
    navController: NavController,
    engine: Game400Engine
) {

    var uiState by remember {
        mutableStateOf(
            GameUiState(
                phase = engine.phase,
                players = engine.players,
                currentPlayerIndex = engine.currentPlayerIndex,
                currentTrick = engine.currentTrick,
                winner = engine.winner
            )
        )
    }

    engine.onGameUpdated = {
        uiState = uiState.copy(
            phase = engine.phase,
            players = engine.players,
            currentPlayerIndex = engine.currentPlayerIndex,
            currentTrick = engine.currentTrick,
            winner = engine.winner,
            team1Score = engine.players.filter { it.teamId == 1 }.sumOf { it.score },
            team2Score = engine.players.filter { it.teamId == 2 }.sumOf { it.score }
        )
    }

    Scaffold(

        // 🔥 TopBar
        topBar = {
            TopAppBar(
                title = { Text("400 Game") }
            )
        }

    ) { padding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFF0F5E2B)) // طاولة خضراء
        ) {

            // 🔥 السكور أعلى اليسار
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            ) {
                Text("Team 1: ${uiState.team1Score}", color = Color.White)
                Text("Team 2: ${uiState.team2Score}", color = Color.White)
            }

            // 🔥 اللاعب فوق
            if (uiState.players.size >= 4) {

                Text(
                    text = uiState.players[2].name,
                    color = Color.White,
                    modifier = Modifier.align(Alignment.TopCenter)
                )

                Text(
                    text = uiState.players[1].name,
                    color = Color.White,
                    modifier = Modifier.align(Alignment.CenterEnd)
                )

                Text(
                    text = uiState.players[3].name,
                    color = Color.White,
                    modifier = Modifier.align(Alignment.CenterStart)
                )
            }

            // 🔥 الورق بالمنتصف
            Row(
                modifier = Modifier.align(Alignment.Center)
            ) {
                uiState.currentTrick.forEach { (_, card) ->
                    CardItem(card)
                }
            }

            // 🔥 يد اللاعب تحت
            val currentPlayer = uiState.players.getOrNull(0)

            currentPlayer?.let { player ->
                LazyRow(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                ) {
                    items(player.hand) { card ->
                        CardItem(card) {
                            engine.playCard(player, card)
                        }
                    }
                }
            }

            // 🔥 نافذة الطلب
            if (uiState.phase == GamePhase.BIDDING) {

                AlertDialog(
                    onDismissRequest = {},
                    title = { Text("اختر الطلب") },
                    text = {

                        Row {
                            (2..13).forEach { bid ->
                                Button(
                                    onClick = {
                                        engine.placeBid(
                                            engine.getCurrentPlayer(),
                                            bid
                                        )
                                    },
                                    modifier = Modifier.padding(4.dp)
                                ) {
                                    Text("$bid")
                                }
                            }
                        }
                    },
                    confirmButton = {}
                )
            }
        }
    }
}

@Composable
fun CardItem(
    card: Card,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .size(60.dp, 90.dp)
            .padding(4.dp),
        onClick = { onClick?.invoke() }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(card.rank + card.suit)
        }
    }
}
