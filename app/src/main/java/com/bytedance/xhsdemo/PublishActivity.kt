package com.bytedance.xhsdemo

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import coil.load
import com.bytedance.xhsdemo.databinding.ActivityPublishBinding
import com.bytedance.xhsdemo.model.Post
import java.util.UUID

class PublishActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPublishBinding
    private var selectedUri: Uri? = null

    private val pickImage =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            selectedUri = uri
            if (uri != null) {
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

        binding.navBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        binding.btnPickImage.setOnClickListener { pickImage.launch("image/*") }
        binding.btnPublish.setOnClickListener { publishPost() }
    }

    private fun publishPost() {
        val title = binding.inputTitle.text?.toString()?.trim().orEmpty()
        val content = binding.inputContent.text?.toString()?.trim().orEmpty()
        if (title.isBlank() && content.isBlank() && selectedUri == null) {
            Toast.makeText(this, getString(R.string.publish_need_content), Toast.LENGTH_SHORT)
                .show()
            return
        }
        // 组装本地 Post 数据并通过 Intent 回传
        val post = Post(
            id = UUID.randomUUID().toString(),
            title = if (title.isNotBlank()) title else "我的新笔记",
            content = if (content.isNotBlank()) content else "记录一下此刻的灵感。",
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
}
