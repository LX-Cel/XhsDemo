package com.bytedance.xhsdemo.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.bytedance.xhsdemo.R
import com.bytedance.xhsdemo.databinding.ItemFeedFooterBinding
import com.bytedance.xhsdemo.databinding.ItemPostBinding
import com.bytedance.xhsdemo.model.Post

enum class FooterState { HIDDEN, LOADING, ERROR, NO_MORE }

class PostAdapter(
    private val onItemClick: (Post) -> Unit,
    private val onRetryClick: () -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val posts = mutableListOf<Post>()
    private var footerState: FooterState = FooterState.HIDDEN

    val dataSize: Int
        get() = posts.size

    override fun getItemViewType(position: Int): Int {
        return if (position < posts.size) TYPE_POST else TYPE_FOOTER
    }

    override fun getItemCount(): Int = posts.size + if (footerState == FooterState.HIDDEN) 0 else 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_POST) {
            val binding = ItemPostBinding.inflate(inflater, parent, false)
            PostViewHolder(binding)
        } else {
            val binding = ItemFeedFooterBinding.inflate(inflater, parent, false)
            FooterViewHolder(binding, onRetryClick)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is PostViewHolder) {
            holder.bind(posts[position], onItemClick)
        } else if (holder is FooterViewHolder) {
            holder.bind(footerState)
        }
    }

    fun submitList(newItems: MutableList<Post>) {
        posts.clear()
        posts.addAll(newItems)
        notifyDataSetChanged()
    }

    fun append(items: List<Post>) {
        if (items.isEmpty()) return
        val start = posts.size
        posts.addAll(items)
        notifyItemRangeInserted(start, items.size)
    }

    fun prepend(item: Post) {
        posts.add(0, item)
        notifyItemInserted(0)
    }

    fun setFooterState(state: FooterState) {
        val hadFooter = footerState != FooterState.HIDDEN
        footerState = state
        val hasFooter = footerState != FooterState.HIDDEN
        when {
            hadFooter && !hasFooter -> notifyItemRemoved(posts.size)
            !hadFooter && hasFooter -> notifyItemInserted(posts.size)
            hasFooter -> notifyItemChanged(posts.size)
        }
    }

    private class PostViewHolder(private val binding: ItemPostBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(post: Post, onClick: (Post) -> Unit) {
            binding.coverImage.load(post.imageUrl) {
                crossfade(true)
                placeholder(R.drawable.bg_image_placeholder)
                error(R.drawable.bg_image_placeholder)
            }
            binding.titleView.text = post.title
            binding.authorView.text = post.authorName
            binding.metaView.text = "${post.comments.size} 条评论 · ${post.likes} 赞"
            binding.avatarView.load(post.authorAvatar) {
                transformations(CircleCropTransformation())
                placeholder(R.drawable.bg_avatar_placeholder)
                error(R.drawable.bg_avatar_placeholder)
            }
            binding.root.setOnClickListener { onClick(post) }
        }
    }

    private class FooterViewHolder(
        private val binding: ItemFeedFooterBinding,
        onRetryClick: () -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.btnFooterRetry.setOnClickListener { onRetryClick() }
        }

        fun bind(state: FooterState) {
            binding.loadingBar.isVisible = state == FooterState.LOADING
            binding.btnFooterRetry.isVisible = state == FooterState.ERROR
            binding.footerText.isVisible = state != FooterState.HIDDEN
            binding.footerText.text = when (state) {
                FooterState.LOADING -> binding.root.context.getString(R.string.loading_more)
                FooterState.ERROR -> binding.root.context.getString(R.string.load_error)
                FooterState.NO_MORE -> binding.root.context.getString(R.string.load_no_more)
                FooterState.HIDDEN -> ""
            }
        }
    }

    companion object {
        const val TYPE_POST = 1
        private const val TYPE_FOOTER = 2
    }
}
