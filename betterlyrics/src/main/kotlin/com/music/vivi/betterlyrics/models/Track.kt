package com.music.vivi.betterlyrics.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SearchResponse(
    val total: Int? = null,
    val source: String? = null,
    val error: String? = null,
    val results: List<TrackResult> = emptyList(),
)

@Serializable
data class TrackResult(
    val id: String,
    @SerialName("track_name")
    val trackName: String,
    @SerialName("artist_name")
    val artistName: String,
    @SerialName("album_name")
    val albumName: String? = null,
    val duration: Int? = null,
    val isrc: String? = null,
    @SerialName("timing_type")
    val timingType: String,
    val lyricsUrl: String,
)
