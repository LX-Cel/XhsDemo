package com.bytedance.xhsdemo

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.bytedance.xhsdemo.data.SessionManager
import com.bytedance.xhsdemo.databinding.ActivitySettingsBinding
import com.bytedance.xhsdemo.databinding.ItemSettingsRowBinding
import com.bytedance.xhsdemo.utils.ToastUtils

// 设置页：集中展示账号、通知、隐私等入口，并提供退出登录能力
class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    // 复用会话管理，支持在设置页更新登录状态
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        // 应用当前主题模式
        sessionManager = SessionManager(this)
        AppCompatDelegate.setDefaultNightMode(sessionManager.getThemeMode())
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 适配状态栏高度并初始化各个设置项
        applyInsets()
        setupViews()

        binding.btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        binding.btnLogout.setOnClickListener { logoutAndBack() }
        binding.btnSwitchAccount.setOnClickListener { logoutAndBack() }
    }

    // 为顶部状态栏占位 View 设置高度
    private fun applyInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.statusBarSpace) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.layoutParams.height = systemBars.top
            v.requestLayout()
            insets
        }
    }

    // 填充设置列表的每一行文案、图标以及点击行为
    private fun setupViews() {
        // Group 1
        setupItem(binding.itemAccount, R.drawable.ic_settings_account, "账号与安全")
        setupItem(binding.itemGeneral, R.drawable.ic_settings, "通用设置")
        setupItem(binding.itemNotification, R.drawable.ic_settings_notification, "通知设置")
        setupItem(binding.itemPrivacy, R.drawable.ic_settings_privacy, "隐私设置")

        // Group 2
        setupItem(binding.itemStorage, R.drawable.ic_settings_storage, "存储空间", "2.17 GB")
        setupItem(binding.itemContent, R.drawable.ic_settings_content, "内容偏好调节")
        setupItem(binding.itemAddress, R.drawable.ic_settings_location, "收货地址")
        setupItem(binding.itemWidget, R.drawable.ic_settings_widget, "添加小组件")
        setupItem(binding.itemMinor, R.drawable.ic_settings_minor, "未成年人模式", "未开启")

        // Group 3
        setupItem(binding.itemLab, R.drawable.ic_settings_lab, "新功能体验")

        // Group 4
        setupItem(binding.itemHelp, R.drawable.ic_headset, "帮助与客服")
        setupItem(binding.itemAbout, R.drawable.ic_settings_about, "关于小红书")
    }

    // 统一初始化单行设置项：设置图标、标题、副标题，并在点击时弹出 Toast
    private fun setupItem(
        itemBinding: ItemSettingsRowBinding,
        iconRes: Int,
        title: String,
        value: String? = null
    ) {
        itemBinding.itemIcon.setImageResource(iconRes)
        itemBinding.itemTitle.text = title
        itemBinding.itemValue.text = value ?: ""
        itemBinding.root.setOnClickListener {
            ToastUtils.show(this, title)
        }
    }

    // 退出当前账号，并跳转回登录页，清空任务栈
    private fun logoutAndBack() {
        sessionManager.setLoggedIn(false)
        val intent = Intent(this, LoginActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finishAffinity()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(
                OVERRIDE_TRANSITION_OPEN,
                R.anim.slide_in_right,
                R.anim.slide_out_left
            )
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
    }

    // 捕获触摸事件以在按下时取消当前 Toast
    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (ev?.action == MotionEvent.ACTION_DOWN) {
            ToastUtils.cancel()
        }
        return super.dispatchTouchEvent(ev)
    }
}
