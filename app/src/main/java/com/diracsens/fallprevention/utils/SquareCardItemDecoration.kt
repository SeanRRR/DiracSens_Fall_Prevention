package com.diracsens.fallprevention.utils

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class SquareCardItemDecoration(private val spanCount: Int, private val spacing: Int) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view)
        val column = position % spanCount

        outRect.left = spacing - column * spacing / spanCount
        outRect.right = (column + 1) * spacing / spanCount

        if (position < spanCount) {
            outRect.top = spacing
        }
        outRect.bottom = spacing

        // Calculate and set the view height to match its width
        parent.post { // Use post to wait for the view to be measured
            val width = view.width
            if (width > 0 && view.layoutParams.height != width) { // Avoid unnecessary re-layouts
                val layoutParams = view.layoutParams
                layoutParams.height = width
                view.layoutParams = layoutParams
            }
        }
    }
} 