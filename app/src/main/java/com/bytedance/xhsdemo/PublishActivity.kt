package com.bytedance.xhsdemo

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import coil.load
import com.bytedance.xhsdemo.databinding.ActivityPublishBinding
import com.bytedance.xhsdemo.model.Post
import com.bytedance.xhsdemo.utils.ToastUtils
import java.util.UUID

// 发布笔记页面：支持选择本地图片和输入标题内容，结果回传给首页
class PublishActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPublishBinding
    // 用户选择的图片 Uri，可能为空
    private var selectedUri: Uri? = null

    // 选择本地图片的 ActivityResultLauncher
    private val pickImage =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            selectedUri = uri
            if (uri != null) {
                // 申请持久化读权限，并预览图片
                grantReadPersistPermission(uri)
                // 选取本地图片后立即预览
                binding.previewImage.load(uri) {
                    crossfade(true)
                }
            } else {
                binding.previewImage.setImageResource(R.drawable.ic_empty)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPublishBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 处理状态栏 Insets
        applyInsets()
        binding.navBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        // 选择图片和发布按钮
        binding.btnPickImage.setOnClickListener { pickImage.launch("image/*") }
        binding.btnPublish.setOnClickListener { publishPost() }
    }

    // 组装一条新的 Post，并通过 setResult 回传给调用方
    private fun publishPost() {
        val title = binding.inputTitle.text?.toString()?.trim().orEmpty()
        val content = binding.inputContent.text?.toString()?.trim().orEmpty()
        if (title.isBlank() && content.isBlank() && selectedUri == null) {
            ToastUtils.show(this, getString(R.string.publish_need_content))
            return
        }
        // 组装本地 Post 数据并通过 Intent 回传
        val post = Post(
            id = UUID.randomUUID().toString(),
            title = if (title.isNotBlank()) title else "我的新笔记",
            content = if (content.isNotBlank()) content else "记录一下此刻的灵感吧～",
            imageUrl = selectedUri?.toString() ?: DEFAULT_COVER,
            authorName = "我",
            authorAvatar = DEFAULT_AVATAR,
            publishTime = "刚刚",
            likes = 0,
            comments = emptyList()
        )
        val intent = Intent().apply {
            putExtra(EXTRA_NEW_POST, post)
        }
        setResult(RESULT_OK, intent)
        finish()
    }

    // 关闭页面时统一使用自定义转场动画
    override fun finish() {
        super.finish()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, R.anim.slide_in_left, R.anim.slide_out_right)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }
    }

    companion object {
        const val EXTRA_NEW_POST = "extra_new_post"
        private const val DEFAULT_COVER =
            "https://images.unsplash.com/photo-1498050108023-c5249f4df085?auto=format&fit=crop&w=1200&q=80"
        private const val DEFAULT_AVATAR =
            "https://images.unsplash.com/photo-1544723795-3fb6469f5b39?auto=format&fit=crop&w=200&q=60"
    }

    // 根据状态栏高度调整顶部占位 View
    private fun applyInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.statusBarSpace) { v, insets ->
            val top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            v.layoutParams.height = top
            v.requestLayout()
            insets
        }
    }

    // 尝试申请持久化读取 Uri 权限，避免下次访问失败
    private fun grantReadPersistPermission(uri: Uri) {
        try {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: Exception) {
            // 非可持久化 Uri 忽略异常
        }
    }

    // 捕获点击事件；按下时取消当前 Toast
    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (ev?.action == MotionEvent.ACTION_DOWN) {
            ToastUtils.cancel()
        }
        return super.dispatchTouchEvent(ev)
    }
}
