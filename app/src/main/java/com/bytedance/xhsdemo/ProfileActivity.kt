package com.bytedance.xhsdemo

import android.content.Intent
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
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
import kotlinx.coroutines.launch

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var sessionManager: SessionManager
    private val viewModel: ProfileViewModel by viewModels {
        val db = AppDatabase.getInstance(applicationContext)
        ProfileViewModelFactory(ProfileRepository(db.profileDao()))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        sessionManager = SessionManager(this)
        AppCompatDelegate.setDefaultNightMode(sessionManager.getThemeMode())
        super.onCreate(savedInstanceState)
        if (!sessionManager.isLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

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

        setupClicks()
        observeState()
        viewModel.load()
        styleTabs()
    }

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

    private fun openSettings() {
        startActivity(Intent(this, SettingsActivity::class.java))
        applyTransitionOpen()
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    override fun finish() {
        super.finish()
        applyTransitionClose()
    }

    private fun navigateToMain(tab: Tab) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("target_tab", tab.name)
        }
        startActivity(intent)
        applyTransitionClose()
    }

    private fun styleTabs() {
        styleTab(binding.bottomBarProfile.tabHome, false)
        styleTab(binding.bottomBarProfile.tabMarket, false)
        styleTab(binding.bottomBarProfile.tabMessage, false)
        styleTab(binding.bottomBarProfile.tabProfile, true)
    }

    private fun styleTab(textView: TextView, selected: Boolean) {
        val color = ContextCompat.getColor(this, if (selected) R.color.black else R.color.xhs_gray)
        textView.setTextColor(color)
        textView.setTypeface(null, if (selected) Typeface.BOLD else Typeface.NORMAL)
        textView.textSize = if (selected) 13f else 12f
    }

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
}
