package com.example.tco2display

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tco2display.ui.Tco2ViewModel
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep screen awake
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Immersive fullscreen
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())

        setContent {
            val vm: Tco2ViewModel = viewModel()
            val tco2 by vm.tco2.collectAsState()

            val bg = Color(0xFF000000)
            val lit = Color(0xFFFFFFFF)
            val ghost = Color.White.copy(alpha = 0.10f)
            val green = Color(0xFF39D353)
            val segFont = FontFamily(Font(R.font.technology_bold, weight = FontWeight.Bold))

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bg)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Top banner
                TopLineWithBrand(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    color = lit.copy(alpha = 0.92f)
                )

                Spacer(Modifier.height(12.dp))

                // Big digits in a weighted center box (prevents overlap)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    FixedSizeSegFontWithGhost(
                        value = tco2,
                        fontFamily = segFont,
                        textColor = lit,
                        ghostColor = ghost,
                        lastDigitColor = green,
                        baseMinSp = 24f,
                        baseMaxSp = 2000f,
                        lastDigitScale = 1.22f,
                        letterSpacingSp = 0f
                    )
                }

                // Bottom meaning card
                WhatThisMeansCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    tco2 = tco2,
                    color = lit
                )
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            val controller = WindowInsetsControllerCompat(window, window.decorView)
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }
    }
}

/* ────────────────────────── Top banner ────────────────────────── */

@Composable
private fun TopLineWithBrand(modifier: Modifier, color: Color, lineThickness: Dp = 3.dp) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.weight(1f).height(lineThickness).background(color))
        val text = buildAnnotatedString {
            withStyle(SpanStyle(color = color, fontSize = 28.sp, fontWeight = FontWeight.Medium)) {
                append("  CO2 ")
            }
            withStyle(SpanStyle(color = color, fontSize = 28.sp, fontWeight = FontWeight.Bold)) {
                append("SAVINGS")
            }
            withStyle(SpanStyle(color = color, fontSize = 28.sp, fontWeight = FontWeight.Normal)) {
                append(" in Tons with ")
            }
            withStyle(SpanStyle(color = color, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)) {
                append("BLUE ENERGY MOTORS")
            }
            append("  ")
        }
        Text(text = text, textAlign = TextAlign.Center)
        Box(Modifier.weight(1f).height(lineThickness).background(color))
    }
}

/* ───────── Number with constant size + ghost (only for present digits) ───────── */

@Composable
private fun FixedSizeSegFontWithGhost(
    value: Double?,
    fontFamily: FontFamily,
    textColor: Color,
    ghostColor: Color,
    lastDigitColor: Color,
    baseMinSp: Float,
    baseMaxSp: Float,
    lastDigitScale: Float,
    letterSpacingSp: Float = 0f,
    fractionDigits: Int = 3
) {
    val measurer = rememberTextMeasurer()

    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val maxWidthPx = with(LocalDensity.current) { maxWidth.toPx() }.roundToInt()
        val constraints = Constraints(maxWidth = maxWidthPx)

        val display = if (value == null) "0.${"0".repeat(fractionDigits)}"
        else String.format(Locale.US, "%.${fractionDigits}f", value)

        val dot = display.indexOf('.')
        val rawInt = if (dot >= 0) display.substring(0, dot) else display
        val trimmedInt = rawInt.replaceFirst(Regex("^0+(?!$)"), "").ifEmpty { "0" }
        val curIntDigits = max(trimmedInt.length, 1)

        val frac = if (dot >= 0) display.substring(dot + 1).padEnd(fractionDigits, '0')
        else "0".repeat(fractionDigits)
        val firstTwo = frac.take(2)
        val last = frac.last().toString()

        fun template(baseSp: Float) = buildAnnotatedString {
            withStyle(SpanStyle(fontSize = baseSp.sp)) { append("8".repeat(curIntDigits)) }
            withStyle(SpanStyle(fontSize = baseSp.sp)) { append("."); append("88") }
            withStyle(SpanStyle(fontSize = (baseSp * lastDigitScale).sp)) { append("8") }
        }

        val baseSizeSp = remember(maxWidthPx, curIntDigits) {
            var low = baseMinSp
            var high = baseMaxSp
            var best = low
            repeat(14) {
                val mid = (low + high) / 2f
                val res = measurer.measure(
                    text = template(mid),
                    style = TextStyle(
                        fontFamily = fontFamily,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = letterSpacingSp.sp,
                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                        fontFeatureSettings = "tnum"
                    ),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip,
                    constraints = constraints
                )
                if (!res.didOverflowWidth) { best = mid; low = mid } else { high = mid }
            }
            best
        }

        val ghostText = buildAnnotatedString {
            withStyle(SpanStyle(fontSize = baseSizeSp.sp)) { append("8".repeat(curIntDigits)) }
            withStyle(SpanStyle(fontSize = baseSizeSp.sp)) { append("."); append("88") }
            withStyle(SpanStyle(fontSize = (baseSizeSp * lastDigitScale).sp)) { append("8") }
        }

        val actualText = buildAnnotatedString {
            withStyle(SpanStyle(color = textColor, fontSize = baseSizeSp.sp)) { append(trimmedInt) }
            withStyle(SpanStyle(color = textColor, fontSize = baseSizeSp.sp)) { append("."); append(firstTwo) }
            withStyle(SpanStyle(color = lastDigitColor, fontSize = (baseSizeSp * lastDigitScale).sp)) { append(last) }
        }

        Text(
            text = ghostText,
            color = ghostColor,
            maxLines = 1,
            softWrap = false,
            textAlign = TextAlign.Center,
            style = TextStyle(
                fontFamily = fontFamily,
                fontWeight = FontWeight.Bold,
                letterSpacing = letterSpacingSp.sp,
                platformStyle = PlatformTextStyle(includeFontPadding = false),
                fontFeatureSettings = "tnum"
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = actualText,
            color = textColor,
            maxLines = 1,
            softWrap = false,
            textAlign = TextAlign.Center,
            style = TextStyle(
                fontFamily = fontFamily,
                fontWeight = FontWeight.Bold,
                letterSpacing = letterSpacingSp.sp,
                platformStyle = PlatformTextStyle(includeFontPadding = false),
                fontFeatureSettings = "tnum"
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/* ───────────────────── Bottom meaning / trees ───────────────────── */

@Composable
private fun WhatThisMeansCard(modifier: Modifier, tco2: Double?, color: Color) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "What This Means",
            color = color.copy(alpha = 0.9f),
            fontSize = 22.sp,
            fontWeight = FontWeight.Medium
        )
        Text(text = "🌳", fontSize = 44.sp)

        // Midpoint trees: between 1–2 trees per ton (0.5–1.0 tCO2 per tree lifetime)
        val tons = tco2 ?: 0.0
        val minTrees = ceil(tons / 1.0).toInt()
        val maxTrees = ceil(tons / 0.5).toInt()
        val midTrees = ((minTrees + maxTrees) / 2.0).roundToInt()
        val nf = remember(midTrees) { NumberFormat.getIntegerInstance(Locale.US) }

        val line = buildAnnotatedString {
            withStyle(SpanStyle(color = color.copy(alpha = 0.9f), fontSize = 22.sp)) {
                append("Equivalent to ")
            }
            withStyle(SpanStyle(color = color, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)) {
                append(nf.format(midTrees))
            }
            withStyle(SpanStyle(color = color.copy(alpha = 0.9f), fontSize = 22.sp)) {
                append(" trees planted")
            }
        }
        Text(text = line, textAlign = TextAlign.Center)
    }
}
