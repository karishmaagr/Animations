package com.karishma.swiggyanimation.music

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun LibraryScreen(
    state: MusicPlayerState.Ready,
    onIntent: (MusicPlayerIntent) -> Unit,
) {
    val palette = state.currentPalette

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(palette.accentSoft.copy(alpha = 0.2f), Color.Transparent),
                    center = Offset(540f, 0f),
                    radius = 700f,
                )
            )
            .background(
                Brush.verticalGradient(listOf(Color(0xFF14101a), Color(0xFF0a0710), Color(0xFF050308)))
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            // Top bar
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { onIntent(MusicPlayerIntent.ShowNowPlaying) }) { ChevronDownIcon() }
                Spacer(Modifier.weight(1f))
                Box(modifier = Modifier.size(width = 36.dp, height = 4.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.35f)))
                Spacer(Modifier.weight(1f))
                IconButton(onClick = {}) { DotsIcon() }
            }

            // Section header
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "UP NEXT",
                        color = Color.White.copy(alpha = 0.45f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Tonight's Queue",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.3).sp,
                    )
                }
                Text(
                    "${state.tracks.size} tracks",
                    color = Color.White.copy(alpha = 0.45f),
                    fontSize = 12.sp,
                )
            }

            Spacer(Modifier.height(8.dp))

            // Track list
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 12.dp, end = 12.dp, bottom = 160.dp,
                ),
            ) {
                itemsIndexed(state.tracks, key = { _, t -> t.id }) { index, track ->
                    LibraryTrackRow(
                        track = track,
                        art = state.artCache[track.id],
                        isCurrent = index == state.currentIndex,
                        isLiked = state.isLiked(track.id),
                        accentColor = state.currentPalette.accent,
                        onClick = { onIntent(MusicPlayerIntent.SelectTrack(index)) },
                    )
                }
            }
        }

        // Floating mini player
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 82.dp),
        ) {
            MiniPlayer(
                track = state.currentTrack,
                art = state.artCache[state.currentTrack.id],
                isPlaying = state.isPlaying,
                accentColor = state.currentPalette.accent,
                onExpand = { onIntent(MusicPlayerIntent.ShowNowPlaying) },
                onTogglePlay = { onIntent(MusicPlayerIntent.PlayPause) },
                onNext = { onIntent(MusicPlayerIntent.Next) },
            )
        }

        // Bottom nav
        BottomNavPill(
            currentTrack = state.currentTrack,
            currentArt = state.artCache[state.currentTrack.id],
            accentColor = state.currentPalette.accent,
            activeTab = "lib",
            onLibraryTap = {},
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
        )
    }
}

@Composable
private fun LibraryTrackRow(
    track: Track,
    art: android.graphics.Bitmap?,
    isCurrent: Boolean,
    isLiked: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isCurrent) Color.White.copy(alpha = 0.06f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Art thumbnail
        Box(
            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFF2a1a3a)),
            contentAlignment = Alignment.Center,
        ) {
            if (art != null) {
                Canvas(Modifier.fillMaxSize()) {
                    val bmp = art.asImageBitmap()
                    val s = minOf(bmp.width, bmp.height)
                    drawImage(bmp,
                        srcOffset = IntOffset((bmp.width-s)/2,(bmp.height-s)/2),
                        srcSize = IntSize(s,s),
                        dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                        filterQuality = FilterQuality.Low)
                }
            }
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                color = if (isCurrent) Color.White else Color.White.copy(alpha = 0.92f),
                fontSize = 15.sp,
                fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = track.artist,
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 13.sp,
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
                color = Color.White.copy(alpha = 0.45f),
                fontSize = 12.sp,
            )
        }
    }
}
