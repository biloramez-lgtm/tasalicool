package com.example.tasalicool

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
import com.example.tasalicool.ui.screens.Game400Screen
import com.example.tasalicool.ui.screens.HomeScreen
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

@Composable
fun TasalicoolNavGraph(navController: NavHostController) {

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
            PlaceholderScreen(navController)
        }

        composable("hand_game") {
            PlaceholderScreen(navController)
        }

        composable("bluetooth") {
            PlaceholderScreen(navController)
        }

        composable("network") {
            PlaceholderScreen(navController)
        }
    }
}

@Composable
fun PlaceholderScreen(navController: NavHostController) {
    HomeScreen(navController)
}
