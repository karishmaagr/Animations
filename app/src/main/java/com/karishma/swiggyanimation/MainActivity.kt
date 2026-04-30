package com.karishma.swiggyanimation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.karishma.swiggyanimation.ui.theme.SwiggyAnimationTheme

private enum class Screen { Home, Restaurant, Cart, Cinema, Music }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SwiggyAnimationTheme {
                var screen by remember { mutableStateOf(Screen.Home) }
                when (screen) {
                    Screen.Home -> HomeScreen(
                        onOpenRestaurant = { screen = Screen.Restaurant },
                        onOpenCinema = { screen = Screen.Cinema },
                        onOpenMusic = { screen = Screen.Music }
                    )
                    Screen.Restaurant -> RestaurantScreen(onViewCart = { screen = Screen.Cart })
                    Screen.Cart -> CartScreen(onBack = { screen = Screen.Restaurant })
                    Screen.Cinema -> CinemaBookingExperience()
                    Screen.Music -> MusicPlayerScreen(onBack = { screen = Screen.Home })
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(
    onOpenRestaurant: () -> Unit,
    onOpenCinema: () -> Unit,
    onOpenMusic: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1923))
            .statusBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "ANIMATION DEMOS",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 4.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Pick a demo to explore",
            color = Color.White.copy(alpha = 0.45f),
            fontSize = 13.sp,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(48.dp))

        DemoCard(
            title = "Food Delivery",
            subtitle = "Scroll animations · Cart flow",
            gradient = Brush.linearGradient(
                listOf(Color(0xFFFF6B35), Color(0xFFFF9800))
            ),
            onClick = onOpenRestaurant
        )

        Spacer(modifier = Modifier.height(20.dp))

        DemoCard(
            title = "Cinema Booking",
            subtitle = "IMAX screen · Seat selection · Ticket",
            gradient = Brush.linearGradient(
                listOf(Color(0xFF1A237E), Color(0xFF7B1FA2))
            ),
            onClick = onOpenCinema
        )

        Spacer(modifier = Modifier.height(20.dp))

        DemoCard(
            title = "Vinyl Music Player",
            subtitle = "Spin physics · Album art · Live theming",
            gradient = Brush.linearGradient(
                listOf(Color(0xFFB14EFF), Color(0xFFFF006E))
            ),
            onClick = onOpenMusic
        )
    }
}

@Composable
private fun DemoCard(
    title: String,
    subtitle: String,
    gradient: Brush,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(gradient)
            .clickable(onClick = onClick)
            .padding(horizontal = 28.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Column {
            Text(
                text = title,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp,
                letterSpacing = 0.5.sp
            )
        }
    }
}
