package com.bytedance.xhsdemo.ui.profile

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import android.widget.EditText
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import coil.load
import coil.transform.CircleCropTransformation
import com.bytedance.xhsdemo.LoginActivity
import com.bytedance.xhsdemo.MainActivity
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

    private val pickMedia = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            viewModel.updateAvatar(uri.toString())
        }
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
        binding.btnDrawer.setOnClickListener { (activity as? MainActivity)?.openDrawer() }
        binding.btnShare.setOnClickListener { toast("分享") }
        binding.btnEdit.setOnClickListener { toast("编辑资料") }
        binding.btnSettings.setOnClickListener { openSettings() }
        binding.quickInspiration.setOnClickListener { toast("创作灵感") }
        binding.quickHistory.setOnClickListener { toast("浏览记录") }
        binding.quickGroup.setOnClickListener { toast("群聊") }
        binding.btnScan.setOnClickListener { toast("扫一扫") }

        binding.avatarImage.setOnClickListener { pickMedia.launch("image/*") }
        binding.profileName.setOnClickListener { showEditNameDialog() }

        binding.tabNote.setOnClickListener { toast("笔记") }
        binding.tabCollect.setOnClickListener { toast("收藏") }
        binding.tabLiked.setOnClickListener { toast("赞过") }
    }

    private fun showEditNameDialog() {
        val editText = EditText(requireContext())
        editText.setText(binding.profileName.text)
        AlertDialog.Builder(requireContext())
            .setTitle("修改昵称")
            .setView(editText)
            .setPositiveButton("确定") { _, _ ->
                val newName = editText.text.toString()
                if (newName.isNotBlank()) {
                    viewModel.updateName(newName)
                }
            }
            .setNegativeButton("取消", null)
            .show()
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
            binding.profileName.setTextColor(Color.WHITE) // Ensure text color is white
            binding.profileSignature.text = profile.signature
            binding.avatarImage.load(profile.avatarUrl) {
                crossfade(true)
                transformations(CircleCropTransformation())
                placeholder(R.drawable.bg_avatar_placeholder)
            }
        }
        binding.emptyContainer.visibility = View.VISIBLE
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
            binding.statusBarSpace.layoutParams.height = systemBars.top
            binding.statusBarSpace.requestLayout()
            v.setPadding(v.paddingLeft, 0, v.paddingRight, v.paddingBottom)
            insets
        }
    }

    private fun styleTabs() {
        styleTab(binding.tabNote, true)
        styleTab(binding.tabCollect, false)
        styleTab(binding.tabLiked, false)
    }

    private fun styleTab(textView: TextView, selected: Boolean) {
        val color =
            ContextCompat.getColor(requireContext(), if (selected) R.color.black else R.color.xhs_gray)
        textView.setTextColor(color)
        textView.setTypeface(null, if (selected) Typeface.BOLD else Typeface.NORMAL)
        textView.textSize = 16f
    }

    private fun applyTransitionOpen() {
        @Suppress("DEPRECATION")
        requireActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        (activity as? MainActivity)?.setNavigationEnabled(true)
        _binding = null
    }
}
