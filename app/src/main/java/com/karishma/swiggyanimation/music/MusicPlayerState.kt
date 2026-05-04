package com.karishma.swiggyanimation.music

import android.graphics.Bitmap

internal sealed class MusicPlayerState {
    data object Loading : MusicPlayerState()
    data class Error(val message: String) : MusicPlayerState()
    data class Ready(
        val tracks: List<Track>,
        val currentIndex: Int = 0,
        val isPlaying: Boolean = false,
        val progress: Float = 0f,
        val isSeeking: Boolean = false,
        val currentDurationMs: Long = 0L,
        val artCache: Map<Long, Bitmap?> = emptyMap(),
        val paletteCache: Map<Long, TrackPalette> = emptyMap(),
        val showLibrary: Boolean = false,
        val likedTracks: Set<Long> = emptySet(),
        val swapDirection: Int = 1,
    ) : MusicPlayerState() {
        val currentTrack: Track get() = tracks[currentIndex]
        val currentPalette: TrackPalette get() = paletteCache[currentTrack.id] ?: TrackPalette.forIndex(currentIndex)
        fun isLiked(trackId: Long) = likedTracks.contains(trackId)
    }
}
