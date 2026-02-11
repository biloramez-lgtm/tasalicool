package com.example.tasalicool.models

import android.content.Context
import com.google.gson.Gson

object GameSaveManager {

    private const val PREF_NAME = "game_save_pref"
    private const val KEY_GAME_STATE = "game_state"

    private val gson = Gson()

    fun saveGame(context: Context, gameState: GameState) {
        val sharedPref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val editor = sharedPref.edit()

        val json = gson.toJson(gameState)
        editor.putString(KEY_GAME_STATE, json)
        editor.apply()
    }

    fun loadGame(context: Context): GameState? {
        val sharedPref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = sharedPref.getString(KEY_GAME_STATE, null)

        return if (json != null) {
            gson.fromJson(json, GameState::class.java)
        } else {
            null
        }
    }

    fun clearSave(context: Context) {
        val sharedPref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        sharedPref.edit().clear().apply()
    }
}
