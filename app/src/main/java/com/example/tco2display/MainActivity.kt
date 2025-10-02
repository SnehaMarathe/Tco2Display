package com.example.tco2display

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tco2display.ui.Tco2ViewModel
import java.util.Locale
import kotlin.math.roundToInt

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

            // Colors like your mock
            val bg = Color(0xFF000000)
            val white = Color.White
            val greenAccent = Color(0xFF39D353)

            // Load the seven-seg font
            val techFont = FontFamily(Font(resId = R.font.technology_bold, weight = FontWeight.Bold))

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bg)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // ── BLUE ENERGY MOTORS ──
                TopBar(title = "BLUE ENERGY MOTORS", color = white, lineThickness = 3.dp)

                // Auto-fit number centered
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth()
                ) {
                    FitNumberWithFontOneLine(
                        value = tco2,
                        fontFamily = techFont,
                        intAndFirstTwoColor = white,
                        lastDigitColor = greenAccent,
                        baseMinSp = 48f,
                        baseMaxSp = 420f,   // allow very large on wide displays
                        lastDigitScale = 1.22f,
                        letterSpacingSp = 0f // adjust if you want tighter/looser spacing
                    )
                }

                // Bottom caption
                Text(
                    text = "CARBON SAVINGS tCO2",
                    color = white,
                    fontFamily = techFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
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

/*──────────────────────────  UI pieces  ─────────────────────────*/

@Composable
private fun TopBar(title: String, color: Color, lineThickness: Dp) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(lineThickness)
                .background(color)
        )
        Text(
            text = title,
            color = color,
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(horizontal = 12.dp),
            textAlign = TextAlign.Center
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(lineThickness)
                .background(color)
        )
    }
}

/**
 * Auto-scales a 3-decimal number into one line using the provided font.
 * - Integer + '.' + first two decimals: intAndFirstTwoColor
 * - Last decimal: lastDigitColor and bigger (lastDigitScale)
 * - When value=null, shows 0000000.000 padded to 7 int digits (change if needed).
 */
@Composable
private fun FitNumberWithFontOneLine(
    value: Double?,
    fontFamily: FontFamily,
    intAndFirstTwoColor: Color,
    lastDigitColor: Color,
    baseMinSp: Float,
    baseMaxSp: Float,
    lastDigitScale: Float,
    letterSpacingSp: Float = 0f,
    integerDigits: Int = 7,
    fractionDigits: Int = 3
) {
    val measurer = rememberTextMeasurer()
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val maxWidthPx = with(LocalDensity.current) { maxWidth.toPx() }.roundToInt()
        val constraints = Constraints(maxWidth = maxWidthPx)

        // Build formatted content
        fun format(value: Double?): Triple<String, String, String> {
            return if (value == null) {
                val intStr = "0".repeat(integerDigits)
                val fracStr = "0".repeat(fractionDigits)
                Triple(intStr, ".", fracStr)
            } else {
                val s = String.format(Locale.US, "%.3f", value)
                val dot = s.indexOf('.')
                val intPart = if (dot >= 0) s.substring(0, dot) else s
                val frac = if (dot >= 0) s.substring(dot + 1) else ""
                Triple(
                    intPart.padStart(integerDigits, '0').takeLast(integerDigits),
                    ".",
                    frac.padEnd(fractionDigits, '0').take(fractionDigits)
                )
            }
        }

        val (intPart, dot, fracPart) = format(value)

        fun styled(baseSp: Float) = buildAnnotatedString {
            // integer
            withStyle(SpanStyle(color = intAndFirstTwoColor, fontSize = baseSp.sp)) {
                append(intPart)
            }
            // dot + first two decimals
            if (fracPart.isNotEmpty()) {
                withStyle(SpanStyle(color = intAndFirstTwoColor, fontSize = baseSp.sp)) {
                    append(dot)
                    append(fracPart.substring(0, 2))
                }
                // last decimal
                withStyle(
                    SpanStyle(
                        color = lastDigitColor,
                        fontSize = (baseSp * lastDigitScale).sp
                    )
                ) {
                    append(fracPart.last().toString())
                }
            }
        }

        // Binary-search the largest base size that fits width (one line)
        var low = baseMinSp
        var high = baseMaxSp
        var best = low
        repeat(12) {
            val mid = (low + high) / 2f
            val text = styled(mid)
            val res = measurer.measure(
                text = text,
                style = TextStyle(
                    fontFamily = fontFamily,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = letterSpacingSp.sp
                ),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
                constraints = constraints
            )
            if (!res.didOverflowWidth) {
                best = mid
                low = mid
            } else {
                high = mid
            }
        }

        Text(
            text = styled(best),
            color = Color.White,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
            style = TextStyle(
                fontFamily = fontFamily,
                fontWeight = FontWeight.Bold,
                letterSpacing = letterSpacingSp.sp
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
