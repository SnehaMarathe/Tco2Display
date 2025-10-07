package com.example.tco2display

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
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
import java.util.Locale
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Immersive fullscreen + keep screen on
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            val vm: Tco2ViewModel = viewModel()
            val tco2 by vm.tco2.collectAsState()

            val bg = Color(0xFF000000)
            val white = Color(0xFFFFFFFF)
            val ghost = Color(0x1AFFFFFF)        // ~10% alpha
            val green = Color(0xFF39D353)

            val techFont = FontFamily(Font(R.font.technology_bold, weight = FontWeight.Bold))

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bg)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Title bar
                TopBar(
                    title = "BLUE ENERGY MOTORS",
                    color = Color(0xFFD0D0D0),
                    lineThickness = 3.dp,
                    titleSize = 26.sp
                )

                // Center number readout (fills width, one line)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center),
                    contentAlignment = Alignment.Center
                ) {
                    SevenSegSingleLine(
                        value = tco2,
                        fontFamily = techFont,
                        colorMain = white,
                        colorGhost = ghost,
                        colorLastDigit = green,
                        maxIntegerDigits = 7,          // sizing anchor
                        lastDigitScale = 1.22f,
                        letterSpacingSp = 0f,
                        // NEW: show unit and make it 2× current size
                        unitText = " tCO2",
                        unitColor = white,
                        unitScale = 2.0f               // << doubled
                    )
                }

                // Bottom truck animation (fully offscreen → across → offscreen)
                TruckMarquee(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    imageWidthFraction = 0.65f,      // truck ~65% of screen width
                    travelPaddingDp = 32.dp
                )
            }
        }
    }
}

/* ───────────────────────────── UI pieces ───────────────────────────── */

