package com.bytedance.xhsdemo

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.viewpager2.widget.ViewPager2
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.GravityCompat
import android.widget.Toast
import androidx.drawerlayout.widget.DrawerLayout
import android.view.View
import android.view.MotionEvent
import com.bytedance.xhsdemo.data.SessionManager
import com.bytedance.xhsdemo.databinding.ActivityMainBinding
import com.bytedance.xhsdemo.utils.ToastUtils
import com.bytedance.xhsdemo.ui.home.HomeFragment
import com.bytedance.xhsdemo.ui.placeholder.PlaceholderFragment
import com.bytedance.xhsdemo.ui.profile.ProfilePageFragment
import com.bytedance.xhsdemo.ui.weather.WeatherFragment
import com.google.android.material.navigation.NavigationBarView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.activity.viewModels
import androidx.activity.result.contract.ActivityResultContracts
import com.bytedance.xhsdemo.ui.PostListViewModel
import com.bytedance.xhsdemo.model.Post

// 应用主界面：负责底部导航 + ViewPager + 侧边抽屉整体框架
class MainActivity : AppCompatActivity() {

    // 和首页共享的帖子列表 ViewModel，用于接收发布页返回的新帖子
    private val homeViewModel: PostListViewModel by viewModels()

    // 发布页面返回结果的注册器：用于接收新发布的 Post
    private val publishLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val post = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                result.data?.getParcelableExtra(PublishActivity.EXTRA_NEW_POST, Post::class.java)
            } else {
                @Suppress("DEPRECATION")
                result.data?.getParcelableExtra(PublishActivity.EXTRA_NEW_POST)
            }
            post?.let {
                homeViewModel.addPost(it)
                binding.viewPager.currentItem = 0
            }
        }
    }

    // ViewBinding 引用，用于访问布局中的 View
    private lateinit var binding: ActivityMainBinding
    // 会话管理：保存登录状态、主题模式等
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        // 先根据本地配置设置主题模式，避免 Activity 闪烁
        sessionManager = SessionManager(this)
        AppCompatDelegate.setDefaultNightMode(sessionManager.getThemeMode())
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        // 未登录时直接跳转登录页，拦截后续 UI 初始化
        if (!sessionManager.isLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 统一处理状态栏/导航栏 Insets，保证沉浸式效果
        applyInsets()
        // 初始化首页四个 Tab 的 ViewPager
        setupViewPager()
        // 初始化底部导航点击事件
        setupBottomNav()
        // 初始化侧边抽屉菜单
        setupDrawer()
    }

    // 初始化侧边抽屉逻辑：包括遮罩颜色和打开/关闭时对底部导航的禁用
    private fun setupDrawer() {
        binding.drawerLayout.setScrimColor(Color.TRANSPARENT)
        binding.drawerLayout.addDrawerListener(object : DrawerLayout.SimpleDrawerListener() {
            override fun onDrawerOpened(drawerView: View) {
                setNavigationEnabled(false)
            }

            override fun onDrawerClosed(drawerView: View) {
                setNavigationEnabled(true)
            }
        })

        // drawerMenu 是侧边栏内容区域，统一在这里绑定点击事件
        with(binding.drawerMenu) {
            itemSetting.setOnClickListener {
                startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
                binding.drawerLayout.closeDrawer(GravityCompat.START)
            }
            // 公共点击监听：从 View 中提取文案并以 Toast 方式提示
            val toastListener = View.OnClickListener { v ->
                val text = when (v) {
                    is android.widget.TextView -> v.text
                    is android.view.ViewGroup -> {
                        var foundText: CharSequence? = null
                        for (i in 0 until v.childCount) {
                            val child = v.getChildAt(i)
                            if (child is android.widget.TextView) {
                                foundText = child.text
                                break
                            }
                        }
                        foundText
                    }
                    else -> null
                }
                text?.let { ToastUtils.show(this@MainActivity, it.toString()) }
            }
            itemAddFriend.setOnClickListener(toastListener)
            itemCreatorCenter.setOnClickListener(toastListener)
            itemDraft.setOnClickListener(toastListener)
            itemComments.setOnClickListener(toastListener)
            itemHistory.setOnClickListener(toastListener)
            itemDownload.setOnClickListener(toastListener)
            itemOrder.setOnClickListener(toastListener)
            itemCart.setOnClickListener(toastListener)
            itemWallet.setOnClickListener(toastListener)
            itemMini.setOnClickListener(toastListener)
            itemCommunity.setOnClickListener(toastListener)
            itemScan.setOnClickListener(toastListener)
            itemHelp.setOnClickListener(toastListener)
        }
    }

    // 供首页标题栏按钮调用，打开左侧抽屉
    fun openDrawer() {
        binding.drawerLayout.openDrawer(GravityCompat.START)
    }

    // 初始化 ViewPager2，承载首页、天气、消息占位页、个人中心四个 Fragment
    private fun setupViewPager() {
        binding.viewPager.adapter = MainPagerAdapter(
            this,
            listOf(
                HomeFragment(),
                WeatherFragment(),
                PlaceholderFragment.newInstance("消息页面敬请期待"),
                ProfilePageFragment()
            )
        )
        // 禁用左右滑动切换，统一由底部导航控制
        binding.viewPager.isUserInputEnabled = false
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateBottomNavState(position)
            }
        })
    }

    // 底部导航按钮点击事件，将 ViewPager 切换到对应页面
    private fun setupBottomNav() {
        binding.btnHome.setOnClickListener { binding.viewPager.currentItem = 0 }
        binding.btnMarket.setOnClickListener { binding.viewPager.currentItem = 1 }
        binding.btnPublish.setOnClickListener { openPublish() }
        binding.btnMessage.setOnClickListener { binding.viewPager.currentItem = 2 }
        binding.btnMe.setOnClickListener { binding.viewPager.currentItem = 3 }
    }

    // 根据当前选中的页面高亮对应底部导航文案
    private fun updateBottomNavState(position: Int) {
        val activeColor = getColor(R.color.black)
        val inactiveColor = getColor(R.color.xhs_gray)
        val activeStyle = android.graphics.Typeface.BOLD
        val inactiveStyle = android.graphics.Typeface.NORMAL

        // Reset all
        listOf(binding.btnHome, binding.btnMarket, binding.btnMessage, binding.btnMe).forEach {
            it.setTextColor(inactiveColor)
            it.typeface = android.graphics.Typeface.defaultFromStyle(inactiveStyle)
        }

        // Set active
        val activeView = when (position) {
            0 -> binding.btnHome
            1 -> binding.btnMarket
            2 -> binding.btnMessage
            3 -> binding.btnMe
            else -> null
        }
        activeView?.setTextColor(activeColor)
        activeView?.typeface = android.graphics.Typeface.defaultFromStyle(activeStyle)
    }

    // 处理系统状态栏/导航栏 Insets，把底部安全区域和抽屉状态栏占位补齐
    private fun applyInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.drawerLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.bottomNavLayout.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.drawerMenu.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.drawerMenu.drawerStatusBarSpace.layoutParams = binding.drawerMenu.drawerStatusBarSpace.layoutParams.apply {
                height = systemBars.top
            }
            insets
        }
    }

    // 打开发布页面，并根据系统版本使用合适的转场动画
    private fun openPublish() {
        publishLauncher.launch(Intent(this, PublishActivity::class.java))
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

    // 主界面 ViewPager 的 Fragment 适配器
    private class MainPagerAdapter(
        activity: FragmentActivity,
        private val fragments: List<Fragment>
    ) : FragmentStateAdapter(activity) {
        override fun getItemCount(): Int = fragments.size
        override fun createFragment(position: Int): Fragment = fragments[position]
    }

    // 控制底部导航区域是否可交互（在抽屉打开时禁用）
    fun setNavigationEnabled(enabled: Boolean) {
        binding.bottomNavLayout.isEnabled = enabled
        binding.btnHome.isEnabled = enabled
        binding.btnMarket.isEnabled = enabled
        binding.btnPublish.isEnabled = enabled
        binding.btnMessage.isEnabled = enabled
        binding.btnMe.isEnabled = enabled
    }

    // 供外部在特殊场景下重置抽屉状态（解锁并关闭）
    fun resetDrawerState() {
        binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
        binding.drawerLayout.closeDrawer(GravityCompat.START, false)
        setNavigationEnabled(true)
    }

    // 统一处理点击事件：按下任意位置时，取消全局 Toast，避免长时间悬浮
    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (ev?.action == MotionEvent.ACTION_DOWN) {
            ToastUtils.cancel()
        }
        return super.dispatchTouchEvent(ev)
    }
}
