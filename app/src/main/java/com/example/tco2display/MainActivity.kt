package com.example.tco2display

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.res.painterResource
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

// animation for the truck image
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.annotation.DrawableRes
import androidx.compose.ui.text.style.TextOverflow

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
            val greenAccent = Color(0xFF6AC73A)  // last decimal + inline unit

            // Sizes — large number, smaller unit; last digit even larger
            val sizeNumber = 170.sp
            val sizeLastDigit = 210.sp
            val sizeUnit = 28.sp

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .padding(horizontal = 24.dp)
            ) {
                // Center big number
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
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
                            ) { append(" tCO2") }
                        }
                    } else {
                        val value = String.format(Locale.US, "%.3f", tco2)
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

                            // inline unit → tCO2 (small)
                            withStyle(
                                SpanStyle(
                                    color = greenAccent,
                                    fontSize = sizeUnit,
                                    fontWeight = FontWeight.SemiBold
                                )
                            ) { append(" tCO2") }
                        }
                    }

                    Text(
                        text = readout,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Truck animation strip pinned to bottom
                TruckImageAnimation(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    resId = R.drawable.truck_inverted,
                    height = 84.dp,     // adjust strip height here
                    durationMs = 7000
                )
            }
        }
    }
}

/** Bottom truck animation using the inverted PNG. Also overlays "Blue Energy Motors" on the trailer. */
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
        val imgWidth = height * 3.6f               // assume ~3.6:1 aspect for the truck image
        val travel = maxWidth - imgWidth
        val xOffset = travel * progress

        // moving container sized to the image
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

            // Trailer text overlay (roughly the right 70% of the image)
            val trailerStart = imgWidth * 0.22f
            val trailerWidth = imgWidth * 0.70f
            Box(
                modifier = Modifier
                    .offset(x = trailerStart, y = height * 0.18f)
                    .width(trailerWidth)
                    .height(height * 0.54f),
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
