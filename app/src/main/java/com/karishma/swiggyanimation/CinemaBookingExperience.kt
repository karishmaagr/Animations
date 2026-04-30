package com.karishma.swiggyanimation

/*
 * Copyright 2026 Kyriakos Georgiopoulos
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.app.Activity
import android.content.ContextWrapper
import android.view.TextureView
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

// ─── Shared-element seat → ticket-stub spec ──────────────────────────────────
/**
 * The arc the seat icon travels when it morphs into the ticket stub.
 *
 * A soft spring (low stiffness, 0.7 damping) so the seat doesn't snap — it
 * *settles* into the ticket like a paper coupon dropped into a slot.
 */
private val SeatToTicketBoundsTransform: BoundsTransform = BoundsTransform { _, _ ->
    spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessLow)
}
private const val SEAT_TICKET_KEY = "seat-stub"

// ─── Ticket dimensions ───────────────────────────────────────────────────────
private val TicketWidth = 360.dp
private val TicketHalfHeight = 270.dp
private val TicketTotalHeight = TicketHalfHeight * 2
private val TicketCornerRadius = 32.dp
private val TicketSideNotchRadius = 12.dp

// ─── Screen / reflection geometry (pre-density local pixels) ─────────────────
// All values are pre-density "design pixels" inside the zoomable content space.
// The outer Box's graphicsLayer scales them to screen pixels.
private const val ScreenBottomWidth = 540f
private const val ScreenPerspective = 1.35f
private const val ScreenTopWidth = ScreenBottomWidth * ScreenPerspective
private const val ScreenCanvasHeight = 145f
private const val ScreenCurveDepth = 50f
private const val ReflectionHeight = 250f
private const val ScreenInsetX = (ScreenTopWidth - ScreenBottomWidth) / 2f
private const val VideoHeight = ScreenTopWidth * (9f / 16f)
private const val MainVideoOffsetY = (ScreenCanvasHeight - VideoHeight) / 2f
private const val ReflectionVideoOffsetY = MainVideoOffsetY + ScreenCurveDepth

// ─── Seat geometry / hit testing ─────────────────────────────────────────────
private const val SeatWidthPx = 38f
private const val SeatHeightPx = 34f
private const val SeatHitRadiusSq = 30f * 30f          // squared radius — skip sqrt on every tap
private const val SeatGlideTravelPx = 1500f            // explosion distance for unselected seats

private val SeatColorAvailable = Color(color = 0xFF64748B)
private val SeatColorOccupied = Color(color = 0xFF1E293B)

/**
 * The U-shape that draws every theater seat (back rails + cushion).
 *
 * Allocated once at class-load. ~95 seats redrawn every animation frame
 * would otherwise allocate ~5,700 [Path] objects per second on the GC.
 * Compose's [DrawScope.drawPath] only reads the path; never mutate this.
 */
private val SeatBackPath: Path = Path().apply {
    val w = SeatWidthPx
    val h = SeatHeightPx
    moveTo(x = 0f, y = 0f)
    lineTo(x = 0f, y = h - 6f)
    quadraticTo(x1 = 0f, y1 = h, x2 = 6f, y2 = h)
    lineTo(x = w - 6f, y = h)
    quadraticTo(x1 = w, y1 = h, x2 = w, y2 = h - 6f)
    lineTo(x = w, y = 0f)
}

// ─── Shared brushes (Compose Color is a value class — these are cheap) ───────
private val PremiumGoldGradient = Brush.linearGradient(
    colors = listOf(
        Color(color = 0xFFFFD166), // bright highlight
        Color(color = 0xFFF4C430), // saffron core
        Color(color = 0xFFD4AF37)  // metallic edge
    )
)

private val RoomBackgroundBrush = Brush.radialGradient(
    colors = listOf(
        Color(color = 0xFF16213E),
        Color(color = 0xFF080B12),
        Color(color = 0xFF000000)
    ),
    radius = 2000f
)

private val ScreenBottomShadeBrush = Brush.verticalGradient(
    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.4f))
)

/**
 * Gradient stops that mimic real *light scatter* from a screen reflection.
 *
 * Light obeys the inverse-square law: intensity falls off as `1 / d²`.
 * Sampling 50 stops over a normalized distance `[1, 3.2]` gives the
 * reflection a physically plausible falloff — bright near the screen,
 * fading rapidly into the floor — without baking a bitmap.
 *
 * Multiplied by `0.65` to keep the brightest sample inside a believable
 * ambient range. Cached at process load; safe to share across draws.
 */
private val InverseSquareReflectionStops: Array<Pair<Float, Color>> = run {
    val steps = 50
    val scatteringRate = 2.2f
    Array(size = steps + 1) { i ->
        val fraction = i / steps.toFloat()
        val distance = 1f + (fraction * scatteringRate)
        val intensity = 1f / (distance * distance)
        val finalAlpha = (intensity * 0.65f).coerceIn(minimumValue = 0f, maximumValue = 1f)
        fraction to Color.Black.copy(alpha = finalAlpha)
    }
}

private val ReflectionEdgeMaskStops: Array<Pair<Float, Color>> = arrayOf(
    0.0f to Color.Transparent,
    0.08f to Color.Black,
    0.92f to Color.Black,
    1.0f to Color.Transparent,
)

private val OriginTopLeft = TransformOrigin(pivotFractionX = 0f, pivotFractionY = 0f)

// ─── Stateless Shape singletons (Compose caches outline-per-size internally) ─
private val ImaxScreenShape: Shape = IMAXScreenShape()
private val ReflectionShape: Shape =
    PhysicsReflectionShape(curveDepth = ScreenCurveDepth, insetX = ScreenInsetX)
private val PremiumGoldTextStyle = TextStyle(brush = PremiumGoldGradient)

// --- Data Models ---
enum class SeatStatus { AVAILABLE, OCCUPIED, VIP }
data class TheaterSeat(
    val id: String, val row: String, val number: Int,
    val status: SeatStatus, val x: Float, val y: Float, val angle: Float
)

