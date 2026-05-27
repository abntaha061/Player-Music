package com.example.util

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.ui.graphics.Color
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class LyricLine(
    val timeMs: Long,
    val text: String
)

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationSeconds: Int,
    val dataPath: String,
    val isSample: Boolean = false,
    val dominantColor: Color = Color(0xFFBD83FF),
    val vibrantColor: Color = Color(0xFF00ADB5),
    val fileSizeMB: String = "8.0 MB"
) {
    // Lazy extraction of LRC lyrics beside the file
    fun getLyrics(): List<LyricLine> {
        val lrcPath = dataPath.substringBeforeLast(".") + ".lrc"
        val file = File(lrcPath)
        if (file.exists() && file.isFile) {
            return try {
                LrcParser.parse(file.readText())
            } catch (e: Exception) {
                emptyList()
            }
        }
        return emptyList()
    }
}

object LrcParser {
    fun parse(lrcContent: String): List<LyricLine> {
        val lines = lrcContent.lines()
        val parsed = mutableListOf<LyricLine>()
        // Pattern matches: [01:23.45], [01:23], [01:23.456]
        val regex = Regex("""\[(\d{2}):(\d{2})(?:\.(\d{1,3}))?\](.*)""")
        
        for (line in lines) {
            val trimmed = line.trim()
            val match = regex.find(trimmed)
            if (match != null) {
                val min = match.groupValues[1].toLong()
                val sec = match.groupValues[2].toLong()
                val msStr = match.groupValues[3]
                val text = match.groupValues[4].trim()
                
                val ms = if (msStr.isNotEmpty()) {
                    val rawMs = msStr.toLong()
                    when (msStr.length) {
                        1 -> rawMs * 100
                        2 -> rawMs * 10
                        else -> rawMs
                    }
                } else {
                    0L
                }
                
                val totalTimeMs = (min * 60 + sec) * 1000 + ms
                parsed.add(LyricLine(totalTimeMs, text))
            }
        }
        return parsed.sortedBy { it.timeMs }
    }
}

object LocalMusicScanner {

    // No-op method to ensure reference safety – we stop writing synthetic/mock tracks completely.
    fun deploySampleLocalTracks(context: Context) {
        // No-op: Completely stopped generating fake tracks to honor user intention
    }

