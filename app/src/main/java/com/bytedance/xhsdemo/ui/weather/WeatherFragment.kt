package com.bytedance.xhsdemo.ui.weather

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bytedance.xhsdemo.databinding.FragmentWeatherBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import androidx.core.graphics.toColorInt

class WeatherFragment : Fragment() {

    private var _binding: FragmentWeatherBinding? = null
    private val binding get() = _binding!!

    private val AMAP_KEY = "b39566ed5bd4bb9742af1e19c0d04961"

    private val cities = mapOf(
        "北京" to "110000",
        "上海" to "310000",
        "广州" to "440100",
        "深圳" to "440300"
    )

    private var currentCity = "上海"

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://restapi.amap.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val service = retrofit.create(WeatherService::class.java)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWeatherBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        applyInsets()
        setupTabs()
        loadWeather(currentCity)

        binding.tvError.setOnClickListener {
            loadWeather(currentCity)
        }
    }

    private fun applyInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.statusBarSpace) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.layoutParams.height = systemBars.top
            v.requestLayout()
            insets
        }
    }

    private fun setupTabs() {
        val tabs = mapOf(
            "北京" to binding.tabBeijing,
            "上海" to binding.tabShanghai,
            "广州" to binding.tabGuangzhou,
            "深圳" to binding.tabShenzhen
        )

        tabs.forEach { (city, view) ->
            view.setOnClickListener {
                if (currentCity != city) {
                    currentCity = city
                    updateTabStyles(tabs)
                    loadWeather(city)
                }
            }
        }
        updateTabStyles(tabs)
    }

    private fun updateTabStyles(tabs: Map<String, TextView>) {
        tabs.forEach { (city, view) ->
            if (city == currentCity) {
                view.setTextColor("#333333".toColorInt())
                view.setTypeface(null, Typeface.BOLD)
                view.textSize = 18f
            } else {
                view.setTextColor("#999999".toColorInt())
                view.setTypeface(null, Typeface.NORMAL)
                view.textSize = 16f
            }
        }
    }

    private fun loadWeather(city: String) {
        val adcode = cities[city] ?: return
        binding.progressBar.isVisible = true
        binding.weatherCard.isVisible = false
        binding.tvError.isVisible = false

        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    service.getWeather(adcode, AMAP_KEY)
                }
                if (response.status == "1" && !response.lives.isNullOrEmpty()) {
                    updateUI(response.lives[0])
                } else {
                    showError()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                showError()
            } finally {
                binding.progressBar.isVisible = false
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateUI(live: WeatherLive) {
        binding.weatherCard.isVisible = true
        binding.tvCity.text = live.city
        binding.tvTemperature.text = "${live.temperature}°"
        binding.tvWeather.text = live.weather
        binding.tvWindDirection.text = "${live.winddirection}风"
        binding.tvWindPower.text = "${live.windpower}级"
        binding.tvHumidity.text = "${live.humidity}%"
        binding.tvReportTime.text = "更新于 ${live.reporttime}"
    }

    private fun showError() {
        binding.tvError.isVisible = true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
