package com.bytedance.xhsdemo

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.bytedance.xhsdemo.data.FakePostRepository
import com.bytedance.xhsdemo.databinding.ActivityMainBinding
import com.bytedance.xhsdemo.model.Post
import com.bytedance.xhsdemo.ui.FooterState
import com.bytedance.xhsdemo.ui.PostAdapter
import com.bytedance.xhsdemo.ui.PostListEvent
import com.bytedance.xhsdemo.ui.PostListState
import com.bytedance.xhsdemo.ui.PostListViewModel
import com.bytedance.xhsdemo.ui.WaterfallSpacingDecoration
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: PostAdapter
    private val viewModel: PostListViewModel by viewModels()

    private val publishLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                val newPost =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        result.data?.getParcelableExtra(
                            PublishActivity.EXTRA_NEW_POST,
                            Post::class.java
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        result.data?.getParcelableExtra<Post>(PublishActivity.EXTRA_NEW_POST)
                    }
                newPost?.let {
                    FakePostRepository.addPost(it)
                    // 发布页回传后刷新首页列表，保证新帖出现在顶部
                    viewModel.refresh()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = getString(R.string.feed_title)

        setupList()
        setupActions()
        observeState()
        // 首屏自动刷新数据
        viewModel.refresh()
    }

    private fun setupList() {
        adapter = PostAdapter(
            onItemClick = { post ->
                val intent = Intent(this, PostDetailActivity::class.java)
                intent.putExtra(PostDetailActivity.EXTRA_POST, post)
                startActivity(intent)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, R.anim.slide_in_right, R.anim.slide_out_left)
                } else {
                    @Suppress("DEPRECATION")
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                }

            },
            onRetryClick = { viewModel.loadMore() }
        )
        val layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        binding.postList.layoutManager = layoutManager
        binding.postList.adapter = adapter
        binding.postList.addItemDecoration(
            WaterfallSpacingDecoration(resources.getDimensionPixelSize(R.dimen.spacing_12))
        )
        binding.postList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy <= 0) return
                val last = layoutManager.findLastVisibleItemPositions(null).maxOrNull() ?: 0
                // 接近列表底部时触发分页加载
                if (last >= adapter.itemCount - 4) {
                    viewModel.loadMore()
                }
            }
        })
    }

    private fun setupActions() {
        binding.swipeRefresh.setOnRefreshListener { viewModel.refresh() }
        binding.btnRetry.setOnClickListener { viewModel.refresh() }
        binding.addFab.setOnClickListener {
            publishLauncher.launch(Intent(this, PublishActivity::class.java))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, R.anim.slide_in_right, R.anim.slide_out_left)
            } else {
                @Suppress("DEPRECATION")
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            }
        }
    }

    private fun observeState() {
        // 在 STARTED 生命周期内收集状态/事件，避免内存泄漏
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.state.collect { renderState(it) }
                }
                launch {
                    viewModel.events.collect { handleEvent(it) }
                }
            }
        }
    }

    private fun renderState(state: PostListState) {
        binding.swipeRefresh.isRefreshing = state.isRefreshing
        adapter.submitList(state.items.toMutableList())
        binding.emptyView.isVisible = state.showEmpty
        binding.errorView.isVisible = state.showError
        adapter.setFooterState(state.footerState)
    }

    private fun handleEvent(event: PostListEvent) {
        when (event) {
            is PostListEvent.ToastMessage -> {
                android.widget.Toast.makeText(this, event.message, android.widget.Toast.LENGTH_SHORT)
                    .show()
            }
        }
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
}
