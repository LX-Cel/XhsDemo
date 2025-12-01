package com.bytedance.xhsdemo

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.viewpager2.widget.ViewPager2
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.bytedance.xhsdemo.data.SessionManager
import com.bytedance.xhsdemo.databinding.ActivityMainBinding
import com.bytedance.xhsdemo.ui.home.HomeFragment
import com.bytedance.xhsdemo.ui.placeholder.PlaceholderFragment
import com.bytedance.xhsdemo.ui.profile.ProfilePageFragment
import com.google.android.material.navigation.NavigationBarView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class MainActivity : AppCompatActivity() {

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
        binding.fabPublish.setOnClickListener { openPublish() }
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
                when (position) {
                    0 -> binding.bottomNav.selectedItemId = R.id.nav_home
                    1 -> binding.bottomNav.selectedItemId = R.id.nav_market
                    2 -> binding.bottomNav.selectedItemId = R.id.nav_message
                    3 -> binding.bottomNav.selectedItemId = R.id.nav_profile
                }
            }
        })
    }

    private fun setupBottomNav() {
        binding.bottomNav.setOnItemSelectedListener(NavigationBarView.OnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> binding.viewPager.currentItem = 0
                R.id.nav_market -> binding.viewPager.currentItem = 1
                R.id.nav_message -> binding.viewPager.currentItem = 2
                R.id.nav_profile -> binding.viewPager.currentItem = 3
                else -> return@OnItemSelectedListener false
            }
            true
        })
    }

    private fun applyInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomNav) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, systemBars.bottom)
            binding.fabPublish.translationY = -systemBars.bottom / 2f
            insets
        }
    }

    private fun openPublish() {
        startActivity(Intent(this, PublishActivity::class.java))
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
}
