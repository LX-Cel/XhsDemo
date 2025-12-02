package com.bytedance.xhsdemo.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bytedance.xhsdemo.PostDetailActivity
import com.bytedance.xhsdemo.MainActivity
import com.bytedance.xhsdemo.R
import com.bytedance.xhsdemo.databinding.FragmentHomeBinding
import com.bytedance.xhsdemo.model.Post
import com.bytedance.xhsdemo.ui.FooterState
import com.bytedance.xhsdemo.ui.PostAdapter
import com.bytedance.xhsdemo.ui.PostListEvent
import com.bytedance.xhsdemo.ui.PostListState
import com.bytedance.xhsdemo.ui.PostListViewModel
import com.bytedance.xhsdemo.ui.WaterfallSpacingDecoration
import com.bytedance.xhsdemo.utils.ToastUtils
import kotlinx.coroutines.launch

// 首页瀑布流列表页面：展示推荐笔记流、支持下拉刷新和自动加载更多
class HomeFragment : Fragment() {

    // ViewBinding 缓存，onDestroyView 时置空，避免内存泄漏
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    // 展示笔记列表的 Adapter
    private lateinit var adapter: PostAdapter
    // 与 Activity 共享的 ViewModel，用于跨 Fragment 共享数据（例如发布新笔记）
    private val viewModel: PostListViewModel by activityViewModels()
    // 防止一次滑动触底期间重复触发 loadMore
    private var autoBottomRefreshTriggered = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // 初始化 RecyclerView、状态栏占位和交互事件
        setupList()
        applyInsets()
        setupActions()
        // 订阅 ViewModel 的状态流和一次性事件
        observeState()
        // 首次进入且没有数据时，自动触发一次刷新
        if (viewModel.state.value.items.isEmpty()) {
            viewModel.refresh()
        }
    }

    // 初始化瀑布流列表，包括布局、间距和滑动到底部自动加载更多
    private fun setupList() {
        adapter = PostAdapter(
            onItemClick = { post ->
                val intent = Intent(requireContext(), PostDetailActivity::class.java)
                intent.putExtra(PostDetailActivity.EXTRA_POST, post)
                startActivity(intent)
            },
            onRetryClick = { viewModel.loadMore() }
        )
        val layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        layoutManager.gapStrategy = StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS
        binding.postList.layoutManager = layoutManager
        binding.postList.adapter = adapter
        binding.postList.itemAnimator = null
        binding.postList.addItemDecoration(
            WaterfallSpacingDecoration(resources.getDimensionPixelSize(R.dimen.spacing_12))
        )
        // 监听滚动，检测是否滚动到底部；当用户向上滚动或正在刷新时不触发
        binding.postList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy <= 0 || binding.swipeRefresh.isRefreshing || autoBottomRefreshTriggered) return
                val last = layoutManager.findLastVisibleItemPositions(null).maxOrNull() ?: 0
                if (last >= adapter.itemCount - 1) {
                    autoBottomRefreshTriggered = true
                    adapter.setFooterState(FooterState.LOADING)
                    viewModel.loadMore()
                }
            }
        })
    }

    // 绑定下拉刷新、重试按钮和左上角菜单按钮
    private fun setupActions() {
        binding.swipeRefresh.setOnRefreshListener {
            binding.postList.scrollToPosition(0)
            viewModel.refresh()
        }
        binding.btnRetry.setOnClickListener { viewModel.refresh() }
        binding.btnMenu.setOnClickListener {
            (activity as? MainActivity)?.openDrawer()
        }
    }

    // 根据系统状态栏高度动态设置顶部占位 View 的高度
    private fun applyInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.statusBarSpace) { v, insets ->
            val top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            v.layoutParams.height = top
            v.requestLayout()
            insets
        }
    }

    // 使用 repeatOnLifecycle 订阅状态和事件流，保证在 STARTED 之后才接收
    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.state.collect { renderState(it) }
                }
                launch {
                    viewModel.events.collect { handleEvent(it) }
                }
            }
        }
    }

    // 渲染列表状态：控制刷新动画、空视图/错误视图显示以及底部加载状态
    private fun renderState(state: PostListState) {
        binding.swipeRefresh.isRefreshing = state.isRefreshing && !state.isInitialLoading
        adapter.submitList(state.items.toMutableList())
        binding.emptyView.isVisible = state.showEmpty && !state.isRefreshing
        binding.errorView.isVisible = state.showError
        binding.swipeRefresh.isVisible = !state.showError && !state.showEmpty
        adapter.setFooterState(state.footerState)
        if (!state.isRefreshing) {
            autoBottomRefreshTriggered = false
        }
    }

    // 渲染一次性事件，例如 Toast 提示
    private fun handleEvent(event: PostListEvent) {
        when (event) {
            is PostListEvent.ToastMessage -> {
                ToastUtils.show(requireContext(), event.message)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
