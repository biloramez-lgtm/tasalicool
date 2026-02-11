package com.example.tasalicool.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.tasalicool.models.Card
import com.example.tasalicool.models.Deck
import com.example.tasalicool.models.Game400Round
import com.example.tasalicool.models.GameType
import com.example.tasalicool.models.Player
import com.example.tasalicool.ui.components.CardView
import com.example.tasalicool.ui.components.CardBackView
import com.example.tasalicool.ui.components.CompactCardView

@Composable
fun Game400Screen(navController: NavHostController) {
    var gameRound by remember {
        mutableStateOf(
            Game400Round(
                players = listOf(
                    Player("p1", "أنت"),
                    Player("p2", "اللاعب 2")
                )
            )
        )
    }
    var selectedCard by remember { mutableStateOf<Card?>(null) }

    gameRound.initialize()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "🎴 لعبة 400",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // معلومات اللاعبين
        gameRound.players.forEach { player ->
            PlayerInfoCard(
                player = player,
                isCurrentPlayer = player == gameRound.getCurrentPlayer()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // أوراق اللعب
        Text(
            text = "أوراق على الطاولة",
            style = MaterialTheme.typography.titleMedium
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // الدك المتبقي
            if (gameRound.deck.size() > 0) {
                CardBackView(
                    onClick = {
                        val card = gameRound.deck.drawCard()
                        card?.let { gameRound.getCurrentPlayer().addCards(listOf(it)) }
                    }
                )
                Text(text = "${gameRound.deck.size()}")
            }

            Spacer(modifier = Modifier.width(16.dp))

            // أوراق الرمي
            if (gameRound.discardPile.isNotEmpty()) {
                CardView(card = gameRound.discardPile.last())
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // يد اللاعب الحالي
        Text(
            text = "أوراقك",
            style = MaterialTheme.typography.titleMedium
        )

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(gameRound.getCurrentPlayer().hand) { card ->
                CompactCardView(
                    card = card,
                    isSelected = card == selectedCard,
                    onClick = { selectedCard = card }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // أزرار الحركة
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    if (selectedCard != null) {
                        if (gameRound.canPlay(selectedCard!!)) {
                            gameRound.playCard(selectedCard!!)
                            selectedCard = null
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = selectedCard != null
            ) {
                Text("لعب الورقة")
            }

            Button(
                onClick = {
                    gameRound.drawFromDeck(gameRound.getCurrentPlayer())
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("سحب ورقة")
            }
        }
    }
}

@Composable
fun PlayerInfoCard(player: Player, isCurrentPlayer: Boolean) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (isCurrentPlayer) "▶ ${player.name}" else player.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "عدد الأوراق: ${player.handSize()}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Text(
                text = "${player.score} نقطة",
                style = MaterialTheme.typography.headlineSmall
            )
        }
    }
}
