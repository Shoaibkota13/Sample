package com.example.weatherapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.weatherapp.Network.WeatherService
import com.example.weatherapp.databinding.ActivityMainBinding
import com.example.weatherapp.models.WeatherResponse
import com.google.android.gms.location.*
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone


class MainActivity : AppCompatActivity() {
    private lateinit var mfusedlocationclient: FusedLocationProviderClient
    private var mdialog: Dialog? = null
    private var isWeatherApiCalled: Boolean = false // Prevent repeated API calls
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        mfusedlocationclient = LocationServices.getFusedLocationProviderClient(this)

        if (!isLocationEnabled()) {
            Toast.makeText(this, "Location is not enabled", Toast.LENGTH_LONG).show()
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        } else {
            Dexter.withActivity(this@MainActivity)
                .withPermissions(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
                .withListener(object : MultiplePermissionsListener {
                    @RequiresApi(Build.VERSION_CODES.S)
                    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                        if (report!!.areAllPermissionsGranted()) {
                            requestLocationData()
                        }
                        if (report.isAnyPermissionPermanentlyDenied) {
                            Toast.makeText(
                                this@MainActivity,
                                "Permission denied permanently",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }

                    override fun onPermissionRationaleShouldBeShown(
                        permissions: MutableList<PermissionRequest>?,
                        token: PermissionToken?
                    ) {
                        showRationalDialogForPermission()
                    }
                }).onSameThread().check()
        }
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    @SuppressLint("MissingPermission")
    @RequiresApi(Build.VERSION_CODES.S)
    private fun requestLocationData() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            1000
        ).build()

        mfusedlocationclient.requestLocationUpdates(
            locationRequest,
            mLocationCallback,
            Looper.getMainLooper()
        )
    }

    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult)
            val location = locationResult.lastLocation
            if (location != null && !isWeatherApiCalled) {
                isWeatherApiCalled = true
                val latitude = location.latitude
                val longitude = location.longitude

                Log.e("longitude", "$longitude")
                Log.e("latitude", "$latitude")

                getweatherdetails(latitude, longitude)

                // Stop location updates to avoid repeated calls
                mfusedlocationclient.removeLocationUpdates(this)
            }
        }
    }

    private fun getweatherdetails(latitude: Double, longitude: Double) {
        if (Constants.isNetworkAvailable(this)) {
            val retrofit: Retrofit = Retrofit.Builder()
                .baseUrl(Constants.baseurl)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val service: WeatherService = retrofit.create(WeatherService::class.java)
            val listcall: Call<WeatherResponse> = service.getweather(
                latitude, longitude, Constants.metric_unit, Constants.appid
            )

            showcustomprogressDialog()
            listcall.enqueue(object : Callback<WeatherResponse> {
                override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                    hideprogress()
                    Log.e("Error", "Bad connection")
                }

                override fun onResponse(
                    call: Call<WeatherResponse>,
                    response: Response<WeatherResponse>
                ) {
                    hideprogress()
                    if (response.isSuccessful) {
                        val weatherlist: WeatherResponse? = response.body()
                        if (weatherlist != null) {
                            setupUi(weatherlist)
                        }
                       Log.e("df","$weatherlist")
                    } else {
                        val rc = response.code()
                        when (rc) {
                            400 -> Log.e("Error", "Bad request")

                        }
                    }
                }
            })
        } else {
            Toast.makeText(this, "Network not available", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showRationalDialogForPermission() {
        AlertDialog.Builder(this)
            .setMessage("It seems permissions are disabled. Please enable them in settings.")
            .setPositiveButton("Go to Settings") { _, _ ->
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }.show()
    }

    private fun showcustomprogressDialog() {
        mdialog = Dialog(this)
        mdialog!!.setContentView(R.layout.progress_bar)
        mdialog!!.show()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu,menu)
        return super.onCreateOptionsMenu(menu)
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId){
            R.id.action_refresh->{
                requestLocationData()
                true
            }

            else -> super.onOptionsItemSelected(item)

        }
    }





    private fun hideprogress() {
        mdialog?.dismiss()
    }

    private fun setupUi(weatherList: WeatherResponse) {
        for (i in weatherList.weather.indices) {
            binding.tvMain.text = weatherList.weather[i].main
            binding.tvDesc.text = weatherList.weather[i].desc
            if (binding.tvDesc.text.isNullOrEmpty()) {
                binding.tvDesc.text = "10"
            }

            binding.tvDegree.text = weatherList.main.temp.toString() + getunit(application.resources.configuration.locales.toString())
            binding.tvSunriseTime.text = unixtime(weatherList.sys.sunrise)
            binding.tvSunsetTime.text = unixtime(weatherList.sys.sunset)
            binding.tvCountry.text=weatherList.sys.country
            binding.tvMax.text = weatherList.main.temp_max.toString() + "Max"
            binding.tvMin.text=weatherList.main.temp_min.toString() + "Min"
            binding.tvWind.text = weatherList.wind.deg.toString()
            binding.tvWindspeed.text = weatherList.wind.speed.toString() + "miles/hour"


            when(weatherList.weather[i].icon){
                "01n"-> binding.ivMain.setImageResource(R.drawable.sunny)
                "02d"-> binding.ivMain.setImageResource(R.drawable.cloud)
                "03d"-> binding.ivMain.setImageResource(R.drawable.cloud)
                "13n"-> binding.ivMain.setImageResource(R.drawable.snowflake)

            }


        }


    }

    private fun getunit(value: String): String {
        var value = "C"
        if("US"==value || "LR"==value || "MM"==value){
            value ="F"
        }
        return value
    }


    private fun unixtime(Timex:Long):String?{
        val date = Date(Timex * 1000)
        val sdf = SimpleDateFormat("HH:mm")
        sdf.timeZone = TimeZone.getDefault()
        return sdf.format(date)
    }

    override fun onDestroy() {
        super.onDestroy()
        mfusedlocationclient.removeLocationUpdates(mLocationCallback)
        hideprogress()
    }
}


