package com.example.tasalicool.network

object NetworkActions {

    // اتصال اللاعبين
    const val PLAYER_JOINED = "PLAYER_JOINED"
    const val PLAYER_LEFT = "PLAYER_LEFT"

    // إدارة اللعبة
    const val GAME_START = "GAME_START"
    const val GAME_STATE_UPDATE = "GAME_STATE_UPDATE"

    // توزيع الأوراق
    const val DEAL_CARDS = "DEAL_CARDS"
    const val REQUEST_CARDS = "REQUEST_CARDS"

    // اللعب
    const val PLAY_CARD = "PLAY_CARD"
    const val UPDATE_SCORE = "UPDATE_SCORE"

    // رسائل عامة
    const val MESSAGE = "MESSAGE"
}
