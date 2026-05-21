package com.karishma.swiggyanimation

import android.graphics.Color as AndroidColor
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
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
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

// ── Data ─────────────────────────────────────────────────────────────────────

data class PokemonCard(
    val id: Int,
    val name: String,
    val type: String,
    val hp: Int,
    val attackName: String,
    val attackDamage: Int,
    val baseColor: Color,
    val accentColor: Color,
    val pokeApiId: Int
)

private val pokemonCards = listOf(
    PokemonCard(1, "Charizard",  "Fire",     150, "Flamethrower", 90,  Color(0xFFE64A19), Color(0xFFFFB74D), 6),
    PokemonCard(2, "Blastoise",  "Water",    140, "Hydro Pump",   85,  Color(0xFF1565C0), Color(0xFF64B5F6), 9),
    PokemonCard(3, "Venusaur",   "Grass",    130, "Solar Beam",   80,  Color(0xFF2E7D32), Color(0xFF81C784), 3),
    PokemonCard(4, "Pikachu",    "Electric", 120, "Thunderbolt",  90,  Color(0xFFF9A825), Color(0xFFFFF176), 25),
    PokemonCard(5, "Mewtwo",     "Psychic",  170, "Psystrike",    100, Color(0xFF6A1B9A), Color(0xFFCE93D8), 150),
    PokemonCard(6, "Dragonite",  "Dragon",   160, "Dragon Rush",  95,  Color(0xFF00695C), Color(0xFF80CBC4), 149),
)

private fun spriteUrl(id: Int) =
    "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/$id.png"

// Continuous orbit keyframes [rotY, rotX]
private val autoKeyframes = listOf(
    floatArrayOf( 28f, -12f), floatArrayOf(-24f,  16f),
    floatArrayOf( 20f, -18f), floatArrayOf(-28f,  10f),
    floatArrayOf( 22f, -15f), floatArrayOf(-20f,  18f),
)

// Sparkle sites (normalised 0..1)
private val sparkleSites = listOf(
    Offset(0.20f, 0.14f), Offset(0.75f, 0.10f), Offset(0.88f, 0.35f),
    Offset(0.12f, 0.65f), Offset(0.82f, 0.72f), Offset(0.63f, 0.90f),
)

// Ambient particle positions for the popup background (normalised screen coords)
private val ambientParticles = List(22) { i ->
    Offset(
        x = ((i * 137.508f) % 100f) / 100f,  // golden-angle dispersion
        y = ((i * 97.3f)    % 100f) / 100f
    )
}

// ── Color helpers ─────────────────────────────────────────────────────────────

private fun hsvColor(hue: Float, sat: Float = 1f, v: Float = 1f): Color =
    Color(AndroidColor.HSVToColor(floatArrayOf(hue.mod(360f), sat, v)))

private fun rainbowAt(baseHue: Float, alpha: Float = 1f): List<Color> =
    List(9) { i -> hsvColor((baseHue + i * 45f).mod(360f), 0.90f, 1f).copy(alpha = alpha) }

// Corner radius used everywhere — increase for rounder feel
private val CARD_CORNER = 22.dp

// ── Screen ───────────────────────────────────────────────────────────────────

@Composable
fun PokemonCardScreen(onBack: () -> Unit) {
    var selectedCard by remember { mutableStateOf<PokemonCard?>(null) }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF06091A))) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.clickable(onClick = onBack).padding(6.dp)) {
                    Text("←", color = Color.White, fontSize = 24.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Pokémon Cards", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Tap to inspect  ·  Drag to tilt", color = Color.White.copy(alpha = 0.38f), fontSize = 11.sp)
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(pokemonCards, key = { it.id }) { card ->
                    GridCard(card = card, onClick = { selectedCard = card })
                }
            }
        }

        // Scrim
        AnimatedVisibility(
            visible = selectedCard != null,
            enter = fadeIn(tween(300)), exit = fadeOut(tween(300))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.92f))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        selectedCard = null
                    }
            )
        }

        // Ambient floating particles behind the popup card
        AnimatedVisibility(
            visible = selectedCard != null,
            enter = fadeIn(tween(600)), exit = fadeOut(tween(200))
        ) {
            val drift by rememberInfiniteTransition(label = "drift").animateFloat(
                initialValue = 0f, targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing)), label = "drift"
            )
            val card = selectedCard
            Canvas(modifier = Modifier.fillMaxSize()) {
                ambientParticles.forEachIndexed { i, pos ->
                    val phase = (drift + i * 0.045f).mod(1f)
                    val y = (pos.y + phase * 0.12f).mod(1f)
                    val alpha = (sin(phase * PI * 2).toFloat().coerceIn(0f, 1f)) * 0.35f
                    val r = (1.5f + (i % 3) * 1.2f).dp.toPx()
                    drawCircle(
                        color = (card?.accentColor ?: Color.White).copy(alpha = alpha),
                        radius = r,
                        center = Offset(size.width * pos.x, size.height * y)
                    )
                }
            }
        }

        // Popup card
        AnimatedVisibility(
            visible = selectedCard != null,
            enter = scaleIn(initialScale = 0.15f,
                animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow)
            ) + fadeIn(tween(150)),
            exit = scaleOut(targetScale = 0.15f, animationSpec = tween(220)) + fadeOut(tween(200)),
            modifier = Modifier.align(Alignment.Center)
        ) {
            selectedCard?.let { card ->
                Box(
                    modifier = Modifier
                        .width(270.dp)
                        .aspectRatio(0.72f)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { }
                ) {
                    PopupCard(card = card)
                }
            }
        }
    }
}

