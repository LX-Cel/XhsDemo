package com.bytedance.xhsdemo.ui.weather

import retrofit2.http.GET
import retrofit2.http.Query

// 高德天气接口定义：封装当前天气实况查询
interface WeatherService {
    @GET("v3/weather/weatherInfo")
    suspend fun getWeather(
        @Query("city") city: String,
        @Query("key") key: String,
        @Query("extensions") extensions: String = "base"
    ): WeatherResponse
}
