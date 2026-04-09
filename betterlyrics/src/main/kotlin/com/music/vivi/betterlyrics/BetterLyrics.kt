package com.music.vivi.betterlyrics

import com.music.vivi.betterlyrics.models.SearchResponse
import com.music.vivi.betterlyrics.models.TrackResult
import com.music.youlyplus.YouLyPlus
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object BetterLyrics {
    private val client by lazy {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(
                    Json {
                        isLenient = true
                        ignoreUnknownKeys = true
                    },
                )
            }

            install(HttpTimeout) {
                requestTimeoutMillis = 15000
                connectTimeoutMillis = 10000
                socketTimeoutMillis = 15000
            }

            defaultRequest {
                url("https://lyrics-api.binimum.org")
            }

            expectSuccess = false
        }
    }

    private suspend fun searchTrack(
        artist: String,
        title: String,
        duration: Int = -1,
        album: String? = null,
        isrc: String? = null,
    ): TrackResult? = runCatching {
        val response = client.get("/") {
            if (!isrc.isNullOrBlank()) {
                parameter("isrc", isrc)
            } else {
                parameter("track", title)
                parameter("artist", artist)
                if (duration > 0) {
                    parameter("duration", duration)
                }
                if (!album.isNullOrBlank()) {
                    parameter("album", album)
                }
            }
        }
        if (response.status != HttpStatusCode.OK) return@runCatching null
        response.body<SearchResponse>().results.firstOrNull()
    }.getOrNull()

    private suspend fun fetchTTML(url: String): String? = runCatching {
        val response = client.get(url)
        if (response.status == HttpStatusCode.OK) response.bodyAsText().takeIf { it.isNotBlank() } else null
    }.getOrNull()

    private fun parseTtmlToLrc(ttml: String): String? {
        val parsedLines = TTMLParser.parseTTML(ttml)
        return if (parsedLines.isEmpty()) null else TTMLParser.toLRC(parsedLines)
    }

    private suspend fun fetchWordLyricsFromLyricsPlus(
        title: String,
        artist: String,
        duration: Int,
        album: String?,
        isrc: String?,
    ): String? = YouLyPlus
        .getLyrics(title, artist, duration, album, isrc = isrc)
        .getOrNull()
        ?.takeIf { wordSyncRegex.containsMatchIn(it) }

    private fun TrackResult.isLineSync(): Boolean = timingType.equals("line", ignoreCase = true)

    private suspend fun resolveLyrics(
        title: String,
        artist: String,
        duration: Int,
        album: String?,
        isrc: String?,
    ): String? {
        val result = searchTrack(artist, title, duration, album, isrc) ?: return null
        val resolvedIsrc = result.isrc ?: isrc

        if (result.isLineSync()) {
            fetchWordLyricsFromLyricsPlus(title, artist, duration, album, resolvedIsrc)?.let { return it }
        }

        val lrcFromStorage = fetchTTML(result.lyricsUrl)?.let(::parseTtmlToLrc)
        if (!lrcFromStorage.isNullOrBlank()) return lrcFromStorage

        return fetchWordLyricsFromLyricsPlus(title, artist, duration, album, resolvedIsrc)
    }

    suspend fun getLyrics(
        title: String,
        artist: String,
        duration: Int,
        album: String? = null,
        isrc: String? = null,
    ) = runCatching {
        resolveLyrics(title, artist, duration, album, isrc)
            ?: throw IllegalStateException("Lyrics unavailable")
    }

    suspend fun getAllLyrics(
        title: String,
        artist: String,
        duration: Int,
        album: String? = null,
        isrc: String? = null,
        callback: (String) -> Unit,
    ) {
        getLyrics(title, artist, duration, album, isrc)
            .onSuccess { lrcString ->
                callback(lrcString)
            }
    }

    private val wordSyncRegex = Regex("<\\d{2}:\\d{2}\\.\\d{2,3}>")
}
