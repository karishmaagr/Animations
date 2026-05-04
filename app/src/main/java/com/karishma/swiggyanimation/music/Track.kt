package com.karishma.swiggyanimation.music

data class Track(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val artworkUrl: String,
    val previewUrl: String,
    val duration: Long,
)