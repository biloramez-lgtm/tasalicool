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
import com.example.tasalicool.network.NetworkGameClient
import com.example.tasalicool.ui.components.*
import kotlinx.coroutines.delay

@Composable
fun Game400Screen(
    navController: NavHostController,
    gameEngine: Game400Engine,
    networkClient: NetworkGameClient? = null
) {

    val engine = gameEngine

    if (engine.players.size < 4) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    var selectedCard by remember { mutableStateOf<Card?>(null) }

    val localPlayer = engine.players[0]
    val leftPlayer = engine.players[1]
    val topPlayer = engine.players[2]
    val rightPlayer = engine.players[3]

    val team1Score = localPlayer.score + topPlayer.score
    val team2Score = leftPlayer.score + rightPlayer.score

    val totalScore = team1Score + team2Score
    var previousTotal by remember { mutableStateOf(totalScore) }

    val scaleAnim = remember { Animatable(1f) }

    LaunchedEffect(totalScore) {
        if (totalScore != previousTotal) {
            previousTotal = totalScore
            scaleAnim.animateTo(1.15f, animationSpec = spring())
            delay(250)
            scaleAnim.animateTo(1f, animationSpec = spring())
        }
    }

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
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = null,
                        tint = Color.White
                    )
                }

                Text(
                    text = "🎴 لعبة 400",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.width(48.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { },
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
            label = "scoreColor"
        )

        Card(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
                .scale(scaleAnim.value),
            colors = CardDefaults.cardColors(
                containerColor = animatedColor
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = Color.White
                )

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = "$team1Score - $team2Score",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
