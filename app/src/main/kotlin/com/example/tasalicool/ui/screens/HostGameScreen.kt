package com.example.tasalicool.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.tasalicool.network.NetworkActions
import com.example.tasalicool.network.NetworkGameServer
import com.example.tasalicool.network.NetworkMessage
import java.net.Inet4Address
import java.net.NetworkInterface

@Composable
fun HostGameScreen(navController: NavHostController) {

    var serverStarted by remember { mutableStateOf(false) }
    var connectedPlayers by remember { mutableStateOf(listOf<String>()) }
    var logs by remember { mutableStateOf(listOf<String>()) }

    val server = remember { NetworkGameServer(5000) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(modifier = Modifier.height(30.dp))

        Text(
            text = "🎮 استضافة لعبة عبر Wi-Fi",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text("IP جهازك:")
        Text(getLocalIpAddress() ?: "غير متصل بالشبكة")

        Spacer(modifier = Modifier.height(30.dp))

        Button(
            onClick = {
                if (!serverStarted) {

                    server.startServer(

                        onClientConnected = { playerId ->
                            connectedPlayers = connectedPlayers + playerId

                            logs = logs + "🟢 Player Joined: $playerId"

                            // إرسال رسالة انضمام
                            server.broadcastMessage(
                                NetworkMessage(
                                    playerId = playerId,
                                    gameType = "LOCAL_WIFI",
                                    action = NetworkActions.PLAYER_JOINED
                                )
                            )
                        },

                        onMessageReceived = { message ->

                            logs = logs + "📩 ${message.playerId}: ${message.action}"

                            if (message.action == NetworkActions.PLAYER_LEFT) {
                                connectedPlayers =
                                    connectedPlayers.filter { it != message.playerId }
                            }
                        }
                    )

                    serverStarted = true
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (serverStarted) "السيرفر يعمل..." else "تشغيل السيرفر")
        }

        Spacer(modifier = Modifier.height(30.dp))

        Text("اللاعبون المتصلون:")
        Spacer(modifier = Modifier.height(10.dp))

        connectedPlayers.forEach {
            Text("• $it")
        }

        Spacer(modifier = Modifier.height(30.dp))

        Text("سجل الأحداث:")
        Spacer(modifier = Modifier.height(10.dp))

        logs.takeLast(5).forEach {
            Text(it)
        }

        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = {
                server.stopServer()
                navController.popBackStack()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("رجوع")
        }
    }
}

/* ============================= */
/* 🔥 الحصول على IP الجهاز */
/* ============================= */

fun getLocalIpAddress(): String? {
    return try {
        val interfaces = NetworkInterface.getNetworkInterfaces()
        for (intf in interfaces) {
            val addresses = intf.inetAddresses
            for (addr in addresses) {
                if (!addr.isLoopbackAddress && addr is Inet4Address) {
                    return addr.hostAddress
                }
            }
        }
        null
    } catch (ex: Exception) {
        null
    }
}
