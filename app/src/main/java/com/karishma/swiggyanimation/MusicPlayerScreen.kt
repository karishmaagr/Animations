package com.karishma.swiggyanimation

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ============================================================
// DATA MODELS
// ============================================================

data class Track(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val duration: Long,
    val uri: Uri,
)

private data class TrackPalette(
    val bg: Color,
    val surface: Color,
    val accent: Color,
    val accentSoft: Color,
) {
    companion object {
        private val Fallbacks = listOf(
            TrackPalette(Color(0xFF0E0B1E), Color(0xFF1A1530), Color(0xFFB14EFF), Color(0xFF7B2FBE)),
            TrackPalette(Color(0xFF1A0018), Color(0xFF2D0030), Color(0xFFFF3D8A), Color(0xFFFF006E)),
            TrackPalette(Color(0xFF001219), Color(0xFF012A36), Color(0xFF00D4FF), Color(0xFF00B4D8)),
            TrackPalette(Color(0xFF0A1500), Color(0xFF162700), Color(0xFFA8E312), Color(0xFF80B918)),
            TrackPalette(Color(0xFF1A0A00), Color(0xFF2E1500), Color(0xFFFFB84D), Color(0xFFFF8500)),
            TrackPalette(Color(0xFF000B1A), Color(0xFF001833), Color(0xFF4D9DFF), Color(0xFF1E66D6)),
        )

        fun forIndex(index: Int) = Fallbacks[((index % Fallbacks.size) + Fallbacks.size) % Fallbacks.size]
    }
}

// ============================================================
// LOADERS
// ============================================================

private suspend fun loadTracksFromDevice(context: Context): List<Track> = withContext(Dispatchers.IO) {
    val projection = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.ALBUM,
        MediaStore.Audio.Media.ALBUM_ID,
        MediaStore.Audio.Media.DURATION,
    )
    val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} > 30000"
    val tracks = mutableListOf<Track>()
    runCatching {
        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            "${MediaStore.Audio.Media.TITLE} ASC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                tracks.add(
                    Track(
                        id = id,
                        title = cursor.getString(titleCol) ?: "Unknown",
                        artist = cursor.getString(artistCol) ?: "Unknown Artist",
                        album = cursor.getString(albumCol) ?: "Unknown Album",
                        albumId = cursor.getLong(albumIdCol),
                        duration = cursor.getLong(durationCol),
                        uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id),
                    )
                )
            }
        }
    }
    tracks
}

private suspend fun loadAlbumArt(context: Context, albumId: Long): Bitmap? = withContext(Dispatchers.IO) {
    val artUri = ContentUris.withAppendedId(
        Uri.parse("content://media/external/audio/albumart"),
        albumId
    )
    runCatching {
        context.contentResolver.openInputStream(artUri)?.use {
            BitmapFactory.decodeStream(it)
        }
    }.getOrNull()
}

private suspend fun extractTrackPalette(bitmap: Bitmap, fallback: TrackPalette): TrackPalette =
    withContext(Dispatchers.Default) {
        val palette = Palette.from(bitmap).maximumColorCount(12).generate()
        val vibrant = palette.vibrantSwatch ?: palette.lightVibrantSwatch
        val darkVibrant = palette.darkVibrantSwatch ?: palette.darkMutedSwatch
        val dominant = palette.dominantSwatch
        val accentSrc = vibrant ?: dominant
        val bgSrc = darkVibrant ?: dominant
        val accent = accentSrc?.rgb?.let(::Color) ?: fallback.accent
        val bgBase = bgSrc?.rgb?.let(::Color) ?: fallback.bg
        TrackPalette(
            bg = darken(bgBase, 0.55f),
            surface = darken(bgBase, 0.25f),
            accent = accent,
            accentSoft = darken(accent, 0.25f),
        )
    }

