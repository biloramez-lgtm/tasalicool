package com.example.tasalicool.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tasalicool.network.NetworkGameServer
import com.example.tasalicool.viewmodel.GameViewModel
import com.google.gson.Gson
import java.net.Inet4Address
import java.net.NetworkInterface

@Composable
fun HostGameScreen(
    navController: NavHostController,
    viewModel: GameViewModel = viewModel()
) {

    val gameEngine = viewModel.engine

    var serverStarted by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("السيرفر غير مشغل") }
    var lobbyPlayers by remember { mutableStateOf(listOf<LobbyUiPlayer>()) }
    var hasNavigatedToGame by remember { mutableStateOf(false) }

    val server = remember { NetworkGameServer(5000, gameEngine) }
    val gson = remember { Gson() }

    DisposableEffect(Unit) {
        onDispose { server.stopServer() }
    }

    LaunchedEffect(Unit) {
        server.setLobbyUpdateListener { lobbyJson ->
            try {
                val players =
                    gson.fromJson(
                        lobbyJson,
                        Array<LobbyUiPlayer>::class.java
                    )?.toList() ?: emptyList()

                lobbyPlayers = players
            } catch (_: Exception) {
                lobbyPlayers = emptyList()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "🎮 Multiplayer Host",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {

                Text("📡 IP Address", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = getWifiIpAddress() ?: "غير متصل بالشبكة",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                if (!serverStarted) {

                    server.startServer(
                        onClientConnected = { playerId ->
                            statusText = "🟢 $playerId connected"
                        },
                        onClientDisconnected = { playerId ->
                            statusText = "🔴 $playerId disconnected"
                        },
                        onGameUpdated = {
                            if (!hasNavigatedToGame) {
                                hasNavigatedToGame = true
                                navController.navigate("game_400")
                            }
                        }
                    )

                    serverStarted = true
                    statusText = "🚀 Server running on port 5000"
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (serverStarted) "السيرفر يعمل..." else "تشغيل السيرفر")
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (serverStarted) {
            Button(
                onClick = {
                    server.stopServer()
                    lobbyPlayers = emptyList()
                    serverStarted = false
                    hasNavigatedToGame = false
                    statusText = "تم إيقاف السيرفر"
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("إيقاف السيرفر")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        Text(statusText)
        Spacer(modifier = Modifier.height(25.dp))

        val totalPlayers = lobbyPlayers.size
        val allReady =
            lobbyPlayers.isNotEmpty() &&
            lobbyPlayers.all { it.isReady }

        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {

                Text(
                    text = "👥 Lobby ($totalPlayers connected)",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(12.dp))

                lobbyPlayers.forEach { player ->
                    PlayerRow(
                        name = player.name,
                        ready = player.isReady,
                        isAI = player.isAI
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        val canStart = serverStarted && allReady

        Button(
            onClick = {
                server.requestStartFromHost()
            },
            enabled = canStart,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                when {
                    lobbyPlayers.isEmpty() ->
                        "بانتظار دخول لاعب واحد على الأقل"
                    !allReady ->
                        "بانتظار جاهزية الجميع"
                    else ->
                        "🚀 Start Game"
                }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("رجوع")
        }
    }
}
