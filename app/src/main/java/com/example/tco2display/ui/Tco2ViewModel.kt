package com.example.tco2display.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tco2display.BuildConfig
import com.example.tco2display.data.IntanglesRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class Tco2ViewModel : ViewModel() {
    private val repo = IntanglesRepository()

    private val _tco2 = MutableStateFlow<Double?>(null)
    val tco2 = _tco2.asStateFlow()

    init {
        viewModelScope.launch {
            while (true) {
                try {
                    val token = BuildConfig.INTANGLES_TOKEN
                    if (!token.isNullOrBlank()) {
                        _tco2.value = repo.fetchAndSumTco2(
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
                    }
                } catch (_: Exception) {
                    // keep last value on error
                }
                delay(5_000)
            }
        }
    }
}
