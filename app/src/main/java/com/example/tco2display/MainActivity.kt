package com.example.tco2display

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
            val blueNumber = Color(0xFFAEBBFF)   // soft periwinkle
            val mintDecimal = Color(0xFFCFEFDB)  // pale mint
            val greenAccent = Color(0xFF6AC73A)  // bold green

            // Sizes tuned for landscape fullscreen
            val sizeNumber = 120.sp    // base size for number
            val sizeLastDigit = 140.sp // bigger final digit
            val sizeUnit = 44.sp       // small inline "kg"
            val sizeHeading = 36.sp
            val sizeBottom =  fortySp() // helper below to avoid magic numbers

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
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

                    // Main readout (styled like the screenshot)
                    val readout = if (tco2 == null) {
                        buildAnnotatedString {
                            withStyle(SpanStyle(color = blueNumber, fontSize = sizeNumber, fontWeight = FontWeight.SemiBold)) {
                                append("—")
                            }
                            withStyle(SpanStyle(color = greenAccent, fontSize = sizeUnit, fontWeight = FontWeight.SemiBold)) {
                                append(" kg")
                            }
                        }
                    } else {
                        val value = String.format(Locale.US, "%.3f", tco2)
                        val dot = value.indexOf('.')
                        val intPart = if (dot >= 0) value.substring(0, dot) else value
                        val fracPart = if (dot >= 0) value.substring(dot + 1) else "" // should be 3 chars

                        val firstTwo = if (fracPart.length >= 2) fracPart.substring(0, 2) else fracPart
                        val lastDigit = if (fracPart.isNotEmpty()) fracPart.last().toString() else ""

                        buildAnnotatedString {
                            // integer part
                            withStyle(SpanStyle(color = blueNumber, fontSize = sizeNumber, fontWeight = FontWeight.SemiBold)) {
                                append(intPart)
                            }
                            // dot + first two decimals (mint)
                            if (dot >= 0) {
                                withStyle(SpanStyle(color = mintDecimal, fontSize = sizeNumber, fontWeight = FontWeight.SemiBold)) {
                                    append(".")
                                    append(firstTwo)
                                }
                            }
                            // last decimal digit (bigger + green)
                            if (lastDigit.isNotEmpty()) {
                                withStyle(SpanStyle(color = greenAccent, fontSize = sizeLastDigit, fontWeight = FontWeight.SemiBold)) {
                                    append(lastDigit)
                                }
                            }
                            // small inline unit "kg" in green
                            withStyle(SpanStyle(color = greenAccent, fontSize = sizeUnit, fontWeight = FontWeight.SemiBold)) {
                                append(" kg")
                            }
                        }
                    }

                    Text(
                        text = readout,
                        color = Color.White, // default for spans without explicit color
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
