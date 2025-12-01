package com.bytedance.xhsdemo.ui.profile

import android.content.Intent
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import coil.load
import coil.transform.CircleCropTransformation
import com.bytedance.xhsdemo.LoginActivity
import com.bytedance.xhsdemo.PublishActivity
import com.bytedance.xhsdemo.R
import com.bytedance.xhsdemo.SettingsActivity
import com.bytedance.xhsdemo.data.ProfileRepository
import com.bytedance.xhsdemo.data.SessionManager
import com.bytedance.xhsdemo.data.local.AppDatabase
import com.bytedance.xhsdemo.databinding.FragmentProfileBinding
import kotlinx.coroutines.launch

class ProfilePageFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var sessionManager: SessionManager
    private val viewModel: ProfileViewModel by viewModels {
        val db = AppDatabase.getInstance(requireContext().applicationContext)
        ProfileViewModelFactory(ProfileRepository(db.profileDao()))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        sessionManager = SessionManager(requireContext())
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (!sessionManager.isLoggedIn()) {
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            requireActivity().finish()
            return
        }
        setupClicks()
        observeState()
        viewModel.load()
        styleTabs()
        applyInsets()
    }

    private fun setupClicks() {
        binding.btnDrawer.setOnClickListener { binding.drawerLayout.openDrawer(GravityCompat.START) }
        binding.btnShare.setOnClickListener { toast("分享") }
        binding.btnEdit.setOnClickListener { toast("编辑资料") }
        binding.btnSettings.setOnClickListener { openSettings() }
        binding.quickInspiration.setOnClickListener { toast("创作灵感") }
        binding.quickHistory.setOnClickListener { toast("浏览记录") }
        binding.quickGroup.setOnClickListener { toast("群聊") }

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
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
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
        startActivity(Intent(requireContext(), SettingsActivity::class.java))
        applyTransitionOpen()
    }

    private fun toast(msg: String) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
    }

    private fun applyInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.profileContent) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, systemBars.top, v.paddingRight, v.paddingBottom)
            insets
        }
    }

    private fun styleTabs() {
        // 内部 tab 栏（笔记/收藏/赞过）
        styleTab(binding.tabNote, true)
        styleTab(binding.tabCollect, false)
        styleTab(binding.tabLiked, false)
    }

    private fun styleTab(textView: TextView, selected: Boolean) {
        val color =
            ContextCompat.getColor(requireContext(), if (selected) R.color.black else R.color.xhs_gray)
        textView.setTextColor(color)
        textView.setTypeface(null, if (selected) Typeface.BOLD else Typeface.NORMAL)
        textView.textSize = if (selected) 16f else 16f
    }

    private fun applyTransitionOpen() {
        @Suppress("DEPRECATION")
        requireActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
