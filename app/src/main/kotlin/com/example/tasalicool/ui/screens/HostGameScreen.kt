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
import com.example.tasalicool.models.Game400Engine
import com.example.tasalicool.network.NetworkGameServer
import com.google.gson.Gson
import java.net.Inet4Address
import java.net.NetworkInterface

@Composable
fun HostGameScreen(
    navController: NavHostController,
    gameEngine: Game400Engine
) {

    var serverStarted by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("السيرفر غير مشغل") }

    var lobbyPlayers by remember { mutableStateOf(listOf<LobbyUiPlayer>()) }

    val maxPlayers = 4
    val server = remember { NetworkGameServer(5000, gameEngine) }
    val gson = remember { Gson() }

    DisposableEffect(Unit) {
        onDispose { server.stopServer() }
    }

    /* ================= LISTEN FOR START FROM SERVER ================= */

    LaunchedEffect(serverStarted) {
        // نراقب تغير حالة اللعبة
        // عند بدء اللعب ينتقل الهوست أيضاً
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

        /* ================= IP CARD ================= */

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

        /* ================= SERVER ================= */

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
                            // عند بدء اللعبة
                            navController.navigate("game400")
                        }
                    )

                    // 👇 أهم نقطة — نسمع تحديثات اللوبي
                    server.setLobbyUpdateListener { lobbyJson ->
                        val players =
                            gson.fromJson(
                                lobbyJson,
                                Array<LobbyUiPlayer>::class.java
                            ).toList()

                        lobbyPlayers = players
                    }

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

        /* ================= LOBBY ================= */

        val totalPlayers = lobbyPlayers.size
        val lobbyFull = totalPlayers == maxPlayers
        val allReady = lobbyPlayers.all { it.isReady || it.isAI }

        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {

                Text(
                    text = "👥 Lobby ($totalPlayers / 4)",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(12.dp))

                lobbyPlayers.forEach { player ->
                    PlayerRow(
                        name = player.name,
                        ready = player.isReady || player.isAI,
                        isAI = player.isAI
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        /* ================= START GAME ================= */

        val canStart = serverStarted && lobbyFull && allReady

        Button(
            onClick = {
                server.requestStartFromHost()
            },
            enabled = canStart,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                when {
                    !lobbyFull -> "بانتظار اكتمال اللاعبين (4)"
                    !allReady -> "بانتظار جاهزية الجميع"
                    else -> "🚀 Start Game"
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

/* ================= LOBBY UI MODEL ================= */

data class LobbyUiPlayer(
    val networkId: String,
    val name: String,
    val isReady: Boolean,
    val isAI: Boolean
)

/* ================= PLAYER ROW ================= */

@Composable
fun PlayerRow(
    name: String,
    ready: Boolean,
    isAI: Boolean = false
) {

    val statusColor =
        if (ready) Color(0xFF4CAF50)
        else Color(0xFFF44336)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(12.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Text(
            text = if (isAI) "🤖 $name" else "👤 $name",
            modifier = Modifier.weight(1f)
        )

        Box(
            modifier = Modifier
                .size(10.dp)
                .background(statusColor, RoundedCornerShape(50))
        )
    }
}

/* ================= WIFI IP ================= */

fun getWifiIpAddress(): String? {
    return try {
        NetworkInterface.getNetworkInterfaces().toList().forEach { intf ->
            if (intf.name.contains("wlan", true)) {
                intf.inetAddresses.toList().forEach { addr ->
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        return addr.hostAddress
                    }
                }
            }
        }
        null
    } catch (_: Exception) {
        null
    }
}
