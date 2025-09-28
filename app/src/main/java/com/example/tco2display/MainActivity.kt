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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tco2display.ui.Tco2ViewModel
import java.util.Locale

// immersive fullscreen helpers
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

// animations
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat   // ✅ needed for InfiniteTransition.animateFloat

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

            // Colors tuned to the screenshot vibe
            val blueNumber = Color(0xFFAEBBFF)   // soft periwinkle (integer part)
            val mintDecimal = Color(0xFFCFEFDB)  // pale mint (first two decimals)
            val greenAccent = Color(0xFF6AC73A)  // bold green (last decimal, unit)

            // Sizes tuned for landscape fullscreen
            val sizeNumber = 120.sp    // base size for number
            val sizeLastDigit = 140.sp // bigger final digit
            val sizeUnit = 44.sp       // small inline "kg"
            val sizeHeading = 36.sp
            val sizeBottom = fortySp() // helper below to avoid magic numbers

            // --- 350ms animated numeric transition ---
            val animatedValue by animateFloatAsState(
                targetValue = (tco2 ?: 0.0).toFloat(),
                animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
                label = "tco2Animated"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                // --- lively truck animation at the bottom ---
                TruckTrailerAnimation(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 16.dp),
                    bodyColor = greenAccent.copy(alpha = 0.80f),
                    wheelColor = Color(0xFF1E1E1E),
                    accent = Color(0xFF9AE58F)
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Heading
                    Text(
                        text = "CO2 Savings",
                        color = Color.White,
                        fontSize = sizeHeading,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Main readout (styled + animated)
                    val readout = if (tco2 == null) {
                        buildAnnotatedString {
                            withStyle(
                                SpanStyle(
                                    color = blueNumber,
                                    fontSize = sizeNumber,
                                    fontWeight = FontWeight.SemiBold
                                )
                            ) { append("—") }
                            withStyle(
                                SpanStyle(
                                    color = greenAccent,
                                    fontSize = sizeUnit,
                                    fontWeight = FontWeight.SemiBold
                                )
                            ) { append(" kg") }
                        }
                    } else {
                        val value = String.format(Locale.US, "%.3f", animatedValue)
                        val dot = value.indexOf('.')
                        val intPart = if (dot >= 0) value.substring(0, dot) else value
                        val fracPart = if (dot >= 0) value.substring(dot + 1) else "" // 3 chars
                        val firstTwo = if (fracPart.length >= 2) fracPart.substring(0, 2) else fracPart
                        val lastDigit = if (fracPart.isNotEmpty()) fracPart.last().toString() else ""

                        buildAnnotatedString {
                            // integer part
                            withStyle(
                                SpanStyle(
                                    color = blueNumber,
                                    fontSize = sizeNumber,
                                    fontWeight = FontWeight.SemiBold
                                )
                            ) { append(intPart) }

                            // dot + first two decimals (mint)
                            if (dot >= 0) {
                                withStyle(
                                    SpanStyle(
                                        color = mintDecimal,
                                        fontSize = sizeNumber,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                ) {
                                    append(".")
                                    append(firstTwo)
                                }
                            }

                            // last decimal digit (bigger + green)
                            if (lastDigit.isNotEmpty()) {
                                withStyle(
                                    SpanStyle(
                                        color = greenAccent,
                                        fontSize = sizeLastDigit,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                ) { append(lastDigit) }
                            }

                            // small inline unit "kg" in green
                            withStyle(
                                SpanStyle(
                                    color = greenAccent,
                                    fontSize = sizeUnit,
                                    fontWeight = FontWeight.SemiBold
                                )
                            ) { append(" kg") }
                        }
                    }

                    Text(
                        text = readout,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Bottom label "tCO2" centered (small, bluish)
                    Text(
                        text = "tCO2",
                        color = blueNumber,
                        fontSize = 44.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

// tiny helper so we can keep sizes grouped at the top if you want to tweak
private fun fortySp() = 44.sp

// -----------------------------------------------------------------------------
// Truck + trailer animation (simple vector drawing, lightweight & looped)
// -----------------------------------------------------------------------------
@Composable
private fun TruckTrailerAnimation(
    modifier: Modifier = Modifier,
    bodyColor: Color = Color(0xFF6AC73A),
    wheelColor: Color = Color(0xFF1E1E1E),
    accent: Color = Color(0xFFA8FFB3)
) {
    val infinite = rememberInfiniteTransition(label = "truckLoop")
    val progress by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7000, easing = LinearEasing)
        ),
        label = "truckProgress"
    )

    Canvas(modifier = modifier) {
        // layout (all Float literals have 'f' to avoid Double/Float ambiguity)
        val groundY = size.height * 0.85f
        val truckH = (size.height * 0.14f).coerceAtMost(160f)
        val truckW = truckH * 3.1f
        val trailerW = truckW * 0.70f
        val cabW = truckW * 0.25f
        val boxH = truckH * 0.62f
        val corner = CornerRadius(truckH * 0.08f, truckH * 0.08f)

        // move from left (off-screen) to right (off-screen)
        val x = (size.width + truckW) * progress - truckW

        // road line (very subtle)
        drawRect(
            color = Color(0x22FFFFFF),
            topLeft = Offset(0f, groundY + truckH * 0.25f),
            size = Size(size.width, 2f)
        )

        // trailer box
        drawRoundRect(
            color = bodyColor.copy(alpha = 0.35f),
            topLeft = Offset(x, groundY - boxH),
            size = Size(trailerW, boxH),
            cornerRadius = corner
        )

        // cab
        drawRoundRect(
            color = bodyColor,
            topLeft = Offset(x + trailerW + truckH * 0.10f, groundY - boxH),
            size = Size(cabW, boxH),
            cornerRadius = corner
        )

        // little window accent on cab
        drawRoundRect(
            color = accent.copy(alpha = 0.9f),
            topLeft = Offset(x + trailerW + truckH * 0.16f, groundY - boxH + truckH * 0.12f),
            size = Size(cabW * 0.45f, boxH * 0.38f),
            cornerRadius = CornerRadius(truckH * 0.05f, truckH * 0.05f)
        )

        // wheels (explicit list type to avoid inference ambiguity)
        val r = truckH * 0.18f
        val yWheel = groundY + r * 0.35f
        val w1x = x + trailerW * 0.25f
        val w2x = x + trailerW * 0.70f
        val w3x = x + trailerW + truckH * 0.20f

        val wheelXs: List<Float> = listOf(w1x, w2x, w3x)
        for (wx: Float in wheelXs) {
            drawCircle(color = wheelColor, radius = r, center = Offset(wx, yWheel))
            drawCircle(color = Color.White.copy(alpha = 0.12f), radius = r * 0.45f, center = Offset(wx, yWheel))
            drawCircle(color = accent.copy(alpha = 0.7f), radius = r * 0.12f, center = Offset(wx, yWheel))
        }
    }
}
