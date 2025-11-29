package com.advancedtext

import android.text.Layout
import android.text.Selection
import android.text.Spannable
import android.text.method.BaseMovementMethod
import android.text.style.ClickableSpan
import android.view.MotionEvent
import android.widget.TextView

class ClickableMovementMethod : BaseMovementMethod() {

    companion object {
        private var sInstance: ClickableMovementMethod? = null

        fun getInstance(): ClickableMovementMethod {
            if (sInstance == null) {
                sInstance = ClickableMovementMethod()
            }
            return sInstance!!
        }
    }

    override fun canSelectArbitrarily(): Boolean = false

    override fun onTouchEvent(widget: TextView, buffer: Spannable, event: MotionEvent): Boolean {
        val action = event.actionMasked
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_DOWN) {

            var x = (event.x - widget.totalPaddingLeft + widget.scrollX).toInt()
            var y = (event.y - widget.totalPaddingTop + widget.scrollY).toInt()

            val layout: Layout = widget.layout
            val links: Array<ClickableSpan>? = if (y < 0 || y > layout.height) {
                null
            } else {
                val line = layout.getLineForVertical(y)
                if (x < layout.getLineLeft(line) || x > layout.getLineRight(line)) null
                else {
                    val offset = layout.getOffsetForHorizontal(line, x.toFloat())
                    buffer.getSpans(offset, offset, ClickableSpan::class.java)
                }
            }

            if (!links.isNullOrEmpty()) {
                if (action == MotionEvent.ACTION_UP) links[0].onClick(widget)
                else Selection.setSelection(buffer, buffer.getSpanStart(links[0]), buffer.getSpanEnd(links[0]))
                return true
            } else {
                Selection.removeSelection(buffer)
            }
        }
        return false
    }

    override fun initialize(widget: TextView, text: Spannable) {
        Selection.removeSelection(text)
    }
}