// --- Flawless Interlocking Shapes ---

/**
 * The CinemaScope screen silhouette: a perspective-warped rectangle whose
 * top edge is *wider* than its bottom edge, with both horizontal edges
 * curved as quadratic Bézier arcs.
 */
class IMAXScreenShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            val w = size.width
            val h = size.height
            val bottomW = w / 1.35f
            val insetX = (w - bottomW) / 2f
            val bottomCtrlY = h - h * (50f / 145f)
            val topCornerY = h * (25f / 145f)
            val topCtrlY = -h * (25f / 145f)

            moveTo(x = insetX, y = h)
            quadraticTo(x1 = w / 2f, y1 = bottomCtrlY, x2 = w - insetX, y2 = h)
            lineTo(x = w, y = topCornerY)
            quadraticTo(x1 = w / 2f, y1 = topCtrlY, x2 = 0f, y2 = topCornerY)
            close()
        }
        return Outline.Generic(path)
    }
}

/**
 * The mirror of the screen — but bowed downward like a still pool of water.
 */
class PhysicsReflectionShape(private val curveDepth: Float, private val insetX: Float) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        return Outline.Generic(Path().apply {
            val w = size.width
            val h = size.height
            moveTo(x = insetX, y = curveDepth)
            quadraticTo(x1 = w / 2f, y1 = 0f, x2 = w - insetX, y2 = curveDepth)
            quadraticTo(x1 = w - (insetX * 0.1f), y1 = h * 0.5f, x2 = w, y2 = h)
            lineTo(x = 0f, y = h)
            quadraticTo(x1 = insetX * 0.1f, y1 = h * 0.5f, x2 = insetX, y2 = curveDepth)
            close()
        })
    }
}