private fun darken(color: Color, factor: Float) = Color(
    red = (color.red * (1f - factor)).coerceIn(0f, 1f),
    green = (color.green * (1f - factor)).coerceIn(0f, 1f),
    blue = (color.blue * (1f - factor)).coerceIn(0f, 1f),
    alpha = color.alpha
)

private fun Long.toTimestamp(): String {
    val totalSec = (this.coerceAtLeast(0L)) / 1000
    return "%d:%02d".format(totalSec / 60, totalSec % 60)
}

// ============================================================
// SCREEN ENTRY
// ============================================================

@Composable
fun MusicPlayerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        )
    }
    var tracks by remember { mutableStateOf<List<Track>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var permissionRequested by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        permissionRequested = true
    }

    LaunchedEffect(Unit) {
        if (!hasPermission && !permissionRequested) {
            launcher.launch(permission)
        }
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission && tracks.isEmpty()) {
            isLoading = true
            tracks = loadTracksFromDevice(context)
            isLoading = false
        }
    }

    when {
        !hasPermission -> PermissionScreen(
            onRequest = { launcher.launch(permission) },
            onBack = onBack
        )
        isLoading -> LoadingScreen()
        tracks.isEmpty() -> EmptyTracksScreen(onBack = onBack)
        else -> PlayerContent(tracks = tracks, onBack = onBack)
    }
}

// ============================================================
// MAIN PLAYER
// ============================================================

