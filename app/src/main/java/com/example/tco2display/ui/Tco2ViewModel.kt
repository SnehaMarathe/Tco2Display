package com.example.tco2display.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tco2display.BuildConfig
import com.example.tco2display.data.IntanglesRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class Tco2ViewModel : ViewModel() {

    // Read once; avoids repeated reflection/lookup
    private val token: String = BuildConfig.INTANGLES_TOKEN.orEmpty()

    // Use your existing repository as-is
    private val repository = IntanglesRepository()

    private val _tco2 = MutableStateFlow<Double?>(null)
    val tco2: StateFlow<Double?> = _tco2

    // Poll cadence & retry timings
    private val pollMillis = 2_000L
    private val quickRetries = longArrayOf(250L, 500L, 1000L)
    private val steadyRetry = 1_500L

    init {
        if (token.isNotBlank()) {
            startPollingExact()
        } else {
            // No token available; keep null and do not spin aggressively
        }
    }

    /**
     * Polls the server every 2s and publishes EXACT values only.
     * No interpolation, no client-side increments.
     */
    private fun startPollingExact() {
        viewModelScope.launch {
            var retryIndex = 0
            while (isActive) {
                try {
                    val value = repository.fetchAndSumTco2(
                        token = token,
                        accId = "962759605811675136",
                        specIds = "966986020958502912,969208267156750336",
                        psize = 300,
                        lang = "en",
                        noDefaultFields = true,
                        proj = "total_fuel_consumed",
                        groups = "",
                        lastloc = true,
                        lngUnit = "kg",
                        lngDensity = 0.45
                    )

                    // Publish exact server value
                    _tco2.value = value

                    // Reset retry ramp and wait normal cadence
                    retryIndex = 0
                    delay(pollMillis)
                } catch (ce: CancellationException) {
                    throw ce
                } catch (_: Exception) {
                    // Quick backoff on transient failure, then a steady retry interval
                    val d = quickRetries.getOrNull(retryIndex) ?: steadyRetry
                    if (retryIndex < quickRetries.lastIndex) retryIndex++
                    delay(d)
                }
            }
        }
    }
}
