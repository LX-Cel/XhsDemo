package com.bytedance.xhsdemo.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bytedance.xhsdemo.data.FakePostRepository
import com.bytedance.xhsdemo.data.PageResult
import com.bytedance.xhsdemo.model.Post
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// 列表 UI 状态：描述当前笔记流的展示数据和加载状态
data class PostListState(
    val items: List<Post> = emptyList(),
    val isRefreshing: Boolean = false,
    val isInitialLoading: Boolean = false,
    val footerState: FooterState = FooterState.HIDDEN,
    val showError: Boolean = false,
    val showEmpty: Boolean = false,
    val hasMore: Boolean = true,
    val page: Int = 1
)

// 一次性事件（单向通知 UI，例如 Toast 提示）
sealed class PostListEvent {
    data class ToastMessage(val message: String) : PostListEvent()
}

// 首页笔记列表的 ViewModel：负责分页加载、刷新和错误处理
class PostListViewModel : ViewModel() {

    // UI 状态 StateFlow，Fragment 通过 collect 渲染界面
    private val _state = MutableStateFlow(PostListState())
    val state = _state.asStateFlow()

    // 事件 SharedFlow，用于发送一次性事件（如 Toast）
    private val _events = MutableSharedFlow<PostListEvent>()
    val events = _events.asSharedFlow()

    // 是否正在加载，防止短时间内重复触发请求
    private var loading = false

    // 下拉刷新入口：重置数据源和分页信息，从第一页重新请求
    fun refresh() {
        if (loading) return
        loading = true
        FakePostRepository.refreshData()
        _state.update {
            it.copy(
                isRefreshing = true,
                isInitialLoading = it.items.isEmpty(),
                showError = false,
                footerState = FooterState.HIDDEN,
                page = 1,
                hasMore = true
            )
        }
        loadPage(1, true)
    }

    // 在列表顶部插入一条新发布的笔记
    fun addPost(post: Post) {
        _state.update {
            it.copy(items = listOf(post) + it.items)
        }
    }

    // 滑动到底部触发的「加载更多」逻辑
    fun loadMore() {
        val current = _state.value
        if (loading || !current.hasMore) return
        loading = true
        _state.update { it.copy(footerState = FooterState.LOADING) }
        loadPage(current.page, false)
    }

    // 通用的分页加载逻辑，FakePostRepository 通过回调返回结果
    private fun loadPage(page: Int, isRefresh: Boolean) {
        FakePostRepository.fetchPosts(page, PAGE_SIZE) { result ->
            viewModelScope.launch {
                loading = false
                result.onSuccess { renderPage(it, isRefresh, page) }
                    .onFailure { handleError(it, isRefresh) }
            }
        }
    }

    // 成功返回数据后，根据是刷新还是加载更多，合并到现有列表中
    private fun renderPage(page: PageResult, isRefresh: Boolean, currentPage: Int) {
        val merged = if (isRefresh) {
            page.items
        } else {
            _state.value.items + page.items
        }
        val nextPage = if (page.items.isNotEmpty()) currentPage + 1 else currentPage
        _state.update {
            it.copy(
                items = merged,
                isRefreshing = false,
                isInitialLoading = false,
                showError = false,
                showEmpty = merged.isEmpty(),
                page = nextPage,
                hasMore = page.hasMore,
                footerState = when {
                    !page.hasMore -> FooterState.NO_MORE
                    else -> FooterState.HIDDEN
                }
            )
        }
    }

    // 统一错误处理：刷新失败直接展示错误视图，加载更多失败则在底部展示错误并弹 Toast
    private suspend fun handleError(error: Throwable, isRefresh: Boolean) {
        if (isRefresh) {
            _state.update {
                it.copy(
                    isRefreshing = false,
                    isInitialLoading = false,
                    showError = true,
                    showEmpty = false,
                    items = emptyList(),
                    footerState = FooterState.HIDDEN
                )
            }
        } else {
            _state.update { it.copy(footerState = FooterState.ERROR) }
            _events.emit(PostListEvent.ToastMessage(error.message ?: "加载失败"))
        }
    }

    companion object {
        private const val PAGE_SIZE = 10
    }
}
