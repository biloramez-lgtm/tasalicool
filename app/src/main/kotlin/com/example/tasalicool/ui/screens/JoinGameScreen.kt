package com.example.tasalicool.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.tasalicool.network.NetworkActions
import com.example.tasalicool.network.NetworkGameClient
import com.example.tasalicool.network.NetworkMessage
import kotlinx.coroutines.*

@Composable
fun JoinGameScreen(navController: NavHostController) {

    var ipAddress by remember { mutableStateOf("") }
    var statusText by remember { mutableStateOf("غير متصل") }
    var connected by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val client = remember { NetworkGameClient("", 5000) }

    DisposableEffect(Unit) {
        onDispose {
            client.disconnect()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(modifier = Modifier.height(30.dp))

        Text(
            text = "🔗 الانضمام إلى لعبة",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(30.dp))

        OutlinedTextField(
            value = ipAddress,
            onValueChange = { ipAddress = it },
            label = { Text("أدخل IP السيرفر") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(statusText)

        Spacer(modifier = Modifier.height(30.dp))

        Button(
            onClick = {

                scope.launch(Dispatchers.IO) {
                    try {

                        val realClient = NetworkGameClient(ipAddress, 5000)
                        realClient.connect()

                        withContext(Dispatchers.Main) {
                            statusText = "تم الاتصال بالسيرفر"
                            connected = true
                        }

                        // 🔥 إرسال رسالة انضمام
                        realClient.sendMessage(
                            NetworkMessage(
                                playerId = "Player_${System.currentTimeMillis()}",
                                gameType = "400",
                                action = NetworkActions.PLAYER_JOINED
                            )
                        )

                        // 🔥 الاستماع للرسائل
                        while (true) {
                            val message = realClient.receiveMessage()
                            if (message != null) {
                                withContext(Dispatchers.Main) {
                                    statusText =
                                        "رسالة من السيرفر: ${message.action}"
                                }
                            }
                        }

                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            statusText = "فشل الاتصال"
                        }
                    }
                }

            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("اتصال")
        }

        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = { navController.popBackStack() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("رجوع")
        }
    }
}
