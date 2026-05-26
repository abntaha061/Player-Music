package com.example.util

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest
import kotlin.math.sin

data class LyricLine(
    val timestampMs: Long,
    val text: String,
    val translation: String = ""
)

object MusicUtil {
    private const val TAG = "MusicUtil"

    /**
     * Parse an LRC lyric format string into structured LyricLines.
     */
    fun parseLrc(lrcContent: String): List<LyricLine> {
        val lines = mutableListOf<LyricLine>()
        val regex = Regex("\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})](.*)")
        val regexSimple = Regex("\\[(\\d{2}):(\\d{2})](.*)")

        lrcContent.lineSequence().forEach { line ->
            val match = regex.find(line)
            if (match != null) {
                val min = match.groupValues[1].toLong()
                val sec = match.groupValues[2].toLong()
                val msStr = match.groupValues[3]
                val text = match.groupValues[4].trim()

                val ms = if (msStr.length == 2) {
                    msStr.toLong() * 10
                } else {
                    msStr.toLong()
                }

                val timestampMs = (min * 60 * 1000) + (sec * 1000) + ms
                lines.add(LyricLine(timestampMs, text))
            } else {
                val matchSimple = regexSimple.find(line)
                if (matchSimple != null) {
                    val min = matchSimple.groupValues[1].toLong()
                    val sec = matchSimple.groupValues[2].toLong()
                    val text = matchSimple.groupValues[3].trim()
                    val timestampMs = (min * 60 * 1000) + (sec * 1000)
                    lines.add(LyricLine(timestampMs, text))
                }
            }
        }

        return lines.sortedBy { it.timestampMs }
    }

