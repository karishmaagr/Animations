package com.karishma.swiggyanimation.music

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

// ── Top bar for Now Playing ──────────────────────────────────────────────────

@Composable
internal fun NowPlayingTopBar(onShowLibrary: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onShowLibrary) {
            ChevronDownIcon()
        }
        Spacer(Modifier.weight(1f))
        // Drag handle
        Box(modifier = Modifier.size(width = 36.dp, height = 4.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.35f)))
        Spacer(Modifier.weight(1f))
        IconButton(onClick = {}) {
            DotsIcon()
        }
    }
}

// ── Heart / like button ──────────────────────────────────────────────────────

@Composable
internal fun HeartButton(liked: Boolean, onClick: () -> Unit) {
    val scale by animateFloatAsState(
        targetValue = if (liked) 1.25f else 1f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessHigh),
        label = "heartScale",
    )
    Box(
        modifier = Modifier
            .size(44.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        HeartIcon(filled = liked, tint = if (liked) Color(0xFFff5a7a) else Color.White.copy(alpha = 0.7f))
    }
}

// ── Transport controls ───────────────────────────────────────────────────────

@Composable
internal fun TransportControls(
    isPlaying: Boolean,
    accentColor: Color,
    onPrev: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
) {
    val playScale by animateFloatAsState(
        targetValue = if (isPlaying) 1.05f else 0.96f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessHigh),
        label = "playScale",
    )
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Shuffle
        IconButton(onClick = {}) { ShuffleIcon() }

        // Prev
        IconButton(onClick = onPrev, size = 52.dp) { PrevIcon() }

        // Play/pause
        Box(
            modifier = Modifier
                .size(56.dp)
                .graphicsLayer { scaleX = playScale; scaleY = playScale }
                .clip(CircleShape)
                .background(Color.White)
                .clickable(onClick = onPlayPause),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedContent(
                targetState = isPlaying,
                transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(150)) },
                label = "playIcon",
            ) { playing ->
                Canvas(Modifier.size(28.dp)) {
                    val w = size.width; val h = size.height
                    if (playing) {
                        drawRoundRect(color = Color(0xFF050308), topLeft = Offset(w * 0.18f, h * 0.12f), size = Size(w * 0.22f, h * 0.76f), cornerRadius = CornerRadius(3f))
                        drawRoundRect(color = Color(0xFF050308), topLeft = Offset(w * 0.60f, h * 0.12f), size = Size(w * 0.22f, h * 0.76f), cornerRadius = CornerRadius(3f))
                    } else {
                        drawPath(Path().apply { moveTo(w * 0.25f, h * 0.10f); lineTo(w * 0.90f, h * 0.50f); lineTo(w * 0.25f, h * 0.90f); close() }, color = Color(0xFF050308))
                    }
                }
            }
        }

        // Next
        IconButton(onClick = onNext, size = 52.dp) { NextIcon() }

        // Repeat
        IconButton(onClick = {}) { RepeatIcon() }
    }
}

// ── Bottom nav pill ──────────────────────────────────────────────────────────

