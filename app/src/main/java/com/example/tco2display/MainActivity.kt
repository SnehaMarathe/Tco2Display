package com.example.tco2display

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tco2display.ui.Tco2ViewModel
import java.util.Locale
import kotlin.math.min

// Immersive fullscreen helpers
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Fullscreen (hide system bars)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())

        setContent {
            val vm: Tco2ViewModel = viewModel()
            val tco2 by vm.tco2.collectAsState()

            val bg = Color(0xFF000000)
            val segOn = Color(0xFFFFFFFF)       // white segments
            val segOff = Color(0x22FFFFFF)      // very dim off segments (optional)
            val lastDigitOn = Color(0xFF39D353) // green last decimal
            val accent = Color.White

            // layout: top title, big 7-segment, bottom caption
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bg)
                    .padding(12.dp)
            ) {
                // Top title with lines
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DividerLine(color = accent, thickness = 3.dp, modifier = Modifier.weight(1f))
                    Text(
                        "BLUE ENERGY MOTORS",
                        color = accent,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 12.dp),
                        textAlign = TextAlign.Center
                    )
                    DividerLine(color = accent, thickness = 3.dp, modifier = Modifier.weight(1f))
                }

                // Center: Seven-segment number
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                ) {
                    SevenSegmentNumber(
                        value = tco2,
                        integerDigits = 7,   // adjust if you want more/less integer columns
                        fractionDigits = 3,  // 3 decimals like your mock
                        segmentColor = segOn,
                        offSegmentColor = segOff,
                        lastDigitColor = lastDigitOn
                    )
                }

                // Bottom caption
                Text(
                    text = "CARBON SAVINGS tCO2",
                    color = accent,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )
            }
        }
    }
}

// Simple divider line used left/right of the title
@Composable
private fun DividerLine(
    color: Color,
    thickness: Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(thickness)
            .background(color)
    )
}

/* ────────────────────────────────────────────────────────────────────────── */
/* Seven-segment display                                                     */
/* ────────────────────────────────────────────────────────────────────────── */

@Composable
private fun SevenSegmentNumber(
    value: Double?,
    integerDigits: Int,
    fractionDigits: Int,
    segmentColor: Color,
    offSegmentColor: Color,
    lastDigitColor: Color,
    gapDp: Dp = 8.dp
) {
    // Build the string 0000000.000 when null; otherwise formatted with zero-padding
    val formatted = remember(value) {
        if (value == null) {
            "0".repeat(integerDigits) + "." + "0".repeat(fractionDigits)
        } else {
            val abs = kotlin.math.abs(value)
            val int = abs.toLong()
            val fracScaled = ((abs - int) * Math.pow(10.0, fractionDigits.toDouble()))
                .toLong()
                .coerceAtMost((Math.pow(10.0, fractionDigits.toDouble()) - 1).toLong())

            val intStr = int.toString().padStart(integerDigits, '0').takeLast(integerDigits)
            val fracStr = fracScaled.toString().padStart(fractionDigits, '0')
            "$intStr.$fracStr"
        }
    }

    // Layout math: we want the digits to fill the available width on a single line.
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val countDigits = integerDigits + fractionDigits // not counting the dot block
        val dotSlots = 1
        val totalSlots = countDigits + dotSlots
        val gaps = (totalSlots - 1)
        val gapPx = with(LocalDensity.current) { gapDp.toPx() }

        // 7-segment aspect ratio: width ≈ 0.56 of height looks good; tune if desired
        val digitAspect = 0.56f

        // Solve for digitHeight so that total width fits:
        // totalWidth = digitWidth*countDigits + dotWidth + gaps*gapPx
        // dotWidth ≈ digitWidth*0.28
        val maxW = constraints.maxWidth.toFloat()
        val digitWidth = (maxW - gapPx * gaps) / (countDigits + 0.28f) // 0.28 slot for dot
        val digitHeightFromW = digitWidth / digitAspect

        // Limit by available height as well (use ~50–60% of screen height)
        val maxH = constraints.maxHeight.toFloat() * 0.6f
        val digitH = min(digitHeightFromW, maxH)
        val digitW = digitH * digitAspect
        val dotW = digitW * 0.28f
        val dotH = digitH * 0.12f

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(with(LocalDensity.current) { digitH.toDp() }),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // integer digits
            for (i in 0 until integerDigits) {
                SevenSegmentDigit(
                    ch = formatted[i],
                    onColor = segmentColor,
                    offColor = offSegmentColor,
                    widthPx = digitW,
                    heightPx = digitH
                )
                if (i != integerDigits - 1) Spacer(Modifier.width(gapDp))
            }

            // dot (square)
            Spacer(Modifier.width(gapDp))
            SevenSegmentDot(widthPx = dotW, heightPx = dotH, color = segmentColor)
            Spacer(Modifier.width(gapDp))

            // fraction digits (last one green)
            for (i in 0 until fractionDigits) {
                val idx = integerDigits + 1 + i
                val isLast = i == fractionDigits - 1
                SevenSegmentDigit(
                    ch = formatted[idx],
                    onColor = if (isLast) lastDigitColor else segmentColor,
                    offColor = if (isLast) lastDigitColor.copy(alpha = 0.15f) else offSegmentColor,
                    widthPx = digitW,
                    heightPx = digitH
                )
                if (i != fractionDigits - 1) Spacer(Modifier.width(gapDp))
            }
        }
    }
}

