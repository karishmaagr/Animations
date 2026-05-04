package com.karishma.swiggyanimation.music

internal sealed class MusicPlayerIntent {
    data object PlayPause : MusicPlayerIntent()
    data object Next : MusicPlayerIntent()
    data object Previous : MusicPlayerIntent()
    data class SelectTrack(val index: Int) : MusicPlayerIntent()
    data class SeekTo(val progress: Float) : MusicPlayerIntent()
    data object SeekStart : MusicPlayerIntent()
    data object SeekEnd : MusicPlayerIntent()
    data object ShowLibrary : MusicPlayerIntent()
    data object ShowNowPlaying : MusicPlayerIntent()
    data class ToggleLike(val trackId: Long) : MusicPlayerIntent()
}
