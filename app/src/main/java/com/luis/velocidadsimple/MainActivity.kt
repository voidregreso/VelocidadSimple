package com.luis.velocidadsimple

import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.Manifest
import android.content.res.Configuration
import android.graphics.Color
import android.location.LocationListener
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.app.ActivityCompat
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.luis.velocidadsimple.databinding.ActivityMainBinding
import java.util.*

class MainActivity : AppCompatActivity(), LocationListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var locationManager: LocationManager

    private var maxSpeed = 0f // Velocidad máxima
    private var sumSpeed = 0f // Velocidad total
    private var countSpeed = 0 // Número de velocidades (i.e. número de mediciones de velocidad)
    private var firstCount = false
    private var startTime: Long = 0

    private lateinit var lineChart: LineChart
    private val speedData = ArrayList<Entry>() // Datos de velocidad para la curva de línea

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        checkLocationPermission()
        setupLineChart()
    }

    private fun checkLocationPermission() {
        val permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, permissions, 1)
            return
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, this)
    }

    private fun setupLineChart() {
        lineChart = binding.lineChart
        lineChart.description = Description().apply {
            text = "Velocidad (km/h) vs. Tiempo (mm:ss)"
            textColor = Color.RED
            textSize = 16f
        }
        lineChart.xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            labelCount = 5
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    val seconds = value / 1000
                    val minutes = seconds / 60
                    val remainingSeconds = seconds % 60
                    return "%02d:%02d".format(minutes.toInt(), remainingSeconds.toInt())
                }
            }
            textColor = getCharTextColor()
        }
        lineChart.axisLeft.apply {
            axisMinimum = 0f
            axisMaximum = 60f
            setDrawGridLines(true)
            setDrawAxisLine(false)
            textSize = 12f
            textColor = getCharTextColor()
        }
        lineChart.axisRight.isEnabled = false
        lineChart.legend.isEnabled = false
        lineChart.setTouchEnabled(false)
    }

    private fun getCharTextColor(): Int {
        val nightModeFlags = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return when (nightModeFlags) {
            Configuration.UI_MODE_NIGHT_YES -> Color.CYAN
            else -> Color.BLUE
        }
    }

    override fun onLocationChanged(location: Location) {
        val speed = location.speed * 3.6f // convert m/s to km/h
        binding.speedText.text = String.format(Locale.getDefault(), "%.2f km/h", speed)

        if(!firstCount) {
            firstCount = true
            startTime = System.currentTimeMillis()
        }
        val endTime = System.currentTimeMillis()

        // Actualizar la velo. max.
        if (speed > maxSpeed) {
            maxSpeed = speed
            binding.maxSpeedText.text = String.format(Locale.getDefault(), "Max speed: %.2f km/h", maxSpeed)
            lineChart.axisLeft.axisMaximum = maxSpeed * 1.2f
        }

        // Actualizar la velo. avg.
        sumSpeed += speed
        countSpeed++
        val avgSpeed = sumSpeed / countSpeed
        binding.avgSpeedText.text = String.format(Locale.getDefault(), "Avg speed: %.2f km/h", avgSpeed)

        // Agregar datos a la curva de línea
        speedData.add(Entry((endTime - startTime).toFloat(), speed))
        val dataSet = LineDataSet(speedData, "Speed Data")

        // Configurar la apariencia de la curva de línea
        dataSet.color = resources.getColor(R.color.purple_500)
        dataSet.setCircleColor(resources.getColor(R.color.purple_500))
        dataSet.lineWidth = 2f
        dataSet.circleRadius = 1f
        dataSet.setDrawCircleHole(false)
        dataSet.setDrawValues(false)
        dataSet.mode = LineDataSet.Mode.CUBIC_BEZIER

        // Actualizar el gráfico de línea
        lineChart.data = LineData(dataSet)
        lineChart.invalidate()
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

    override fun onProviderEnabled(provider: String) {}

    override fun onProviderDisabled(provider: String) {}
}