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

            val bg = Color(0xFF000000)
            val white = Color(0xFFD0D0D0)  // subtle grey-white like your mock
            val green = Color(0xFF39D353)

            // seven-segment font
            val techFont = FontFamily(Font(R.font.technology_bold, weight = FontWeight.Bold))

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bg)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // ── BLUE ENERGY MOTORS ──
                TopBar(title = "BLUE ENERGY MOTORS", color = white, lineThickness = 3.dp)

                // Fixed-size, one-line number (no leading zeros; last digit green)
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth()
                ) {
                    FixedSizeSevenSegNumber(
                        value = tco2,
                        fontFamily = techFont,
                        intAndFirstTwoColor = white,
                        lastDigitColor = green,
                        // choose the *maximum* integer length you expect (for sizing once)
                        maxIntegerDigits = 7,
                        baseMinSp = 48f,
                        baseMaxSp = 420f,
                        lastDigitScale = 1.22f,
                        letterSpacingSp = 0f
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
 * Fixed-size (constant) seven-seg number:
 * - Computes the font size **once per layout size** using a *reference template*
 *   (e.g., "8888888.88X", where X is scaled as last digit).
 * - Then reuses that size for all subsequent value updates → no spacing jumps.
 * - Removes **all leading zeros** (but keeps "0" if the integer part is zero).
 */
@Composable
private fun FixedSizeSevenSegNumber(
    value: Double?,
    fontFamily: FontFamily,
    intAndFirstTwoColor: Color,
    lastDigitColor: Color,
    maxIntegerDigits: Int,
    baseMinSp: Float,
    baseMaxSp: Float,
    lastDigitScale: Float,
    letterSpacingSp: Float = 0f,
    fractionDigits: Int = 3
) {
    val measurer = rememberTextMeasurer()

    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val density = LocalDensity.current
        val maxWidthPx = with(density) { maxWidth.toPx() }.roundToInt()
        val constraints = Constraints(maxWidth = maxWidthPx)

        // Build worst-case template (for sizing once): all 8s (widest),
        // dot, first two decimals as 8, and last decimal scaled (represented by 'X').
        fun templateStyled(baseSp: Float) = buildAnnotatedString {
            // integer part (max columns)
            withStyle(SpanStyle(color = intAndFirstTwoColor, fontSize = baseSp.sp)) {
                append("8".repeat(maxIntegerDigits))
            }
            // '.' + first two decimals as 8
            withStyle(SpanStyle(color = intAndFirstTwoColor, fontSize = baseSp.sp)) {
                append(".")
                append("88")
            }
            // last decimal bigger
            withStyle(
                SpanStyle(
                    color = lastDigitColor,
                    fontSize = (baseSp * lastDigitScale).sp
                )
            ) { append("8") }
        }

        // Binary-search the **base** size ONCE per layout (constraints key)
        val baseSizeSp = rememberTextMeasurer().let {
            var low = baseMinSp
            var high = baseMaxSp
            var best = low
            repeat(12) {
                val mid = (low + high) / 2f
                val res = measurer.measure(
                    text = templateStyled(mid),
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
            best
        }

        // Format actual value (remove leading zeros)
        val display = run {
            if (value == null) "0.000"
            else String.format(Locale.US, "%.3f", value)
        }
        val dot = display.indexOf('.')
        val rawInt = if (dot >= 0) display.substring(0, dot) else display
        val trimmedInt = rawInt.replaceFirst(Regex("^0+(?!$)"), "") // drop all leading zeros, keep one zero
        val frac = if (dot >= 0) display.substring(dot + 1) else "000"
        val firstTwo = frac.take(2).padEnd(2, '0')
        val lastDigit = if (frac.isNotEmpty()) frac.last().toString() else "0"

        // Build the visible text using the **fixed** base size
        val styled = buildAnnotatedString {
            withStyle(
                SpanStyle(
                    color = intAndFirstTwoColor,
                    fontSize = baseSizeSp.sp
                )
            ) { append(trimmedInt) }
            withStyle(
                SpanStyle(
                    color = intAndFirstTwoColor,
                    fontSize = baseSizeSp.sp
                )
            ) { append("."); append(firstTwo) }
            withStyle(
                SpanStyle(
                    color = lastDigitColor,
                    fontSize = (baseSizeSp * lastDigitScale).sp
                )
            ) { append(lastDigit) }
        }

        Text(
            text = styled,
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
