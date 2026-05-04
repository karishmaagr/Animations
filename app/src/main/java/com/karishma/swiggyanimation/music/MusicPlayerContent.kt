package com.karishma.swiggyanimation.music

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val SWIPE_THRESHOLD = 100f

@Composable
internal fun PlayerContent(
    state: MusicPlayerState.Ready,
    onIntent: (MusicPlayerIntent) -> Unit,
) {
    val accentColor by animateColorAsState(state.currentPalette.accent, tween(600), label = "accent")
    var dragOffset by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(Color(0xFF18101e), Color(0xFF0a0710), Color(0xFF050308)))
            ),
    ) {
        // Color-bleed tint from album palette at top
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(accentColor.copy(alpha = 0.33f), Color.Transparent),
                        center = Offset(540f, 0f),
                        radius = 720f,
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            NowPlayingTopBar(onShowLibrary = { onIntent(MusicPlayerIntent.ShowLibrary) })

            Spacer(Modifier.height(16.dp))

            // Track title + artist + heart
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    AnimatedContent(
                        targetState = state.currentIndex,
                        transitionSpec = { (fadeIn(tween(250)) + slideInVertically { -12 }) togetherWith fadeOut(tween(180)) },
                        label = "title",
                    ) { idx ->
                        Text(
                            text = state.tracks[idx].title,
                            color = Color.White,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    AnimatedContent(
                        targetState = state.currentIndex,
                        transitionSpec = { (fadeIn(tween(250)) + slideInVertically { -8 }) togetherWith fadeOut(tween(150)) },
                        label = "artist",
                    ) { idx ->
                        Text(
                            text = state.tracks[idx].artist,
                            color = Color.White.copy(alpha = 0.55f),
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                HeartButton(
                    liked = state.isLiked(state.currentTrack.id),
                    onClick = { onIntent(MusicPlayerIntent.ToggleLike(state.currentTrack.id)) },
                )
            }

            Spacer(Modifier.height(12.dp))

            // Disc carousel with swipe
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .pointerInput(state.tracks.size) {
                        detectHorizontalDragGestures(
                            onDragStart = { dragOffset = 0f },
                            onDragEnd = {
                                if (dragOffset < -SWIPE_THRESHOLD) onIntent(MusicPlayerIntent.Next)
                                else if (dragOffset > SWIPE_THRESHOLD) onIntent(MusicPlayerIntent.Previous)
                                dragOffset = 0f
                            },
                            onDragCancel = { dragOffset = 0f },
                            onHorizontalDrag = { _, d -> dragOffset = (dragOffset + d).coerceIn(-280f, 280f) },
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                val discSize = 300.dp
                val prevIdx = (state.currentIndex - 1 + state.tracks.size) % state.tracks.size
                val nextIdx = (state.currentIndex + 1) % state.tracks.size

                PeekDisc(
                    bitmap = state.artCache[state.tracks[prevIdx].id],
                    fromRight = false,
                    discSize = discSize,
                    modifier = Modifier.align(Alignment.CenterStart).offset(x = (-discSize * 0.27f)),
                )
                PeekDisc(
                    bitmap = state.artCache[state.tracks[nextIdx].id],
                    fromRight = true,
                    discSize = discSize,
                    modifier = Modifier.align(Alignment.CenterEnd).offset(x = (discSize * 0.27f)),
                )
                GlassDisc(
                    currentIndex = state.currentIndex,
                    artCache = state.artCache,
                    tracks = state.tracks,
                    swapDirection = state.swapDirection,
                    size = discSize,
                )
            }

            // Current time
            val duration = if (state.currentDurationMs > 0) state.currentDurationMs else state.currentTrack.duration
            Text(
                text = (duration * state.progress).toLong().toTimestamp(),
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 14.dp),
            )

            TransportControls(
                isPlaying = state.isPlaying,
                accentColor = accentColor,
                onPrev = { onIntent(MusicPlayerIntent.Previous) },
                onPlayPause = { onIntent(MusicPlayerIntent.PlayPause) },
                onNext = { onIntent(MusicPlayerIntent.Next) },
            )

            Spacer(Modifier.height(100.dp))
        }

        // Floating bottom nav pill
        BottomNavPill(
            currentTrack = state.currentTrack,
            currentArt = state.artCache[state.currentTrack.id],
            accentColor = accentColor,
            activeTab = "np",
            onLibraryTap = { onIntent(MusicPlayerIntent.ShowLibrary) },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
        )
    }
}
