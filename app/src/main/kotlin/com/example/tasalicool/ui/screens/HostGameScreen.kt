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
import java.net.Inet4Address
import java.net.NetworkInterface

@Composable
fun HostGameScreen(
    navController: NavHostController,
    gameEngine: Game400Engine
) {

    var serverStarted by remember { mutableStateOf(false) }
    var connectedPlayers by remember { mutableStateOf(listOf<String>()) }
    var statusText by remember { mutableStateOf("السيرفر غير مشغل") }

    val maxPlayers = 4
    val aiCount = maxPlayers - connectedPlayers.size

    val server = remember { NetworkGameServer(5000, gameEngine) }

    DisposableEffect(Unit) {
        onDispose { server.stopServer() }
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
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text("📡 IP Address", style = MaterialTheme.typography.titleMedium)

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = getWifiIpAddress() ?: "غير متصل بالشبكة",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        /* ================= SERVER BUTTON ================= */

        Button(
            onClick = {

                if (!serverStarted) {

                    server.startServer(
                        onClientConnected = { playerId ->
                            connectedPlayers = connectedPlayers + playerId
                            statusText = "🟢 $playerId connected"
                        },
                        onClientDisconnected = { playerId ->
                            connectedPlayers =
                                connectedPlayers.filter { it != playerId }
                            statusText = "🔴 $playerId disconnected"
                        },
                        onGameUpdated = {
                            statusText = "🔄 Game updated"
                        }
                    )

                    serverStarted = true
                    statusText = "🚀 Server running on port 5000"
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (serverStarted)
                    "السيرفر يعمل..."
                else
                    "تشغيل السيرفر"
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (serverStarted) {
            Button(
                onClick = {
                    server.stopServer()
                    connectedPlayers = emptyList()
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

        /* ================= PLAYERS LIST ================= */

        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {

                Text(
                    text = "👥 Players (${connectedPlayers.size}/4)",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(12.dp))

                connectedPlayers.forEach { player ->
                    PlayerRow(name = player, ready = true)
                }

                repeat(aiCount) {
                    PlayerRow(name = "AI Player", ready = true, isAI = true)
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        /* ================= START GAME ================= */

        val canStart =
            serverStarted &&
                    connectedPlayers.isNotEmpty() &&
                    connectedPlayers.size <= 4

        Button(
            onClick = {
                navController.navigate("game400")
            },
            enabled = canStart,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("🚀 Start Game")
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