@Composable
private fun PlayerContent(tracks: List<Track>, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var currentIndex by remember { mutableStateOf(0) }
    var isPlaying by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    var isSeeking by remember { mutableStateOf(false) }

    val albumArtCache = remember { mutableStateMapOf<Long, ImageBitmap?>() }
    val paletteCache = remember { mutableStateMapOf<Long, TrackPalette>() }

    val currentTrack = tracks[currentIndex]
    val currentPalette = paletteCache[currentTrack.albumId] ?: TrackPalette.forIndex(currentIndex)

    // Load art + palette for current and prefetch neighbors
    LaunchedEffect(currentIndex, tracks) {
        listOf(currentIndex, (currentIndex + 1) % tracks.size, (currentIndex - 1 + tracks.size) % tracks.size)
            .distinct()
            .forEach { idx ->
                val track = tracks[idx]
                if (!albumArtCache.containsKey(track.albumId)) {
                    val bitmap = loadAlbumArt(context, track.albumId)
                    albumArtCache[track.albumId] = bitmap?.asImageBitmap()
                    if (bitmap != null) {
                        paletteCache[track.albumId] =
                            extractTrackPalette(bitmap, TrackPalette.forIndex(idx))
                    } else {
                        paletteCache[track.albumId] = TrackPalette.forIndex(idx)
                    }
                }
            }
    }

    // ExoPlayer
    val exoPlayer = remember { ExoPlayer.Builder(context).build() }
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) {
                    currentIndex = (currentIndex + 1) % tracks.size
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    LaunchedEffect(currentIndex) {
        exoPlayer.setMediaItem(MediaItem.fromUri(tracks[currentIndex].uri))
        exoPlayer.prepare()
        exoPlayer.play()
    }

    // Progress polling
    LaunchedEffect(isPlaying, currentIndex) {
        while (isActive) {
            if (isPlaying && !isSeeking) {
                val pos = exoPlayer.currentPosition.coerceAtLeast(0L)
                val dur = if (exoPlayer.duration > 0) exoPlayer.duration
                else tracks[currentIndex].duration.coerceAtLeast(1L)
                progress = (pos.toFloat() / dur.toFloat()).coerceIn(0f, 1f)
            }
            delay(180)
        }
    }

    // Vinyl rotation: spin up + continuous when playing, decelerate on pause
    val vinylAngle = remember { Animatable(0f) }
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            vinylAngle.animateTo(
                targetValue = vinylAngle.value + 50f,
                animationSpec = tween(700, easing = FastOutSlowInEasing)
            )
            while (isActive) {
                val normalized = vinylAngle.value % 360f
                vinylAngle.snapTo(normalized)
                vinylAngle.animateTo(
                    targetValue = normalized + 360f,
                    animationSpec = tween(3200, easing = LinearEasing)
                )
            }
        } else {
            vinylAngle.animateTo(
                targetValue = vinylAngle.value + 90f,
                animationSpec = tween(1600, easing = CubicBezierEasing(0f, 0f, 0.2f, 1f))
            )
        }
    }

    // Needle arm
    val needleAngle = remember { Animatable(-32f) }
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            needleAngle.animateTo(
                targetValue = -6f,
                animationSpec = spring(
                    dampingRatio = 0.55f,
                    stiffness = Spring.StiffnessMedium
                )
            )
        } else {
            needleAngle.animateTo(
                targetValue = -32f,
                animationSpec = spring(
                    dampingRatio = 0.85f,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
    }

    // Track flip animation between songs
    val flipRotation = remember { Animatable(0f) }
    var displayedIndex by remember { mutableStateOf(currentIndex) }
    LaunchedEffect(currentIndex) {
        if (displayedIndex != currentIndex) {
            flipRotation.animateTo(90f, tween(180, easing = FastOutLinearInEasing))
            displayedIndex = currentIndex
            flipRotation.snapTo(-90f)
            flipRotation.animateTo(0f, tween(280, easing = LinearOutSlowInEasing))
        }
    }

    // Animated palette colors
    val bgColor by animateColorAsState(
        targetValue = currentPalette.bg,
        animationSpec = tween(800),
        label = "bg"
    )
    val surfaceColor by animateColorAsState(
        targetValue = currentPalette.surface,
        animationSpec = tween(800),
        label = "surface"
    )
    val accentColor by animateColorAsState(
        targetValue = currentPalette.accent,
        animationSpec = tween(600),
        label = "accent"
    )
    val accentSoft by animateColorAsState(
        targetValue = currentPalette.accentSoft,
        animationSpec = tween(600),
        label = "accentSoft"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        bgColor,
                        darken(bgColor, 0.35f),
                        Color.Black
                    )
                )
            )
    ) {
        // Subtle radial accent glow at the top
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(accentSoft.copy(alpha = 0.35f), Color.Transparent),
                        center = Offset(540f, 200f),
                        radius = 900f
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            TopBar(onBack = onBack)

            Spacer(Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp),
                contentAlignment = Alignment.Center
            ) {
                val displayedTrack = tracks[displayedIndex]
                val displayedArt = albumArtCache[displayedTrack.albumId]

                VinylRecord(
                    albumArt = displayedArt,
                    rotationDegrees = vinylAngle.value,
                    flipRotationY = flipRotation.value,
                    accentColor = accentColor,
                    surfaceColor = surfaceColor,
                    modifier = Modifier
                        .size(280.dp)
                        .align(Alignment.Center)
                )

                NeedleArm(
                    rotationDegrees = needleAngle.value,
                    accentColor = accentColor,
                    modifier = Modifier
                        .size(width = 130.dp, height = 200.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = (-8).dp, y = (-4).dp)
                )
            }

            Spacer(Modifier.height(20.dp))

            AnimatedContent(
                targetState = displayedIndex,
                transitionSpec = {
                    (fadeIn(tween(280)) + slideInVertically { -16 }) togetherWith
                            fadeOut(tween(180))
                },
                label = "trackInfo"
            ) { idx ->
                val t = tracks[idx]
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = t.title,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = t.artist,
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            TrackProgressBar(
                progress = progress,
                duration = if (exoPlayer.duration > 0) exoPlayer.duration else tracks[currentIndex].duration,
                accentColor = accentColor,
                onSeekStart = { isSeeking = true },
                onSeek = { p ->
                    progress = p
                    val dur = if (exoPlayer.duration > 0) exoPlayer.duration
                    else tracks[currentIndex].duration
                    exoPlayer.seekTo((p * dur).toLong())
                },
                onSeekEnd = { isSeeking = false },
                modifier = Modifier.padding(horizontal = 32.dp)
            )

            Spacer(Modifier.height(20.dp))

            PlayerControls(
                isPlaying = isPlaying,
                accentColor = accentColor,
                onPrev = {
                    currentIndex = (currentIndex - 1 + tracks.size) % tracks.size
                },
                onPlayPause = {
                    if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                },
                onNext = {
                    currentIndex = (currentIndex + 1) % tracks.size
                },
            )

            Spacer(Modifier.height(20.dp))

            TrackList(
                tracks = tracks,
                currentIndex = currentIndex,
                albumArtCache = albumArtCache,
                accentColor = accentColor,
                surfaceColor = surfaceColor,
                onTrackSelect = { idx -> currentIndex = idx },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        }
    }
}

