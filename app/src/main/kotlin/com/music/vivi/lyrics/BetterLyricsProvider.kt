/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.lyrics

import android.content.Context
import com.music.vivi.betterlyrics.BetterLyrics
import com.music.vivi.constants.EnableBetterLyricsKey
import com.music.vivi.utils.dataStore
import com.music.vivi.utils.get

object BetterLyricsProvider : LyricsProvider {
    override val name = "BetterLyrics"
    // ISRC: 2 letters + 3 alphanumeric registrant chars + 7 digits.
    private val isrcRegex = Regex("^[A-Z]{2}[A-Z0-9]{3}\\d{7}$")

    override fun isEnabled(context: Context): Boolean = context.dataStore[EnableBetterLyricsKey] ?: true

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String?,
    ): Result<String> = BetterLyrics.getLyrics(title, artist, duration, album, extractIsrc(id))

    override suspend fun getAllLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String?,
        callback: (String) -> Unit,
    ) {
        BetterLyrics.getAllLyrics(title, artist, duration, album, extractIsrc(id), callback)
    }

    private fun extractIsrc(value: String): String? {
        val normalized = value.uppercase()
        return normalized.takeIf { isrcRegex.matches(it) }
    }
}
