package com.bytedance.xhsdemo.ui

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

// 瀑布流间距装饰：为 Post 列表项设置左右 & 顶部/底部间距
class WaterfallSpacingDecoration(private val space: Int) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view)
        val adapter = parent.adapter
        if (position == RecyclerView.NO_POSITION) return
        // 对 Footer 等非内容项不添加间距
        if (adapter is PostAdapter && adapter.getItemViewType(position) != PostAdapter.TYPE_POST) {
            return
        }
        // 左右各留一半间距，顶部第一行额外增加一整行间距
        val half = space / 2
        outRect.left = half
        outRect.right = half
        outRect.bottom = space
        if (position < 2) {
            outRect.top = space
        }
    }
}