// ============================================================
// TOP BAR
// ============================================================

@Composable
private fun TopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.10f))
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.size(18.dp)) {
                val path = Path().apply {
                    moveTo(size.width * 0.7f, 0f)
                    lineTo(size.width * 0.25f, size.height * 0.5f)
                    lineTo(size.width * 0.7f, size.height)
                }
                drawPath(
                    path = path,
                    color = Color.White,
                    style = Stroke(width = 2.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
        }
        Spacer(Modifier.weight(1f))
        Text(
            text = "NOW PLAYING",
            color = Color.White.copy(alpha = 0.55f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.5.sp,
        )
        Spacer(Modifier.weight(1f))
        Spacer(Modifier.size(40.dp))
    }
}

// ============================================================
// VINYL RECORD
// ============================================================

@Composable
private fun VinylRecord(
    albumArt: ImageBitmap?,
    rotationDegrees: Float,
    flipRotationY: Float,
    accentColor: Color,
    surfaceColor: Color,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    Canvas(
        modifier = modifier.graphicsLayer {
            rotationY = flipRotationY
            cameraDistance = 14f * density.density
        }
    ) {
        val radius = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)

        // Soft outer shadow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.Black.copy(alpha = 0.5f), Color.Transparent),
                center = center,
                radius = radius * 1.05f
            ),
            radius = radius * 1.05f,
            center = center
        )

        rotate(rotationDegrees, center) {
            // Vinyl body
            drawCircle(color = Color(0xFF0A0A0A), radius = radius, center = center)

            // Concentric grooves
            val groovePaint = Color(0xFF1C1C1C)
            for (i in 0..24) {
                val r = radius * (0.36f + i * 0.025f)
                if (r < radius * 0.97f) {
                    drawCircle(
                        color = if (i % 2 == 0) groovePaint else Color(0xFF161616),
                        radius = r,
                        center = center,
                        style = Stroke(width = 0.7f)
                    )
                }
            }

            // Outer rim
            drawCircle(
                color = Color(0xFF2C2C2C),
                radius = radius,
                center = center,
                style = Stroke(width = 2.5f)
            )

            // Center label area
            val labelRadius = radius * 0.36f
            if (albumArt != null) {
                drawCircle(color = Color.Black, radius = labelRadius, center = center)
                val labelRect = Rect(
                    center.x - labelRadius,
                    center.y - labelRadius,
                    center.x + labelRadius,
                    center.y + labelRadius
                )
                val clipPath = Path().apply { addOval(labelRect) }
                withTransform({ clipPath(clipPath, ClipOp.Intersect) }) {
                    val bw = albumArt.width
                    val bh = albumArt.height
                    val bmpAspect = bw.toFloat() / bh.toFloat()
                    val targetAspect = 1f
                    val (srcW, srcH) = if (bmpAspect > targetAspect) {
                        (bh * targetAspect).toInt() to bh
                    } else {
                        bw to (bw / targetAspect).toInt()
                    }
                    val srcX = (bw - srcW) / 2
                    val srcY = (bh - srcH) / 2
                    drawImage(
                        image = albumArt,
                        srcOffset = IntOffset(srcX.coerceAtLeast(0), srcY.coerceAtLeast(0)),
                        srcSize = IntSize(
                            srcW.coerceAtMost(bw),
                            srcH.coerceAtMost(bh)
                        ),
                        dstOffset = IntOffset(
                            (center.x - labelRadius).toInt(),
                            (center.y - labelRadius).toInt()
                        ),
                        dstSize = IntSize(
                            (labelRadius * 2f).toInt(),
                            (labelRadius * 2f).toInt()
                        ),
                        filterQuality = FilterQuality.Medium
                    )
                }
            } else {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(accentColor, surfaceColor, Color(0xFF111111)),
                        center = center,
                        radius = labelRadius
                    ),
                    radius = labelRadius,
                    center = center,
                )
            }

            // Label inner ring + outer ring
            drawCircle(
                color = Color(0xFF333333),
                radius = labelRadius,
                center = center,
                style = Stroke(width = 1.5f)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.12f),
                radius = labelRadius * 0.96f,
                center = center,
                style = Stroke(width = 0.8f)
            )

            // Spindle
            drawCircle(color = Color(0xFF050505), radius = 9f, center = center)
            drawCircle(
                color = Color(0xFF505050),
                radius = 9f,
                center = center,
                style = Stroke(width = 1f)
            )
            drawCircle(color = Color(0xFF0A0A0A), radius = 4f, center = center)
        }

        // Static sheen overlay (does not rotate, sits on top)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.10f),
                    Color.White.copy(alpha = 0.02f),
                    Color.Transparent
                ),
                center = Offset(center.x - radius * 0.35f, center.y - radius * 0.35f),
                radius = radius * 0.85f
            ),
            radius = radius,
            center = center,
        )
    }
}

