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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.tasalicool.models.*
import com.example.tasalicool.ui.components.*
import kotlinx.coroutines.delay

@Composable
fun Game400Screen(navController: NavHostController) {

    val context = LocalContext.current

    val engine = remember {
        Game400Engine(
            context = context,
            players = listOf(
                Player("p1", "أنت", teamId = 0, isLocal = true),
                Player("p2", "يسار", teamId = 1, difficulty = AIDifficulty.HARD),
                Player("p3", "شريكك", teamId = 0, difficulty = AIDifficulty.NORMAL),
                Player("p4", "يمين", teamId = 1, difficulty = AIDifficulty.HARD)
            )
        )
    }

    var selectedCard by remember { mutableStateOf<Card?>(null) }
    var uiTrigger by remember { mutableStateOf(0) }
    var showRoundDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        engine.startNewRound()
        uiTrigger++
    }

    LaunchedEffect(engine.currentPlayerIndex, uiTrigger) {
        if (engine.roundActive && !engine.getCurrentPlayer().isLocal) {
            delay(600)
            engine.playAITurnIfNeeded()
            uiTrigger++
        }

        if (!engine.roundActive && !engine.isGameOver()) {
            showRoundDialog = true
        }
    }

    val localPlayer = engine.players.first { it.isLocal }
    val leftPlayer = engine.players[1]
    val topPlayer = engine.players[2]
    val rightPlayer = engine.players[3]

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
            dismissButton = {
                OutlinedButton(onClick = {
                    navController.popBackStack()
                }) { Text("العودة") }
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

    if (engine.isGameOver()) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {
                Button(onClick = { navController.popBackStack() }) {
                    Text("إنهاء")
                }
            },
            title = { Text("🏆 انتهت اللعبة") },
            text = { Text("الفائز: ${engine.gameWinner?.name}") }
        )
    }

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
                "🎴 لعبة 400 - Hybrid Elite AI",
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
                        if (engine.getCurrentPlayer().isLocal)
                            selectedCard = card
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
                        engine.getCurrentPlayer().isLocal &&
                        engine.roundActive,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("لعب الورقة")
        }
    }
}
