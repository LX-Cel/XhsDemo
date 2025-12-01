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
import com.google.android.material.navigation.NavigationBarView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.activity.viewModels
import androidx.activity.result.contract.ActivityResultContracts
import com.bytedance.xhsdemo.ui.PostListViewModel
import com.bytedance.xhsdemo.model.Post

class MainActivity : AppCompatActivity() {

    private val homeViewModel: PostListViewModel by viewModels()

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

    private lateinit var binding: ActivityMainBinding
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        sessionManager = SessionManager(this)
        AppCompatDelegate.setDefaultNightMode(sessionManager.getThemeMode())
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (!sessionManager.isLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applyInsets()
        setupViewPager()
        setupBottomNav()
        setupDrawer()
    }

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

        with(binding.drawerMenu) {
            itemSetting.setOnClickListener {
                startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
                binding.drawerLayout.closeDrawer(GravityCompat.START)
            }
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

    fun openDrawer() {
        binding.drawerLayout.openDrawer(GravityCompat.START)
    }

    private fun setupViewPager() {
        binding.viewPager.adapter = MainPagerAdapter(
            this,
            listOf(
                HomeFragment(),
                PlaceholderFragment.newInstance("市集页面敬请期待"),
                PlaceholderFragment.newInstance("消息页面敬请期待"),
                ProfilePageFragment()
            )
        )
        binding.viewPager.isUserInputEnabled = false
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateBottomNavState(position)
            }
        })
    }

    private fun setupBottomNav() {
        binding.btnHome.setOnClickListener { binding.viewPager.currentItem = 0 }
        binding.btnMarket.setOnClickListener { binding.viewPager.currentItem = 1 }
        binding.btnPublish.setOnClickListener { openPublish() }
        binding.btnMessage.setOnClickListener { binding.viewPager.currentItem = 2 }
        binding.btnMe.setOnClickListener { binding.viewPager.currentItem = 3 }
    }

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

    private class MainPagerAdapter(
        activity: FragmentActivity,
        private val fragments: List<Fragment>
    ) : FragmentStateAdapter(activity) {
        override fun getItemCount(): Int = fragments.size
        override fun createFragment(position: Int): Fragment = fragments[position]
    }

    fun setNavigationEnabled(enabled: Boolean) {
        binding.bottomNavLayout.isEnabled = enabled
        binding.btnHome.isEnabled = enabled
        binding.btnMarket.isEnabled = enabled
        binding.btnPublish.isEnabled = enabled
        binding.btnMessage.isEnabled = enabled
        binding.btnMe.isEnabled = enabled
    }

    fun resetDrawerState() {
        binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
        binding.drawerLayout.closeDrawer(GravityCompat.START, false)
        setNavigationEnabled(true)
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (ev?.action == MotionEvent.ACTION_DOWN) {
            ToastUtils.cancel()
        }
        return super.dispatchTouchEvent(ev)
    }
}
