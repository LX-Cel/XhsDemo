package com.bytedance.xhsdemo.ui.weather

import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherService {
    @GET("v3/weather/weatherInfo")
    suspend fun getWeather(
        @Query("city") city: String,
        @Query("key") key: String,
        @Query("extensions") extensions: String = "base"
    ): WeatherResponse
}
