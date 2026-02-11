package com.example.tasalicool.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.tasalicool.models.*
import com.example.tasalicool.ui.components.CardView
import com.example.tasalicool.ui.components.CompactCardView
import kotlinx.coroutines.delay

@Composable
fun Game400Screen(navController: NavHostController) {

    val context = LocalContext.current

    val engine = remember {
        Game400Engine(
            context = context,
            players = listOf(
                Player("p1", "أنت", teamId = 0, isLocal = true),
                Player("p2", "لاعب 2", teamId = 1),
                Player("p3", "شريكك", teamId = 0),
                Player("p4", "لاعب 4", teamId = 1)
            )
        )
    }

    var selectedCard by remember { mutableStateOf<Card?>(null) }
    var uiTrigger by remember { mutableStateOf(0) }

    /* ================= START ROUND ================= */

    LaunchedEffect(Unit) {
        engine.startNewRound()
        uiTrigger++
    }

    /* ================= AUTO AI LOOP ================= */

    LaunchedEffect(uiTrigger) {

        while (
            engine.roundActive &&
            !engine.getCurrentPlayer().isLocal
        ) {
            delay(600)
            engine.playAITurnIfNeeded()
            uiTrigger++
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        /* ================= HEADER ================= */

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }

            Text(
                text = "🎴 لعبة 400 - Elite AI",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        /* ================= PLAYERS INFO ================= */

        engine.players.forEach { player ->
            PlayerInfoCard(
                player = player,
                isCurrentPlayer = player == engine.getCurrentPlayer()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        /* ================= CURRENT TRICK ================= */

        Text("الأكلة الحالية", style = MaterialTheme.typography.titleMedium)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            engine.currentTrick.forEach { pair ->
                CardView(card = pair.second)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        /* ================= LOCAL HAND ================= */

        val localPlayer = engine.players.first { it.isLocal }

        Text("أوراقك", style = MaterialTheme.typography.titleMedium)

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
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

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                selectedCard?.let {
                    val success = engine.playCard(localPlayer, it)
                    if (success) {
                        selectedCard = null
                        uiTrigger++
                    }
                }
            },
            enabled = selectedCard != null &&
                    engine.getCurrentPlayer().isLocal &&
                    engine.roundActive,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("لعب الورقة")
        }

        Spacer(modifier = Modifier.height(16.dp))

        /* ================= ROUND END ================= */

        if (!engine.roundActive && !engine.isGameOver()) {

            Text(
                text = "انتهت الجولة",
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    engine.startNewRound()
                    uiTrigger++
                }
            ) {
                Text("جولة جديدة")
            }
        }

        /* ================= GAME OVER ================= */

        if (engine.isGameOver()) {

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "🏆 الفائز: ${engine.gameWinner?.name}",
                style = MaterialTheme.typography.headlineMedium
            )
        }
    }
}

@Composable
fun PlayerInfoCard(player: Player, isCurrentPlayer: Boolean) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            Column {
                Text(
                    text = if (isCurrentPlayer) "▶ ${player.name}" else player.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "طلب: ${player.bid} | أكلات: ${player.tricksWon}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Text(
                text = "${player.score} نقطة",
                style = MaterialTheme.typography.titleLarge
            )
        }
    }
}
