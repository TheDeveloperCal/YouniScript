package com.youniscript.app.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.youniscript.app.ui.theme.YouniColors

/** Presentation-only settings for a writing page. These values never alter page text. */
data class PageStyle(
    val id: String,
    val name: String,
    val paper: Color,
    val ink: Color,
    val mutedInk: Color,
    val bodyFont: FontFamily,
    val bodySizeSp: Int,
    val lineHeightSp: Int,
    val titleFont: FontFamily = FontFamily.Serif,
    val titleSizeSp: Int = 29,
    val headingSizeSp: Int = 26,
    val subheadingSizeSp: Int = 21,
    val horizontalMarginDp: Int = 24,
    val borderColor: Color = mutedInk.copy(alpha = 0.25f),
    val ruled: Boolean = false,
    val ornament: Boolean = false,
    val pageNumber: Boolean = false,
    val initialLetter: Boolean = false,
    val parchmentTreatment: Boolean = false,
    val border: Boolean = false,
    val runningHeader: String? = null,
)

object PageStyles {
    private val ivory = Color(0xFFFBF9F3)
    private val ink = Color(0xFF292820)
    private val parchment = Color(0xFFF0E4CA)
    private val oldInk = Color(0xFF382E21)

    val all = listOf(
        PageStyle("modern-paper", "Modern Paper", ivory, ink, Color(0xFF706F64), FontFamily.Serif, 18, 30),
        PageStyle("classic-book", "Classic Book", Color(0xFFF8F3E8), Color(0xFF322D25), Color(0xFF756B5D), FontFamily.Serif, 19, 32, horizontalMarginDp = 32, pageNumber = true, runningHeader = "A PERSONAL BOOK"),
        PageStyle("notebook", "Notebook", Color(0xFFFFFEFA), ink, Color(0xFF77766F), FontFamily.SansSerif, 17, 32, ruled = true),
        PageStyle("typewriter", "Typewriter", Color(0xFFF5F1E7), Color(0xFF393832), Color(0xFF77736A), FontFamily.Monospace, 16, 28, titleFont = FontFamily.Monospace, titleSizeSp = 24, headingSizeSp = 21),
        PageStyle("parchment", "Parchment", parchment, oldInk, Color(0xFF77664E), FontFamily.Serif, 18, 31, parchmentTreatment = true),
        PageStyle("ancient-manuscript", "Ancient Manuscript", Color(0xFFEFE0C0), Color(0xFF3E3021), Color(0xFF776046), FontFamily.Serif, 18, 32, horizontalMarginDp = 30, ornament = true, parchmentTreatment = true, border = true),
        PageStyle("medieval-manuscript", "Medieval Manuscript", Color(0xFFF1E2C4), Color(0xFF3B2D20), Color(0xFF745B41), FontFamily.Serif, 19, 33, headingSizeSp = 28, horizontalMarginDp = 30, ornament = true, initialLetter = true, parchmentTreatment = true),
        PageStyle("literary", "Literary", Color(0xFFFCFAF5), Color(0xFF292820), Color(0xFF706F64), FontFamily.Serif, 20, 34, horizontalMarginDp = 34, pageNumber = true),
        PageStyle("dark-journal", "Dark Journal", Color(0xFF252923), Color(0xFFE9E7DC), Color(0xFFB0B3A5), FontFamily.Serif, 18, 30),
    )

    val default = all.first()
    fun find(id: String?): PageStyle = all.firstOrNull { it.id == id } ?: default
}

