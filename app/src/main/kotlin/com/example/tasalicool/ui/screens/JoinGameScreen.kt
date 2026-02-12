package com.example.tasalicool.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.tasalicool.models.Game400Engine
import com.example.tasalicool.network.NetworkGameClient

@Composable
fun JoinGameScreen(
    navController: NavHostController,
    gameEngine: Game400Engine
) {

    var ipAddress by remember { mutableStateOf("") }
    var statusText by remember { mutableStateOf("غير متصل") }
    var connected by remember { mutableStateOf(false) }

    val client = remember { NetworkGameClient(gameEngine) }

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

                client.connect(
                    hostIp = ipAddress,
                    port = 5000,

                    onConnected = {
                        statusText = "تم الاتصال بالسيرفر"
                        connected = true
                    },

                    onDisconnected = {
                        statusText = "انقطع الاتصال"
                        connected = false
                    }
                )

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