// ============================================================
// NEEDLE ARM
// ============================================================

@Composable
private fun NeedleArm(
    rotationDegrees: Float,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val pivotX = size.width * 0.85f
        val pivotY = size.height * 0.12f
        val tipLengthY = size.height * 0.78f

        rotate(rotationDegrees, Offset(pivotX, pivotY)) {
            // Arm shadow
            drawLine(
                color = Color.Black.copy(alpha = 0.4f),
                start = Offset(pivotX + 2f, pivotY + 2f),
                end = Offset(pivotX - 18f + 2f, tipLengthY + 2f),
                strokeWidth = 7f,
                cap = StrokeCap.Round
            )
            // Arm body (silver gradient)
            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFFE0E0E0), Color(0xFF9E9E9E), Color(0xFFE0E0E0)),
                    start = Offset(pivotX, pivotY),
                    end = Offset(pivotX - 18f, tipLengthY)
                ),
                start = Offset(pivotX, pivotY),
                end = Offset(pivotX - 18f, tipLengthY),
                strokeWidth = 6f,
                cap = StrokeCap.Round
            )
            // Cartridge head (small block at tip)
            val tipX = pivotX - 18f
            val tipY = tipLengthY
            drawCircle(
                color = Color(0xFF2A2A2A),
                radius = 11f,
                center = Offset(tipX, tipY)
            )
            drawCircle(
                color = accentColor,
                radius = 8f,
                center = Offset(tipX, tipY)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.5f),
                radius = 3f,
                center = Offset(tipX - 2f, tipY - 2f)
            )
            // Pivot base
            drawCircle(
                color = Color(0xFF3A3A3A),
                radius = 18f,
                center = Offset(pivotX, pivotY)
            )
            drawCircle(
                color = Color(0xFF8E8E8E),
                radius = 14f,
                center = Offset(pivotX, pivotY)
            )
            drawCircle(
                color = Color(0xFF2A2A2A),
                radius = 14f,
                center = Offset(pivotX, pivotY),
                style = Stroke(width = 1.5f)
            )
            drawCircle(
                color = Color(0xFFBDBDBD),
                radius = 5f,
                center = Offset(pivotX, pivotY)
            )
        }
    }
}

