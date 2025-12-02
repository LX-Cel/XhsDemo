package com.bytedance.xhsdemo

import android.content.Intent
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import coil.load
import coil.transform.CircleCropTransformation
import com.bytedance.xhsdemo.data.ProfileRepository
import com.bytedance.xhsdemo.data.SessionManager
import com.bytedance.xhsdemo.data.local.AppDatabase
import com.bytedance.xhsdemo.databinding.ActivityProfileBinding
import com.bytedance.xhsdemo.ui.profile.ProfileUiState
import com.bytedance.xhsdemo.ui.profile.ProfileViewModel
import com.bytedance.xhsdemo.ui.profile.ProfileViewModelFactory
import com.bytedance.xhsdemo.utils.ToastUtils
import kotlinx.coroutines.launch

// 独立的个人主页 Activity：底部有自己的导航栏，支持从各个 Tab 进入
class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var sessionManager: SessionManager
    // 个人资料 ViewModel，依赖本地 ProfileDao
    private val viewModel: ProfileViewModel by viewModels {
        val db = AppDatabase.getInstance(applicationContext)
        ProfileViewModelFactory(ProfileRepository(db.profileDao()))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // 与主页面保持一致，根据会话中记录的主题模式设置夜间/日间
        sessionManager = SessionManager(this)
        AppCompatDelegate.setDefaultNightMode(sessionManager.getThemeMode())
        super.onCreate(savedInstanceState)
        // 未登录时直接跳转登录页
        if (!sessionManager.isLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 自定义返回键逻辑：优先关闭抽屉，其次再退出 Activity
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        // 绑定各种点击事件和 Tab 切换样式
        setupClicks()
        // 订阅个人资料状态
        observeState()
        // 首次进入时加载数据
        viewModel.load()
        // 默认高亮「我」Tab
        styleTabs()
    }

    // 绑定顶部按钮、底部导航和抽屉菜单的点击事件
    private fun setupClicks() {
        binding.btnDrawer.setOnClickListener { binding.drawerLayout.openDrawer(GravityCompat.START) }
        binding.btnShare.setOnClickListener { toast("分享") }
        binding.btnEdit.setOnClickListener { toast("编辑资料") }
        binding.btnSettings.setOnClickListener { openSettings() }
        binding.quickInspiration.setOnClickListener { toast("创作灵感") }
        binding.quickHistory.setOnClickListener { toast("浏览记录") }
        binding.quickGroup.setOnClickListener { toast("群聊") }

        binding.bottomBarProfile.tabHome.setOnClickListener { navigateToMain(Tab.HOME) }
        binding.bottomBarProfile.tabMarket.setOnClickListener { navigateToMain(Tab.MARKET) }
        binding.bottomBarProfile.tabMessage.setOnClickListener { navigateToMain(Tab.MESSAGE) }
        binding.bottomBarProfile.tabProfile.setOnClickListener { styleTabs() }
        binding.bottomBarProfile.addFab.setOnClickListener {
            startActivity(Intent(this, PublishActivity::class.java))
            applyTransitionOpen()
        }

        binding.itemAddFriend.setOnClickListener { toast(binding.itemAddFriend.text.toString()) }
        binding.itemCreatorCenter.setOnClickListener { toast(binding.itemCreatorCenter.text.toString()) }
        binding.itemDraft.setOnClickListener { toast(binding.itemDraft.text.toString()) }
        binding.itemComments.setOnClickListener { toast(binding.itemComments.text.toString()) }
        binding.itemHistory.setOnClickListener { toast(binding.itemHistory.text.toString()) }
        binding.itemDownload.setOnClickListener { toast(binding.itemDownload.text.toString()) }
        binding.itemOrder.setOnClickListener { toast(binding.itemOrder.text.toString()) }
        binding.itemCart.setOnClickListener { toast(binding.itemCart.text.toString()) }
        binding.itemWallet.setOnClickListener { toast(binding.itemWallet.text.toString()) }
        binding.itemMini.setOnClickListener { toast(binding.itemMini.text.toString()) }
        binding.itemCommunity.setOnClickListener { toast(binding.itemCommunity.text.toString()) }
        binding.itemScan.setOnClickListener { toast(binding.itemScan.text.toString()) }
        binding.itemHelp.setOnClickListener { toast(binding.itemHelp.text.toString()) }
        binding.itemSetting.setOnClickListener { openSettings() }

        binding.tabNote.setOnClickListener { toast("笔记") }
        binding.tabCollect.setOnClickListener { toast("收藏") }
        binding.tabLiked.setOnClickListener { toast("赞过") }
    }

    // 订阅 ViewModel 状态和事件，并渲染到 UI
    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.state.collect { renderState(it) }
                }
                launch {
                    viewModel.events.collect { toast(it) }
                }
            }
        }
    }

    // 渲染个人资料信息（头像、昵称、签名）
    private fun renderState(state: ProfileUiState) {
        state.profile?.let { profile ->
            binding.profileName.text = profile.displayName
            binding.profileSignature.text = profile.signature
            binding.avatarImage.load(profile.avatarUrl) {
                crossfade(true)
                transformations(CircleCropTransformation())
                placeholder(R.drawable.bg_avatar_placeholder)
            }
        }
        binding.emptyContainer.isVisible = true
    }

    // 打开设置页面，沿用统一转场动画
    private fun openSettings() {
        startActivity(Intent(this, SettingsActivity::class.java))
        applyTransitionOpen()
    }

    // 显示统一 Toast
    private fun toast(msg: String) {
        ToastUtils.show(this, msg)
    }

    // 重写 finish，加上关闭时的动画
    override fun finish() {
        super.finish()
        applyTransitionClose()
    }

    // 从个人页跳转回主页面，携带目标 Tab 信息
    private fun navigateToMain(tab: Tab) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("target_tab", tab.name)
        }
        startActivity(intent)
        applyTransitionClose()
    }

    // 重置底部导航四个 Tab 的样式，并高亮当前页面
    private fun styleTabs() {
        styleTab(binding.bottomBarProfile.tabHome, false)
        styleTab(binding.bottomBarProfile.tabMarket, false)
        styleTab(binding.bottomBarProfile.tabMessage, false)
        styleTab(binding.bottomBarProfile.tabProfile, true)
    }

    // 设置单个 Tab 的文字颜色、粗细和字号
    private fun styleTab(textView: TextView, selected: Boolean) {
        val color = ContextCompat.getColor(this, if (selected) R.color.black else R.color.xhs_gray)
        textView.setTextColor(color)
        textView.setTypeface(null, if (selected) Typeface.BOLD else Typeface.NORMAL)
        textView.textSize = if (selected) 13f else 12f
    }

    // 进入新页面时的转场动画
    private fun applyTransitionOpen() {
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

    // 关闭当前页面时的转场动画
    private fun applyTransitionClose() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(
                OVERRIDE_TRANSITION_CLOSE,
                R.anim.slide_in_left,
                R.anim.slide_out_right
            )
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }
    }

    private enum class Tab { HOME, MARKET, MESSAGE, PROFILE }

    // 捕获全局点击事件，按下时取消正在显示的 Toast
    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (ev?.action == MotionEvent.ACTION_DOWN) {
            ToastUtils.cancel()
        }
        return super.dispatchTouchEvent(ev)
    }
}
