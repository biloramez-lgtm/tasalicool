package com.example.tasalicool.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController

@Composable
fun HomeScreen(navController: NavHostController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "🃏 tasalicool",
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "ألعاب الورق العربية",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.secondary
        )

        Spacer(modifier = Modifier.height(32.dp))

        // لعبة 400
        GameCard(
            title = "لعبة 400",
            description = "لعبة الورق الكلاسيكية\nللاعبين 2-4",
            icon = "🎴",
            onClick = { navController.navigate("game_400") }
        )

        // Solitaire
        GameCard(
            title = "Solitaire",
            description = "لعبة فردية\nللعب بمفردك",
            icon = "🎯",
            onClick = { navController.navigate("solitaire") }
        )

        // Hand Game
        GameCard(
            title = "Hand Game",
            description = "لعبة اليد\nللاعبين متعددين",
            icon = "🤝",
            onClick = { navController.navigate("hand_game") }
        )

        Spacer(modifier = Modifier.height(32.dp))

        // خيارات الاتصال
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { navController.navigate("bluetooth") },
                modifier = Modifier.weight(1f)
            ) {
                Text("📱 Bluetooth")
            }

            Button(
                onClick = { navController.navigate("network") },
                modifier = Modifier.weight(1f)
            ) {
                Text("🌐 Network")
            }
        }
    }
}

@Composable
fun GameCard(
    title: String,
    description: String,
    icon: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = icon, style = MaterialTheme.typography.displaySmall)
            Text(text = title, style = MaterialTheme.typography.titleLarge)
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
                Text("ابدأ اللعب")
            }
        }
    }
}
