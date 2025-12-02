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

sealed class PostListEvent {
    data class ToastMessage(val message: String) : PostListEvent()
}

class PostListViewModel : ViewModel() {

    private val _state = MutableStateFlow(PostListState())
    val state = _state.asStateFlow()

    private val _events = MutableSharedFlow<PostListEvent>()
    val events = _events.asSharedFlow()

    private var loading = false

    fun refresh() {
        // 下拉刷新入口：重置页码并清空错误/底部状态，刷新一批新笔记
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

    fun addPost(post: Post) {
        _state.update {
            it.copy(items = listOf(post) + it.items)
        }
    }

    fun loadMore() {
        val current = _state.value
        if (loading || !current.hasMore) return
        loading = true
        _state.update { it.copy(footerState = FooterState.LOADING) }
        loadPage(current.page, false)
    }

    private fun loadPage(page: Int, isRefresh: Boolean) {
        FakePostRepository.fetchPosts(page, PAGE_SIZE) { result ->
            viewModelScope.launch {
                loading = false
                result.onSuccess { renderPage(it, isRefresh, page) }
                    .onFailure { handleError(it, isRefresh) }
            }
        }
    }

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