    // Scans direct public music files as well as querying MediaStore for exhaustive results
    fun scanDeviceMusic(context: Context): List<Song> {
        val songsList = mutableListOf<Song>()
        val resolver: ContentResolver = context.contentResolver
        
        // 1. MediaStore query (fetches user's actual local storage MP3 / audio files)
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"

        try {
            val cursor = resolver.query(uri, projection, selection, null, null)
            cursor?.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dataCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

                while (c.moveToNext()) {
                    val id = c.getLong(idCol)
                    val title = c.getString(titleCol) ?: "Unknown Song"
                    val artist = c.getString(artistCol) ?: "Unknown Artist"
                    val album = c.getString(albumCol) ?: "Local Album"
                    val durationMs = c.getLong(durationCol)
                    val dataPath = c.getString(dataCol)
                    
                    if (dataPath != null && File(dataPath).exists()) {
                        val durationSec = (durationMs / 1000).toInt().coerceAtLeast(1)
                        val colors = getColorsForText(title)

                        if (songsList.none { it.dataPath == dataPath }) {
                            val sizeMB = computeFileSizeMB(dataPath)
                            songsList.add(
                                Song(
                                    id = id.toString(),
                                    title = title,
                                    artist = artist,
                                    album = album,
                                    durationSeconds = durationSec,
                                    dataPath = dataPath,
                                    isSample = false,
                                    dominantColor = colors.first,
                                    vibrantColor = colors.second,
                                    fileSizeMB = sizeMB
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // MediaStore query failed
        }

        // 2. Direct scan of standard public Music directory as fallback/complementation
        // to discover real files immediately even if MediaStore hasn't indexed them yet.
        val publicMusicDirs = listOf(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
            File("/sdcard/Music"),
            File("/storage/emulated/0/Music")
        )

        for (dir in publicMusicDirs) {
            if (dir.exists() && dir.isDirectory) {
                scanDirRecursive(dir, songsList)
            }
        }

        return songsList
    }

    private data class SongMetadata(
        val durationSeconds: Int,
        val artist: String?,
        val album: String?
    )

    private fun getSongMetadata(dataPath: String): SongMetadata {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(dataPath)
            val durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMs = durStr?.toLongOrNull() ?: 0L
            val durationSec = (durationMs / 1000).toInt().coerceAtLeast(1)
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)
            val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
            SongMetadata(durationSec, artist, album)
        } catch (e: Exception) {
            SongMetadata(30, null, null)
        } finally {
            try { retriever.release() } catch (e: Exception) {}
        }
    }

    private fun computeFileSizeMB(dataPath: String): String {
        val file = File(dataPath)
        return if (file.exists() && file.isFile) {
            val bytes = file.length()
            if (bytes > 0) {
                String.format(java.util.Locale.US, "%.1f MB", bytes.toDouble() / (1024 * 1024))
            } else {
                "8.0 MB"
            }
        } else {
            "8.0 MB"
        }
    }

    private fun scanDirRecursive(dir: File, songsList: MutableList<Song>) {
        val files = dir.listFiles() ?: return
        for (file in files) {
            if (file.isDirectory) {
                scanDirRecursive(file, songsList)
            } else if (file.isFile) {
                val ext = file.extension.lowercase()
                if (ext == "mp3" || ext == "wav" || ext == "m4a" || ext == "ogg" || ext == "flac") {
                    val dataPath = file.absolutePath
                        if (songsList.none { it.dataPath == dataPath }) {
                            val meta = getSongMetadata(dataPath)
                            val title = file.nameWithoutExtension.replace('_', ' ')
                            val artist = meta.artist ?: "Unknown Artist"
                            val album = meta.album ?: "Local Album"
                            val colors = getColorsForText(title)
                            val sizeMB = computeFileSizeMB(dataPath)
                            
                            songsList.add(
                                Song(
                                    id = "file_${file.name.hashCode()}",
                                    title = title,
                                    artist = artist,
                                    album = album,
                                    durationSeconds = meta.durationSeconds,
                                    dataPath = dataPath,
                                    isSample = false,
                                    dominantColor = colors.first,
                                    vibrantColor = colors.second,
                                    fileSizeMB = sizeMB
                                )
                            )
                        }
                }
            }
        }
    }

    // Dynamic, aesthetic color generation from title text to make sure every file looks fantastic
    fun getColorsForText(text: String): Pair<Color, Color> {
        val h = text.hashCode().coerceAtLeast(0)
        val hue1 = (h % 360).toFloat()
        val hue2 = ((h + 120) % 360).toFloat()
        return Pair(
            Color.hsl(hue1, 0.65f, 0.45f),
            Color.hsl(hue2, 0.85f, 0.55f)
        )
    }

    // Dual-Source Album Art Extractor (uses both MetadataRetriever and Uri withAppendedId loaders)
    fun extractEmbeddedAlbumArt(context: Context, dataPath: String, songId: String?): Bitmap? {
        val file = File(dataPath)
        var bitmap: Bitmap? = null

        // 1. MediaMetadataRetriever Extraction
        if (file.exists() && file.isFile) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(dataPath)
                val artBytes = retriever.embeddedPicture
                if (artBytes != null) {
                    bitmap = BitmapFactory.decodeByteArray(artBytes, 0, artBytes.size)
                }
            } catch (e: Exception) {
                // Fallback
            } finally {
                try { retriever.release() } catch (e: Exception) {}
            }
        }

        if (bitmap != null) return bitmap

        // 2. MediaStore AlbumArt ContentUris Extraction
        if (songId != null) {
            try {
                val mediaId = songId.toLongOrNull()
                if (mediaId != null) {
                    val uri = Uri.parse("content://media/external/audio/media/$mediaId/albumart")
                    context.contentResolver.openInputStream(uri).use { stream ->
                        bitmap = BitmapFactory.decodeStream(stream)
                    }
                }
            } catch (e: Exception) {
                // Fallback
            }

            if (bitmap != null) return bitmap

            // Android Q loader fallback
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    val mediaId = songId.toLongOrNull()
                    if (mediaId != null) {
                        val trackUri = Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, songId)
                        bitmap = context.contentResolver.loadThumbnail(trackUri, android.util.Size(600, 600), null)
                    }
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }

        return bitmap
    }
}
