package com.karishma.swiggyanimation.music

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val swapIn = CubicBezierEasing(0.2f, 0.7f, 0.3f, 1f)
private val swapOut = CubicBezierEasing(0.5f, 0f, 0.7f, 0.4f)

@Composable
internal fun GlassDisc(
    currentIndex: Int,
    artCache: Map<Long, Bitmap?>,
    tracks: List<Track>,
    swapDirection: Int,
    size: Dp = 300.dp,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val sizePx = with(density) { size.toPx() }
    val ringW = sizePx * 0.07f
    val innerSizeDp = with(density) { (sizePx - ringW * 4f).toDp() }

    Box(
        modifier = modifier
            .size(size)
            .shadow(elevation = 40.dp, shape = CircleShape, clip = false)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF1a1420), Color(0xFF0a0810), Color(0xFF050308)),
                )
            ),
        contentAlignment = Alignment.Center,
    ) {
        // Glass ring sheen overlay
        Canvas(Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val r = this.size.minDimension / 2f
            // Outer rim highlight
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0f),
                        Color.White.copy(alpha = 0.18f),
                        Color.White.copy(alpha = 0f),
                        Color.White.copy(alpha = 0f),
                        Color.White.copy(alpha = 0.08f),
                        Color.White.copy(alpha = 0f),
                    ),
                    center = center,
                ),
                radius = r,
                center = center,
                style = Stroke(width = ringW),
            )
            // Inner shadow ring
            drawCircle(
                color = Color.Black.copy(alpha = 0.5f),
                radius = r - ringW * 2f,
                center = center,
                style = Stroke(width = 2f),
            )
        }

        // Swapping album art — scales in/out on track change
        AnimatedContent(
            targetState = currentIndex,
            transitionSpec = {
                val dir = swapDirection
                (slideInHorizontally(tween(700, easing = swapIn)) { (it * dir * 0.9f).toInt() } +
                 scaleIn(tween(700, easing = swapIn), initialScale = 0.12f)) togetherWith
                (slideOutHorizontally(tween(550, easing = swapOut)) { (-it * dir * 1.1f).toInt() } +
                 scaleOut(tween(550, easing = swapOut), targetScale = 0.15f) +
                 fadeOut(tween(400)))
            },
            label = "discSwap",
            modifier = Modifier.size(innerSizeDp).clip(CircleShape),
        ) { idx ->
            val bitmap = artCache[tracks[idx].id]
            SpinningArt(bitmap = bitmap, innerSizeDp = innerSizeDp)
        }

        // Center spindle dot
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF555555), Color(0xFF111111)),
                    )
                ),
        )
    }
}

@Composable
private fun SpinningArt(bitmap: Bitmap?, innerSizeDp: Dp) {
    val infiniteTransition = rememberInfiniteTransition(label = "spin")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing)),
        label = "discRotation",
    )

    Box(
        modifier = Modifier
            .size(innerSizeDp)
            .clip(CircleShape)
            .graphicsLayer { rotationZ = rotation },
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Canvas(Modifier.fillMaxSize()) {
                val bmp = bitmap.asImageBitmap()
                val s = minOf(bmp.width, bmp.height)
                drawImage(
                    image = bmp,
                    srcOffset = androidx.compose.ui.unit.IntOffset((bmp.width - s) / 2, (bmp.height - s) / 2),
                    srcSize = androidx.compose.ui.unit.IntSize(s, s),
                    dstSize = androidx.compose.ui.unit.IntSize(size.width.toInt(), size.height.toInt()),
                    filterQuality = androidx.compose.ui.graphics.FilterQuality.Medium,
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.sweepGradient(
                            listOf(Color(0xFF2a1a3a), Color(0xFF1a0a2a), Color(0xFF2a1a3a))
                        )
                    )
            )
        }
    }
}

// Partial disc peeking in from left or right edge
@Composable
internal fun PeekDisc(
    bitmap: Bitmap?,
    fromRight: Boolean,
    discSize: Dp,
    modifier: Modifier = Modifier,
) {
    val peekSize = discSize * 0.5f
    Box(
        modifier = modifier
            .size(peekSize)
            .clip(CircleShape)
            .background(Color(0xFF0a0810))
            .graphicsLayer { alpha = 0.55f },
        contentAlignment = Alignment.Center,
    ) {
        val innerSize = peekSize * 0.7f
        Box(
            modifier = Modifier
                .size(innerSize)
                .clip(CircleShape),
        ) {
            if (bitmap != null) {
                Canvas(Modifier.fillMaxSize()) {
                    val bmp = bitmap.asImageBitmap()
                    val s = minOf(bmp.width, bmp.height)
                    drawImage(
                        image = bmp,
                        srcOffset = androidx.compose.ui.unit.IntOffset((bmp.width - s) / 2, (bmp.height - s) / 2),
                        srcSize = androidx.compose.ui.unit.IntSize(s, s),
                        dstSize = androidx.compose.ui.unit.IntSize(size.width.toInt(), size.height.toInt()),
                        filterQuality = androidx.compose.ui.graphics.FilterQuality.Low,
                    )
                }
            }
        }
    }
}