/** Tiny live-rendered page sample shared by the style picker and Library previews. */
@Composable
fun PageStyleMiniature(style: PageStyle, modifier: Modifier = Modifier) {
    Box(
        modifier
            .background(style.paper, RoundedCornerShape(5.dp))
            .border(0.75.dp, style.borderColor, RoundedCornerShape(5.dp))
            .drawBehind {
                if (style.parchmentTreatment) {
                    drawRect(
                        Brush.radialGradient(
                            colors = listOf(Color.White.copy(alpha = 0.16f), style.paper.copy(alpha = 0f)),
                            center = Offset(size.width * 0.22f, size.height * 0.16f),
                            radius = size.maxDimension * 0.9f,
                        ),
                    )
                }
                if (style.ruled) {
                    var y = 25.dp.toPx()
                    while (y < size.height - 4.dp.toPx()) {
                        drawLine(style.mutedInk.copy(alpha = 0.19f), Offset(5.dp.toPx(), y), Offset(size.width - 5.dp.toPx(), y), 0.6.dp.toPx())
                        y += 13.dp.toPx()
                    }
                }
            }
            .padding(horizontal = 7.dp, vertical = 6.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            style.runningHeader?.let {
                Text(it, color = style.mutedInk, fontSize = 5.sp, letterSpacing = 0.4.sp, maxLines = 1)
            }
            if (style.ornament) {
                Text("—  ❧  —", color = style.mutedInk, fontFamily = FontFamily.Serif, fontSize = 7.sp)
            }
            Row(verticalAlignment = androidx.compose.ui.Alignment.Top) {
                if (style.initialLetter) {
                    Text("A", color = style.ink, fontFamily = FontFamily.Serif, fontSize = 19.sp, lineHeight = 19.sp)
                    Spacer(Modifier.width(2.dp))
                }
                Text(
                    "A Quiet Page",
                    color = style.ink,
                    fontFamily = style.titleFont,
                    fontSize = (style.titleSizeSp / 4).coerceIn(5, 8).sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 9.sp,
                    maxLines = 1,
                )
            }
            Text(
                "Words find their place here.",
                color = style.mutedInk,
                fontFamily = style.bodyFont,
                fontSize = 5.sp,
                lineHeight = 7.sp,
                maxLines = 2,
            )
            if (style.pageNumber) {
                Spacer(Modifier.weight(1f))
                Text("·  1  ·", Modifier.fillMaxWidth(), color = style.mutedInk, fontSize = 5.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
    }
}

@Composable
fun PageStylePickerDialog(
    selectedStyleId: String?,
    onSelect: (PageStyle) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp).widthIn(max = 480.dp),
            shape = RoundedCornerShape(20.dp),
            color = YouniColors.paper,
            tonalElevation = 2.dp,
        ) {
            Column(Modifier.padding(start = 18.dp, top = 18.dp, end = 18.dp, bottom = 12.dp)) {
                Text("Choose a page style", color = YouniColors.ink, fontFamily = FontFamily.Serif, fontSize = 23.sp)
                Text("A different look for the same words.", Modifier.padding(top = 3.dp, bottom = 14.dp), color = YouniColors.mutedInk, fontSize = 13.sp)
                LazyColumn(Modifier.heightIn(max = 560.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    items(PageStyles.all, key = PageStyle::id) { style ->
                        val selected = style.id == (selectedStyleId ?: PageStyles.default.id)
                        Row(
                            Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selected) YouniColors.selection else Color.Transparent)
                                .clickable { onSelect(style) }
                                .semantics { contentDescription = "${style.name} page style${if (selected) ", selected" else ""}" }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            PageStyleMiniature(style, Modifier.width(62.dp).height(78.dp))
                            Column(Modifier.weight(1f).padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                // Picker labels live on the warm dialog surface, not on the miniature page.
                                // In particular, Dark Journal's page ink is intentionally light.
                                Text(style.name, color = YouniColors.ink, fontFamily = FontFamily.Serif, fontSize = 16.sp)
                                Text("A Quiet Page", color = YouniColors.secondaryInk, fontFamily = style.bodyFont, fontSize = 12.sp)
                                if (style.ruled || style.ornament || style.pageNumber || style.initialLetter) {
                                    Text(
                                        when {
                                            style.initialLetter -> "Manuscript initial"
                                            style.ruled -> "Subtle notebook ruling"
                                            style.pageNumber -> "Book-like page detail"
                                            else -> "Restrained ornament"
                                        },
                                        color = YouniColors.mutedInk,
                                        fontSize = 10.sp,
                                    )
                                }
                            }
                            if (selected) Text("✓", Modifier.padding(horizontal = 5.dp), color = YouniColors.sage, fontSize = 18.sp)
                        }
                    }
                }
                androidx.compose.material3.TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("Close", color = YouniColors.sage)
                }
            }
        }
    }
}
