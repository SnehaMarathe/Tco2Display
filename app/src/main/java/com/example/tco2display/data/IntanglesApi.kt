package com.example.tco2display.data

import kotlinx.serialization.json.JsonElement
import retrofit2.http.GET
import retrofit2.http.HeaderMap
import retrofit2.http.Query

interface IntanglesApi {
    @GET("/vehicle/fuel_consumed")
    suspend fun fuelConsumed(
        @HeaderMap headers: Map<String, String>,
        @Query("pnum") pnum: Int,
        @Query("psize") psize: Int,
        @Query("no_default_fields") noDefaultFields: Boolean,
        @Query("proj") proj: String,
        @Query("spec_ids") specIds: String,
        @Query("groups") groups: String,
        @Query("lastloc") lastloc: Boolean,
        @Query("acc_id") accId: String,
        @Query("lang") lang: String
    ): JsonElement
}
