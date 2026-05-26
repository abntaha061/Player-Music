package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey val filePath: String,
    val title: String,
    val artist: String,
    val durationMs: Long,
    val fileSize: Long,
    val albumName: String = "LRC Album",
    val checksumMd5: String = "",
    val checksumSha1: String = "",
    val checksumSha256: String = "",
    var isFavorite: Boolean = false,
    var playCount: Int = 0,
    var lastPlayedTimestamp: Long = 0L,
    val hasLyrics: Boolean = false,
    val lyricsPath: String = "",
    val lyricsText: String = ""
)
