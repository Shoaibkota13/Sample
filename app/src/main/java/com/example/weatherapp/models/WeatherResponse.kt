package com.example.weatherapp.models

import java.io.Serializable

data class WeatherResponse(
    val coord :Coord,
    val weather :List<WeatherList>,
    val base:String,
    val main :Main,
    val visibility :Int,
    val wind :wind,
    val clouds:clouds,
    val dt :Int,
    val sys :Sys,
    val id:Int,
    val name:String,
    val cod:Int

):Serializable