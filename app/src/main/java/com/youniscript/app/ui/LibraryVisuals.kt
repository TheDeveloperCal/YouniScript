package com.youniscript.app.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.produceState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class BookCoverTemplate(
    val id: String,
    val name: String,
    val paper: Color,
    val ink: Color,
    val accent: Color,
    val typeface: FontFamily = FontFamily.Serif,
    val ornament: String? = null,
)

/** Lightweight reusable visual foundation for future book-cover editing. */
object BookCoverTemplates {
    val all = listOf(
        BookCoverTemplate("minimal", "Minimal", Color(0xFFE9E6DC), Color(0xFF292820), Color(0xFF59664F), FontFamily.SansSerif),
        BookCoverTemplate("classic", "Classic", Color(0xFFF6F0E4), Color(0xFF342D24), Color(0xFF9A704F), ornament = "—  ❦  —"),
        BookCoverTemplate("parchment", "Parchment", Color(0xFFEEDFC0), Color(0xFF443321), Color(0xFF806547), ornament = "❧"),
        BookCoverTemplate("ancient", "Ancient", Color(0xFFE6D3AD), Color(0xFF3B2C20), Color(0xFF987348), ornament = "◈"),
        BookCoverTemplate("manuscript", "Manuscript", Color(0xFFE4DED0), Color(0xFF302C25), Color(0xFF70624F), ornament = "—  ❧  —"),
        BookCoverTemplate("literary", "Literary", Color(0xFFF9F7F0), Color(0xFF292820), Color(0xFF626A59), ornament = "✳"),
        BookCoverTemplate("dark", "Dark", Color(0xFF292D27), Color(0xFFECE9DD), Color(0xFF9BA785), ornament = "✦"),
        BookCoverTemplate("modern", "Modern", Color(0xFFD9E0D2), Color(0xFF293326), Color(0xFF59664F), FontFamily.SansSerif),
    )
}

@Composable
fun BookCover(
    title: String,
    subtitle: String? = null,
    author: String? = null,
    template: BookCoverTemplate = BookCoverTemplates.all.first(),
    modifier: Modifier = Modifier,
    imageUri: String? = null,
) {
    BoxWithConstraints(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .background(template.paper)
            .border(1.dp, template.accent.copy(alpha = 0.52f), RoundedCornerShape(8.dp))
            .semantics { contentDescription = buildString { append("Book cover: "); append(title); subtitle?.let { append(", $it") }; author?.let { append(", by $it") } } },
    ) {
        val scale = (maxHeight.value / 230f).coerceIn(0.5f, 1f)
        val coverImage = rememberCoverImage(imageUri)
        if (coverImage != null) {
            Image(coverImage.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(Modifier.fillMaxSize().background(template.paper.copy(alpha = 0.68f)))
        }
        Row(Modifier.fillMaxSize()) {
            Box(Modifier.width((7f * scale).dp).fillMaxHeight().background(template.accent.copy(alpha = 0.72f)))
            Column(
                Modifier.fillMaxSize().padding(horizontal = (13f * scale).dp, vertical = (15f * scale).dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("YOUNISCRIPT", color = template.accent, fontSize = (7f * scale).sp, letterSpacing = (1.3f * scale).sp, fontWeight = FontWeight.SemiBold)
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy((4f * scale).dp)) {
                    template.ornament?.let { Text(it, color = template.accent, fontFamily = FontFamily.Serif, fontSize = (13f * scale).sp) }
                    Text(title, color = template.ink, fontFamily = template.typeface, fontSize = (19f * scale).sp, lineHeight = (23f * scale).sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    subtitle?.takeIf(String::isNotBlank)?.let { Text(it, color = template.ink.copy(alpha = 0.75f), fontFamily = template.typeface, fontSize = (10f * scale).sp, lineHeight = (13f * scale).sp, maxLines = 2, overflow = TextOverflow.Ellipsis) }
                }
                author?.takeIf(String::isNotBlank)?.let { Text(it, color = template.ink.copy(alpha = 0.8f), fontSize = (9f * scale).sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun rememberCoverImage(uriString: String?): Bitmap? {
    val context = LocalContext.current
    val bitmap = produceState<Bitmap?>(initialValue = null, uriString) {
        value = if (uriString == null) null else withContext(Dispatchers.IO) {
            runCatching {
                val uri = Uri.parse(uriString)
                val resolver = context.contentResolver
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                var sample = 1
                while (bounds.outWidth / sample > 1200 || bounds.outHeight / sample > 1600) sample *= 2
                val options = BitmapFactory.Options().apply { inSampleSize = sample; inPreferredConfig = Bitmap.Config.RGB_565 }
                resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            }.getOrNull()
        }
    }
    return bitmap.value
}

@Composable
fun JournalCover(
    title: String,
    dateRange: String? = null,
    latestEntry: String? = null,
    paper: Color = Color(0xFFE6E8DE),
    ink: Color = Color(0xFF30362E),
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .clip(RoundedCornerShape(9.dp))
            .background(paper)
            .border(1.dp, ink.copy(alpha = 0.18f), RoundedCornerShape(9.dp))
            .semantics {
                contentDescription = buildString {
                    append("Journal: ")
                    append(title)
                    dateRange?.let { append(", $it") }
                    latestEntry?.let { append(", last entry "); append(it) }
                }
            },
    ) {
        Box(Modifier.width(10.dp).fillMaxHeight().background(ink.copy(alpha = 0.76f)))
        Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Text("JOURNAL", color = ink.copy(alpha = 0.68f), fontSize = 8.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.SemiBold)
            Text(title, color = ink, fontFamily = FontFamily.Serif, fontSize = 21.sp, lineHeight = 25.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                dateRange?.let { Text(it, color = ink.copy(alpha = 0.76f), fontSize = 10.sp, maxLines = 1) }
                latestEntry?.let { Text("Last entry · $it", color = ink.copy(alpha = 0.64f), fontSize = 9.sp, maxLines = 1) }
            }
        }
    }
}