@Composable
private fun SevenSegmentDot(widthPx: Float, heightPx: Float, color: Color) {
    val w = with(LocalDensity.current) { widthPx.toDp() }
    val h = with(LocalDensity.current) { heightPx.toDp() }
    Box(
        modifier = Modifier
            .size(w, h)
            .background(color, shape = androidx.compose.foundation.shape.RoundedCornerShape(2.dp))
    )
}

@Composable
private fun SevenSegmentDigit(
    ch: Char,
    onColor: Color,
    offColor: Color,
    widthPx: Float,
    heightPx: Float
) {
    // Segment order: a (top), b (upper-right), c (lower-right), d (bottom),
    // e (lower-left), f (upper-left), g (middle)
    val segMap: Map<Char, BooleanArray> = remember {
        mapOf(
            '0' to booleanArrayOf(true,  true,  true,  true,  true,  true,  false),
            '1' to booleanArrayOf(false, true,  true,  false, false, false, false),
            '2' to booleanArrayOf(true,  true,  false, true,  true,  false, true ),
            '3' to booleanArrayOf(true,  true,  true,  true,  false, false, true ),
            '4' to booleanArrayOf(false, true,  true,  false, false, true,  true ),
            '5' to booleanArrayOf(true,  false, true,  true,  false, true,  true ),
            '6' to booleanArrayOf(true,  false, true,  true,  true,  true,  true ),
            '7' to booleanArrayOf(true,  true,  true,  false, false, false, false),
            '8' to booleanArrayOf(true,  true,  true,  true,  true,  true,  true ),
            '9' to booleanArrayOf(true,  true,  true,  true,  false, true,  true )
        )
    }
    val active = segMap[ch] ?: booleanArrayOf(false, false, false, false, false, false, false)

    val w = with(LocalDensity.current) { widthPx.toDp() }
    val h = with(LocalDensity.current) { heightPx.toDp() }
    val thickness = heightPx * 0.15f // segment thickness relative to height
    val t = with(LocalDensity.current) { thickness.toDp() }
    val radius = with(LocalDensity.current) { (thickness * 0.35f).toDp() }

    androidx.compose.foundation.Canvas(
        modifier = Modifier.size(w, h)
    ) {
        val stroke = Stroke(width = 2f)
        fun segColor(on: Boolean) = if (on) onColor else offColor

        // a: top
        drawRoundRect(
            color = segColor(active[0]),
            topLeft = Offset(t.toPx(), 0f),
            size = Size(size.width - 2 * t.toPx(), t.toPx()),
            cornerRadius = CornerRadius(radius.toPx(), radius.toPx())
        )

        // d: bottom
        drawRoundRect(
            color = segColor(active[3]),
            topLeft = Offset(t.toPx(), size.height - t.toPx()),
            size = Size(size.width - 2 * t.toPx(), t.toPx()),
            cornerRadius = CornerRadius(radius.toPx(), radius.toPx())
        )

        // g: middle
        drawRoundRect(
            color = segColor(active[6]),
            topLeft = Offset(t.toPx(), size.height / 2f - t.toPx() / 2f),
            size = Size(size.width - 2 * t.toPx(), t.toPx()),
            cornerRadius = CornerRadius(radius.toPx(), radius.toPx())
        )

        // f: upper-left
        drawRoundRect(
            color = segColor(active[5]),
            topLeft = Offset(0f, t.toPx()),
            size = Size(t.toPx(), size.height / 2f - t.toPx()),
            cornerRadius = CornerRadius(radius.toPx(), radius.toPx())
        )

        // e: lower-left
        drawRoundRect(
            color = segColor(active[4]),
            topLeft = Offset(0f, size.height / 2f),
            size = Size(t.toPx(), size.height / 2f - t.toPx()),
            cornerRadius = CornerRadius(radius.toPx(), radius.toPx())
        )

        // b: upper-right
        drawRoundRect(
            color = segColor(active[1]),
            topLeft = Offset(size.width - t.toPx(), t.toPx()),
            size = Size(t.toPx(), size.height / 2f - t.toPx()),
            cornerRadius = CornerRadius(radius.toPx(), radius.toPx())
        )

        // c: lower-right
        drawRoundRect(
            color = segColor(active[2]),
            topLeft = Offset(size.width - t.toPx(), size.height / 2f),
            size = Size(t.toPx(), size.height / 2f - t.toPx()),
            cornerRadius = CornerRadius(radius.toPx(), radius.toPx())
        )

        // Thin white outline (optional – adds punch)
        drawRoundRect(
            color = Color.White.copy(alpha = 0.10f),
            topLeft = Offset(0f, 0f),
            size = Size(size.width, size.height),
            style = stroke,
            cornerRadius = CornerRadius(radius.toPx(), radius.toPx())
        )
    }
}