@Composable
private fun TopBar(title: String, color: Color, lineThickness: Dp, titleSize: androidx.compose.ui.unit.TextUnit) {
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
            fontSize = titleSize,
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
 * Single-line seven-seg style number:
 *  • Fills width (binary-search font size per layout; includes unit if present)
 *  • Ghost “8” only for present digits (int digits + ".xx" + last)
 *  • Last decimal bigger + green
 *  • Optional unit with its own scale/color
 */
@Composable
private fun SevenSegSingleLine(
    value: Double?,
    fontFamily: FontFamily,
    colorMain: Color,
    colorGhost: Color,
    colorLastDigit: Color,
    maxIntegerDigits: Int,
    lastDigitScale: Float,
    letterSpacingSp: Float,
    unitText: String? = null,
    unitColor: Color = colorMain,
    unitScale: Float = 1.0f,
    baseMinSp: Float = 48f,
    baseMaxSp: Float = 640f
) {
    val measurer = rememberTextMeasurer()

    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val density = LocalDensity.current
        val maxWidthPx = with(density) { maxWidth.toPx() }.roundToInt()
        val constraints = Constraints(maxWidth = maxWidthPx)

        // format current value
        val display = if (value == null) "0.000"
                      else String.format(Locale.US, "%.3f", value)
        val dot = display.indexOf('.')
        val rawInt = if (dot >= 0) display.substring(0, dot) else display
        val trimmedInt = rawInt.replaceFirst(Regex("^0+(?!$)"), "").ifEmpty { "0" }
        val frac = if (dot >= 0) display.substring(dot + 1) else "000"
        val firstTwo = frac.take(2).padEnd(2, '0')
        val lastDigit = (frac.getOrNull(2) ?: '0').toString()

        // worst-case sizing template with “8” (widest) + optional unit
        fun template(baseSp: Float) = buildAnnotatedString {
            withStyle(SpanStyle(color = colorMain, fontSize = baseSp.sp, fontWeight = FontWeight.Bold)) {
                append("8".repeat(maxIntegerDigits))
                append(".88")
            }
            withStyle(SpanStyle(color = colorLastDigit, fontSize = (baseSp * lastDigitScale).sp, fontWeight = FontWeight.Bold)) {
                append("8")
            }
            if (!unitText.isNullOrEmpty()) {
                withStyle(SpanStyle(color = unitColor, fontSize = (baseSp * unitScale).sp, fontWeight = FontWeight.Bold)) {
                    append(unitText)
                }
            }
        }

        // binary search font size to fit width (includes unit if present)
        val baseSizeSp = run {
            var lo = baseMinSp
            var hi = baseMaxSp
            var best = lo
            repeat(14) {
                val mid = (lo + hi) / 2f
                val res = measurer.measure(
                    text = template(mid),
                    style = TextStyle(fontFamily = fontFamily, letterSpacing = letterSpacingSp.sp),
                    maxLines = 1, softWrap = false, overflow = TextOverflow.Clip, constraints = constraints
                )
                if (!res.didOverflowWidth) { best = mid; lo = mid } else { hi = mid }
            }
            best
        }

        // ghost (only for present digits; no ghost for unit)
        val ghostStyled = buildAnnotatedString {
            withStyle(SpanStyle(color = colorGhost, fontSize = baseSizeSp.sp, fontWeight = FontWeight.Bold)) {
                append("8".repeat(trimmedInt.length))
                append(".88")
            }
            withStyle(SpanStyle(color = colorGhost, fontSize = (baseSizeSp * lastDigitScale).sp, fontWeight = FontWeight.Bold)) {
                append("8")
            }
        }

        // actual number + optional unit
        val actualStyled = buildAnnotatedString {
            withStyle(SpanStyle(color = colorMain, fontSize = baseSizeSp.sp, fontWeight = FontWeight.Bold)) {
                append(trimmedInt)
                append(".")
                append(firstTwo)
            }
            withStyle(SpanStyle(color = colorLastDigit, fontSize = (baseSizeSp * lastDigitScale).sp, fontWeight = FontWeight.Bold)) {
                append(lastDigit)
            }
            if (!unitText.isNullOrEmpty()) {
                withStyle(SpanStyle(color = unitColor, fontSize = (baseSizeSp * unitScale).sp, fontWeight = FontWeight.Bold)) {
                    append(unitText)
                }
            }
        }

        // Layer ghost behind actual text
        Text(
            text = ghostStyled,
            color = colorGhost,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
            style = TextStyle(fontFamily = fontFamily, letterSpacing = letterSpacingSp.sp),
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = actualStyled,
            color = colorMain,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
            style = TextStyle(fontFamily = fontFamily, letterSpacing = letterSpacingSp.sp),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/** Bottom marquee that moves the truck fully offscreen → across → offscreen, looped. */
@Composable
private fun TruckMarquee(
    modifier: Modifier,
    imageWidthFraction: Float,
    travelPaddingDp: Dp
) {
    BoxWithConstraints(modifier = modifier) {
        val density = LocalDensity.current
        val screenW = maxWidth
        val truckW = screenW * imageWidthFraction
        val travelPad = travelPaddingDp

        val screenWPx = with(density) { screenW.toPx() }
        val truckWPx = with(density) { truckW.toPx() }
        val travelPadPx = with(density) { travelPad.toPx() }

        val startX = -(truckWPx + travelPadPx)
        val endX = screenWPx + travelPadPx

        val x = remember { Animatable(startX) }

        LaunchedEffect(screenWPx, truckWPx) {
            while (true) {
                x.snapTo(startX)
                x.animateTo(
                    targetValue = endX,
                    animationSpec = tween(
                        durationMillis = 10_000,
                        easing = LinearEasing
                    )
                )
            }
        }

        val xDp = with(density) { x.value.toDp() }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height((screenW * 0.18f).coerceAtMost(160.dp))
        ) {
            Spacer(modifier = Modifier.width(xDp))
            Image(
                painter = painterResource(id = R.drawable.truck_inverted),
                contentDescription = "Truck",
                modifier = Modifier
                    .width(truckW)
                    .fillMaxHeight()
            )
        }
    }
}
