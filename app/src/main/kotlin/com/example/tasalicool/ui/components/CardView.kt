package com.example.tasalicool.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tasalicool.models.Card
import com.example.tasalicool.models.Suit

/* =========================================================
   🎴 MAIN CARD VIEW – ELITE VERSION
   ========================================================= */

@Composable
fun CardView(
    card: Card,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    onClick: (() -> Unit)? = null
) {

    val cardColor = remember(card.suit) {
        when (card.suit) {
            Suit.HEARTS, Suit.DIAMONDS -> Color.Red
            Suit.CLUBS, Suit.SPADES -> Color.Black
        }
    }

    val suitSymbol = remember(card.suit) {
        when (card.suit) {
            Suit.HEARTS -> "♥"
            Suit.DIAMONDS -> "♦"
            Suit.CLUBS -> "♣"
            Suit.SPADES -> "♠"
        }
    }

    val backgroundColor by animateColorAsState(
        if (isSelected) Color(0xFFFFF3C4) else Color.White,
        label = "card_bg"
    )

    val borderWidth by animateDpAsState(
        if (isSelected) 3.dp else 1.dp,
        label = "card_border"
    )

    val elevation by animateDpAsState(
        if (isSelected) 10.dp else 4.dp,
        label = "card_elevation"
    )

    Box(
        modifier = modifier
            .size(width = 80.dp, height = 120.dp)
            .shadow(elevation, RoundedCornerShape(10.dp))
            .background(backgroundColor, RoundedCornerShape(10.dp))
            .border(
                borderWidth,
                if (isSelected) Color(0xFFFFC107) else Color.Gray,
                RoundedCornerShape(10.dp)
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

            CornerContent(card.rank.displayName, suitSymbol, cardColor)

            Text(
                text = suitSymbol,
                fontSize = 30.sp,
                color = cardColor
            )

            CornerContent(suitSymbol, card.rank.displayName, cardColor)
        }
    }
}

/* ========================================================= */

@Composable
private fun CornerContent(
    top: String,
    bottom: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = top,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = bottom,
            fontSize = 14.sp,
            color = color
        )
    }
}

/* =========================================================
   🎴 CARD BACK
   ========================================================= */

@Composable
fun CardBackView(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(width = 80.dp, height = 120.dp)
            .shadow(6.dp, RoundedCornerShape(10.dp))
            .background(Color(0xFF1565C0), RoundedCornerShape(10.dp))
            .border(
                1.dp,
                Color(0xFF0D47A1),
                RoundedCornerShape(10.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "♠♥♦♣",
            fontSize = 22.sp,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}

/* =========================================================
   🎴 COMPACT CARD – PLAYER HAND
   ========================================================= */

@Composable
fun CompactCardView(
    card: Card,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    onClick: (() -> Unit)? = null
) {

    val cardColor = remember(card.suit) {
        when (card.suit) {
            Suit.HEARTS, Suit.DIAMONDS -> Color.Red
            Suit.CLUBS, Suit.SPADES -> Color.Black
        }
    }

    val suitSymbol = remember(card.suit) {
        when (card.suit) {
            Suit.HEARTS -> "♥"
            Suit.DIAMONDS -> "♦"
            Suit.CLUBS -> "♣"
            Suit.SPADES -> "♠"
        }
    }

    val elevation by animateDpAsState(
        if (isSelected) 8.dp else 2.dp,
        label = "compact_elevation"
    )

    Box(
        modifier = modifier
            .size(width = 55.dp, height = 75.dp)
            .shadow(elevation, RoundedCornerShape(6.dp))
            .background(
                if (isSelected) Color(0xFFFFF3C4) else Color.White,
                RoundedCornerShape(6.dp)
            )
            .border(
                if (isSelected) 2.dp else 1.dp,
                if (isSelected) Color(0xFFFFC107) else Color.Gray,
                RoundedCornerShape(6.dp)
            )
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = card.rank.displayName,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = cardColor
            )
            Text(
                text = suitSymbol,
                fontSize = 14.sp,
                color = cardColor
            )
        }
    }
}