// ── Grid card — static, no holographic animation ──────────────────────────────

@Composable
private fun GridCard(card: PokemonCard, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.72f)
            // drawBehind is BEFORE clip — glow intentionally bleeds outside card
            .drawBehind {
                val r = size.maxDimension * 0.55f
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(card.baseColor.copy(alpha = 0.25f), Color.Transparent),
                        center = center, radius = r
                    ), radius = r
                )
            }
            // clip BEFORE compositing layer — prevents any draw from escaping the shape
            .clip(RoundedCornerShape(CARD_CORNER))
            .graphicsLayer { }        // compositing layer: BlendMode.Screen works correctly
            .background(
                Brush.linearGradient(
                    listOf(card.baseColor, Color(0xFF101828), card.accentColor.copy(alpha = 0.80f))
                )
            )
            .drawWithContent {
                drawContent()
                drawGlowBorder(CARD_CORNER.toPx())
            }
            .clickable(onClick = onClick)
    ) {
        CardContent(card = card)
    }
}

// ── Popup card ────────────────────────────────────────────────────────────────

@Composable
private fun PopupCard(card: PokemonCard) {
    val scope = rememberCoroutineScope()
    val rotX = remember { Animatable(0f) }
    val rotY = remember { Animatable(0f) }
    var userDragging by remember { mutableStateOf(false) }
    var introComplete by remember { mutableStateOf(false) }

    // Particle burst progress (0 → 1, plays once on open)
    val burstProgress = remember { Animatable(0f) }
    val breath by rememberInfiniteTransition(label = "breath").animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(5000, easing = CubicBezierEasing(0.4f, 0f, 0.6f, 1f)),
            RepeatMode.Reverse
        ), label = "breath"
    )

    // Intro wiggle → then hand off to continuous orbit
    LaunchedEffect(Unit) {
        launch { burstProgress.animateTo(1f, tween(900, easing = LinearOutSlowInEasing)) }
        rotY.animateTo( 40f, tween(360, easing = CubicBezierEasing(0.2f, 0f, 0.5f, 1f)))
        rotY.animateTo(-40f, tween(480))
        rotY.animateTo( 20f, tween(300))
        rotY.animateTo(-10f, tween(230))
        rotY.animateTo(  0f, spring(dampingRatio = 0.52f, stiffness = Spring.StiffnessLow))
        introComplete = true
    }

    // Continuous Lissajous orbit — runs when intro is done and user is not dragging
    LaunchedEffect(introComplete, userDragging) {
        if (!introComplete || userDragging) return@LaunchedEffect
        var idx = 0
        while (true) {
            val kf = autoKeyframes[idx % autoKeyframes.size]
            val ey = 1700 + (idx % 3) * 250
            val ex = 2200 + (idx % 4) * 180
            val ease = CubicBezierEasing(0.45f, 0f, 0.55f, 1f)
            kotlinx.coroutines.coroutineScope {
                launch { rotY.animateTo(kf[0], tween(ey, easing = ease)) }
                launch { rotX.animateTo(kf[1], tween(ex, easing = ease)) }
            }
            idx++
        }
    }

    val nx = (rotX.value / 42f).coerceIn(-1f, 1f)
    val ny = (rotY.value / 42f).coerceIn(-1f, 1f)
    val mag = (abs(nx) + abs(ny)).coerceIn(0f, 1f)
    val burst = burstProgress.value

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                rotationX = rotX.value
                rotationY = rotY.value
                cameraDistance = 7f * density
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { userDragging = true },
                    onDragEnd    = { userDragging = false },
                    onDragCancel = { userDragging = false }
                ) { change, drag ->
                    change.consume()
                    scope.launch {
                        rotY.snapTo((rotY.value + drag.x * 0.20f).coerceIn(-42f, 42f))
                        rotX.snapTo((rotX.value - drag.y * 0.20f).coerceIn(-42f, 42f))
                    }
                }
            }
            // drawBehind BEFORE clip — outer glow bleeds outside card shape intentionally
            .drawBehind { drawOuterGlow(card, mag, breath) }
            // clip BEFORE compositing layer — Screen-blend foil can't escape the shape
            .clip(RoundedCornerShape(CARD_CORNER))
            .graphicsLayer { }        // compositing layer: BlendMode.Screen works correctly
            .background(
                Brush.linearGradient(
                    listOf(card.baseColor, Color(0xFF101828), card.accentColor.copy(alpha = 0.80f))
                )
            )
            .drawWithContent {
                drawContent()
                drawHoloFoil(nx, ny, mag, breath)
                drawGlare(nx, ny, mag)
                if (mag > 0.35f) drawSparkles(breath)
                drawFlashStreak(mag, breath)
                drawParticleBurst(burst, card)
                drawGlowBorder(CARD_CORNER.toPx())
            }
    ) {
        CardContent(card = card)
    }
}

