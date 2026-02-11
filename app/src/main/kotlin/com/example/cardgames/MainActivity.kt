package com.example.cardgames

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.cardgames.ui.screens.Game400Screen
import com.example.cardgames.ui.screens.HomeScreen
import com.example.cardgames.ui.theme.CardGamesTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CardGamesTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    CardGamesNavigation(navController)
                }
            }
        }
    }
}

@Composable
fun CardGamesNavigation(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            HomeScreen(navController)
        }
        composable("game_400") {
            Game400Screen(navController)
        }
        composable("solitaire") {
            SolitaireScreen(navController)
        }
        composable("hand_game") {
            HandGameScreen(navController)
        }
        composable("bluetooth") {
            BluetoothScreen(navController)
        }
        composable("network") {
            NetworkScreen(navController)
        }
    }
}

@Composable
fun SolitaireScreen(navController: NavHostController) {
    HomeScreen(navController) // Placeholder
}

@Composable
fun HandGameScreen(navController: NavHostController) {
    HomeScreen(navController) // Placeholder
}

@Composable
fun BluetoothScreen(navController: NavHostController) {
    HomeScreen(navController) // Placeholder
}

@Composable
fun NetworkScreen(navController: NavHostController) {
    HomeScreen(navController) // Placeholder
}