@Composable
internal fun BottomNavPill(
    currentTrack: Track,
    currentArt: Bitmap?,
    accentColor: Color,
    activeTab: String,
    onLibraryTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(Color(0xFF14101c).copy(alpha = 0.85f))
            .then(
                Modifier.background(
                    Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.04f), Color.White.copy(alpha = 0.02f)))
                )
            ),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = {}, tint = if (activeTab == "home") Color.White else Color.White.copy(alpha = 0.55f)) { HomeIcon() }
            IconButton(onClick = onLibraryTap, tint = if (activeTab == "lib") Color.White else Color.White.copy(alpha = 0.55f)) { ListIcon() }

            // Center: current album art circle (active tab indicator)
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2a1a3a))
                    .then(Modifier.then(
                        if (true) Modifier.then(
                            Modifier.background(
                                brush = Brush.linearGradient(listOf(Color.White.copy(alpha = 0f), Color.White.copy(alpha = 0f)))
                            )
                        ) else Modifier
                    )),
                contentAlignment = Alignment.Center,
            ) {
                if (currentArt != null) {
                    Canvas(Modifier.fillMaxSize()) {
                        val bmp = currentArt.asImageBitmap()
                        val s = minOf(bmp.width, bmp.height)
                        drawImage(bmp,
                            srcOffset = androidx.compose.ui.unit.IntOffset((bmp.width-s)/2,(bmp.height-s)/2),
                            srcSize = androidx.compose.ui.unit.IntSize(s,s),
                            dstSize = androidx.compose.ui.unit.IntSize(size.width.toInt(),size.height.toInt()),
                            filterQuality = FilterQuality.Low)
                    }
                }
                // White ring for "now playing" indicator
                Canvas(Modifier.fillMaxSize()) {
                    drawCircle(color = Color.White.copy(alpha = 0.9f), radius = size.minDimension/2f, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f))
                }
            }

            IconButton(onClick = {}, tint = Color.White.copy(alpha = 0.55f)) { EqualizerIcon() }
            IconButton(onClick = {}, tint = Color.White.copy(alpha = 0.55f)) { BellIcon() }
        }
    }
}

// ── Mini player (library view) ───────────────────────────────────────────────

@Composable
internal fun MiniPlayer(
    track: Track,
    art: Bitmap?,
    isPlaying: Boolean,
    accentColor: Color,
    onExpand: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFF14101c).copy(alpha = 0.9f))
            .clickable(onClick = onExpand)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Art
        Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(Color(0xFF2a1a3a))) {
            if (art != null) {
                Canvas(Modifier.fillMaxSize()) {
                    val bmp = art.asImageBitmap()
                    val s = minOf(bmp.width, bmp.height)
                    drawImage(bmp,
                        srcOffset = androidx.compose.ui.unit.IntOffset((bmp.width-s)/2,(bmp.height-s)/2),
                        srcSize = androidx.compose.ui.unit.IntSize(s,s),
                        dstSize = androidx.compose.ui.unit.IntSize(size.width.toInt(),size.height.toInt()),
                        filterQuality = FilterQuality.Low)
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(track.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(track.artist, color = Color.White.copy(alpha = 0.55f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconButton(onClick = { onTogglePlay() }, tint = Color.White) {
            Canvas(Modifier.size(22.dp)) {
                val w = size.width; val h = size.height
                if (isPlaying) {
                    drawRoundRect(color = Color.White, topLeft = Offset(w*0.18f,h*0.15f), size = Size(w*0.22f,h*0.70f), cornerRadius = CornerRadius(3f))
                    drawRoundRect(color = Color.White, topLeft = Offset(w*0.60f,h*0.15f), size = Size(w*0.22f,h*0.70f), cornerRadius = CornerRadius(3f))
                } else {
                    drawPath(Path().apply { moveTo(w*0.25f,h*0.12f); lineTo(w*0.88f,h*0.50f); lineTo(w*0.25f,h*0.88f); close() }, color = Color.White)
                }
            }
        }
        IconButton(onClick = onNext, tint = Color.White) { NextIcon(size = 20.dp) }
    }
}

// ── Progress bar ─────────────────────────────────────────────────────────────

@Composable
internal fun TrackProgressBar(
    progress: Float,
    duration: Long,
    accentColor: Color,
    onSeekStart: () -> Unit,
    onSeek: (Float) -> Unit,
    onSeekEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    var barWidthPx by remember { mutableFloatStateOf(1f) }
    val scratchAnim = remember { androidx.compose.animation.core.Animatable(0f) }
    val scope = rememberCoroutineScope()
    var dragX by remember { mutableFloatStateOf(0f) }

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            onSeekStart()
                            barWidthPx = size.width.toFloat().coerceAtLeast(1f)
                            dragX = offset.x
                            onSeek((offset.x / barWidthPx).coerceIn(0f, 1f))
                        },
                        onDragEnd = { onSeekEnd() },
                        onDragCancel = { onSeekEnd() },
                        onHorizontalDrag = { _, dragAmount ->
                            dragX = (dragX + dragAmount).coerceIn(0f, barWidthPx)
                            scope.launch {
                                scratchAnim.animateTo((dragAmount * 0.4f).coerceIn(-12f, 12f), tween(60))
                                scratchAnim.animateTo(0f, tween(180))
                            }
                            onSeek((dragX / barWidthPx).coerceIn(0f, 1f))
                        },
                    )
                },
            contentAlignment = Alignment.CenterStart,
        ) {
            Canvas(modifier = Modifier.fillMaxWidth().height(4.dp)) {
                barWidthPx = size.width
                drawRoundRect(color = Color.White.copy(alpha = 0.18f), size = size, cornerRadius = CornerRadius(2f))
                drawRoundRect(color = accentColor, size = Size(size.width * progress.coerceIn(0f, 1f), size.height), cornerRadius = CornerRadius(2f))
            }
            val thumbHalfPx = with(density) { 8.dp.toPx() }
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .offset { IntOffset(x = (progress.coerceIn(0f, 1f) * barWidthPx - thumbHalfPx).toInt(), y = 0) }
                    .graphicsLayer { rotationZ = scratchAnim.value }
                    .clip(CircleShape)
                    .background(accentColor),
            )
        }
        Spacer(Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text((duration * progress.coerceIn(0f, 1f)).toLong().toTimestamp(), color = Color.White.copy(alpha = 0.45f), fontSize = 11.sp)
            Text(duration.toTimestamp(), color = Color.White.copy(alpha = 0.45f), fontSize = 11.sp)
        }
    }
}

