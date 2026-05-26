package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MusicPlayerViewModel
import com.example.ui.components.glassmorphic
import com.example.ui.theme.NeonAccent
import com.example.ui.theme.TextMuted

@Composable
fun SettingsScreen(viewModel: MusicPlayerViewModel) {
    val allSongs by viewModel.allSongs.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = "الإعدادات والنظام / Settings",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "تهيئة النظام وقراءة مجلد الموسيقى والـ LRC",
            color = TextMuted,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 2.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Standard Storage info box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassmorphic(cornerRadius = 16.dp, alpha = 0.08f)
                .padding(16.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Folder,
                        contentDescription = null,
                        tint = NeonAccent,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "مسار التخزين وقواعد الفحص:",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                BulletRuleItem("يبحث التطبيق تلقائياً في مسار الموسيقى بالنظام /Music/ عن كافة ملفات الصوت ليعمل Offline دون إنترنت.")
                BulletRuleItem("اقتران ذكي: يجب تطابق الاسم لملف الصوت والكلمات .lrc لربط الكلمات برمجياً وتزامنها (مثال: 3atma.mp3 يقابله 3atma.lrc).")
                BulletRuleItem("مسار التخزين النشط حالياً بالتطبيق:")
                Text(
                    text = "/storage/emulated/0/Music/\n+ App ExternalFilesDir",
                    color = NeonAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp, top = 2.dp, bottom = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Action Options
        Text(
            text = "إجراءات التخزين والمزامنة / System Actions:",
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        SettingsActionRow(
            title = "مسح الذاكرة الآن / Scan Memory",
            subtitle = "البحث عن مسارات .mp3 و .lrc جديدة",
            icon = Icons.Filled.Refresh,
            modifier = Modifier.testTag("action_scan_now")
        ) {
            viewModel.scanFiles()
        }

        Spacer(modifier = Modifier.height(10.dp))

        SettingsActionRow(
            title = "توليد ملفات ومسارات تجريبية / Seed Demo",
            subtitle = "توليد ٣ أغاني عربية سنثسايزر وكلمات .lrc متزامنة كاملة",
            icon = Icons.Filled.FolderZip,
            modifier = Modifier.testTag("action_seed_demo")
        ) {
            viewModel.seedDemoTracks()
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Credits / Specifications
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassmorphic(cornerRadius = 16.dp, alpha = 0.04f)
                .padding(16.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Icon(
                    imageVector = Icons.Filled.VerifiedUser,
                    contentDescription = null,
                    tint = NeonAccent.copy(alpha = 0.6f),
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Player Music - v1.0.0",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "نظام مشغل الموسيقى الزجاجي مع كلمات الـ LRC وفحص سلامة الملفات",
                    color = TextMuted,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "تم التطوير بالاعتماد الكامل على Jetpack Compose و Room Database و MediaPlayer.",
                    color = TextMuted.copy(alpha = 0.7f),
                    fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(80.dp)) // Avoid Mini Player overlap
    }
}

@Composable
fun BulletRuleItem(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(text = "• ", color = NeonAccent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text(text = text, color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
    }
}

@Composable
fun SettingsActionRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .glassmorphic(cornerRadius = 14.dp, alpha = 0.06f)
            .clickable(onClick = onClick)
            .padding(14.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.4f)
            )
        }
    }
}