// ── Canvas helpers ────────────────────────────────────────────────────────────

/** Multi-layer glow behind the card — intensifies with tilt. */
private fun DrawScope.drawOuterGlow(card: PokemonCard, mag: Float, breath: Float) {
    val r = size.maxDimension * 0.88f
    val pulse = 0.40f + breath * 0.12f          // gentle pulsing base alpha
    drawCircle(
        brush = Brush.radialGradient(
            listOf(
                card.baseColor.copy(alpha = pulse + mag * 0.45f),
                card.accentColor.copy(alpha = (pulse * 0.5f) + mag * 0.15f),
                Color.Transparent
            ),
            center = center, radius = r
        ), radius = r
    )
}

/**
 * Holographic foil — inspired by poke-holo.simey.me.
 *
 * Three layers, all blended via Screen so they merge into each other:
 *  1. Sweep (conic) gradient at the specular point — continuously shifting rainbow arc.
 *  2. Dominant-hue soft radial bloom — the "correct" colour for this tilt angle.
 *  3. Complementary hue on the opposing side — warm/cool split on real holo cards.
 *  4. Chromatic edge brightening — coloured rim that catches light.
 *
 * At rest: a very faint breath-driven shimmer keeps the card alive.
 */
private fun DrawScope.drawHoloFoil(
    tiltX: Float, tiltY: Float, mag: Float, breath: Float
) {
    // Resting shimmer
    val restAlpha = breath * 0.06f
    if (restAlpha > 0.005f) {
        val bAngle = breath * 2f * PI.toFloat()
        val bDrift = size.minDimension * 0.60f
        val bCentre = Offset(center.x + cos(bAngle) * bDrift, center.y + sin(bAngle) * bDrift)
        drawCircle(
            brush = Brush.sweepGradient(rainbowAt(breath * 360f), bCentre),
            radius = size.maxDimension * 1.1f, center = bCentre,
            alpha = restAlpha, blendMode = BlendMode.Screen
        )
    }

    if (mag < 0.04f) return

    val aa = (mag * 0.55f).coerceIn(0f, 0.55f)
    val tAngle = atan2(tiltY.toDouble(), tiltX.toDouble())
    val baseHue = ((tAngle / (2 * PI) + 1.0) % 1.0 * 360f).toFloat()

    val specX = center.x + tiltY * size.width  * 0.45f
    val specY = center.y - tiltX * size.height * 0.45f
    val spec  = Offset(specX, specY)

    // Layer 1 — conic sweep at specular point
    drawCircle(
        brush = Brush.sweepGradient(rainbowAt(baseHue), spec),
        radius = size.maxDimension * 1.1f, center = spec,
        alpha = aa * 0.55f, blendMode = BlendMode.Screen
    )

    // Layer 2 — dominant-hue radial bloom
    drawCircle(
        brush = Brush.radialGradient(
            listOf(hsvColor(baseHue, 0.80f), hsvColor(baseHue + 40f, 0.70f).copy(alpha = 0.4f), Color.Transparent),
            center = spec, radius = size.maxDimension * 0.72f
        ),
        radius = size.maxDimension * 0.72f, center = spec,
        alpha = aa * 0.65f, blendMode = BlendMode.Screen
    )

    // Layer 3 — complementary hue on opposing side
    val compHue = (baseHue + 180f).mod(360f)
    val comp    = Offset(center.x - tiltY * size.width * 0.38f, center.y + tiltX * size.height * 0.38f)
    drawCircle(
        brush = Brush.radialGradient(
            listOf(hsvColor(compHue, 0.75f), hsvColor(compHue + 40f, 0.65f).copy(alpha = 0.35f), Color.Transparent),
            center = comp, radius = size.maxDimension * 0.58f
        ),
        radius = size.maxDimension * 0.58f, center = comp,
        alpha = aa * 0.42f, blendMode = BlendMode.Screen
    )

    // Layer 4 — chromatic edge brightening (hue-shifted, not plain white)
    val rim = (mag * 0.22f).coerceIn(0f, 0.22f)
    val edgeHue = (baseHue + 60f).mod(360f)
    val ec = hsvColor(edgeHue, 0.6f)
    drawRect(
        brush = Brush.horizontalGradient(listOf(
            ec.copy(alpha = rim * (0.5f - tiltY * 0.5f).coerceIn(0f, 1f)),
            Color.Transparent, Color.Transparent,
            ec.copy(alpha = rim * (0.5f + tiltY * 0.5f).coerceIn(0f, 1f))
        )), blendMode = BlendMode.Screen
    )
    drawRect(
        brush = Brush.verticalGradient(listOf(
            ec.copy(alpha = rim * (0.5f + tiltX * 0.5f).coerceIn(0f, 1f)),
            Color.Transparent, Color.Transparent,
            ec.copy(alpha = rim * (0.5f - tiltX * 0.5f).coerceIn(0f, 1f))
        )), blendMode = BlendMode.Screen
    )
}

