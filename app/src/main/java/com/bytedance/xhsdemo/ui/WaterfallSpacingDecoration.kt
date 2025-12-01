package com.bytedance.xhsdemo.ui

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

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
        if (adapter is PostAdapter && adapter.getItemViewType(position) != PostAdapter.TYPE_POST) {
            return
        }
        val half = space / 2
        outRect.left = half
        outRect.right = half
        outRect.bottom = space
        if (position < 2) {
            outRect.top = space
        }
    }
}
