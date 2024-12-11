package com.example.weatherapp.models

import java.io.Serializable

data class WeatherList (
    val id:Int,
    val main:String,
    val desc :String,
    val icon :String
):Serializable