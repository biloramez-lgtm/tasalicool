package com.example.cardgames.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import java.io.InputStream
import java.io.OutputStream

class BluetoothManager(private val context: Context) {
    
    private val bluetoothManager: BluetoothManager = 
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter

    fun isBluetoothEnabled(): Boolean = bluetoothAdapter?.isEnabled ?: false

    fun enableBluetooth(): Boolean {
        return if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled) {
            bluetoothAdapter.enable()
        } else {
            false
        }
    }

    fun getAvailableDevices(): List<String> {
        val devices = mutableListOf<String>()
        bluetoothAdapter?.bondedDevices?.forEach { device ->
            devices.add("${device.name} (${device.address})")
        }
        return devices
    }

    fun startDiscovery() {
        bluetoothAdapter?.startDiscovery()
    }

    fun cancelDiscovery() {
        bluetoothAdapter?.cancelDiscovery()
    }
}

// نموذج لاتصال Bluetooth
data class BluetoothMessage(
    val playerID: String,
    val action: String,
    val cardData: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

// أنواع الإجراءات
object BluetoothActions {
    const val GAME_START = "GAME_START"
    const val CARD_PLAYED = "CARD_PLAYED"
    const val CARD_DRAWN = "CARD_DRAWN"
    const val TURN_CHANGED = "TURN_CHANGED"
    const val GAME_OVER = "GAME_OVER"
    const val PLAYER_JOINED = "PLAYER_JOINED"
    const val PLAYER_LEFT = "PLAYER_LEFT"
}
