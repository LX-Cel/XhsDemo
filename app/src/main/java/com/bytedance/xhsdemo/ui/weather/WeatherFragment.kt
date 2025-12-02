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

// 天气页面 Fragment：通过高德天气接口展示多个城市的实时天气
class WeatherFragment : Fragment() {

    private var _binding: FragmentWeatherBinding? = null
    private val binding get() = _binding!!

    // 高德地图开放平台的 key
    private val AMAP_KEY = "b39566ed5bd4bb9742af1e19c0d04961"

    // 城市名称到行政编码的映射
    private val cities = mapOf(
        "北京" to "110000",
        "上海" to "310000",
        "广州" to "440100",
        "深圳" to "440300"
    )

    // 当前选中的城市
    private var currentCity = "上海"

    // Retrofit 客户端和 WeatherService 接口
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
        // 初始化顶部城市 Tab，并默认加载当前城市天气
        setupTabs()
        loadWeather(currentCity)

        binding.tvError.setOnClickListener {
            loadWeather(currentCity)
        }
    }

    // 处理状态栏 Insets，高度写入占位 View
    private fun applyInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.statusBarSpace) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.layoutParams.height = systemBars.top
            v.requestLayout()
            insets
        }
    }

    // 初始化城市 Tab 点击事件
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

    // 根据当前选中的城市更新 Tab 文本样式
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

    // 根据城市名加载天气数据：控制 loading / error / 内容区域显示
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
    // 将接口返回的天气实况数据渲染到 UI
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

    // 显示错误提示文案
    private fun showError() {
        binding.tvError.isVisible = true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
