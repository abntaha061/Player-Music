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
    val vibrantColor: Color = Color(0xFF00ADB5)
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
        
        // Fallback procedural lyrics if no LRC file exists
        return listOf(
            LyricLine(0L, "• Real-time Playback of Local MP3 •"),
            LyricLine(2000L, "Title: $title"),
            LyricLine(5000L, "Artist: $artist"),
            LyricLine(8000L, "Album: $album"),
            LyricLine(11000L, "File: ${File(dataPath).name}"),
            LyricLine(14000L, "Enjoying local high-fidelity audio..."),
            LyricLine(20000L, "• End of Metadata Summary •")
        )
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

    // Direct directories to write and scan fallback sample files safely
    fun getSampleMusicDir(context: Context): File {
        return context.getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: context.filesDir
    }

    // Creates beautiful 20-second simulated local audio tracks if the storage is completely empty
    fun deploySampleLocalTracks(context: Context) {
        val musicDir = getSampleMusicDir(context)
        if (!musicDir.exists()) {
            musicDir.mkdirs()
        }

        val track1Sound = File(musicDir, "Midnight_Coffee.mp3")
        val track1Lrc = File(musicDir, "Midnight_Coffee.lrc")
        val track2Sound = File(musicDir, "Neon_Horizon.mp3")
        val track2Lrc = File(musicDir, "Neon_Horizon.lrc")

        if (!track1Sound.exists()) {
            writeProceduralWavFile(track1Sound, 220f) // Soft warm A3 bass wave
            track1Lrc.writeText(
                """
                [00:00.00]• Organic Coffeehouse Jazz Ambient •
                [00:02.50]Pouring a dynamic warm cup of coffee...
                [00:06.00]Raindrops rhythmically splashing against the window pane
                [00:10.50]Warm lights softly reflecting off wet pavement outside
                [00:15.00]This lrc file is parsed directly from local storage
                [00:19.00]Enjoying a peaceful late night melody
                [00:23.00]Keeping thoughts comfortable and cozy
                [00:27.00]• Coffee bean aroma fading out •
                """.trimIndent()
            )
        }

        if (!track2Sound.exists()) {
            writeProceduralWavFile(track2Sound, 330f) // Fluid bright E4 wave
            track2Lrc.writeText(
                """
                [00:00.00]• Neon Horizon Retro-wave Pad •
                [00:03.00]Starting the low-latency synthesizer engine
                [00:07.50]Sailing into an endless cyber sunset drive
                [00:12.00]Watching neon grid towers slide past
                [00:16.50]Real-time physical .mp3 player playback active
                [00:21.00]Perfect coordination with matched LRC files
                [00:25.00]Glow vectors shining in the rear mirror
                [00:29.00]• Sunset loop complete •
                """.trimIndent()
            )
        }
    }

    // Writes a valid, lightweight WAVE container containing synthetic audio
    private fun writeProceduralWavFile(file: File, frequency: Float) {
        val sampleRate = 11025
        val durationSeconds = 30
        val numSamples = sampleRate * durationSeconds
        val subChunk2Size = numSamples * 2 // 16-bit mono
        val chunkSize = 36 + subChunk2Size

        FileOutputStream(file).use { fos ->
            // RIFF Header
            fos.write("RIFF".toByteArray())
            fos.write(intToByteArray(chunkSize))
            fos.write("WAVE".toByteArray())

            // Format Chunk
            fos.write("fmt ".toByteArray())
            fos.write(intToByteArray(16)) // subchunk1Size
            fos.write(shortToByteArray(1)) // audioFormat: 1 = PCM
            fos.write(shortToByteArray(1)) // numChannels: 1 = Mono
            fos.write(intToByteArray(sampleRate))
            fos.write(intToByteArray(sampleRate * 2)) // byteRate
            fos.write(shortToByteArray(2)) // blockAlign
            fos.write(shortToByteArray(16)) // bitsPerSample

            // Data Chunk
            fos.write("data".toByteArray())
            fos.write(intToByteArray(subChunk2Size))

            // Procedural Sine Audio Generation
            val buffer = ByteBuffer.allocate(2048)
            buffer.order(ByteOrder.LITTLE_ENDIAN)

            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                val angle = 2.0 * Math.PI * frequency * t
                val sample = (Math.sin(angle) * 32767.0 * 0.4).toInt().toShort()
                
                if (!buffer.hasRemaining()) {
                    fos.write(buffer.array())
                    buffer.clear()
                }
                buffer.putShort(sample)
            }
            if (buffer.position() > 0) {
                fos.write(buffer.array(), 0, buffer.position())
            }
        }
    }

    private fun intToByteArray(value: Int): ByteArray {
        return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array()
    }

    private fun shortToByteArray(value: Short): ByteArray {
        return ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(value).array()
    }

    // Scans direct files as well as querying MediaStore for exhaustive results
    fun scanDeviceMusic(context: Context): List<Song> {
        val songsList = mutableListOf<Song>()
        
        // 1. Direct local files scan (reliable, bypasses media index delays on emulators)
        val sampleDir = getSampleMusicDir(context)
        if (sampleDir.exists()) {
            val audioFiles = sampleDir.listFiles { file ->
                val ext = file.extension.lowercase()
                ext == "mp3" || ext == "wav" || ext == "m4a" || ext == "ogg"
            }
            audioFiles?.forEach { file ->
                val title = file.nameWithoutExtension.replace('_', ' ')
                val artist = if (file.name.contains("Midnight")) "Jazz Ambience" else "Synthesizer Retro"
                val duration = 30 // Sample tracks are 30s
                
                // Color mapping: generate custom pleasant palettes for local tracks
                val colors = getColorsForText(title)

                songsList.add(
                    Song(
                        id = "local_file_${file.name.hashCode()}",
                        title = title,
                        artist = artist,
                        album = "Aurora Local",
                        durationSeconds = duration,
                        dataPath = file.absolutePath,
                        isSample = true,
                        dominantColor = colors.first,
                        vibrantColor = colors.second
                    )
                )
            }
        }

        // 2. MediaStore query (fetches user's actual local storage MP3 files)
        val resolver: ContentResolver = context.contentResolver
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

                        // Avoid adding duplicates that direct folder already scanned
                        if (songsList.none { it.dataPath == dataPath }) {
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
                                    vibrantColor = colors.second
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // MediaStore query failed, fall back safely
        }

        return songsList
    }

    // Dynamic, aesthetic color generation from title text to make sure every file looks fantastic
    fun getColorsForText(text: String): Pair<Color, Color> {
        val h = text.hashCode().coerceAtLeast(0)
        val hue1 = (h % 360).toFloat()
        val hue2 = ((h + 120) % 360).toFloat()
        
        // Return pleasant high-contrast colors
        return Pair(
            Color.hsl(hue1, 0.65f, 0.45f),
            Color.hsl(hue2, 0.85f, 0.55f)
        )
    }

    // Embedded Album Art extractor
    fun extractEmbeddedAlbumArt(dataPath: String): Bitmap? {
        val file = File(dataPath)
        if (!file.exists()) return null
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(dataPath)
            val artBytes = retriever.embeddedPicture
            if (artBytes != null) {
                BitmapFactory.decodeByteArray(artBytes, 0, artBytes.size)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
}
