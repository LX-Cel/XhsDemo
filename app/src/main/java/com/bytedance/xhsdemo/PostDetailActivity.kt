package com.bytedance.xhsdemo

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import coil.load
import coil.transform.CircleCropTransformation
import com.bytedance.xhsdemo.databinding.ActivityPostDetailBinding
import com.bytedance.xhsdemo.databinding.ItemCommentBinding
import com.bytedance.xhsdemo.model.Post
import com.bytedance.xhsdemo.utils.ToastUtils

// 笔记详情页：展示封面、内容、作者信息以及评论列表
class PostDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPostDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityPostDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 让顶部工具栏根据状态栏高度增加内边距，支持沉浸式
        ViewCompat.setOnApplyWindowInsetsListener(binding.detailToolbar) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, systemBars.top, v.paddingRight, v.paddingBottom)
            insets
        }

        binding.navBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // 兼容不同 Android 版本的 Parcelable 读取方式
        val post = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_POST, Post::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra<Post>(EXTRA_POST)
        }
        if (post == null) {
            // 没有收到笔记数据，直接关闭页面避免空白
            finish()
            return
        }
        // 帖子数据存在才渲染详情，否则直接关闭页面
        bindPost(post)
    }

    // 将 Post 对象内容渲染到 UI，并根据评论情况动态填充评论区
    private fun bindPost(post: Post) {
        binding.detailCover.load(post.imageUrl) {
            crossfade(true)
            placeholder(R.drawable.bg_placeholder_white)
            error(R.drawable.bg_placeholder_white)
        }
        binding.detailTitle.text = post.title
        binding.detailContent.text = post.content
        binding.detailAuthor.text = post.authorName
        binding.detailTime.text = post.publishTime
        binding.detailAvatar.load(post.authorAvatar) {
            transformations(CircleCropTransformation())
            placeholder(R.drawable.bg_avatar_placeholder)
            error(R.drawable.bg_avatar_placeholder)
        }
        // 先清空旧的评论 View
        binding.commentContainer.removeAllViews()
        if (post.comments.isEmpty()) {
            val tv = TextView(this).apply {
                text = getString(R.string.no_comment)
                setTextColor(ContextCompat.getColor(this@PostDetailActivity, R.color.black))
                textSize = 14f
            }
            binding.commentContainer.addView(tv)
        } else {
            val inflater = LayoutInflater.from(this)
            post.comments.forEach { comment ->
                val itemBinding = ItemCommentBinding.inflate(inflater, binding.commentContainer, false)
                itemBinding.commentAuthor.text = comment.userName
                itemBinding.commentContent.text = comment.content
                itemBinding.commentAvatar.load(comment.userAvatar) {
                    transformations(CircleCropTransformation())
                    placeholder(R.drawable.bg_avatar_placeholder)
                    error(R.drawable.bg_avatar_placeholder)
                }
                binding.commentContainer.addView(itemBinding.root)
            }
        }
    }

    companion object {
        const val EXTRA_POST = "extra_post"
    }

    // 关闭详情页时使用与其他页面一致的转场动画
    override fun finish() {
        super.finish()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, R.anim.slide_in_left, R.anim.slide_out_right)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }
    }

    // 捕获触摸事件，按下时取消当前 Toast
    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (ev?.action == MotionEvent.ACTION_DOWN) {
            ToastUtils.cancel()
        }
        return super.dispatchTouchEvent(ev)
    }
}