// ============================================================
// PROGRESS BAR
// ============================================================

@Composable
private fun TrackProgressBar(
    progress: Float,
    duration: Long,
    accentColor: Color,
    onSeekStart: () -> Unit,
    onSeek: (Float) -> Unit,
    onSeekEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    var barWidthPx by remember { mutableStateOf(1f) }
    val scratchAnim = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var dragX by remember { mutableStateOf(0f) }

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
                            val p = (offset.x / barWidthPx).coerceIn(0f, 1f)
                            onSeek(p)
                        },
                        onDragEnd = { onSeekEnd() },
                        onDragCancel = { onSeekEnd() },
                        onHorizontalDrag = { _, dragAmount ->
                            dragX = (dragX + dragAmount).coerceIn(0f, barWidthPx)
                            scope.launch {
                                scratchAnim.animateTo(
                                    (dragAmount * 0.4f).coerceIn(-12f, 12f),
                                    tween(60)
                                )
                                scratchAnim.animateTo(0f, tween(180))
                            }
                            onSeek((dragX / barWidthPx).coerceIn(0f, 1f))
                        }
                    )
                },
            contentAlignment = Alignment.CenterStart
        ) {
            Canvas(modifier = Modifier.fillMaxWidth().height(4.dp)) {
                barWidthPx = size.width
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.18f),
                    size = size,
                    cornerRadius = CornerRadius(2f, 2f)
                )
                drawRoundRect(
                    color = accentColor,
                    size = Size(size.width * progress.coerceIn(0f, 1f), size.height),
                    cornerRadius = CornerRadius(2f, 2f)
                )
            }
            // Thumb
            val thumbSize = 16.dp
            val thumbHalfPx = with(density) { (thumbSize / 2).toPx() }
            Box(
                modifier = Modifier
                    .size(thumbSize)
                    .offset {
                        IntOffset(
                            x = (progress.coerceIn(0f, 1f) * barWidthPx - thumbHalfPx).toInt(),
                            y = 0
                        )
                    }
                    .graphicsLayer { rotationZ = scratchAnim.value }
                    .clip(CircleShape)
                    .background(accentColor)
            )
        }

        Spacer(Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = (duration * progress.coerceIn(0f, 1f)).toLong().toTimestamp(),
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 11.sp,
            )
            Text(
                text = duration.toTimestamp(),
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 11.sp,
            )
        }
    }
}

// ============================================================
// CONTROLS
// ============================================================