/** Specular glare — only visible when tilting. */
private fun DrawScope.drawGlare(tiltX: Float, tiltY: Float, mag: Float) {
    val alpha = ((mag - 0.18f) * 0.65f).coerceIn(0f, 0.65f)
    if (alpha < 0.01f) return
    val gc = Offset(center.x + tiltY * size.width * 0.45f, center.y - tiltX * size.height * 0.45f)
    val r  = size.minDimension * 0.28f
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color.White, Color.White.copy(alpha = 0.2f), Color.Transparent),
            center = gc, radius = r
        ), radius = r, center = gc, alpha = alpha, blendMode = BlendMode.Screen
    )
}

/** Star sparkles — only when user is actively tilting past threshold. */
private fun DrawScope.drawSparkles(breath: Float) {
    sparkleSites.forEachIndexed { i, pos ->
        val phase = (breath + i * 0.167f).mod(1f)
        val brt   = if (phase < 0.5f) phase * 2f else (1f - phase) * 2f
        val a     = (brt * 0.80f).coerceIn(0f, 1f)
        if (a < 0.10f) return@forEachIndexed
        val c   = Offset(size.width * pos.x, size.height * pos.y)
        val arm = (1.8f + brt * 2.8f).dp.toPx()
        drawLine(Color.White.copy(alpha = a), c - Offset(arm, 0f), c + Offset(arm, 0f), arm * 0.5f)
        drawLine(Color.White.copy(alpha = a), c - Offset(0f, arm), c + Offset(0f, arm), arm * 0.5f)
        drawLine(Color.White.copy(alpha = a * 0.4f),
            c - Offset(arm * 0.65f, arm * 0.65f), c + Offset(arm * 0.65f, arm * 0.65f), arm * 0.3f)
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color.White.copy(alpha = a * 0.3f), Color.Transparent),
                center = c, radius = arm * 2.5f
            ), radius = arm * 2.5f, center = c, blendMode = BlendMode.Screen
        )
    }
}

/**
 * Flash streak — a bright rainbow line that zips across the card
 * when the tilt is deep, simulating catching a real specular flash on foil.
 */
