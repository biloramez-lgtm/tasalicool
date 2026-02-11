package com.example.tasalicool.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController

@Composable
fun HomeScreen(navController: NavHostController) {

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0E3B2E),
                        Color(0xFF0A2A21)
                    )
                )
            )
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.height(30.dp))

            Text(
                text = "🃏 tasalicool",
                style = MaterialTheme.typography.displayMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "ألعاب الورق العربية",
                style = MaterialTheme.typography.titleMedium,
                color = Color.LightGray
            )

            Spacer(modifier = Modifier.height(40.dp))

            /* ================= FEATURED GAME ================= */

            FeaturedGameCard(
                onClick = {
                    navController.navigate("game_400") {
                        launchSingleTop = true
                    }
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            /* ================= RESUME GAME ================= */

            GameCard(
                title = "متابعة اللعبة",
                description = "استكمال الجولة المحفوظة",
                icon = "💾",
                onClick = {
                    navController.navigate("resume_game") {
                        launchSingleTop = true
                    }
                }
            )

            Spacer(modifier = Modifier.height(30.dp))

            /* ================= WIFI LOCAL MODE ================= */

            Text(
                text = "اللعب المحلي عبر Wi-Fi",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            GameCard(
                title = "استضافة لعبة",
                description = "إنشاء سيرفر محلي واللعب مع أصدقائك",
                icon = "📡",
                onClick = {
                    navController.navigate("host_game")
                }
            )

            GameCard(
                title = "الانضمام للعبة",
                description = "أدخل IP الجهاز المضيف للانضمام",
                icon = "🌐",
                onClick = {
                    navController.navigate("join_game")
                }
            )

            Spacer(modifier = Modifier.height(40.dp))

            /* ================= ABOUT BUTTON ================= */

            OutlinedButton(
                onClick = {
                    navController.navigate("about") {
                        launchSingleTop = true
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("حول التطبيق")
            }

            Spacer(modifier = Modifier.height(30.dp))

            /* ================= FOOTER ================= */

            Text(
                text = "Developed by Mr million",
                color = Color.LightGray,
                style = MaterialTheme.typography.bodySmall
            )

            Text(
                text = "© 2026 All Rights Reserved",
                color = Color.LightGray,
                style = MaterialTheme.typography.bodySmall
            )

            Text(
                text = "Contact: klosemiroslave40@gmail.com",
                color = Color.LightGray,
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

/* ================= FEATURED GAME CARD ================= */

@Composable
private fun FeaturedGameCard(
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1B5E20)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = MaterialTheme.shapes.large,
        onClick = onClick
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = "🎴",
                style = MaterialTheme.typography.displaySmall
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "لعبة 400",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "أقوى ذكاء اصطناعي • 4 لاعبين",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = Color(0xFFE0E0E0)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("ابدأ اللعب الآن")
            }
        }
    }
}

/* ================= NORMAL GAME CARD ================= */

@Composable
private fun GameCard(
    title: String,
    description: String,
    icon: String,
    onClick: () -> Unit
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = MaterialTheme.shapes.large,
        onClick = onClick
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = icon,
                style = MaterialTheme.typography.displaySmall
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("ابدأ اللعب")
            }
        }
    }
}
