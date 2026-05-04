package com.karishma.swiggyanimation.music

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class MusicPlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow<MusicPlayerState>(MusicPlayerState.Loading)
    val state: StateFlow<MusicPlayerState> = _state.asStateFlow()

    private val exoPlayer = ExoPlayer.Builder(application).build()

    init {
        setupPlayerListener()
        startProgressPolling()
        loadTracks()
    }

    fun onIntent(intent: MusicPlayerIntent) {
        val ready = _state.value as? MusicPlayerState.Ready ?: return
        when (intent) {
            MusicPlayerIntent.PlayPause ->
                if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()

            MusicPlayerIntent.Next -> {
                val next = (ready.currentIndex + 1) % ready.tracks.size
                playAt(next, swapDir = 1)
            }

            MusicPlayerIntent.Previous -> {
                val prev = (ready.currentIndex - 1 + ready.tracks.size) % ready.tracks.size
                playAt(prev, swapDir = -1)
            }

            is MusicPlayerIntent.SelectTrack -> {
                val dir = if (intent.index > ready.currentIndex) 1 else -1
                playAt(intent.index, swapDir = dir)
            }

            is MusicPlayerIntent.SeekTo -> {
                val dur = ready.currentDurationMs.coerceAtLeast(1L)
                exoPlayer.seekTo((intent.progress * dur).toLong())
                _state.update { s -> (s as? MusicPlayerState.Ready)?.copy(progress = intent.progress) ?: s }
            }

            MusicPlayerIntent.SeekStart ->
                _state.update { s -> (s as? MusicPlayerState.Ready)?.copy(isSeeking = true) ?: s }

            MusicPlayerIntent.SeekEnd ->
                _state.update { s -> (s as? MusicPlayerState.Ready)?.copy(isSeeking = false) ?: s }

            MusicPlayerIntent.ShowLibrary ->
                _state.update { s -> (s as? MusicPlayerState.Ready)?.copy(showLibrary = true) ?: s }

            MusicPlayerIntent.ShowNowPlaying ->
                _state.update { s -> (s as? MusicPlayerState.Ready)?.copy(showLibrary = false) ?: s }

            is MusicPlayerIntent.ToggleLike -> {
                val current = ready.likedTracks
                val updated = if (current.contains(intent.trackId)) current - intent.trackId else current + intent.trackId
                _state.update { s -> (s as? MusicPlayerState.Ready)?.copy(likedTracks = updated) ?: s }
            }
        }
    }

    private fun playAt(index: Int, swapDir: Int) {
        val ready = _state.value as? MusicPlayerState.Ready ?: return
        _state.update { s ->
            (s as? MusicPlayerState.Ready)?.copy(
                currentIndex = index,
                progress = 0f,
                swapDirection = swapDir,
            ) ?: s
        }
        exoPlayer.setMediaItem(MediaItem.fromUri(ready.tracks[index].previewUrl))
        exoPlayer.prepare()
        exoPlayer.play()
        prefetchArtwork(index, ready.tracks)
    }

    private fun setupPlayerListener() {
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    _state.update { s ->
                        (s as? MusicPlayerState.Ready)?.copy(currentDurationMs = exoPlayer.duration.coerceAtLeast(1L)) ?: s
                    }
                } else if (playbackState == Player.STATE_ENDED) {
                    val ready = _state.value as? MusicPlayerState.Ready ?: return
                    playAt((ready.currentIndex + 1) % ready.tracks.size, swapDir = 1)
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _state.update { s -> (s as? MusicPlayerState.Ready)?.copy(isPlaying = isPlaying) ?: s }
            }
        })
    }

    private fun startProgressPolling() {
        viewModelScope.launch {
            while (isActive) {
                val ready = _state.value as? MusicPlayerState.Ready
                if (ready != null && ready.isPlaying && !ready.isSeeking) {
                    val pos = exoPlayer.currentPosition.coerceAtLeast(0L)
                    val dur = ready.currentDurationMs.coerceAtLeast(1L)
                    _state.update { s ->
                        (s as? MusicPlayerState.Ready)?.copy(progress = (pos.toFloat() / dur).coerceIn(0f, 1f)) ?: s
                    }
                }
                delay(180)
            }
        }
    }

    private fun loadTracks() {
        viewModelScope.launch {
            _state.value = MusicPlayerState.Loading
            val tracks = ItunesRepository.fetchTracks()
            if (tracks.isEmpty()) {
                _state.value = MusicPlayerState.Error("Could not load tracks. Check your internet connection.")
                return@launch
            }
            _state.value = MusicPlayerState.Ready(tracks = tracks)
            exoPlayer.setMediaItem(MediaItem.fromUri(tracks[0].previewUrl))
            exoPlayer.prepare()
            prefetchArtwork(0, tracks)
        }
    }

    private fun prefetchArtwork(centerIndex: Int, tracks: List<Track>) {
        val indices = listOf(
            centerIndex,
            (centerIndex + 1) % tracks.size,
            (centerIndex - 1 + tracks.size) % tracks.size,
        ).distinct()

        indices.forEach { idx ->
            val track = tracks[idx]
            val ready = _state.value as? MusicPlayerState.Ready ?: return@forEach
            if (ready.artCache.containsKey(track.id)) return@forEach
            viewModelScope.launch {
                val bitmap = ItunesRepository.loadArtwork(track.artworkUrl)
                val palette = if (bitmap != null) extractPalette(bitmap, TrackPalette.forIndex(idx))
                              else TrackPalette.forIndex(idx)
                _state.update { s ->
                    (s as? MusicPlayerState.Ready)?.copy(
                        artCache = s.artCache + (track.id to bitmap),
                        paletteCache = s.paletteCache + (track.id to palette),
                    ) ?: s
                }
            }
        }
    }

    private suspend fun extractPalette(bitmap: Bitmap, fallback: TrackPalette): TrackPalette =
        withContext(Dispatchers.Default) {
            val palette = Palette.from(bitmap).maximumColorCount(12).generate()
            val vibrant = palette.vibrantSwatch ?: palette.lightVibrantSwatch
            val darkVibrant = palette.darkVibrantSwatch ?: palette.darkMutedSwatch
            val dominant = palette.dominantSwatch
            val accent = (vibrant ?: dominant)?.rgb?.let { Color(it) } ?: fallback.accent
            val bgBase = (darkVibrant ?: dominant)?.rgb?.let { Color(it) } ?: fallback.bg
            TrackPalette(
                bg = darken(bgBase, 0.55f),
                surface = darken(bgBase, 0.25f),
                accent = accent,
                accentSoft = darken(accent, 0.25f),
            )
        }

    override fun onCleared() {
        exoPlayer.release()
        super.onCleared()
    }
}
