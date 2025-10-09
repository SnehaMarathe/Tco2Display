package com.example.tco2display.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tco2display.BuildConfig
import com.example.tco2display.data.IntanglesRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * ViewModel that:
 * 1) Polls the server every ~2s for the latest total tCO2 saved.
 * 2) Estimates a per-second rate and "ticks" the value every 100ms between polls
 *    so the on-screen number feels live.
 */
class Tco2ViewModel : ViewModel() {

    private val repository = IntanglesRepository()

    private val _tco2 = MutableStateFlow<Double?>(null)
    val tco2: StateFlow<Double?> = _tco2

    // Internal state for interpolation
    private var lastServerValue: Double? = null
    private var lastServerAtNanos: Long = System.nanoTime()
    private var estimatedRatePerSec: Double = 0.0

    // Poll interval and UI tick interval
    private val pollMillis = 2_000L   // fetch from API every 2s
    private val tickMillis = 100L     // update UI every 100ms

    init {
        startPollingAndTicking()
    }

    private fun startPollingAndTicking() {
        // 1) Server polling loop (every ~2s)
        viewModelScope.launch {
            var backoff = 1_000L
            while (isActive) {
                try {
                    val token = BuildConfig.INTANGLES_TOKEN.orEmpty()
                    if (token.isBlank()) {
                        // No token → keep previous value and try again later
                        delay(pollMillis)
                        continue
                    }

                    val now = System.nanoTime()
                    val newVal = repository.fetchAndSumTco2(
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

                    // Update interpolation rate from last successful sample
                    lastServerValue?.let { last ->
                        val dt = (now - lastServerAtNanos) / 1_000_000_000.0
                        if (dt > 0) {
                            val dv = newVal - last
                            estimatedRatePerSec = dv / dt
                        }
                    }

                    lastServerValue = newVal
                    lastServerAtNanos = now
                    _tco2.value = newVal

                    // Reset backoff on success
                    backoff = 1_000L

                    delay(pollMillis)
                } catch (ce: CancellationException) {
                    throw ce
                } catch (_: Exception) {
                    // Network or parse error; back off briefly, then retry
                    delay(backoff)
                    backoff = (backoff * 2).coerceAtMost(10_000L)
                }
            }
        }

        // 2) Lightweight interpolation tick (every 100ms)
        viewModelScope.launch {
            while (isActive) {
                val base = lastServerValue
                if (base != null) {
                    val dt = (System.nanoTime() - lastServerAtNanos) / 1_000_000_000.0
                    // Project forward along the last observed rate
                    val projected = base + estimatedRatePerSec * dt

                    // Optional clamp: don’t drift more than a full poll interval ahead
                    // val maxAhead = base + estimatedRatePerSec * (pollMillis / 1000.0)
                    // _tco2.value = projected.coerceAtMost(maxAhead)

                    _tco2.value = projected
                }
                delay(tickMillis)
            }
        }
    }
}
