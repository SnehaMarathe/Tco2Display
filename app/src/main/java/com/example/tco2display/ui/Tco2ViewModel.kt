package com.example.tco2display.ui

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tco2display.BuildConfig
import com.example.tco2display.data.IntanglesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class Tco2ViewModel : ViewModel() {
    private val repo = IntanglesRepository()

    private val _tco2 = MutableStateFlow<Double?>(null)
    val tco2 = _tco2.asStateFlow()

    companion object {
        private const val PERIOD_MS = 2_000L // poll every 2 seconds
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                val start = SystemClock.elapsedRealtime()
                try {
                    val token = BuildConfig.INTANGLES_TOKEN
                    if (!token.isNullOrBlank()) {
                        val value = repo.fetchAndSumTco2(
                            token = token,
                            accId = "962759605811675136",
                            specIds = "966986020958502912,969208267156750336",
                            psize = 300,           // you can raise to 500/1000 to reduce pages
                            lang = "en",
                            noDefaultFields = true,
                            proj = "total_fuel_consumed",
                            groups = "",
                            lastloc = true,
                            lngUnit = "kg",
                            lngDensity = 0.45
                        )
                        _tco2.value = value
                    } else {
                        _tco2.value = null
                    }
                } catch (_: Exception) {
                    // Keep last value on any error
                }

                // Keep a steady 2s cadence (if fetch took longer than 2s, we skip the delay)
                val elapsed = SystemClock.elapsedRealtime() - start
                val sleep = (PERIOD_MS - elapsed).coerceAtLeast(0L)
                delay(sleep)
            }
        }
    }
}