    /**
     * Calculates MD5, SHA-1, and SHA-256 for a File.
     */
    fun calculateChecksums(file: File): Triple<String, String, String> {
        return try {
            val md5 = MessageDigest.getInstance("MD5")
            val sha1 = MessageDigest.getInstance("SHA-1")
            val sha256 = MessageDigest.getInstance("SHA-256")

            val buffer = ByteArray(8192)
            var read: Int
            file.inputStream().use { isStream ->
                while (isStream.read(buffer).also { read = it } > 0) {
                    md5.update(buffer, 0, read)
                    sha1.update(buffer, 0, read)
                    sha256.update(buffer, 0, read)
                }
            }

            Triple(
                md5.digest().joinToString("") { "%02x".format(it) },
                sha1.digest().joinToString("") { "%02x".format(it) },
                sha256.digest().joinToString("") { "%02x".format(it) }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating integrity checksums", e)
            Triple("Unknown MD5", "Unknown SHA-1", "Unknown SHA-256")
        }
    }

    /**
     * Calculates MD5, SHA-1, and SHA-256 for a String (like LRC cached text).
     */
    fun calculateStringChecksums(text: String): Triple<String, String, String> {
        return try {
            val bytes = text.toByteArray(Charsets.UTF_8)
            val md5 = MessageDigest.getInstance("MD5").digest(bytes)
            val sha1 = MessageDigest.getInstance("SHA-1").digest(bytes)
            val sha256 = MessageDigest.getInstance("SHA-256").digest(bytes)

            Triple(
                md5.joinToString("") { "%02x".format(it) },
                sha1.joinToString("") { "%02x".format(it) },
                sha256.joinToString("") { "%02x".format(it) }
            )
        } catch (e: Exception) {
            Triple("Unknown MD5", "Unknown SHA-1", "Unknown SHA-256")
        }
    }

    /**
     * Generate a fully valid synthesised .wav audio file.
     * Generates a pleasant arpeggio/musical sweep so it sounds like music!
     */
    fun generateSyntheticWav(context: Context, filename: String, durationSeconds: Int, baseFreq: Double): File {
        val dir = File(context.getExternalFilesDir(null) ?: context.filesDir, "Music")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, filename)

        // Wave settings: 16-bit mono 22050Hz (smaller, runs fast, fully playable)
        val sampleRate = 22050
        val numSamples = durationSeconds * sampleRate
        val dataSize = numSamples * 2 // 16-bit = 2 bytes per sample
        val totalSize = 36 + dataSize

        FileOutputStream(file).use { out ->
            // RIFF WAVE header
            out.write("RIFF".toByteArray())
            out.write(intToByteArray(totalSize), 0, 4)
            out.write("WAVE".toByteArray())

            // Format chunk (fmt)
            out.write("fmt ".toByteArray())
            out.write(intToByteArray(16), 0, 4) // subchunk size (16 for PCM)
            out.write(shortToByteArray(1), 0, 2) // AudioFormat: 1 = PCM uncompressed
            out.write(shortToByteArray(1), 0, 2) // NumChannels: 1 = Mono
            out.write(intToByteArray(sampleRate), 0, 4) // SampleRate
            out.write(intToByteArray(sampleRate * 2), 0, 4) // ByteRate (sampleRate * numChannels * bitsPerSample/8)
            out.write(shortToByteArray(2), 0, 2) // BlockAlign (numChannels * bitsPerSample/8)
            out.write(shortToByteArray(16), 0, 2) // BitsPerSample: 16-bit

            // Data chunk
            out.write("data".toByteArray())
            out.write(intToByteArray(dataSize), 0, 4)

            // Generate synthesizer arpeggio data
            val buffer = ShortArray(sampleRate) // Progressively write in chunks
            for (sec in 0 until durationSeconds) {
                // Change frequency over time to create an arpeggiated melodic soundscape
                val freqIndex = sec % 8
                val chordFreq = when (freqIndex) {
                    0 -> baseFreq          // Tonic
                    1 -> baseFreq * 1.25   // Major third
                    2 -> baseFreq * 1.5    // Perfect fifth
                    3 -> baseFreq * 1.875  // Major seventh
                    4 -> baseFreq * 2.0    // Octave
                    5 -> baseFreq * 1.5    // Fifth descending
                    6 -> baseFreq * 1.25   // Third descending
                    else -> baseFreq * 0.9 // Dominant lead-in
                }

                for (i in 0 until sampleRate) {
                    val t = (sec.toDouble() + i.toDouble() / sampleRate)
                    // Synthesize sound: base arpeggio + layered sub-octave + subtle frequency modulation
                    val value = sin(2.0 * Math.PI * chordFreq * t) +
                                0.3 * sin(2.0 * Math.PI * (chordFreq / 2.0) * t) +
                                0.2 * sin(2.0 * Math.PI * (chordFreq * 3.0) * t)
                    
                    // Simple envelope to stop clicks between notes (decay/attack effect)
                    val envelope = if (i < 2000) {
                        i / 2000.0 // attack
                    } else if (i > sampleRate - 3000) {
                        (sampleRate - i) / 3000.0 // decay
                    } else {
                        1.0
                    }

                    val normalizedVal = (value * 12000.0 * envelope).toInt().coerceIn(-32768, 32767)
                    buffer[i] = normalizedVal.toShort()
                }

                // Write ShortArray to Byte stream
                val byteBuffer = ByteArray(sampleRate * 2)
                for (i in 0 until sampleRate) {
                    val sample = buffer[i].toInt()
                    byteBuffer[i * 2] = (sample and 0xff).toByte()
                    byteBuffer[i * 2 + 1] = ((sample shr 8) and 0xff).toByte()
                }
                out.write(byteBuffer)
            }
        }
        return file
    }

    private fun intToByteArray(value: Int): ByteArray {
        return byteArrayOf(
            (value and 0xff).toByte(),
            ((value shr 8) and 0xff).toByte(),
            ((value shr 16) and 0xff).toByte(),
            ((value shr 24) and 0xff).toByte()
        )
    }

    private fun shortToByteArray(value: Short): ByteArray {
        return byteArrayOf(
            (value.toInt() and 0xff).toByte(),
            ((value.toInt() shr 8) and 0xff).toByte()
        )
    }
}
