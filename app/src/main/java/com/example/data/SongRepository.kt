package com.example.data

import android.content.Context
import android.media.MediaMetadataRetriever
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.example.util.MusicUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

class SongRepository(
    private val context: Context,
    private val songDao: SongDao
) {
    private val TAG = "SongRepository"

    val allSongs: Flow<List<SongEntity>> = songDao.getAllSongs()
    val favoriteSongs: Flow<List<SongEntity>> = songDao.getFavoriteSongs()
    val recentlyPlayed: Flow<List<SongEntity>> = songDao.getRecentlyPlayedFlow()
    val mostPlayed: Flow<List<SongEntity>> = songDao.getMostPlayedFlow()

    suspend fun insertSong(song: SongEntity) = songDao.insertSong(song)
    suspend fun updateSong(song: SongEntity) = songDao.updateSong(song)
    suspend fun incrementPlayCount(filePath: String) {
        val timestamp = System.currentTimeMillis()
        songDao.incrementPlayCount(filePath, timestamp)
    }
    suspend fun toggleFavorite(filePath: String, isFavorite: Boolean) {
        songDao.toggleFavorite(filePath, isFavorite)
    }

    suspend fun getSongByPath(path: String) = songDao.getSongByPath(path)

    /**
     * Scan the local environment using both:
     * 1. Android MediaStore content provider.
     * 2. Direct folder recursive scanning of system Music and app specific folders.
     */
    suspend fun scanLocalMusic(): Int = withContext(Dispatchers.IO) {
        Log.d(TAG, "Starting storage scanning...")
        val discoveredMap = mutableMapOf<String, SongEntity>()

        // 1. Scan standard system MediaStore
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.ALBUM
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"

        try {
            context.contentResolver.query(
                uri,
                projection,
                selection,
                null,
                null
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)

                while (cursor.moveToNext()) {
                    val filePath = cursor.getString(dataColumn) ?: continue
                    val file = File(filePath)
                    if (file.exists() && file.isFile) {
                        val title = cursor.getString(titleColumn) ?: file.nameWithoutExtension.replace('_', ' ')
                        val artist = cursor.getString(artistColumn) ?: "فنان غير معروف / Unknown Artist"
                        val durationMs = cursor.getLong(durationColumn)
                        val size = cursor.getLong(sizeColumn)
                        val albumName = cursor.getString(albumColumn) ?: "مكتبة الموسيقى"

                        // Search for sidecar LRC file next to file in primary parent directory!
                        val lrcFile = File(file.parentFile, "${file.nameWithoutExtension}.lrc")
                        var hasLyrics = false
                        var lyricsPath = ""
                        var lrcText = ""
                        if (lrcFile.exists() && lrcFile.isFile) {
                            hasLyrics = true
                            lyricsPath = lrcFile.absolutePath
                            lrcText = lrcFile.readText(Charsets.UTF_8).trim()
                        }

                        val checksums = MusicUtil.calculateChecksums(file)

                        val song = SongEntity(
                            filePath = filePath,
                            title = title,
                            artist = artist,
                            durationMs = if (durationMs > 0) durationMs else 180000L,
                            fileSize = size,
                            albumName = albumName,
                            checksumMd5 = checksums.first,
                            checksumSha1 = checksums.second,
                            checksumSha256 = checksums.third,
                            hasLyrics = hasLyrics,
                            lyricsPath = lyricsPath,
                            lyricsText = lrcText
                        )
                        discoveredMap[filePath] = song
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "MediaStore query failed: ${e.message}", e)
        }

        // 2. Direct recursive scanning of system Music folder & app specific path
        val systemMusicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
        val discoveredList = mutableListOf<SongEntity>()
        scanFolderRecursive(systemMusicDir, discoveredList)

        val appMusicDir = File(context.getExternalFilesDir(null) ?: context.filesDir, "Music")
        scanFolderRecursive(appMusicDir, discoveredList)

        // Merge results into Map to remove duplicate filepaths
        for (song in discoveredList) {
            if (!discoveredMap.containsKey(song.filePath)) {
                discoveredMap[song.filePath] = song
            }
        }

        val finalSongs = discoveredMap.values.toList()
        if (finalSongs.isNotEmpty()) {
            songDao.insertSongs(finalSongs)
            Log.d(TAG, "Successfully scanned and populated ${finalSongs.size} local source tracks inside database.")
        }

        return@withContext finalSongs.size
    }

    private fun getMediaFileMetadata(file: File): Triple<String, String, Long> {
        val retriever = MediaMetadataRetriever()
        var title: String? = null
        var artist: String? = null
        var durationMs = 180000L // default fallback
        try {
            retriever.setDataSource(file.absolutePath)
            title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            if (durationStr != null) {
                val d = durationStr.toLong()
                if (d > 0) {
                    durationMs = d
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read properties/tags of file ${file.name}: ${e.message}")
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                // ignore
            }
        }
        
        val finalTitle = if (title.isNullOrBlank()) file.nameWithoutExtension.replace('_', ' ') else title
        val finalArtist = if (artist.isNullOrBlank()) "فنان غير معروف / Unknown Artist" else artist
        return Triple(finalTitle, finalArtist, durationMs)
    }

    private fun scanFolderRecursive(folder: File, resultList: MutableList<SongEntity>) {
        if (!folder.exists() || !folder.isDirectory) return

        val files = folder.listFiles() ?: return
        for (file in files) {
            if (file.isDirectory) {
                scanFolderRecursive(file, resultList)
            } else if (file.isFile && (
                file.name.endsWith(".mp3", ignoreCase = true) || 
                file.name.endsWith(".wav", ignoreCase = true) ||
                file.name.endsWith(".m4a", ignoreCase = true)
            )) {
                val filePath = file.absolutePath
                val (title, artist, durationMs) = getMediaFileMetadata(file)
                val size = file.length()
                
                // Track down matching LRC lyric file: e.g. "SongName.lrc" instead of "SongName.mp3"
                val lrcFile = File(file.parentFile, "${file.nameWithoutExtension}.lrc")
                var hasLyrics = false
                var lyricsPath = ""
                var lrcText = ""

                if (lrcFile.exists() && lrcFile.isFile) {
                    hasLyrics = true
                    lyricsPath = lrcFile.absolutePath
                    lrcText = lrcFile.readText(Charsets.UTF_8).trim()
                }

                // Calculate real file integrity checksums (MD5, SHA-1, SHA-256) as required!
                val checksums = MusicUtil.calculateChecksums(file)

                val song = SongEntity(
                    filePath = filePath,
                    title = title,
                    artist = artist,
                    durationMs = durationMs,
                    fileSize = size,
                    albumName = "مكتبة الموسيقى",
                    checksumMd5 = checksums.first,
                    checksumSha1 = checksums.second,
                    checksumSha256 = checksums.third,
                    hasLyrics = hasLyrics,
                    lyricsPath = lyricsPath,
                    lyricsText = lrcText
                )
                resultList.add(song)
            }
        }
    }

    /**
     * Generate or re-generate high-quality Arabic Wave synthesised demo songs
     * with standard `.lrc` lyric sidecars matching their names.
     */
    suspend fun loadDemoMusic(): Int = withContext(Dispatchers.IO) {
        val appMusicDir = File(context.getExternalFilesDir(null) ?: context.filesDir, "Music")
        if (!appMusicDir.exists()) appMusicDir.mkdirs()

        val demoSongs = listOf(
            DemoTrackSpec(
                filename = "3ein_Se7reya.wav",
                title = "عين سحرية / 3ein Se7reya",
                artist = "ألوان النور / Colors of Light",
                album = "فلكلور كوني / Cosmic Folk",
                frequency = 329.63, // E4 arpeggio starting freq
                duration = 201,     // 03:21 (201s)
                lyrics = """
                    [00:00.00] عين سحرية - ألوان النور (3ein Se7reya)
                    [00:03.00] نرى الأفق البعيد يصحو بنور الفجر المشرق
                    [00:08.00] نسير معاً والدروب تروي لنا حكايات الصبر العميقة
                    [00:14.00] عين سحرية تبوح بالباطن المكتوم في حنايا الصدر
                    [00:20.00] والقلب ينبض شوقاً كأمواج مد غامر وطهر مطهر
                    [00:26.00] نجوم السماء تلمع بأمل غامر وجسر من الأحلام
                    [00:32.00] سنعثر يوماً على الأمان الباقي في حنايا هذا الدهر
                    [00:38.00] عين سحرية ترقب الغد المشرق بشغف وسلام وسكون
                    [01:00.00] الموسيقى تأخذنا إلى عالم جديد وجميل
                    [01:10.00] نبني الجسور ونكسر القيود لنسافر بحرية
                    [01:25.00] عين سحرية ترى العبر من خلف الأبواب المغلقة
                    [01:40.00] وندعو بالوفاء والمسرة لكل قلب يتوق للنور
                """.trimIndent()
            ),
            DemoTrackSpec(
                filename = "3atma.wav",
                title = "عتمة / 3atma",
                artist = "لحن الهدوء الممزوج / Soft Symphony",
                album = "صفاء الروح / Soul Serenity",
                frequency = 220.00, // A3 deep low arpeggio
                duration = 165,     // 02:45
                lyrics = """
                    [00:00.00] عتمة - لحن الهدوء الممزوج (3atma)
                    [00:03.00] في هدأة الليل نبحر بلا منبر وبلا شراع يحمينا
                    [00:09.00] نسامر العتمة الدامسة ونمحو مر الفراق والضياع الحزين
                    [00:15.00] ونوقد شمعة في الزوايا الدافئة لننهي فصول الوداع مريرة
                    [00:21.00] حلم تلو حلم يتسع كحلقات كون وساع يتجاوز الخيال
                    [00:27.00] عتمة الليل تنجلي تماماً حين تشرق شمس القاع وتشرق القلوب
                    [00:34.00] نبحث عن السلام في عتمة الليل السرية المريحة
                    [01:00.00] نغمات دافئة تتسلل كأشعة النور الذهبية
                    [01:15.00] الصمت ليس نهاية، بل تمهيد لبداية أروع بكثير
                """.trimIndent()
            ),
            DemoTrackSpec(
                filename = "Aml_Bokra.wav",
                title = "أمل بكرة / Aml Bokra",
                artist = "نبضات الكوكب / Planet Heartbeats",
                album = "تطلعات المستقبل / Futures",
                frequency = 261.63, // C4 arpeggio
                duration = 242,     // 04:02
                lyrics = """
                    [00:00.00] أمل بكرة - نبضات الكوكب (Aml Bokra)
                    [00:03.00] نزرع الأمنيات الغضة في كف الغد القريب والواعد
                    [00:09.00] غداً تضحك العيون وتبتسم شمس ساطعة لا تغيب أبداً
                    [00:15.00] أمل بكرة جسر متين نعبره بقلب قوي وعزيمة نجيب
                    [00:21.00] لا حزن يدوم في القلوب فالمستقبل رحيب وجميل ومرحب
                    [00:27.00] نرفع الأيدي نحو العلا ملبين نداء المجيب الخفي
                    [00:34.00] أمل بكرة يجمعنا من المشرق والمغرب السليب بالمودة
                    [00:40.00] نمضي قدماً لبناء عالم ملؤه السلام والعدل والأمان
                    [01:10.00] معاً نصنع المستحيل ونعزف أنشودة الأمل الخالدة
                """.trimIndent()
            )
        )

        val inserted = mutableListOf<SongEntity>()

        for (spec in demoSongs) {
            // Write audio file
            val wavFile = MusicUtil.generateSyntheticWav(context, spec.filename, spec.duration, spec.frequency)
            
            // Write matching LRC file as sibling in storage
            val lrcFile = File(wavFile.parentFile, "${wavFile.nameWithoutExtension}.lrc")
            lrcFile.writeText(spec.lyrics, Charsets.UTF_8)

            // Compute actual integrity check files (checksums!)
            val checksums = MusicUtil.calculateChecksums(wavFile)
            val lrcChecksums = MusicUtil.calculateStringChecksums(spec.lyrics)

            val song = SongEntity(
                filePath = wavFile.absolutePath,
                title = spec.title,
                artist = spec.artist,
                durationMs = spec.duration * 1000L,
                fileSize = wavFile.length(),
                albumName = spec.album,
                checksumMd5 = checksums.first,
                checksumSha1 = checksums.second,
                checksumSha256 = checksums.third,
                hasLyrics = true,
                lyricsPath = lrcFile.absolutePath,
                lyricsText = spec.lyrics
            )
            inserted.add(song)
        }

        songDao.insertSongs(inserted)
        Log.d(TAG, "Successfully seeded ${inserted.size} high-quality synthesised demo songs and synced LRC lyr files.")
        
        return@withContext inserted.size
    }

    /**
     * Clear all database data
     */
    suspend fun clearAll() = songDao.clearAll()
}

data class DemoTrackSpec(
    val filename: String,
    val title: String,
    val artist: String,
    val album: String,
    val frequency: Double,
    val duration: Int,
    val lyrics: String
)
