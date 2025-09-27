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

// For immersive fullscreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- Fullscreen (hide system bars) ---
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())

        setContent {
            val vm: Tco2ViewModel = viewModel()
            val tco2 by vm.tco2.collectAsState()

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
                        fontSize = 28.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Value line: BIG number, smaller unit, last decimal digit green
                    val readout = if (tco2 == null) {
                        buildAnnotatedString {
                            withStyle(SpanStyle(color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.SemiBold)) {
                                append("—")
                            }
                            withStyle(SpanStyle(color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)) {
                                append(" tCO2")
                            }
                        }
                    } else {
                        val valueOnly = String.format(Locale.US, "%.3f", tco2)
                        val unit = " tCO2"
                        val green = Color(0xFF22C55E)

                        buildAnnotatedString {
                            // record where the number starts
                            val start = length
                            // BIG number
                            withStyle(SpanStyle(color = Color.White, fontSize = 100.sp, fontWeight = FontWeight.SemiBold)) {
                                append(valueOnly)
                            }
                            // make the last decimal digit green
                            val lastDigitIndex = start + valueOnly.length - 1
                            if (valueOnly.contains('.') && valueOnly.isNotEmpty()) {
                                addStyle(
                                    SpanStyle(color = green),
                                    start = lastDigitIndex,
                                    end = lastDigitIndex + 1
                                )
                            }
                            // smaller unit
                            withStyle(SpanStyle(color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.SemiBold)) {
                                append(unit)
                            }
                        }
                    }

                    Text(
                        text = readout,
                        color = Color.White, // default for any text without spans
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
