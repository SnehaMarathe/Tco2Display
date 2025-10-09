package com.example.tco2display.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tco2display.BuildConfig
import com.example.tco2display.data.IntanglesRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class Tco2ViewModel : ViewModel() {

    private val token: String = BuildConfig.INTANGLES_TOKEN.orEmpty()
    private val repository = IntanglesRepository()

    private val _tco2 = MutableStateFlow<Double?>(null)
    val tco2: StateFlow<Double?> = _tco2

    private val pollMillis = 500L  // ← 0.5s cadence

    init {
        if (token.isNotBlank()) startPollingExact()
    }

    private fun startPollingExact() {
        viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                val started = System.nanoTime()
                try {
                    val value = withContext(Dispatchers.IO) {
                        repository.fetchAndSumTco2(
                            token = token,
                            accId = "962759605811675136",
                            specIds = "966986020958502912,969208267156750336",
                            psize = 1000,       // fewer pages → faster
                            lang = "en",
                            noDefaultFields = true,
                            proj = "total_fuel_consumed",
                            groups = "",
                            lastloc = true,     // keep accurate
                            lngUnit = "kg",
                            lngDensity = 0.45
                        )
                    }
                    _tco2.value = value
                } catch (ce: CancellationException) {
                    throw ce
                } catch (_: Exception) {
                    // quick pause on transient error
                    // (keeps loop responsive without long backoffs)
                    delay(300)
                }
                val elapsedMs = (System.nanoTime() - started) / 1_000_000
                val wait = (pollMillis - elapsedMs).coerceAtLeast(0L)
                delay(wait)
            }
        }
    }
}
