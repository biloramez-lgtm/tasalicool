package com.example.tasalicool.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.tasalicool.models.*
import com.example.tasalicool.ui.components.*
import kotlinx.coroutines.delay

@Composable
fun Game400Screen(navController: NavHostController) {

    val engine = remember { Game400Engine() }

    var selectedCard by remember { mutableStateOf<Card?>(null) }
    var uiTrigger by remember { mutableStateOf(0) }
    var showRoundDialog by remember { mutableStateOf(false) }

    LaunchedEffect(engine.currentPlayerIndex, engine.phase, uiTrigger) {

        if (engine.isAITurn()) {
            delay(700)
            val ai = engine.getCurrentPlayer()
            val card = ai.hand.firstOrNull()
            card?.let {
                engine.playCard(ai, it)
                uiTrigger++
            }
        }

        if (engine.phase == GamePhase.ROUND_END) {
            showRoundDialog = true
        }
    }

    val localPlayer = engine.players[0]
    val leftPlayer = engine.players[1]
    val topPlayer = engine.players[2]
    val rightPlayer = engine.players[3]

    /* ================= ROUND END ================= */

    if (showRoundDialog) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {
                Button(onClick = {
                    showRoundDialog = false
                    engine.startNewRound()
                    uiTrigger++
                }) { Text("جولة جديدة") }
            },
            title = { Text("انتهت الجولة") },
            text = {
                Column {
                    engine.players.forEach {
                        Text("${it.name} : ${it.score}")
                    }
                }
            }
        )
    }

    /* ================= GAME OVER ================= */

    if (engine.phase == GamePhase.GAME_OVER) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {
                Button(onClick = { navController.popBackStack() }) {
                    Text("إنهاء")
                }
            },
            title = { Text("🏆 انتهت اللعبة") },
            text = {
                Text("الفائز: ${engine.winner?.name ?: ""}")
            }
        )
    }

    /* ================= UI ================= */

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0E3B2E))
            .padding(12.dp)
    ) {

        Row(verticalAlignment = Alignment.CenterVertically) {

            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Default.ArrowBack, null, tint = Color.White)
            }

            Text(
                "🎴 لعبة 400",
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        PlayerSideInfo(
            player = topPlayer,
            isCurrentTurn = engine.getCurrentPlayer() == topPlayer
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            PlayerVerticalInfo(
                player = leftPlayer,
                isCurrentTurn = engine.getCurrentPlayer() == leftPlayer
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {

                Text("الأكلة", color = Color.White)
                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    engine.currentTrick.forEach {
                        CardView(card = it.second)
                    }
                }
            }

            PlayerVerticalInfo(
                player = rightPlayer,
                isCurrentTurn = engine.getCurrentPlayer() == rightPlayer
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        PlayerSideInfo(
            player = localPlayer,
            isCurrentTurn = engine.getCurrentPlayer() == localPlayer
        )

        Spacer(modifier = Modifier.height(6.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(localPlayer.hand) { card ->
                CompactCardView(
                    card = card,
                    isSelected = card == selectedCard,
                    onClick = {
                        if (engine.phase == GamePhase.PLAYING &&
                            !engine.isAITurn()
                        ) {
                            selectedCard = card
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                selectedCard?.let {
                    if (engine.playCard(localPlayer, it)) {
                        selectedCard = null
                        uiTrigger++
                    }
                }
            },
            enabled =
                selectedCard != null &&
                engine.phase == GamePhase.PLAYING &&
                !engine.isAITurn(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("لعب الورقة")
        }
    }
}
