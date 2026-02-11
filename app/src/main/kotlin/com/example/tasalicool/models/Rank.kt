package com.example.tasalicool.models

import java.io.Serializable

enum class Rank(
    val displayName: String,
    val value: Int
) : Serializable {

    ACE("A", 14),
    KING("K", 13),
    QUEEN("Q", 12),
    JACK("J", 11),
    TEN("10", 10),
    NINE("9", 9),
    EIGHT("8", 8),
    SEVEN("7", 7),
    SIX("6", 6),
    FIVE("5", 5),
    FOUR("4", 4),
    THREE("3", 3),
    TWO("2", 2);

    companion object {
        fun fromName(name: String): Rank? {
            return values().find { it.name == name }
        }

        fun fromValue(value: Int): Rank? {
            return values().find { it.value == value }
        }
    }
}
