package com.example.tco2display

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tco2display.ui.Tco2ViewModel
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val vm: Tco2ViewModel = viewModel()
            val tco2 by vm.tco2.collectAsState()

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                val annotated = if (tco2 == null) {
                    buildAnnotatedString { append("— tCO2") }
                } else {
                    val valueOnly = String.format(Locale.US, "%.3f", tco2)
                    val unit = " tCO2"
                    buildAnnotatedString {
                        append(valueOnly)
                        // Color ONLY the last digit after the decimal
                        val dot = valueOnly.indexOf('.')
                        if (dot >= 0 && valueOnly.length > dot + 1) {
                            val lastDigitIndex = valueOnly.length - 1
                            addStyle(
                                SpanStyle(color = Color(0xFF22C55E)), // green
                                start = lastDigitIndex,
                                end = lastDigitIndex + 1
                            )
                        }
                        append(unit)
                    }
                }

                Text(
                    text = annotated,
                    color = Color.White,              // default color for the rest
                    fontSize = 72.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    textAlign = TextAlign.Center,
                    lineHeight = 76.sp
                )
            }
        }
    }
}