// ── Playing indicator bars ───────────────────────────────────────────────────

@Composable
internal fun PlayingIndicator(color: Color) {
    val transition = rememberInfiniteTransition(label = "bars")
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.height(16.dp)) {
        repeat(3) { i ->
            val h by transition.animateFloat(
                initialValue = 4f, targetValue = 14f,
                animationSpec = infiniteRepeatable(
                    animation = tween(420),
                    repeatMode = androidx.compose.animation.core.RepeatMode.Reverse,
                    initialStartOffset = androidx.compose.animation.core.StartOffset(i * 140),
                ),
                label = "bar$i",
            )
            Box(modifier = Modifier.width(3.dp).height(h.dp).clip(RoundedCornerShape(1.5.dp)).background(color))
        }
    }
}

// ── Shared icon button ───────────────────────────────────────────────────────

@Composable
internal fun IconButton(
    onClick: () -> Unit,
    size: androidx.compose.ui.unit.Dp = 44.dp,
    tint: Color = Color.White.copy(alpha = 0.85f),
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier.size(size).clip(CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(modifier = Modifier.graphicsLayer { colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(tint) }) {
            content()
        }
    }
}

// ── SVG-like icon drawables ──────────────────────────────────────────────────

@Composable internal fun ChevronDownIcon() {
    Canvas(Modifier.size(22.dp)) {
        val p = Path().apply { moveTo(size.width*0.2f,size.height*0.35f); lineTo(size.width*0.5f,size.height*0.65f); lineTo(size.width*0.8f,size.height*0.35f) }
        drawPath(p, Color.White, style = androidx.compose.ui.graphics.drawscope.Stroke(width=2.2f, cap=StrokeCap.Round, join=androidx.compose.ui.graphics.StrokeJoin.Round))
    }
}
@Composable internal fun DotsIcon() {
    Canvas(Modifier.size(22.dp)) {
        val cx = size.width/2f; val r = 1.6f
        drawCircle(Color.White, r, Offset(cx, size.height*0.2f))
        drawCircle(Color.White, r, Offset(cx, size.height*0.5f))
        drawCircle(Color.White, r, Offset(cx, size.height*0.8f))
    }
}
@Composable internal fun HeartIcon(filled: Boolean, tint: Color) {
    Canvas(Modifier.size(26.dp)) {
        val path = Path().apply {
            val w = size.width; val h = size.height
            moveTo(w/2f, h*0.88f)
            cubicTo(w*0.1f,h*0.6f, 0f,h*0.3f, w*0.25f,h*0.18f)
            cubicTo(w*0.38f,h*0.1f, w/2f,h*0.22f, w/2f,h*0.22f)
            cubicTo(w/2f,h*0.22f, w*0.62f,h*0.1f, w*0.75f,h*0.18f)
            cubicTo(w,h*0.3f, w,h*0.6f, w/2f,h*0.88f)
            close()
        }
        if (filled) drawPath(path, tint)
        else drawPath(path, tint, style = androidx.compose.ui.graphics.drawscope.Stroke(width=1.8f, cap=StrokeCap.Round, join=androidx.compose.ui.graphics.StrokeJoin.Round))
    }
}
@Composable internal fun ShuffleIcon() {
    Canvas(Modifier.size(20.dp)) {
        val s = androidx.compose.ui.graphics.drawscope.Stroke(width=2f, cap=StrokeCap.Round, join=androidx.compose.ui.graphics.StrokeJoin.Round)
        val w=size.width;val h=size.height
        drawPath(Path().apply { moveTo(0f,h*0.8f);lineTo(w*0.4f,h*0.2f);lineTo(w,h*0.2f) }, Color.White, style=s)
        drawPath(Path().apply { moveTo(w*0.8f,h*0.05f);lineTo(w,h*0.2f);lineTo(w*0.8f,h*0.35f) }, Color.White, style=s)
        drawPath(Path().apply { moveTo(0f,h*0.2f);lineTo(w*0.4f,h*0.8f);lineTo(w,h*0.8f) }, Color.White, style=s)
        drawPath(Path().apply { moveTo(w*0.8f,h*0.65f);lineTo(w,h*0.8f);lineTo(w*0.8f,h*0.95f) }, Color.White, style=s)
    }
}
@Composable internal fun PrevIcon() {
    Canvas(Modifier.size(26.dp)) {
        val w=size.width;val h=size.height
        drawPath(Path().apply { moveTo(w*0.75f,h*0.15f);lineTo(w*0.25f,h*0.5f);lineTo(w*0.75f,h*0.85f);close() }, Color.White)
        drawRoundRect(Color.White, topLeft=Offset(w*0.18f,h*0.15f), size=Size(w*0.12f,h*0.7f), cornerRadius=CornerRadius(3f))
    }
}
@Composable internal fun NextIcon(size: androidx.compose.ui.unit.Dp = 26.dp) {
    Canvas(Modifier.size(size)) {
        val w=this.size.width;val h=this.size.height
        drawPath(Path().apply { moveTo(w*0.25f,h*0.15f);lineTo(w*0.75f,h*0.5f);lineTo(w*0.25f,h*0.85f);close() }, Color.White)
        drawRoundRect(Color.White, topLeft=Offset(w*0.68f,h*0.15f), size=Size(w*0.12f,h*0.7f), cornerRadius=CornerRadius(3f))
    }
}
@Composable internal fun RepeatIcon() {
    Canvas(Modifier.size(20.dp)) {
        val s = androidx.compose.ui.graphics.drawscope.Stroke(width=2f, cap=StrokeCap.Round, join=androidx.compose.ui.graphics.StrokeJoin.Round)
        val w=size.width;val h=size.height
        drawPath(Path().apply { moveTo(w*0.7f,h*0.05f);lineTo(w,h*0.25f);lineTo(w*0.7f,h*0.45f) }, Color.White, style=s)
        drawPath(Path().apply { moveTo(w*0.15f,h*0.45f);arcTo(androidx.compose.ui.geometry.Rect(0f,0f,w,h*0.5f),180f,-180f,false) }, Color.White, style=s)
        drawPath(Path().apply { moveTo(w*0.3f,h*0.95f);lineTo(0f,h*0.75f);lineTo(w*0.3f,h*0.55f) }, Color.White, style=s)
        drawPath(Path().apply { moveTo(w*0.85f,h*0.55f);arcTo(androidx.compose.ui.geometry.Rect(0f,h*0.5f,w,h),0f,-180f,false) }, Color.White, style=s)
    }
}
@Composable internal fun HomeIcon() {
    Canvas(Modifier.size(22.dp)) {
        val s = androidx.compose.ui.graphics.drawscope.Stroke(width=1.8f, cap=StrokeCap.Round, join=androidx.compose.ui.graphics.StrokeJoin.Round)
        val w=size.width;val h=size.height
        drawPath(Path().apply { moveTo(w*0.1f,h*0.5f);lineTo(w/2f,h*0.08f);lineTo(w*0.9f,h*0.5f) }, Color.White, style=s)
        drawPath(Path().apply { moveTo(w*0.2f,h*0.45f);lineTo(w*0.2f,h*0.92f);lineTo(w*0.8f,h*0.92f);lineTo(w*0.8f,h*0.45f) }, Color.White, style=s)
    }
}
@Composable internal fun ListIcon() {
    Canvas(Modifier.size(22.dp)) {
        val s = androidx.compose.ui.graphics.drawscope.Stroke(width=1.8f, cap=StrokeCap.Round)
        val w=size.width;val h=size.height
        drawLine(Color.White, Offset(w*0.35f,h*0.25f), Offset(w*0.9f,h*0.25f), strokeWidth=1.8f, cap=StrokeCap.Round)
        drawLine(Color.White, Offset(w*0.35f,h*0.5f), Offset(w*0.9f,h*0.5f), strokeWidth=1.8f, cap=StrokeCap.Round)
        drawLine(Color.White, Offset(w*0.35f,h*0.75f), Offset(w*0.9f,h*0.75f), strokeWidth=1.8f, cap=StrokeCap.Round)
        drawCircle(Color.White, 1.5f, Offset(w*0.18f,h*0.25f))
        drawCircle(Color.White, 1.5f, Offset(w*0.18f,h*0.5f))
        drawCircle(Color.White, 1.5f, Offset(w*0.18f,h*0.75f))
    }
}
@Composable internal fun EqualizerIcon() {
    Canvas(Modifier.size(22.dp)) {
        val w=size.width;val h=size.height
        val strokeW = 1.8f
        drawLine(Color.White, Offset(w*0.22f,h*0.15f), Offset(w*0.22f,h*0.85f), strokeWidth=strokeW, cap=StrokeCap.Round)
        drawLine(Color.White, Offset(w*0.44f,h*0.38f), Offset(w*0.44f,h*0.62f), strokeWidth=strokeW, cap=StrokeCap.Round)
        drawLine(Color.White, Offset(w*0.66f,h*0.22f), Offset(w*0.66f,h*0.78f), strokeWidth=strokeW, cap=StrokeCap.Round)
        drawLine(Color.White, Offset(w*0.88f,h*0.44f), Offset(w*0.88f,h*0.56f), strokeWidth=strokeW, cap=StrokeCap.Round)
    }
}
@Composable internal fun BellIcon() {
    Canvas(Modifier.size(22.dp)) {
        val s = androidx.compose.ui.graphics.drawscope.Stroke(width=1.8f, cap=StrokeCap.Round, join=androidx.compose.ui.graphics.StrokeJoin.Round)
        val w=size.width;val h=size.height
        drawPath(Path().apply {
            moveTo(w*0.5f,h*0.88f); cubicTo(w*0.35f,h*0.88f,w*0.22f,h*0.78f,w*0.12f,h*0.65f)
            lineTo(w*0.88f,h*0.65f); cubicTo(w*0.78f,h*0.78f,w*0.65f,h*0.88f,w*0.5f,h*0.88f)
        }, Color.White, style=s)
        drawPath(Path().apply {
            moveTo(w*0.5f,h*0.1f); cubicTo(w*0.25f,h*0.1f,w*0.12f,h*0.35f,w*0.12f,h*0.65f)
            lineTo(w*0.88f,h*0.65f); cubicTo(w*0.88f,h*0.35f,w*0.75f,h*0.1f,w*0.5f,h*0.1f)
        }, Color.White, style=s)
        drawPath(Path().apply { moveTo(w*0.42f,h*0.88f); cubicTo(w*0.42f,h*0.94f,w*0.58f,h*0.94f,w*0.58f,h*0.88f) }, Color.White, style=s)
    }
}
