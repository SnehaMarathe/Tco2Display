package com.example.tco2display.data

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit

class IntanglesRepository {

    // kg CO2 saved per kg LNG (to match platform)
    private val SAVINGS_PER_KG = 0.926

    private val baseUrl = "https://apis.intangles.com"
    private val referer = "https://bemblueedge.intangles.com/"
    private val origin = "https://bemblueedge.intangles.com"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private fun buildHeaders(token: String) = mapOf(
        "Accept" to "application/json, text/plain, */*",
        "intangles-session-type" to "web",
        "intangles-user-lang" to "en",
        "intangles-user-token" to token,
        "intangles-user-tz" to "Asia/Calcutta",
        "Referer" to referer,
        "Origin" to origin,
        "User-Agent" to "android-okhttp/4.x"
    )

    private val client: OkHttpClient by lazy {
        val log = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        OkHttpClient.Builder()
            .addInterceptor(log)
            .build()
    }

    private val api: IntanglesApi by lazy {
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(IntanglesApi::class.java)
    }

    /**
     * Streams pages and returns TOTAL tCO2 saved (in tonnes).
     * Mirrors the Python behavior: detects the fuel key on first page, sums over pagination,
     * converts LNG to kg (if needed), then multiplies by SAVINGS_PER_KG and divides by 1000.
     */
    suspend fun fetchAndSumTco2(
        token: String,
        accId: String,
        specIds: String,
        psize: Int,
        lang: String,
        noDefaultFields: Boolean,
        proj: String,
        groups: String,
        lastloc: Boolean,
        lngUnit: String,
        lngDensity: Double
    ): Double {
        var totalInput = 0.0
        var fuelKey: String? = null
        var pnum = 1

        while (true) {
            val payload = api.fuelConsumed(
                headers = buildHeaders(token),
                pnum = pnum,
                psize = psize,
                noDefaultFields = noDefaultFields,
                proj = proj,
                specIds = specIds,
                groups = groups,
                lastloc = lastloc,
                accId = accId,
                lang = lang
            )

            val rows = iterPayloadRows(payload)
            if (rows.isEmpty()) break

            if (fuelKey == null) {
                val sample = rows.take(10)
                fuelKey = detectFuelKey(sample) ?: error("Could not detect a fuel field.")
            }

            var pageSum = 0.0
            for (row in rows) {
                getValueByDotted(row, fuelKey!!)?.let { pageSum += it }
            }
            totalInput += pageSum

            if (rows.size < psize) break
            pnum += 1
        }

        val totalLngKg = when (lngUnit.lowercase()) {
            "kg" -> totalInput
            "l", "lt", "litre", "liter" -> totalInput * lngDensity
            else -> error("Invalid lngUnit: $lngUnit")
        }
        return (totalLngKg * SAVINGS_PER_KG) / 1000.0
    }

    // ------------------------ Helpers (mirror Python) ------------------------

    /**
     * Yield row-like JsonObjects from common API shapes.
     * Supports: List<Obj>, { result: List/Obj }, { data: List/Obj }, or a single Obj.
     */
    private fun iterPayloadRows(payload: JsonElement): List<JsonObject> = when (payload) {
        is JsonArray -> payload.mapNotNull { it as? JsonObject }
        is JsonObject -> {
            val result = mutableListOf<JsonObject>()
            var matched = false
            for (k in listOf("result", "data")) {
                payload[k]?.let { v ->
                    matched = true
                    when (v) {
                        is JsonArray -> v.forEach { (it as? JsonObject)?.let(result::add) }
                        is JsonObject -> result.add(v)
                        else -> { /* ignore non-object/list containers */ }
                    }
                }
            }
            if (!matched) result.add(payload)
            result
        }
        else -> emptyList()
    }

    private val preferredKeys = listOf(
        "total_fuel_consumed",
        "data.total_fuel_consumed",
        "fuel_consumed",
        "total_fuel",
        "fuel_total",
        "fuel"
    )

    /**
     * Walks all leaves and returns (dottedKey, valueElement).
     * The 'else' branch keeps this exhaustive for sealed JsonElement hierarchy.
     */
    private fun walkKeys(elem: JsonElement, prefix: String = ""): Sequence<Pair<String, JsonElement>> = sequence {
        when (elem) {
            is JsonObject -> for ((k, v) in elem) {
                val nk = if (prefix.isEmpty()) k else "$prefix.$k"
                yieldAll(walkKeys(v, nk))
            }
            is JsonArray -> for (v in elem) yieldAll(walkKeys(v, prefix))
            else -> yield(prefix to elem) // handles JsonPrimitive (JsonLiteral/JsonNull) and any other leaf
        }
    }

    /**
     * Try to find a likely fuel key by preference and fuzzy match.
     */
    private fun detectFuelKey(sampleRows: List<JsonObject>): String? {
        val lowers = buildSet {
            for (row in sampleRows) {
                for ((k, v) in walkKeys(row)) {
                    if (k.isNotBlank() && v is JsonPrimitive) add(k.lowercase())
                }
            }
        }
        for (pref in preferredKeys) if (pref.lowercase() in lowers) return pref
        return lowers.firstOrNull { it.contains("fuel") && (it.contains("consum") || it.contains("total")) }
    }

    /**
     * Retrieve numeric value at dotted path; if strict lookup fails, deep-scan once.
     */
    private fun getValueByDotted(row: JsonObject, dotted: String): Double? {
        fun getStrict(obj: JsonElement?, parts: List<String>, i: Int): JsonElement? {
            if (obj == null) return null
            if (i == parts.size) return obj
            val p = parts[i]
            return (obj as? JsonObject)?.get(p)?.let { getStrict(it, parts, i + 1) }
        }
        val parts = dotted.split(".")
        val strict = getStrict(row, parts, 0)
        val leaf = strict ?: walkKeys(row).firstOrNull { it.first.equals(dotted, ignoreCase = true) }?.second
        return (leaf as? JsonPrimitive)?.toDoubleOrNullRelaxed()
    }

    /**
     * Parse JsonPrimitive to Double:
     *  - numeric primitives
     *  - strings like "1,234.56"
     */
    private fun JsonPrimitive.toDoubleOrNullRelaxed(): Double? {
        return if (isString) {
            this.content.trim().replace(",", "").toDoubleOrNull()
        } else {
            try { this.double } catch (_: Exception) { null }
        }
    }
}
