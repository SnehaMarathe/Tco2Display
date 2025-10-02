package com.example.tco2display

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
            val segOff = Color(0x22FFFFFF)      // dim "off" segments
            val lastDigitOn = Color(0xFF39D353) // green last decimal
            val accent = Color.White

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
                        text = "BLUE ENERGY MOTORS",
                        color = accent,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 12.dp),
                        textAlign = TextAlign.Center
                    )
                    DividerLine(color = accent, thickness = 3.dp, modifier = Modifier.weight(1f))
                }

                // Center seven-segment number
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                ) {
                    SevenSegmentNumber(
                        value = tco2,
                        integerDigits = 7,   // adjust if you need more/less integer places
                        fractionDigits = 3,  // three decimals
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
    // Build "0000000.000" when null; otherwise format with zero-padding
    val formatted = if (value == null) {
        "0".repeat(integerDigits) + "." + "0".repeat(fractionDigits)
    } else {
        val abs = kotlin.math.abs(value)
        val int = abs.toLong()
        val scale = Math.pow(10.0, fractionDigits.toDouble())
        val fracScaled = ((abs - int) * scale).toLong()
            .coerceAtMost((scale - 1).toLong())

        val intStr = int.toString().padStart(integerDigits, '0').takeLast(integerDigits)
        val fracStr = fracScaled.toString().padStart(fractionDigits, '0')
        "$intStr.$fracStr"
    }

    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val density = LocalDensity.current
        val maxWpx = with(density) { maxWidth.toPx() }
        val maxHpx = with(density) { maxHeight.toPx() }
        val gapPx = with(density) { gapDp.toPx() }

        val countDigits = integerDigits + fractionDigits
        val dotSlots = 1
        val totalSlots = countDigits + dotSlots
        val gaps = (totalSlots - 1)

        // 7-segment aspect ratio; tweak if preferred
        val digitAspect = 0.56f

        // Fit to width (dot takes ~0.28 of a digit width)
        val digitWidthPx = (maxWpx - gapPx * gaps) / (countDigits + 0.28f)
        val digitHeightFromW = digitWidthPx / digitAspect

        // Also limit by height (use ~60% of available height)
        val digitHPx = min(digitHeightFromW, maxHpx * 0.60f)
        val digitWPx = digitHPx * digitAspect
        val dotWPx = digitWPx * 0.28f
        val dotHPx = digitHPx * 0.12f

        val digitHDp = with(density) { digitHPx.dp }
        val dotWDp = with(density) { dotWPx.dp }
        val dotHDp = with(density) { dotHPx.dp }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(digitHDp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Integer digits
            for (i in 0 until integerDigits) {
                SevenSegmentDigit(
                    ch = formatted[i],
                    onColor = segmentColor,
                    offColor = offSegmentColor,
                    widthPx = digitWPx,
                    heightPx = digitHPx
                )
                if (i != integerDigits - 1) Spacer(Modifier.width(gapDp))
            }

            // Dot (square)
            Spacer(Modifier.width(gapDp))
            SevenSegmentDot(widthDp = dotWDp, heightDp = dotHDp, color = segmentColor)
            Spacer(Modifier.width(gapDp))

            // Fraction digits (last one green)
            for (i in 0 until fractionDigits) {
                val idx = integerDigits + 1 + i
                val isLast = i == fractionDigits - 1
                SevenSegmentDigit(
                    ch = formatted[idx],
                    onColor = if (isLast) lastDigitColor else segmentColor,
                    offColor = if (isLast) lastDigitColor.copy(alpha = 0.15f) else offSegmentColor,
                    widthPx = digitWPx,
                    heightPx = digitHPx
                )
                if (i != fractionDigits - 1) Spacer(Modifier.width(gapDp))
            }
        }
    }
}

@Composable
private fun SevenSegmentDot(widthDp: Dp, heightDp: Dp, color: Color) {
    Box(
        modifier = Modifier
            .size(widthDp, heightDp)
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
    // Segment order: a, b, c, d, e, f, g
    val segMap: Map<Char, BooleanArray> = mapOf(
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
    val active = segMap[ch] ?: booleanArrayOf(false, false, false, false, false, false, false)

    val density = LocalDensity.current
    val wDp = with(density) { widthPx.dp }
    val hDp = with(density) { heightPx.dp }

    // segment thickness and corner radius (in px)
    val thicknessPx = heightPx * 0.15f
    val radiusPx = thicknessPx * 0.35f

    Canvas(modifier = Modifier.size(wDp, hDp)) {
        val t = thicknessPx
        val r = radiusPx
        val stroke = Stroke(width = 2f)

        fun segColor(on: Boolean) = if (on) onColor else offColor

        // a: top
        drawRoundRect(
            color = segColor(active[0]),
            topLeft = Offset(t, 0f),
            size = Size(size.width - 2 * t, t),
            cornerRadius = CornerRadius(r, r)
        )
        // d: bottom
        drawRoundRect(
            color = segColor(active[3]),
            topLeft = Offset(t, size.height - t),
            size = Size(size.width - 2 * t, t),
            cornerRadius = CornerRadius(r, r)
        )
        // g: middle
        drawRoundRect(
            color = segColor(active[6]),
            topLeft = Offset(t, size.height / 2f - t / 2f),
            size = Size(size.width - 2 * t, t),
            cornerRadius = CornerRadius(r, r)
        )
        // f: upper-left
        drawRoundRect(
            color = segColor(active[5]),
            topLeft = Offset(0f, t),
            size = Size(t, size.height / 2f - t),
            cornerRadius = CornerRadius(r, r)
        )
        // e: lower-left
        drawRoundRect(
            color = segColor(active[4]),
            topLeft = Offset(0f, size.height / 2f),
            size = Size(t, size.height / 2f - t),
            cornerRadius = CornerRadius(r, r)
        )
        // b: upper-right
        drawRoundRect(
            color = segColor(active[1]),
            topLeft = Offset(size.width - t, t),
            size = Size(t, size.height / 2f - t),
            cornerRadius = CornerRadius(r, r)
        )
        // c: lower-right
        drawRoundRect(
            color = segColor(active[2]),
            topLeft = Offset(size.width - t, size.height / 2f),
            size = Size(t, size.height / 2f - t),
            cornerRadius = CornerRadius(r, r)
        )

        // subtle outline
        drawRoundRect(
            color = Color.White.copy(alpha = 0.10f),
            topLeft = Offset(0f, 0f),
            size = Size(size.width, size.height),
            style = stroke,
            cornerRadius = CornerRadius(r, r)
        )
    }
}