@Composable
private fun PlayerControls(
    isPlaying: Boolean,
    accentColor: Color,
    onPrev: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
) {
    val playPauseScale by animateFloatAsState(
        targetValue = if (isPlaying) 1.05f else 0.96f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessHigh),
        label = "playScale"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ControlButton(
            onClick = onPrev,
            size = 52.dp,
            background = Color.White.copy(alpha = 0.08f)
        ) {
            Canvas(Modifier.size(20.dp)) {
                val w = size.width
                val h = size.height
                drawPath(
                    Path().apply {
                        moveTo(w * 0.55f, h * 0.5f)
                        lineTo(w, h * 0.1f)
                        lineTo(w, h * 0.9f)
                        close()
                    }, color = Color.White
                )
                drawPath(
                    Path().apply {
                        moveTo(0f, h * 0.5f)
                        lineTo(w * 0.55f, h * 0.1f)
                        lineTo(w * 0.55f, h * 0.9f)
                        close()
                    }, color = Color.White
                )
            }
        }

        Box(
            modifier = Modifier
                .size(76.dp)
                .graphicsLayer { scaleX = playPauseScale; scaleY = playPauseScale }
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(accentColor, darken(accentColor, 0.25f)),
                        radius = 200f
                    )
                )
                .clickable(onClick = onPlayPause),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = isPlaying,
                transitionSpec = {
                    (fadeIn(tween(180)) togetherWith fadeOut(tween(180)))
                },
                label = "playIcon"
            ) { playing ->
                Canvas(Modifier.size(28.dp)) {
                    val w = size.width
                    val h = size.height
                    if (playing) {
                        drawRoundRect(
                            color = Color.White,
                            topLeft = Offset(w * 0.18f, h * 0.12f),
                            size = Size(w * 0.22f, h * 0.76f),
                            cornerRadius = CornerRadius(3f, 3f)
                        )
                        drawRoundRect(
                            color = Color.White,
                            topLeft = Offset(w * 0.60f, h * 0.12f),
                            size = Size(w * 0.22f, h * 0.76f),
                            cornerRadius = CornerRadius(3f, 3f)
                        )
                    } else {
                        drawPath(
                            Path().apply {
                                moveTo(w * 0.22f, h * 0.10f)
                                lineTo(w * 0.92f, h * 0.50f)
                                lineTo(w * 0.22f, h * 0.90f)
                                close()
                            }, color = Color.White
                        )
                    }
                }
            }
        }

        ControlButton(
            onClick = onNext,
            size = 52.dp,
            background = Color.White.copy(alpha = 0.08f)
        ) {
            Canvas(Modifier.size(20.dp)) {
                val w = size.width
                val h = size.height
                drawPath(
                    Path().apply {
                        moveTo(w * 0.45f, h * 0.5f)
                        lineTo(0f, h * 0.1f)
                        lineTo(0f, h * 0.9f)
                        close()
                    }, color = Color.White
                )
                drawPath(
                    Path().apply {
                        moveTo(w, h * 0.5f)
                        lineTo(w * 0.45f, h * 0.1f)
                        lineTo(w * 0.45f, h * 0.9f)
                        close()
                    }, color = Color.White
                )
            }
        }
    }
}

@Composable
private fun ControlButton(
    onClick: () -> Unit,
    size: Dp,
    background: Color,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(background)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

// ============================================================
// TRACK LIST
// ============================================================

@Composable
private fun TrackList(
    tracks: List<Track>,
    currentIndex: Int,
    albumArtCache: Map<Long, ImageBitmap?>,
    accentColor: Color,
    surfaceColor: Color,
    onTrackSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "UP NEXT",
                color = Color.White.copy(alpha = 0.45f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.5.sp,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "${tracks.size} songs",
                color = Color.White.copy(alpha = 0.35f),
                fontSize = 11.sp,
            )
        }
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            itemsIndexed(tracks, key = { _, t -> t.id }) { index, track ->
                val isCurrent = index == currentIndex
                var thumb by remember(track.albumId) {
                    mutableStateOf(albumArtCache[track.albumId])
                }
                LaunchedEffect(track.albumId) {
                    if (thumb == null && !albumArtCache.containsKey(track.albumId)) {
                        val bmp = loadAlbumArt(context, track.albumId)?.asImageBitmap()
                        thumb = bmp
                    } else {
                        thumb = albumArtCache[track.albumId]
                    }
                }
                TrackRow(
                    index = index,
                    track = track,
                    thumbnail = thumb ?: albumArtCache[track.albumId],
                    isCurrent = isCurrent,
                    accentColor = accentColor,
                    surfaceColor = surfaceColor,
                    onClick = { onTrackSelect(index) }
                )
            }
        }
    }
}

