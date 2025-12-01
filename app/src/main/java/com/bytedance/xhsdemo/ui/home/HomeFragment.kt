package com.bytedance.xhsdemo.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
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
import com.bytedance.xhsdemo.ui.PostAdapter
import com.bytedance.xhsdemo.ui.PostListEvent
import com.bytedance.xhsdemo.ui.PostListState
import com.bytedance.xhsdemo.ui.PostListViewModel
import com.bytedance.xhsdemo.ui.WaterfallSpacingDecoration
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: PostAdapter
    private val viewModel: PostListViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupList()
        applyInsets()
        setupActions()
        observeState()
        viewModel.refresh()
    }

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
                if (last >= adapter.itemCount - 4) {
                    viewModel.loadMore()
                }
            }
        })
    }

    private fun setupActions() {
        binding.swipeRefresh.setOnRefreshListener { viewModel.refresh() }
        binding.btnRetry.setOnClickListener { viewModel.refresh() }
        binding.btnMenu.setOnClickListener {
            (activity as? MainActivity)?.openDrawer()
        }
    }

    private fun applyInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.statusBarSpace) { v, insets ->
            val top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            v.layoutParams.height = top
            v.requestLayout()
            insets
        }
    }

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
                android.widget.Toast.makeText(requireContext(), event.message, android.widget.Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
