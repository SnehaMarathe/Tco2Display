package com.example.tco2display

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
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

// immersive fullscreen helpers
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

            // Colors
            val blueNumber = Color(0xFFAEBBFF)   // integer part
            val mintDecimal = Color(0xFFCFEFDB)  // first two decimals
            val greenAccent = Color(0xFF6AC73A)  // last decimal (bigger)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .padding(horizontal = 24.dp)
            ) {
                // Center: auto-fit number on one line
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth()
                ) {
                    FitNumberTextOneLine(
                        value = tco2,
                        intColor = blueNumber,
                        midDecimalColor = mintDecimal,
                        lastDigitColor = greenAccent,
                        baseMinSp = 48f,
                        baseMaxSp = 340f,
                        lastDigitScale = 1.24f // last digit ~24% larger
                    )
                }

                // Bottom: truck animation (30% smaller -> ~60dp high)
                TruckImageAnimation(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    resId = R.drawable.truck_inverted,
                    height = 60.dp,         // ~30% smaller than 84.dp
                    durationMs = 7000
                )
            }
        }
    }
}

/**
 * Auto-scales the styled number to fill width in ONE line (no wrap).
 * - Integer part: intColor, base size
 * - '.' + first two decimals: midDecimalColor, base size
 * - Last decimal: lastDigitColor, base size * lastDigitScale
 * - No unit text
 */
@Composable
private fun FitNumberTextOneLine(
    value: Double?,
    intColor: Color,
    midDecimalColor: Color,
    lastDigitColor: Color,
    baseMinSp: Float,
    baseMaxSp: Float,
    lastDigitScale: Float
) {
    val measurer = rememberTextMeasurer()
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val density = LocalDensity.current
        val maxWidthPx = with(density) { maxWidth.toPx() }.roundToInt()
        val constraints = Constraints(maxWidth = maxWidthPx)

        fun styledText(baseSp: Float) =
            if (value == null) {
                buildAnnotatedString {
                    withStyle(
                        SpanStyle(
                            color = intColor,
                            fontSize = baseSp.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    ) { append("—") }
                }
            } else {
                val formatted = String.format(Locale.US, "%.3f", value)
                val dot = formatted.indexOf('.')
                val intPart = if (dot >= 0) formatted.substring(0, dot) else formatted
                val fracPart = if (dot >= 0) formatted.substring(dot + 1) else ""
                val firstTwo = if (fracPart.length >= 2) fracPart.substring(0, 2) else fracPart
                val lastDigit = if (fracPart.isNotEmpty()) fracPart.last().toString() else ""

                buildAnnotatedString {
                    withStyle(
                        SpanStyle(
                            color = intColor,
                            fontSize = baseSp.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    ) { append(intPart) }

                    if (dot >= 0) {
                        withStyle(
                            SpanStyle(
                                color = midDecimalColor,
                                fontSize = baseSp.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        ) {
                            append(".")
                            append(firstTwo)
                        }
                    }

                    if (lastDigit.isNotEmpty()) {
                        withStyle(
                            SpanStyle(
                                color = lastDigitColor,
                                fontSize = (baseSp * lastDigitScale).sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        ) { append(lastDigit) }
                    }
                }
            }

        // Binary search the largest base size that fits width
        var low = baseMinSp
        var high = baseMaxSp
        var best = low
        repeat(12) {
            val mid = (low + high) / 2f
            val text = styledText(mid)
            val result = measurer.measure(
                text = text,
                style = TextStyle.Default,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
                constraints = constraints
            )
            if (!result.didOverflowWidth) {
                best = mid; low = mid
            } else {
                high = mid
            }
        }

        Text(
            text = styledText(best),
            color = Color.White,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Bottom truck animation using the inverted PNG.
 * Truck starts fully off-screen left and exits fully off-screen right.
 * Overlays "Blue Energy Motors" centered on the trailer.
 */
@Composable
private fun TruckImageAnimation(
    modifier: Modifier = Modifier,
    @DrawableRes resId: Int,
    height: Dp,
    durationMs: Int = 7000
) {
    val infinite = rememberInfiniteTransition(label = "truckLoop")
    val progress by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(durationMs, easing = LinearEasing)),
        label = "truckProgress"
    )

    BoxWithConstraints(modifier = modifier.height(height)) {
        val imgWidth = height * 3.6f           // ~3.6:1 aspect for the PNG
        val track = maxWidth + imgWidth        // off-left to off-right distance
        val xOffset = (-imgWidth) + (track * progress)

        // Moving container sized to the image
        Box(
            modifier = Modifier
                .offset(x = xOffset)
                .width(imgWidth)
                .height(height)
        ) {
            Image(
                painter = painterResource(resId),
                contentDescription = "Truck",
                modifier = Modifier.fillMaxSize()
            )

            // Trailer text overlay — tuned to sit inside the trailer region
            val trailerLeftRatio   = 0.285f
            val trailerTopRatio    = 0.26f
            val trailerWidthRatio  = 0.63f
            val trailerHeightRatio = 0.42f

            val trailerLeft   = imgWidth * trailerLeftRatio
            val trailerTop    = height   * trailerTopRatio
            val trailerWidth  = imgWidth * trailerWidthRatio
            val trailerHeight = height   * trailerHeightRatio

            Box(
                modifier = Modifier
                    .offset(x = trailerLeft, y = trailerTop)
                    .width(trailerWidth)
                    .height(trailerHeight),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Blue Energy Motors",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
