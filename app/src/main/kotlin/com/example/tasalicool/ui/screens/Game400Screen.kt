package com.example.tasalicool.ui.screens

import androidx.compose.animation.Animatable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
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

    val localPlayer = engine.players[0]

    val team1Score = engine.players[0].score + engine.players[2].score
    val team2Score = engine.players[1].score + engine.players[3].score

    val winningTeam = engine.winner?.teamId

    val totalScore = team1Score + team2Score
    var previousTotal by remember { mutableStateOf(totalScore) }

    val scaleAnim = remember { Animatable(1f) }

    LaunchedEffect(totalScore) {
        if (totalScore != previousTotal) {
            previousTotal = totalScore
            scaleAnim.animateTo(1.18f, spring(dampingRatio = 0.4f))
            delay(250)
            scaleAnim.animateTo(1f, spring(dampingRatio = 0.6f))
        }
    }

    LaunchedEffect(Unit) {
        engine.startGame()
    }

    LaunchedEffect(engine.currentTrick.size) {
        if (engine.currentTrick.size == 4) {
            delay(1200)
            engine.clearTrickAfterDelay()
        }
    }

    val leftPlayer = engine.players[1]
    val topPlayer = engine.players[2]
    val rightPlayer = engine.players[3]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0E3B2E))
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {

                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                }

                Text(
                    "🎴 لعبة 400",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.width(48.dp))
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
                                engine.getCurrentPlayer() == localPlayer
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
                        }
                    }
                },
                enabled =
                    selectedCard != null &&
                    engine.phase == GamePhase.PLAYING &&
                    engine.getCurrentPlayer() == localPlayer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("لعب الورقة")
            }
        }

        val targetColor = when {
            team1Score > team2Score -> Color(0xFF2E7D32)
            team2Score > team1Score -> Color(0xFF1565C0)
            else -> Color(0xFF424242)
        }

        val animatedColor by animateColorAsState(
            targetValue = targetColor,
            animationSpec = spring(),
            label = ""
        )

        Card(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
                .scale(scaleAnim.value),
            colors = CardDefaults.cardColors(containerColor = animatedColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            ) {

                Icon(Icons.Default.EmojiEvents, null, tint = Color.White)

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    "$team1Score - $team2Score",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        if (engine.phase == GamePhase.GAME_OVER && winningTeam != null) {

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.75f)),
                contentAlignment = Alignment.Center
            ) {

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1B5E20)
                    ),
                    elevation = CardDefaults.cardElevation(20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        Icon(
                            Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = Color.Yellow,
                            modifier = Modifier.size(64.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            "🏆 الفريق $winningTeam فاز!",
                            color = Color.White,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = { engine.startNewRound() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("إعادة مباراة")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = { navController.popBackStack() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("الخروج")
                        }
                    }
                }
            }
        }
    }
}
