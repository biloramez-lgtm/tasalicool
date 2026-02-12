package com.example.tasalicool

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.example.tasalicool.ui.screens.*
import com.example.tasalicool.ui.theme.TasalicoolTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            TasalicoolTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    TasalicoolNavGraph(navController)
                }
            }
        }
    }
}

/* ========================================================= */
/* ================= NAVIGATION GRAPH ====================== */
/* ========================================================= */

@Composable
fun TasalicoolNavGraph(
    navController: NavHostController
) {

    NavHost(
        navController = navController,
        startDestination = "home"
    ) {

        /* ================= HOME ================= */
        composable("home") {
            HomeScreen(navController)
        }

        /* ================= GAME 400 ================= */
        composable("game_400") {
            Game400Screen(navController)
        }

        /* ================= RESUME GAME ================= */
        composable("resume_game") {
            Game400Screen(navController)
        }

        /* ================= ABOUT ================= */
        composable("about") {
            AboutScreen(navController)
        }

        /* ================= WIFI LOCAL HOST ================= */
        composable("host_game") {
            HostGameScreen(navController)
        }

        /* ================= WIFI LOCAL JOIN ================= */
        composable("join_game") {
            JoinGameScreen(navController)
        }

        /* ================= OTHER PLACEHOLDERS ================= */
        composable("solitaire") {
            PlaceholderScreen("Solitaire", navController)
        }

        composable("hand_game") {
            PlaceholderScreen("Hand Game", navController)
        }
    }
}

/* ========================================================= */
/* ================= PLACEHOLDER SCREEN ==================== */
/* ========================================================= */

@Composable
fun PlaceholderScreen(
    title: String,
    navController: NavHostController
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {

        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            Text(
                text = "🚧",
                style = MaterialTheme.typography.displayLarge
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "$title\nقريباً...",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            ) {
                Text("العودة للرئيسية")
            }
        }
    }
}
