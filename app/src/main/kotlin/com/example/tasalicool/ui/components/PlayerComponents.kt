package com.example.tasalicool.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.tasalicool.models.Player

@Composable
fun PlayerSideInfo(
    player: Player,
    isCurrentTurn: Boolean = false
) {
    val borderColor =
        if (isCurrentTurn) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.outline

    Card(
        modifier = Modifier
            .padding(8.dp)
            .border(2.dp, borderColor, RoundedCornerShape(12.dp)),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .width(130.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = player.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Score: ${player.score}",
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = "Bid: ${player.bid}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun PlayerVerticalInfo(
    player: Player,
    isCurrentTurn: Boolean = false
) {
    val backgroundColor =
        if (isCurrentTurn)
            MaterialTheme.colorScheme.primaryContainer
        else
            MaterialTheme.colorScheme.surfaceVariant

    Column(
        modifier = Modifier
            .padding(8.dp)
            .background(backgroundColor, RoundedCornerShape(16.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = player.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Score: ${player.score}",
            style = MaterialTheme.typography.bodyLarge
        )

        Text(
            text = "Bid: ${player.bid}",
            style = MaterialTheme.typography.bodyMedium
        )

        if (isCurrentTurn) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "YOUR TURN",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
