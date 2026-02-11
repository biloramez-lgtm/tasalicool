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
                Player("p2", "يسار", teamId = 1),
                Player("p3", "شريكك", teamId = 0),
                Player("p4", "يمين", teamId = 1)
            )
        )
    }

    var selectedCard by remember { mutableStateOf<Card?>(null) }
    var uiTrigger by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        engine.startNewRound()
        uiTrigger++
    }

    LaunchedEffect(uiTrigger) {
        while (engine.roundActive && !engine.getCurrentPlayer().isLocal) {
            delay(700)
            engine.playAITurnIfNeeded()
            uiTrigger++
        }
    }

    val localPlayer = engine.players.first { it.isLocal }
    val leftPlayer = engine.players[1]
    val topPlayer = engine.players[2]
    val rightPlayer = engine.players[3]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0E3B2E))
            .padding(12.dp)
    ) {

        /* ================= HEADER ================= */

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
            }

            Text(
                text = "🎴 لعبة 400 - Legendary AI",
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        /* ================= TOP PLAYER ================= */

        PlayerSideInfo(topPlayer, engine)

        Spacer(modifier = Modifier.height(8.dp))

        /* ================= TABLE CENTER ================= */

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            PlayerVerticalInfo(leftPlayer, engine)

            Column(horizontalAlignment = Alignment.CenterHorizontally) {

                Text("الأكلة", color = Color.White)

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    engine.currentTrick.forEach {
                        CardView(card = it.second)
                    }
                }
            }

            PlayerVerticalInfo(rightPlayer, engine)
        }

        Spacer(modifier = Modifier.height(8.dp))

        /* ================= LOCAL PLAYER ================= */

        PlayerSideInfo(localPlayer, engine)

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
            enabled = selectedCard != null &&
                    engine.getCurrentPlayer().isLocal &&
                    engine.roundActive,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("لعب الورقة")
        }
    }
}

/* ================= PLAYER UI COMPONENTS ================= */

@Composable
fun PlayerSideInfo(player: Player, engine: Game400Engine) {

    val isCurrent = player == engine.getCurrentPlayer()

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent)
                Color(0xFF1B5E20)
            else Color(0xFF1F4D3A)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(player.name, color = Color.White)
                Text(
                    "طلب ${player.bid} | أكلات ${player.tricksWon}",
                    color = Color.LightGray
                )
            }

            Text(
                "${player.score}",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun PlayerVerticalInfo(player: Player, engine: Game400Engine) {

    val isCurrent = player == engine.getCurrentPlayer()

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent)
                Color(0xFF1B5E20)
            else Color(0xFF1F4D3A)
        )
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(player.name, color = Color.White)
            Text("${player.tricksWon}", color = Color.White)
        }
    }
}
