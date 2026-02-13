package com.example.tasalicool.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.tasalicool.models.Game400Engine

class GameViewModel : ViewModel() {

    val engine = Game400Engine()

    // هذا فقط لإجبار Compose على إعادة الرسم
    val refresh = mutableStateOf(0)

    init {
        engine.onGameUpdated = {
            refresh.value++
        }
        engine.startGame()
    }
}
