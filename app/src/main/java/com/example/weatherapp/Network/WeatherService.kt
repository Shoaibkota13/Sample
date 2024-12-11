package com.example.weatherapp.Network

import com.example.weatherapp.models.WeatherResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherService {

    @GET("2.5/weather")
    fun getweather(
        @Query("lat") lat:Double,
        @Query("lon") lon:Double,
        @Query("units") units :String?,
        @Query("appid") appid:String?,
    ) : Call<WeatherResponse>
}