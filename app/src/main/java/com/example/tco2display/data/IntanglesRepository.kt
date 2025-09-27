package com.example.tco2display.data

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.*
import okhttp3.ConnectionPool
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

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

    private fun baseHeaders(token: String) = mutableMapOf(
        "Accept" to "application/json, text/plain, */*",
        "intangles-session-type" to "web",
        "intangles-user-lang" to "en",
        "intangles-user-token" to token,
        "intangles-user-tz" to "Asia/Calcutta",
        "Referer" to referer,
        "Origin" to origin,
        "User-Agent" to "android-okhttp/4.x",
        // OkHttp adds gzip by default; adding here is harmless
        "Accept-Encoding" to "gzip"
    )

    private val client: OkHttpClient by lazy {
        val log = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        OkHttpClient.Builder()
            .addInterceptor(log)
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(7, TimeUnit.SECONDS)
            .writeTimeout(7, TimeUnit.SECONDS)
            .callTimeout(12, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .connectionPool(ConnectionPool(5, 30, TimeUnit.SECONDS))
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

    // ---------- Caches to avoid re-work on each 5s poll ----------
    private var cachedFuelKey: String? = null

    private data class PageCache(
        val etag: String?,
        val sum: Double,
        val rowsSize: Int
    )
    private val pageCache = mutableMapOf<Int, PageCache>()

    /**
     * Streams pages and returns TOTAL tCO2 saved (in tonnes).
     * Fast path improvements:
     *  - Cache fuel key once.
     *  - Conditional GET per page via ETag (If-None-Match) – skips unchanged pages.
     *  - Keep per-page sum to avoid reparsing.
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
        var pnum = 1

        while (true) {
            // Prepare headers with conditional ETag for this page (if known)
            val headers = baseHeaders(token)
            pageCache[pnum]?.etag?.let { headers["If-None-Match"] = it }

            val resp = api.fuelConsumed(
                headers = headers,
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

            var rowsSizeForStopCheck: Int
            var pageSum: Double

            if (resp.code() == 304) {
                // Unchanged page – use cached sum/size
                val cached = pageCache[pnum]
                if (cached != null) {
                    pageSum = cached.sum
                    rowsSizeForStopCheck = cached.rowsSize
                } else {
                    // Shouldn't happen, fallback to 0 for safety
                    pageSum = 0.0
                    rowsSizeForStopCheck = psize // keep scanning
                }
            } else {
                resp.errorBody()?.let {
                    throw IllegalStateException("HTTP ${resp.code()} ${it.string().take(200)}")
                }
                val body = resp.body() ?: JsonNull

                val rows = iterPayloadRows(body)
                rowsSizeForStopCheck = rows.size

                // Detect fuel key once (first successful page)
                if (cachedFuelKey == null) {
                    val sample = rows.take(10)
                    cachedFuelKey = detectFuelKey(sample)
                        ?: error("Could not detect a fuel field.")
                }

                // Sum page
                pageSum = 0.0
                val key = cachedFuelKey!!
                for (row in rows) getValueByDotted(row, key)?.let { pageSum += it }

                // Cache page (sum + rows size + ETag if provided)
                val etag = resp.headers()["ETag"]
                pageCache[pnum] = PageCache(etag = etag, sum = pageSum, rowsSize = rowsSizeForStopCheck)
            }

            totalInput += pageSum

            // pagination end?
            if (rowsSizeForStopCheck < psize) break
            pnum += 1
        }

        // Convert to kg and compute tCO2 saved
        val totalLngKg = when (lngUnit.lowercase()) {
            "kg" -> totalInput
            "l", "lt", "litre", "liter" -> totalInput * lngDensity
            else -> error("Invalid lngUnit: $lngUnit")
        }
        return (totalLngKg * SAVINGS_PER_KG) / 1000.0
    }

    // ------------------------ Helpers (mirror Python) ------------------------

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
                        else -> { /* ignore other types */ }
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

    private fun walkKeys(elem: JsonElement, prefix: String = ""): Sequence<Pair<String, JsonElement>> = sequence {
        when (elem) {
            is JsonObject -> for ((k, v) in elem) {
                val nk = if (prefix.isEmpty()) k else "$prefix.$k"
                yieldAll(walkKeys(v, nk))
            }
            is JsonArray -> for (v in elem) yieldAll(walkKeys(v, prefix))
            else -> yield(prefix to elem)
        }
    }

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

    private fun getValueByDotted(row: JsonObject, dotted: String): Double? {
        fun getStrict(obj: JsonElement?, parts: List<String>, i: Int): JsonElement? {
            if (obj == null) return null
            if (i == parts.size) return obj
            return (obj as? JsonObject)?.get(parts[i])?.let { getStrict(it, parts, i + 1) }
        }
        val parts = dotted.split(".")
        val strict = getStrict(row, parts, 0)
        val leaf = strict ?: walkKeys(row).firstOrNull { it.first.equals(dotted, ignoreCase = true) }?.second
        return (leaf as? JsonPrimitive)?.toDoubleOrNullRelaxed()
    }

    private fun JsonPrimitive.toDoubleOrNullRelaxed(): Double? {
        return if (isString) content.trim().replace(",", "").toDoubleOrNull()
        else try { double } catch (_: Exception) { null }
    }
}
