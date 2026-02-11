package com.example.cardgames.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cardgames.models.Card
import com.example.cardgames.models.Rank
import com.example.cardgames.models.Suit

@Composable
fun CardView(
    card: Card,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val cardColor = when (card.suit) {
        Suit.HEARTS, Suit.DIAMONDS -> Color.Red
        Suit.CLUBS, Suit.SPADES -> Color.Black
    }

    val suitSymbol = when (card.suit) {
        Suit.HEARTS -> "♥"
        Suit.DIAMONDS -> "♦"
        Suit.CLUBS -> "♣"
        Suit.SPADES -> "♠"
    }

    Box(
        modifier = modifier
            .size(width = 80.dp, height = 120.dp)
            .background(
                color = if (isSelected) Color(0xFFFFE082) else Color.White,
                shape = RoundedCornerShape(8.dp)
            )
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) Color(0xFFFBC02D) else Color.Gray,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // الزاوية العلوية
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(4.dp)
            ) {
                Text(
                    text = card.rank.displayName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = cardColor
                )
                Text(
                    text = suitSymbol,
                    fontSize = 16.sp,
                    color = cardColor
                )
            }

            // الرمز الأوسط
            Text(
                text = suitSymbol,
                fontSize = 28.sp,
                color = cardColor
            )

            // الزاوية السفلية
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(4.dp)
            ) {
                Text(
                    text = suitSymbol,
                    fontSize = 16.sp,
                    color = cardColor
                )
                Text(
                    text = card.rank.displayName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = cardColor
                )
            }
        }
    }
}

@Composable
fun CardBackView(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .size(width = 80.dp, height = 120.dp)
            .background(
                color = Color(0xFF1565C0),
                shape = RoundedCornerShape(8.dp)
            )
            .border(
                width = 1.dp,
                color = Color(0xFF0D47A1),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "♠♥♦♣",
                fontSize = 20.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun CompactCardView(
    card: Card,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val cardColor = when (card.suit) {
        Suit.HEARTS, Suit.DIAMONDS -> Color.Red
        Suit.CLUBS, Suit.SPADES -> Color.Black
    }

    val suitSymbol = when (card.suit) {
        Suit.HEARTS -> "♥"
        Suit.DIAMONDS -> "♦"
        Suit.CLUBS -> "♣"
        Suit.SPADES -> "♠"
    }

    Box(
        modifier = modifier
            .size(width = 50.dp, height = 70.dp)
            .background(
                color = if (isSelected) Color(0xFFFFE082) else Color.White,
                shape = RoundedCornerShape(4.dp)
            )
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) Color(0xFFFBC02D) else Color.Gray,
                shape = RoundedCornerShape(4.dp)
            )
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = card.rank.displayName,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = cardColor
            )
            Text(
                text = suitSymbol,
                fontSize = 12.sp,
                color = cardColor
            )
        }
    }
}