@Composable
private fun TrackRow(
    index: Int,
    track: Track,
    thumbnail: ImageBitmap?,
    isCurrent: Boolean,
    accentColor: Color,
    surfaceColor: Color,
    onClick: () -> Unit,
) {
    val rowBg by animateColorAsState(
        targetValue = if (isCurrent) accentColor.copy(alpha = 0.14f) else Color.Transparent,
        animationSpec = tween(300),
        label = "rowBg"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(surfaceColor),
            contentAlignment = Alignment.Center
        ) {
            if (thumbnail != null) {
                Canvas(Modifier.fillMaxSize()) {
                    val bw = thumbnail.width
                    val bh = thumbnail.height
                    val s = minOf(bw, bh)
                    drawImage(
                        image = thumbnail,
                        srcOffset = IntOffset((bw - s) / 2, (bh - s) / 2),
                        srcSize = IntSize(s, s),
                        dstOffset = IntOffset.Zero,
                        dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                        filterQuality = FilterQuality.Low
                    )
                }
            } else {
                val palette = TrackPalette.forIndex(index)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(listOf(palette.accent, palette.bg))
                        )
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                color = if (isCurrent) accentColor else Color.White,
                fontSize = 14.sp,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = track.artist,
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(Modifier.width(8.dp))

        if (isCurrent) {
            PlayingIndicator(color = accentColor)
        } else {
            Text(
                text = track.duration.toTimestamp(),
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun PlayingIndicator(color: Color) {
    val transition = rememberInfiniteTransition(label = "playingBars")
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.height(16.dp)
    ) {
        repeat(3) { i ->
            val h by transition.animateFloat(
                initialValue = 4f,
                targetValue = 14f,
                animationSpec = infiniteRepeatable(
                    animation = tween(420, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                    initialStartOffset = StartOffset(i * 140)
                ),
                label = "bar$i"
            )
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(h.dp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(color)
            )
        }
    }
}

// ============================================================
// PERMISSION / LOADING / EMPTY STATES
// ============================================================

@Composable
private fun PermissionScreen(onRequest: () -> Unit, onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF1A0A2E), Color(0xFF0E0B1E), Color.Black)
                )
            )
            .statusBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(40.dp)
        ) {
            val transition = rememberInfiniteTransition(label = "iconPulse")
            val pulse by transition.animateFloat(
                initialValue = 0.95f,
                targetValue = 1.05f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "pulse"
            )
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .graphicsLayer { scaleX = pulse; scaleY = pulse }
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color(0xFFB14EFF), Color(0xFF7B2FBE), Color.Transparent)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("🎵", fontSize = 56.sp)
            }
            Spacer(Modifier.height(28.dp))
            Text(
                "Connect to your music",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "We need access to your audio library to spin up your personal vinyl player.",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(36.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFFB14EFF), Color(0xFF7B2FBE))
                        )
                    )
                    .clickable(onClick = onRequest)
                    .padding(horizontal = 36.dp, vertical = 14.dp)
            ) {
                Text(
                    "Grant Access",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    letterSpacing = 0.8.sp
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "Go back",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 13.sp,
                modifier = Modifier
                    .clickable(onClick = onBack)
                    .padding(8.dp)
            )
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF1A0A2E), Color(0xFF0E0B1E), Color.Black)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val transition = rememberInfiniteTransition(label = "loading")
            val rotation by transition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1100, easing = LinearEasing)
                ),
                label = "spin"
            )
            Canvas(Modifier.size(64.dp)) {
                rotate(rotation) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(Color.Transparent, Color(0xFFB14EFF))
                        ),
                        startAngle = 0f,
                        sweepAngle = 280f,
                        useCenter = false,
                        style = Stroke(width = 5f, cap = StrokeCap.Round)
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            Text(
                "Loading your music...",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
private fun EmptyTracksScreen(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF1A0A2E), Color(0xFF0E0B1E), Color.Black)
                )
            )
            .statusBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(40.dp)
        ) {
            Text("📀", fontSize = 64.sp)
            Spacer(Modifier.height(24.dp))
            Text(
                "No music found",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Add some songs to your device, then come back to spin them up.",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(32.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White.copy(alpha = 0.12f))
                    .clickable(onClick = onBack)
                    .padding(horizontal = 32.dp, vertical = 14.dp)
            ) {
                Text("Go back", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}