private fun DrawScope.drawFlashStreak(mag: Float, breath: Float) {
    if (mag < 0.55f) return
    // Breath acts as the streak trigger — fires briefly each cycle
    val phase = (breath * 3f).mod(1f)
    val streakAlpha = when {
        phase < 0.08f -> phase / 0.08f
        phase < 0.18f -> 1f - (phase - 0.08f) / 0.10f
        else          -> 0f
    } * (mag - 0.55f) / 0.45f
    if (streakAlpha < 0.02f) return

    val startX = size.width  * 0.10f
    val startY = size.height * 0.20f
    val endX   = size.width  * 0.90f
    val endY   = size.height * 0.80f

    // Wide soft glow
    drawLine(
        brush = Brush.linearGradient(
            rainbowAt(0f, alpha = 1f),
            start = Offset(startX, startY), end = Offset(endX, endY)
        ),
        start = Offset(startX, startY), end = Offset(endX, endY),
        strokeWidth = 14.dp.toPx(),
        alpha = streakAlpha * 0.25f,
        blendMode = BlendMode.Screen
    )
    // Crisp bright core
    drawLine(
        color = Color.White.copy(alpha = streakAlpha * 0.80f),
        start = Offset(startX, startY), end = Offset(endX, endY),
        strokeWidth = 1.2.dp.toPx(),
        blendMode = BlendMode.Screen
    )
}

/**
 * Particle burst — coloured dots fly outward from the card centre on open.
 * Driven by [burstProgress] 0→1 (plays once).
 */
private fun DrawScope.drawParticleBurst(burstProgress: Float, card: PokemonCard) {
    if (burstProgress <= 0f || burstProgress >= 1f) return
    val numParticles = 14
    repeat(numParticles) { i ->
        val angle = (i.toFloat() / numParticles) * 2 * PI
        val dist  = burstProgress * size.minDimension * 0.62f
        val alpha = (1f - burstProgress).coerceIn(0f, 1f).let { it * it }
        val r     = (4f - burstProgress * 2.5f).dp.toPx().coerceAtLeast(0.5f)
        val pos   = Offset(center.x + cos(angle).toFloat() * dist,
                           center.y + sin(angle).toFloat() * dist)
        val hue   = (i.toFloat() / numParticles) * 360f
        drawCircle(
            color = hsvColor(hue, 0.8f).copy(alpha = alpha),
            radius = r, center = pos, blendMode = BlendMode.Screen
        )
    }
}

/**
 * Multi-pass simulated-glow border.
 *
 * Instead of BlurMaskFilter (which is ignored on hardware canvas), we draw
 * the same rounded rect multiple times with growing stroke width and
 * decreasing alpha — exactly how a Gaussian blur looks if you stack rings.
 * The result is a genuine soft-glow halo around every edge.
 */
private fun DrawScope.drawGlowBorder(cornerR: Float) {
    val cornerRadius = CornerRadius(cornerR)
    val rainbow = rainbowAt(0f)

    // Outer glow passes (widest → narrowest)
    val passes = 6
    for (p in passes downTo 1) {
        val t  = p.toFloat() / passes
        val sw = t * 10.dp.toPx()
        val a  = (1f - t) * 0.30f
        drawRoundRect(
            brush = Brush.sweepGradient(rainbow, center),
            cornerRadius = cornerRadius,
            style = Stroke(width = sw),
            alpha = a,
            blendMode = BlendMode.Screen
        )
    }

    // Crisp definitive stroke on top
    drawRoundRect(
        brush = Brush.sweepGradient(rainbow, center),
        cornerRadius = cornerRadius,
        style = Stroke(width = 1.3.dp.toPx()),
        alpha = 0.90f
    )
}

// ── Card UI content ───────────────────────────────────────────────────────────

@Composable
private fun CardContent(card: PokemonCard) {
    Column(modifier = Modifier.fillMaxSize().padding(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(card.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("HP ", color = Color.White.copy(alpha = 0.50f), fontSize = 8.sp)
                Text("${card.hp}", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black)
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.Black.copy(alpha = 0.22f)),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = spriteUrl(card.pokeApiId),
                contentDescription = card.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().padding(4.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .background(card.baseColor.copy(alpha = 0.38f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
                Text(card.type.uppercase(), color = Color.White, fontSize = 7.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
            }
            Text("★★★", color = card.accentColor, fontSize = 10.sp)
        }

        Spacer(modifier = Modifier.height(7.dp))
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.12f)))
        Spacer(modifier = Modifier.height(7.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("ATTACK", color = Color.White.copy(alpha = 0.35f), fontSize = 6.5.sp, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(1.dp))
                Text(card.attackName, color = Color.White.copy(alpha = 0.88f), fontSize = 9.5.sp)
            }
            Text("${card.attackDamage}", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Black)
        }
    }
}
