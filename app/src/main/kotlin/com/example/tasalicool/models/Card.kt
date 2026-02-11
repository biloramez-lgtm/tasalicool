package com.example.tasalicool.models

import java.io.Serializable

data class Card(
    val suit: Suit,
    val rank: Rank
) : Serializable {

    fun isTrump(): Boolean = suit == Suit.HEARTS

    fun strength(): Int =
        if (isTrump()) rank.value + 20 else rank.value
}