/**
 * A single-screen cinema booking flow with four physical "moments":
 *
 * 1. **Pre-show** — a 3-2-1 countdown plays inside an IMAX-shaped screen.
 * 2. **Selection** — pinch / pan to inspect the seat grid.
 * 3. **Confirmation** — the screen *collapses* with a CRT shutoff.
 * 4. **Ticket** — the chosen seat morphs into the ticket stub.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun CinemaBookingExperience() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val view = LocalView.current

    // --- Dynamic Dark Theme Status Bar ---
    if (!view.isInEditMode) {
        LaunchedEffect(key1 = Unit) {
            var currentContext: android.content.Context = context
            var activity: Activity? = null
            while (currentContext is ContextWrapper) {
                if (currentContext is Activity) {
                    activity = currentContext
                    break
                }
                currentContext = currentContext.baseContext
            }
            activity?.window?.let { window ->
                window.statusBarColor = android.graphics.Color.parseColor("#16213E")
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            }
        }
    }

    val seats = remember(key1 = "modern_cinema_grid_v5") { generatePerfectCinemaSeats() }

    var initialScale by remember { mutableFloatStateOf(value = 0f) }
    var initialOffset by remember { mutableStateOf(value = Offset.Unspecified) }

    var scale by remember { mutableFloatStateOf(value = 1f) }
    var offset by remember { mutableStateOf(value = Offset.Unspecified) }

    var selectedSeat by remember { mutableStateOf<TheaterSeat?>(value = null) }
    var isConfirmed by remember { mutableStateOf(value = false) }
    var showTicket by remember { mutableStateOf(value = false) }
    var showThankYou by remember { mutableStateOf(value = false) }

    val crtProgress = remember { Animatable(initialValue = 0f) }
    val seatGlideProgress = remember { Animatable(initialValue = 0f) }
    val videoUrl = "https://www.w3schools.com/html/mov_bbb.mp4"

    var isCountdownActive by remember { mutableStateOf(value = true) }
    var currentCountdownNumber by remember { mutableIntStateOf(value = 3) }

    val mainPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUrl))
            repeatMode = Player.REPEAT_MODE_ALL
            volume = 0f
            prepare()
        }
    }

    val reflectionPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUrl))
            repeatMode = Player.REPEAT_MODE_ALL
            volume = 0f
            prepare()
        }
    }

    LaunchedEffect(key1 = Unit) {
        for (i in 3 downTo 1) {
            currentCountdownNumber = i
            delay(timeMillis = 1000)
        }
        isCountdownActive = false
        mainPlayer.playWhenReady = true
        reflectionPlayer.playWhenReady = true
    }

    DisposableEffect(key1 = Unit) {
        onDispose {
            mainPlayer.release()
            reflectionPlayer.release()
        }
    }

    LaunchedEffect(key1 = isConfirmed) {
        if (isConfirmed) {
            showThankYou = true
            delay(timeMillis = 1200)

            crtProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 750, easing = LinearEasing)
            )

            scope.launch {
                seatGlideProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = 20f
                    )
                )
            }
            delay(timeMillis = 1000)
            showTicket = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = RoomBackgroundBrush)
            .statusBarsPadding()
            .onSizeChanged { size ->
                if (initialScale == 0f && size.width > 0) {
                    initialScale = size.width / 880f
                    val localContentCenterY = 320f
                    initialOffset = Offset(
                        x = size.width / 2f,
                        y = (size.height / 2f) - (localContentCenterY * initialScale)
                    )
                    scale = initialScale
                    offset = initialOffset
                }
            }
            .pointerInput(key1 = Unit) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    if (isConfirmed || offset == Offset.Unspecified) return@detectTransformGestures
                    val oldScale = scale
                    scale = (scale * zoom).coerceIn(minimumValue = 0.5f, maximumValue = 3f)
                    val contentCentroid = (centroid - offset) / oldScale
                    offset = centroid - (contentCentroid * scale) + pan
                }
            }
            .pointerInput(key1 = Unit) {
                detectTapGestures { tapOffset ->
                    if (isConfirmed || offset == Offset.Unspecified) return@detectTapGestures
                    val localTap = (tapOffset - offset) / scale
                    val hit = seats.find { s ->
                        val dx = localTap.x - s.x
                        val dy = localTap.y - s.y
                        (dx * dx + dy * dy) < SeatHitRadiusSq
                    }
                    selectedSeat = if (hit != null && hit.status != SeatStatus.OCCUPIED) {
                        if (selectedSeat?.id == hit.id) null else hit
                    } else null
                }
            }
    ) {
        // --- 0. Spatial Title Animation ---
        // Optimization: Zero recompositions during drag. Alpha is deferred directly to the graphicsLayer.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 56.dp)
                .graphicsLayer {
                    if (isConfirmed || initialScale == 0f || initialOffset == Offset.Unspecified) {
                        alpha = 0f
                    } else {
                        val byZoom = (1f - (abs(scale - initialScale) * 5f)).coerceIn(
                            minimumValue = 0f,
                            maximumValue = 1f
                        )
                        val byPan = (1f - ((initialOffset.y - offset.y) / 150f)).coerceIn(
                            minimumValue = 0f,
                            maximumValue = 1f
                        )
                        alpha = minOf(a = byZoom, b = byPan)
                    }
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "JETPACK COMPOSE THEATERS",
                color = Color.White,
                fontSize = 22.sp,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Black,
                letterSpacing = 6.sp
            )
            Spacer(modifier = Modifier.height(height = 6.dp))
            Text(
                text = "SELECT YOUR SEAT",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 4.sp
            )
        }

        if (initialOffset != Offset.Unspecified) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX = offset.x
                        translationY = offset.y
                        scaleX = scale
                        scaleY = scale
                        transformOrigin = OriginTopLeft
                    }
            ) {
                val screenDensity = LocalDensity.current.density

                // Avoids 'remember' overhead for simple arithmetic
                val topWidthDp = (ScreenTopWidth / screenDensity).dp
                val canvasMaxHeightDp = (ScreenCanvasHeight / screenDensity).dp
                val reflectionHeightDp = (ReflectionHeight / screenDensity).dp
                val videoHeightDp = (VideoHeight / screenDensity).dp
                val reflectionVideoOffsetDp = (ReflectionVideoOffsetY / screenDensity).dp

                // --- 1. THE MAIN SCREEN ---
                // CRT shutoff physics deferred to graphicsLayer.
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = (-ScreenTopWidth / 2f).roundToInt(),
                                y = (-ScreenCanvasHeight).roundToInt()
                            )
                        }
                        .size(width = topWidthDp, height = canvasMaxHeightDp)
                        .clip(shape = ImaxScreenShape)
                        .graphicsLayer {
                            val p = crtProgress.value
                            val yPhase = (p / 0.6f).coerceIn(minimumValue = 0f, maximumValue = 1f)
                            scaleY = 1f - (sin(x = yPhase * PI / 2).toFloat() * 0.995f)

                            scaleX = if (p < 0.4f) {
                                1f + (sin(x = (p / 0.4f) * PI).toFloat() * 0.03f)
                            } else {
                                val xPhase = ((p - 0.4f) / 0.6f).coerceIn(
                                    minimumValue = 0f,
                                    maximumValue = 1f
                                )
                                1f - sin(x = xPhase * PI / 2).toFloat()
                            }
                            alpha = (1f - ((p - 0.9f) * 10f)).coerceIn(
                                minimumValue = 0f,
                                maximumValue = 1f
                            )
                        }
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Box(modifier = Modifier.size(width = topWidthDp, height = videoHeightDp)) {
                            CinemaVideoPlayer(
                                player = mainPlayer,
                                modifier = Modifier.fillMaxSize()
                            )

                            AnimatedVisibility(
                                visible = isCountdownActive,
                                enter = fadeIn(animationSpec = tween(durationMillis = 300)),
                                exit = fadeOut(animationSpec = tween(durationMillis = 500)),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(color = Color.Black),
                                    contentAlignment = Alignment.Center
                                ) {
                                    ClassicCountdownLoader(
                                        currentNumber = currentCountdownNumber,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(brush = ScreenBottomShadeBrush)
                    )

                    AnimatedVisibility(
                        visible = showThankYou,
                        enter = fadeIn(animationSpec = tween(durationMillis = 600)),
                        modifier = Modifier.align(alignment = Alignment.Center)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(color = Color.Black.copy(alpha = 0.7f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "THANK YOU\nENJOY THE SHOW",
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 6.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 22.sp,
                                modifier = Modifier.graphicsLayer {
                                    rotationX = 45f
                                    scaleY = 0.85f
                                    cameraDistance = 8f
                                }
                            )
                        }
                    }

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val p = crtProgress.value
                        if (p in 0.3f..0.9f) {
                            val flashAlpha = sin(x = ((p - 0.3f) / 0.6f) * PI).toFloat() * 0.8f
                            drawRect(color = Color.White.copy(alpha = flashAlpha))
                        }
                    }
                }

                // --- 2. THE REALISTIC VIDEO REFLECTION ---
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = (-ScreenTopWidth / 2f).roundToInt(),
                                y = (-ScreenCurveDepth).roundToInt()
                            )
                        }
                        .size(width = topWidthDp, height = reflectionHeightDp)
                        .graphicsLayer {
                            alpha = 0.85f * (1f - (crtProgress.value * 4f)).coerceIn(
                                minimumValue = 0f,
                                maximumValue = 1f
                            )
                            compositingStrategy = CompositingStrategy.Offscreen
                        }
                        .drawWithCache {
                            val verticalMask = Brush.verticalGradient(
                                colorStops = InverseSquareReflectionStops,
                                startY = ScreenCurveDepth,
                                endY = size.height
                            )
                            val horizontalMask = Brush.horizontalGradient(
                                colorStops = ReflectionEdgeMaskStops,
                                startX = 0f,
                                endX = size.width
                            )
                            onDrawWithContent {
                                drawContent()
                                drawRect(brush = verticalMask, blendMode = BlendMode.DstIn)
                                drawRect(brush = horizontalMask, blendMode = BlendMode.DstIn)
                            }
                        }
                        .clip(shape = ReflectionShape)
                        .blur(radius = 28.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .offset(y = reflectionVideoOffsetDp)
                            .size(width = topWidthDp, height = videoHeightDp)
                            .graphicsLayer {
                                scaleY = -1f
                                scaleX = 1.05f
                            }
                    ) {
                        CinemaVideoPlayer(
                            player = reflectionPlayer,
                            modifier = Modifier.fillMaxSize()
                        )

                        AnimatedVisibility(
                            visible = isCountdownActive,
                            enter = fadeIn(animationSpec = tween(durationMillis = 300)),
                            exit = fadeOut(animationSpec = tween(durationMillis = 500)),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(color = Color.Black),
                                contentAlignment = Alignment.Center
                            ) {
                                ClassicCountdownLoader(
                                    currentNumber = currentCountdownNumber,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        AnimatedVisibility(
                            visible = showThankYou,
                            enter = fadeIn(animationSpec = tween(durationMillis = 600)),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Box(modifier = Modifier.background(color = Color.Black.copy(alpha = 0.7f)))
                        }
                    }
                }
            }
        }

        // --- 3. The Canvas Overlay (Seats) ---
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (offset == Offset.Unspecified) return@Canvas
            val origin = selectedSeat
            val originId = origin?.id
            val originX = origin?.x ?: 0f
            val originY = origin?.y ?: 0f

            val glide = seatGlideProgress.value
            val glideAlpha = (1f - (glide * 3f)).coerceIn(minimumValue = 0f, maximumValue = 1f)
            val glideDistance = SeatGlideTravelPx * glide

            withTransform({
                translate(left = offset.x, top = offset.y)
                scale(scaleX = scale, scaleY = scale, pivot = Offset.Zero)
            }) {
                seats.fastForEach { seat ->
                    val isSelected = originId == seat.id
                    if (isConfirmed && isSelected) return@fastForEach

                    var renderX = seat.x
                    var renderY = seat.y
                    var alpha = 1f

                    if (isConfirmed && origin != null && !isSelected) {
                        val dx = seat.x - originX
                        val dy = seat.y - originY
                        // Avoiding sqrt if the distance is 0 to guard against NaN
                        if (dx != 0f || dy != 0f) {
                            val dist = sqrt(x = dx * dx + dy * dy)
                            renderX += (dx / dist) * glideDistance
                            renderY += (dy / dist) * glideDistance
                        }
                        alpha = glideAlpha
                    }

                    if (alpha > 0f) {
                        drawTheaterSeat(
                            seat = seat,
                            isSelected = isSelected,
                            targetX = renderX,
                            targetY = renderY,
                            alpha = alpha
                        )
                    }
                }
            }
        }

        // --- 4. UI Overlays ---
        AnimatedVisibility(
            visible = selectedSeat != null && !isConfirmed,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessLow)
            ) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(alignment = Alignment.BottomCenter)
        ) {
            selectedSeat?.let { seat ->
                CinemaSeatBottomSheet(
                    seat = seat,
                    onConfirm = { isConfirmed = true }
                )
            }
        }

        // --- 5. Split Ticket Transition ---
        SharedTransitionLayout(modifier = Modifier.fillMaxSize()) {
            val onDismiss: () -> Unit = {
                scope.launch {
                    showTicket = false
                    isConfirmed = false
                    showThankYou = false
                    crtProgress.snapTo(targetValue = 0f)
                    seatGlideProgress.snapTo(targetValue = 0f)
                    scale = initialScale
                    offset = initialOffset
                    selectedSeat = null
                }
            }

            AnimatedVisibility(
                visible = isConfirmed && !showTicket && selectedSeat != null,
                enter = fadeIn(animationSpec = tween(durationMillis = 120)),
                exit = fadeOut(animationSpec = tween(durationMillis = 120)),
                modifier = Modifier.fillMaxSize()
            ) {
                val seat = selectedSeat
                if (seat != null) {
                    val densityMultiplier = LocalDensity.current.density
                    val seatPxW = 38f * scale
                    val seatPxH = 34f * scale
                    val seatWDp = (seatPxW / densityMultiplier).dp
                    val seatHDp = (seatPxH / densityMultiplier).dp

                    Box(modifier = Modifier.fillMaxSize()) {
                        SeatChip(
                            modifier = Modifier
                                .offset {
                                    // Deferring layout calculations here prevents recomposition
                                    if (offset == Offset.Unspecified) return@offset IntOffset.Zero
                                    val seatLeft = offset.x + scale * seat.x - seatPxW / 2f
                                    val seatTop = offset.y + scale * seat.y - seatPxH / 2f
                                    IntOffset(x = seatLeft.roundToInt(), y = seatTop.roundToInt())
                                }
                                .size(width = seatWDp, height = seatHDp)
                                .sharedElement(
                                    sharedContentState = rememberSharedContentState(key = SEAT_TICKET_KEY),
                                    animatedVisibilityScope = this@AnimatedVisibility,
                                    boundsTransform = SeatToTicketBoundsTransform
                                )
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .align(alignment = Alignment.Center)
                    .size(width = TicketWidth, height = TicketTotalHeight)
            ) {
                val halfSlideSpring = spring<IntOffset>(
                    dampingRatio = 0.7f,
                    stiffness = Spring.StiffnessLow
                )

                AnimatedVisibility(
                    visible = showTicket,
                    enter = slideInVertically(
                        initialOffsetY = { -(it + 270) },
                        animationSpec = halfSlideSpring
                    ) + fadeIn(animationSpec = tween(durationMillis = 180)),
                    exit = slideOutVertically(
                        targetOffsetY = { -(it + 270) },
                        animationSpec = tween(durationMillis = 320, easing = LinearOutSlowInEasing)
                    ) + fadeOut(animationSpec = tween(durationMillis = 220)),
                    modifier = Modifier.align(alignment = Alignment.TopCenter)
                ) {
                    selectedSeat?.let { seat ->
                        TicketTopHalf(
                            seat = seat,
                            modifier = Modifier
                                .size(width = TicketWidth, height = TicketHalfHeight)
                                .pointerInput(key1 = Unit) { detectTapGestures { onDismiss() } }
                        )
                    }
                }

                AnimatedVisibility(
                    visible = showTicket,
                    enter = slideInVertically(
                        initialOffsetY = { it + 270 },
                        animationSpec = halfSlideSpring
                    ) + fadeIn(animationSpec = tween(durationMillis = 180)),
                    exit = slideOutVertically(
                        targetOffsetY = { it + 270 },
                        animationSpec = tween(durationMillis = 320, easing = LinearOutSlowInEasing)
                    ) + fadeOut(animationSpec = tween(durationMillis = 220)),
                    modifier = Modifier.align(alignment = Alignment.BottomCenter)
                ) {
                    selectedSeat?.let { seat ->
                        TicketBottomHalf(
                            seat = seat,
                            sharedSeatSlot = {
                                SeatChip(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .sharedElement(
                                            sharedContentState = rememberSharedContentState(key = SEAT_TICKET_KEY),
                                            animatedVisibilityScope = this@AnimatedVisibility,
                                            boundsTransform = SeatToTicketBoundsTransform
                                        )
                                )
                            },
                            modifier = Modifier
                                .size(width = TicketWidth, height = TicketHalfHeight)
                                .pointerInput(key1 = Unit) { detectTapGestures { onDismiss() } }
                        )
                    }
                }
            }
        }
    }
}

// --- Classic Countdown Loader ---
@Composable
fun ClassicCountdownLoader(currentNumber: Int, modifier: Modifier = Modifier) {
    val sweepAngle = remember { Animatable(initialValue = 360f) }

    LaunchedEffect(key1 = currentNumber) {
        sweepAngle.snapTo(targetValue = 360f)
        sweepAngle.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 1000, easing = LinearEasing)
        )
    }

    Box(
        modifier = modifier.graphicsLayer {
            rotationX = 45f
            scaleY = 0.85f
            cameraDistance = 8f
        },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = (size.minDimension / 2f) * 0.85f
            val center = Offset(x = size.width / 2f, y = size.height / 2f)
            val strokeColor = Color.White.copy(alpha = 0.6f)

            drawLine(
                color = strokeColor,
                start = Offset(x = center.x, y = 0f),
                end = Offset(x = center.x, y = size.height),
                strokeWidth = 3f
            )
            drawLine(
                color = strokeColor,
                start = Offset(x = 0f, y = center.y),
                end = Offset(x = size.width, y = center.y),
                strokeWidth = 3f
            )

            drawCircle(color = strokeColor, radius = radius, style = Stroke(width = 3f))
            drawCircle(color = strokeColor, radius = radius * 0.9f, style = Stroke(width = 6f))

            drawArc(
                color = Color.White.copy(alpha = 0.15f),
                startAngle = -90f,
                sweepAngle = sweepAngle.value,
                useCenter = true,
                topLeft = Offset(x = center.x - radius, y = center.y - radius),
                size = Size(width = radius * 2f, height = radius * 2f)
            )
        }

        Text(
            text = currentNumber.toString(),
            color = Color.White.copy(alpha = 0.55f),
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// --- Video Player Composable ---
@OptIn(UnstableApi::class)
@Composable
fun CinemaVideoPlayer(player: ExoPlayer, modifier: Modifier = Modifier) {
    AndroidView(
        factory = { ctx ->
            TextureView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        update = { view -> player.setVideoTextureView(view) },
        onRelease = { view -> player.clearVideoTextureView(view) },
        modifier = modifier
    )
}

// --- Drawing Extensions ---
private val SeatStrokeStyle = Stroke(width = 4f, cap = StrokeCap.Round)
private val SeatCushionTopLeft = Offset(x = 6f, y = SeatHeightPx - 14f)
private val SeatCushionSize = Size(width = SeatWidthPx - 12f, height = 10f)
private val SeatCushionCorner = CornerRadius(x = 2f, y = 2f)

fun DrawScope.drawTheaterSeat(
    seat: TheaterSeat, isSelected: Boolean, targetX: Float, targetY: Float, alpha: Float
) {
    withTransform({
        translate(left = targetX - SeatWidthPx / 2f, top = targetY - SeatHeightPx / 2f)
    }) {
        if (isSelected) {
            drawPath(
                path = SeatBackPath,
                brush = PremiumGoldGradient,
                alpha = alpha,
                style = SeatStrokeStyle
            )
            drawRoundRect(
                brush = PremiumGoldGradient,
                alpha = alpha,
                topLeft = SeatCushionTopLeft,
                size = SeatCushionSize,
                cornerRadius = SeatCushionCorner
            )
        } else {
            val seatColor =
                if (seat.status == SeatStatus.OCCUPIED) SeatColorOccupied else SeatColorAvailable
            drawPath(
                path = SeatBackPath,
                color = seatColor,
                alpha = alpha,
                style = SeatStrokeStyle
            )
            drawRoundRect(
                color = seatColor,
                alpha = alpha,
                topLeft = SeatCushionTopLeft,
                size = SeatCushionSize,
                cornerRadius = SeatCushionCorner
            )
        }
    }
}

fun generatePerfectCinemaSeats(): List<TheaterSeat> {
    val seats = mutableListOf<TheaterSeat>()
    val rows = listOf("A", "B", "C", "D", "E", "F", "G", "H", "I")
    val stepX = 64f
    val stepY = 70f
    val layoutPattern = List(size = 11) { true }
    val totalWidth = layoutPattern.size * stepX
    val startX = -totalWidth / 2f + (stepX / 2f)
    var currentY = 190f

    rows.forEachIndexed { rowIndex, rowStr ->
        val currentPattern = if (rowIndex == 0 || rowIndex == rows.lastIndex) {
            listOf(false) + List(size = 9) { true } + listOf(false)
        } else layoutPattern

        currentPattern.forEachIndexed { colIndex, isSeat ->
            if (isSeat) {
                val x = startX + (colIndex * stepX)
                val status =
                    if (Random.nextFloat() > 0.85f) SeatStatus.OCCUPIED else SeatStatus.AVAILABLE
                seats.add(
                    TheaterSeat(
                        id = "$rowStr${colIndex + 1}",
                        row = rowStr,
                        number = colIndex + 1,
                        status = status,
                        x = x,
                        y = currentY,
                        angle = 0f
                    )
                )
            }
        }
        currentY += stepY
    }
    return seats
}

// --- UI Components ---
private val TopHalfShape = PerforatedHalfShape(
    cornerRadius = TicketCornerRadius,
    sideNotchRadius = TicketSideNotchRadius,
    notchedEdge = NotchedEdge.BOTTOM
)
private val BottomHalfShape = PerforatedHalfShape(
    cornerRadius = TicketCornerRadius,
    sideNotchRadius = TicketSideNotchRadius,
    notchedEdge = NotchedEdge.TOP
)

private val TicketColorBackground = Color(color = 0xFFF4F4F6)
private val TicketColorText = Color(color = 0xFF1D1D1F)
private val TicketColorSubtitle = Color(color = 0xFF86868B)
private val TicketAccent = Color(color = 0xFFFFD166)

private val TicketPaperBrush = Brush.verticalGradient(
    colors = listOf(Color(color = 0xFFFFFFFF), TicketColorBackground)
)

private val QrGoldBrush = Brush.linearGradient(
    colors = listOf(
        Color(color = 0xFFFFD166),
        Color(color = 0xFFF4C430),
        Color(color = 0xFFD4AF37)
    ),
    start = Offset.Zero,
    end = Offset(x = 500f, y = 500f)
)

private const val QrGridSize = 25
private const val QrFinderSize = 8
private const val QrSeed = 12345L

@Composable
fun GenerativeGradientQrCode(modifier: Modifier = Modifier) {
    Spacer(
        modifier = modifier
            .drawWithCache {
                val dimension = minOf(a = size.width, b = size.height)
                val cellSize = dimension / QrGridSize

                val offsetX = (size.width - dimension) / 2f
                val offsetY = (size.height - dimension) / 2f
                val r = Random(seed = QrSeed)

                val gridPoints = mutableListOf<Offset>()
                for (row in 0 until QrGridSize) {
                    for (col in 0 until QrGridSize) {
                        val inTopLeft = row < QrFinderSize && col < QrFinderSize
                        val inTopRight = row < QrFinderSize && col >= QrGridSize - QrFinderSize
                        val inBottomLeft = row >= QrGridSize - QrFinderSize && col < QrFinderSize

                        if (!inTopLeft && !inTopRight && !inBottomLeft) {
                            if (r.nextBoolean()) {
                                gridPoints.add(
                                    Offset(
                                        x = offsetX + col * cellSize + 1.5f,
                                        y = offsetY + row * cellSize + 1.5f
                                    )
                                )
                            }
                        }
                    }
                }

                val cellGlyphSize = Size(width = cellSize - 3f, height = cellSize - 3f)
                val cellCorner = CornerRadius(x = 4f, y = 4f)

                onDrawBehind {
                    fun drawEye(row: Int, col: Int) {
                        val origin =
                            Offset(x = offsetX + col * cellSize, y = offsetY + row * cellSize)
                        val eyeSize = 7 * cellSize
                        drawRoundRect(
                            brush = QrGoldBrush,
                            topLeft = origin,
                            size = Size(width = eyeSize, height = eyeSize),
                            cornerRadius = CornerRadius(x = 12f, y = 12f)
                        )
                        drawRect(
                            color = Color.White,
                            topLeft = origin + Offset(x = cellSize, y = cellSize),
                            size = Size(
                                width = eyeSize - 2 * cellSize,
                                height = eyeSize - 2 * cellSize
                            )
                        )
                        drawRoundRect(
                            brush = QrGoldBrush,
                            topLeft = origin + Offset(x = 2 * cellSize, y = 2 * cellSize),
                            size = Size(
                                width = eyeSize - 4 * cellSize,
                                height = eyeSize - 4 * cellSize
                            ),
                            cornerRadius = CornerRadius(x = 6f, y = 6f)
                        )
                    }
                    drawEye(row = 0, col = 0)
                    drawEye(row = 0, col = QrGridSize - 7)
                    drawEye(row = QrGridSize - 7, col = 0)

                    gridPoints.fastForEach { point ->
                        drawRoundRect(
                            brush = QrGoldBrush,
                            topLeft = point,
                            size = cellGlyphSize,
                            cornerRadius = cellCorner
                        )
                    }
                }
            }
    )
}

@Composable
fun TicketTopHalf(seat: TheaterSeat, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .shadow(
                elevation = 24.dp,
                shape = TopHalfShape,
                spotColor = Color.Black.copy(alpha = 0.6f)
            )
            .border(width = 1.dp, color = Color.White.copy(alpha = 0.6f), shape = TopHalfShape)
            .clip(shape = TopHalfShape)
            .background(brush = TicketPaperBrush)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 26.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(weight = 1f),
                contentAlignment = Alignment.Center
            ) {
                GenerativeGradientQrCode(modifier = Modifier.size(size = 130.dp))
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "FOLLOW FOR MORE",
                    color = TicketColorText,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(height = 4.dp))
                Text(
                    text = "70mm IMAX  ·  2h 21min",
                    color = TicketColorSubtitle,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(height = 8.dp))
            }
        }

        RealisticPerforationEffect(
            modifier = Modifier.align(alignment = Alignment.BottomCenter),
            isTopHalf = true
        )
        @Suppress("UNUSED_EXPRESSION") seat
    }
}

@Composable
fun TicketBottomHalf(
    seat: TheaterSeat,
    sharedSeatSlot: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .shadow(
                elevation = 24.dp,
                shape = BottomHalfShape,
                spotColor = Color.Black.copy(alpha = 0.6f)
            )
            .border(width = 1.dp, color = Color.White.copy(alpha = 0.6f), shape = BottomHalfShape)
            .clip(shape = BottomHalfShape)
            .background(brush = TicketPaperBrush)
    ) {
        RealisticPerforationEffect(
            modifier = Modifier.align(alignment = Alignment.TopCenter),
            isTopHalf = false
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "ADMIT ONE",
                color = TicketAccent,
                style = PremiumGoldTextStyle,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp,
                modifier = Modifier.align(alignment = Alignment.Start)
            )
            Spacer(modifier = Modifier.height(height = 16.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "SEAT",
                    color = TicketColorSubtitle,
                    fontSize = 11.sp,
                    letterSpacing = 3.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(height = 8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(width = 38.dp, height = 34.dp),
                        contentAlignment = Alignment.Center,
                        content = sharedSeatSlot
                    )
                    Spacer(modifier = Modifier.width(width = 12.dp))
                    Text(
                        text = "${seat.row}${seat.number}",
                        color = TicketColorText,
                        fontSize = 52.sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 56.sp
                    )
                }
            }

            Spacer(modifier = Modifier.weight(weight = 1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "DATE",
                        color = TicketColorSubtitle,
                        fontSize = 9.sp,
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(height = 4.dp))
                    Text(
                        text = "SAT 25 APR 2026",
                        color = TicketColorText,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "HALL · TIME",
                        color = TicketColorSubtitle,
                        fontSize = 9.sp,
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(height = 4.dp))
                    Text(
                        text = "07 · 20:30",
                        color = TicketColorText,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(height = 16.dp))

            Text(
                text = "ZG·2026·${seat.row}${
                    seat.number.toString().padStart(length = 3, padChar = '0')
                }",
                color = TicketColorSubtitle,
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                letterSpacing = 1.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private val PerforationDashEffect: PathEffect =
    PathEffect.dashPathEffect(intervals = floatArrayOf(16f, 16f), phase = 0f)
private val PerforationShadowColor = Color(color = 0xFF0A0D14)

@Composable
fun RealisticPerforationEffect(modifier: Modifier = Modifier, isTopHalf: Boolean) {
    val density = LocalDensity.current.density
    val snR = TicketSideNotchRadius.value * density

    Spacer(
        modifier = modifier
            .fillMaxWidth()
            .height(height = 4.dp)
            .drawWithCache {
                val yOffset = if (isTopHalf) size.height else 0f
                val startX = snR
                val endX = size.width - snR
                onDrawBehind {
                    drawLine(
                        color = Color.White,
                        start = Offset(x = startX, y = yOffset + 2f),
                        end = Offset(x = endX, y = yOffset + 2f),
                        strokeWidth = 8f,
                        cap = StrokeCap.Round,
                        pathEffect = PerforationDashEffect
                    )
                    drawLine(
                        color = PerforationShadowColor,
                        start = Offset(x = startX, y = yOffset),
                        end = Offset(x = endX, y = yOffset),
                        strokeWidth = 8f,
                        cap = StrokeCap.Round,
                        pathEffect = PerforationDashEffect
                    )
                }
            }
    )
}

@Composable
private fun SeatChip(modifier: Modifier = Modifier) {
    val uPath = remember { Path() }
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        val cornerX = (6f / 38f) * w
        val cornerY = (6f / 34f) * h
        val strokeW = (4f / 38f) * w

        uPath.reset()
        uPath.moveTo(x = 0f, y = 0f)
        uPath.lineTo(x = 0f, y = h - cornerY)
        uPath.quadraticTo(x1 = 0f, y1 = h, x2 = cornerX, y2 = h)
        uPath.lineTo(x = w - cornerX, y = h)
        uPath.quadraticTo(x1 = w, y1 = h, x2 = w, y2 = h - cornerY)
        uPath.lineTo(x = w, y = 0f)

        drawPath(
            path = uPath,
            brush = PremiumGoldGradient,
            style = Stroke(width = strokeW, cap = StrokeCap.Round)
        )

        val cushionH = (10f / 34f) * h
        val cushionTop = h - (14f / 34f) * h
        val cushionInset = (6f / 38f) * w
        drawRoundRect(
            brush = PremiumGoldGradient,
            topLeft = Offset(x = cushionInset, y = cushionTop),
            size = Size(width = w - 2f * cushionInset, height = cushionH),
            cornerRadius = CornerRadius(x = (2f / 38f) * w, y = (2f / 34f) * h)
        )
    }
}

private enum class NotchedEdge { TOP, BOTTOM }

private class PerforatedHalfShape(
    private val cornerRadius: Dp,
    private val sideNotchRadius: Dp,
    private val notchedEdge: NotchedEdge
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val cR = with(density) { cornerRadius.toPx() }
        val snR = with(density) { sideNotchRadius.toPx() }
        val w = size.width
        val h = size.height

        val path = Path().apply {
            when (notchedEdge) {
                NotchedEdge.TOP -> {
                    moveTo(x = 0f, y = snR)
                    arcTo(
                        rect = Rect(left = -snR, top = -snR, right = snR, bottom = snR),
                        startAngleDegrees = 90f,
                        sweepAngleDegrees = -90f,
                        forceMoveTo = false
                    )
                    lineTo(x = w - snR, y = 0f)
                    arcTo(
                        rect = Rect(left = w - snR, top = -snR, right = w + snR, bottom = snR),
                        startAngleDegrees = 180f,
                        sweepAngleDegrees = -90f,
                        forceMoveTo = false
                    )
                    lineTo(x = w, y = h - cR)
                    arcTo(
                        rect = Rect(left = w - 2f * cR, top = h - 2f * cR, right = w, bottom = h),
                        startAngleDegrees = 0f,
                        sweepAngleDegrees = 90f,
                        forceMoveTo = false
                    )
                    lineTo(x = cR, y = h)
                    arcTo(
                        rect = Rect(left = 0f, top = h - 2f * cR, right = 2f * cR, bottom = h),
                        startAngleDegrees = 90f,
                        sweepAngleDegrees = 90f,
                        forceMoveTo = false
                    )
                    close()
                }

                NotchedEdge.BOTTOM -> {
                    moveTo(x = 0f, y = cR)
                    arcTo(
                        rect = Rect(left = 0f, top = 0f, right = 2f * cR, bottom = 2f * cR),
                        startAngleDegrees = 180f,
                        sweepAngleDegrees = 90f,
                        forceMoveTo = false
                    )
                    lineTo(x = w - cR, y = 0f)
                    arcTo(
                        rect = Rect(left = w - 2f * cR, top = 0f, right = w, bottom = 2f * cR),
                        startAngleDegrees = 270f,
                        sweepAngleDegrees = 90f,
                        forceMoveTo = false
                    )
                    lineTo(x = w, y = h - snR)
                    arcTo(
                        rect = Rect(
                            left = w - snR,
                            top = h - snR,
                            right = w + snR,
                            bottom = h + snR
                        ), startAngleDegrees = 270f, sweepAngleDegrees = -90f, forceMoveTo = false
                    )
                    lineTo(x = snR, y = h)
                    arcTo(
                        rect = Rect(left = -snR, top = h - snR, right = snR, bottom = h + snR),
                        startAngleDegrees = 0f,
                        sweepAngleDegrees = -90f,
                        forceMoveTo = false
                    )
                    close()
                }
            }
        }
        return Outline.Generic(path)
    }
}

@Composable
fun CinemaSeatBottomSheet(seat: TheaterSeat, onConfirm: () -> Unit) {
    var triggerStagger by remember { mutableStateOf(value = false) }

    LaunchedEffect(key1 = Unit) {
        delay(timeMillis = 50)
        triggerStagger = true
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(all = 16.dp)
            .padding(bottom = 24.dp)
            .shadow(
                elevation = 30.dp,
                shape = RoundedCornerShape(size = 24.dp),
                ambientColor = Color.Black
            ),
        colors = CardDefaults.cardColors(containerColor = Color(color = 0xFF1E293B)),
        shape = RoundedCornerShape(size = 24.dp)
    ) {
        Column(modifier = Modifier.padding(all = 24.dp)) {

            // --- Stagger 1: Seat & Price Row ---
            AnimatedStaggerItem(visible = triggerStagger, index = 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AnimatedContent(
                        targetState = seat,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(durationMillis = 300)) togetherWith fadeOut(
                                animationSpec = tween(durationMillis = 300)
                            )
                        },
                        label = "SeatTextTransition"
                    ) { targetSeat ->
                        Column {
                            InitialFadingLetterText(
                                text = "Row ${targetSeat.row} - Seat ${targetSeat.number}"
                            )

                            Spacer(modifier = Modifier.height(height = 2.dp))
                            Text(
                                text = "70MM IMAX PREMIER SEATING",
                                color = Color(color = 0xFF94A3B8),
                                fontSize = 11.sp,
                                letterSpacing = 1.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Text(
                        text = "$15.00",
                        style = TextStyle(brush = PremiumGoldGradient),
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(height = 24.dp))

            // --- Stagger 2: Gradient Checkout Button ---
            AnimatedStaggerItem(visible = triggerStagger, index = 1) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(height = 60.dp)
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(size = 18.dp),
                            spotColor = Color(color = 0xFFFFC107).copy(alpha = 0.5f)
                        )
                        .clip(shape = RoundedCornerShape(size = 18.dp))
                        .background(brush = PremiumGoldGradient)
                        .clickable { onConfirm() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Checkout & Print Ticket",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.Black,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}

@Composable
fun InitialFadingLetterText(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        text.forEachIndexed { index, char ->
            if (char == ' ') {
                Spacer(modifier = Modifier.width(width = 6.dp))
            } else {
                val alphaAnim = remember(key1 = text) { Animatable(initialValue = 0f) }

                LaunchedEffect(key1 = text) {
                    delay(timeMillis = index * 35L)
                    alphaAnim.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(durationMillis = 200)
                    )
                    alphaAnim.animateTo(
                        targetValue = 0.4f,
                        animationSpec = tween(durationMillis = 200)
                    )
                    alphaAnim.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(durationMillis = 250)
                    )
                }

                Text(
                    text = char.toString(),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.graphicsLayer { alpha = alphaAnim.value }
                )
            }
        }
    }
}

@Composable
fun PlayfulButtonText(text: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "PlayfulText")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "WavePhase"
    )

    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        text.forEachIndexed { index, char ->
            if (char == ' ') {
                Spacer(modifier = Modifier.width(width = 6.dp))
            } else {
                Text(
                    text = char.toString(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.Black,
                    modifier = Modifier
                        .offset {
                            val sine = sin(x = phase + (index * 0.35f)).toFloat()
                            IntOffset(x = 0, y = (sine * -2f).dp.roundToPx())
                        }
                        .graphicsLayer {
                            val sine = sin(x = phase + (index * 0.35f)).toFloat()
                            alpha = 0.3f + ((sine + 1f) / 2f) * 0.7f
                        }
                )
            }
        }
    }
}

@Composable
private fun AnimatedStaggerItem(
    visible: Boolean,
    index: Int,
    content: @Composable () -> Unit
) {
    val enterDelay = 100 + (index * 80)

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(
            animationSpec = tween(durationMillis = 400, delayMillis = enterDelay)
        ) + slideInVertically(
            initialOffsetY = { it / 2 },
            animationSpec = tween(
                durationMillis = 500,
                delayMillis = enterDelay,
                easing = FastOutSlowInEasing
            )
        )
    ) {
        content()
    }
}