package com.advancedtext

import android.content.Context
import android.graphics.Color
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.BackgroundColorSpan
import android.util.AttributeSet
import android.view.ContextMenu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReactContext
import com.facebook.react.uimanager.events.RCTEventEmitter
import android.text.Selection

class AdvancedTextView : TextView, View.OnCreateContextMenuListener {
    private var highlightedWords: List<HighlightedWord> = emptyList()
    private var menuOptions: List<String> = emptyList()
    private var indicatorWordIndex: Int = -1
    private var lastSelectedText: String = ""
    private var isSelectionEnabled: Boolean = true

    constructor(context: Context?) : super(context) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init()
    }

    private fun init() {
        movementMethod = LinkMovementMethod.getInstance()
        setTextIsSelectable(true)
        setOnCreateContextMenuListener(this)

        setOnLongClickListener {
            if (isSelectionEnabled) {
                false
            } else {
                true
            }
        }
    }

    fun setHighlightedWords(highlightedWords: List<HighlightedWord>) {
        this.highlightedWords = highlightedWords
        updateTextWithHighlights()
    }

    fun setMenuOptions(menuOptions: List<String>) {
        this.menuOptions = menuOptions
    }

    fun setIndicatorWordIndex(index: Int) {
        this.indicatorWordIndex = index
        updateTextWithHighlights()
    }

    fun setAdvancedText(text: String) {
        this.text = text
        updateTextWithHighlights()
    }

    private fun updateTextWithHighlights() {
        val text = this.text.toString()
        if (text.isEmpty()) return

        val spannableString = SpannableString(text)
        val words = text.split("\\s+".toRegex())

        var currentIndex = 0
        words.forEachIndexed { wordIndex, word ->
            if (word.isNotEmpty()) {
                val wordStart = text.indexOf(word, currentIndex)
                if (wordStart >= 0) {
                    val wordEnd = wordStart + word.length

                    highlightedWords.find { it.index == wordIndex }?.let { highlightedWord ->
                        val color = Color.parseColor(highlightedWord.highlightColor)
                        spannableString.setSpan(
                            BackgroundColorSpan(color),
                            wordStart,
                            wordEnd,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    }

                    if (wordIndex == indicatorWordIndex) {
                        spannableString.setSpan(
                            IndicatorSpan(),
                            wordStart,
                            wordEnd,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    }

                    spannableString.setSpan(
                        WordClickableSpan(wordIndex, word),
                        wordStart,
                        wordEnd,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )

                    currentIndex = wordEnd
                }
            }
        }

        this.text = spannableString
    }

    override fun onCreateContextMenu(menu: ContextMenu?, v: View?, menuInfo: ContextMenu.ContextMenuInfo?) {
        val selectionStart = selectionStart
        val selectionEnd = selectionEnd

        if (selectionStart >= 0 && selectionEnd >= 0 && selectionStart != selectionEnd) {
            lastSelectedText = text.subSequence(selectionStart, selectionEnd).toString()

            menu?.clear()

            menuOptions.forEachIndexed { index, option ->
                menu?.add(0, index, index, option)?.setOnMenuItemClickListener {
                    onMenuItemClick(it, lastSelectedText)
                    true
                }
            }

            sendSelectionEvent(lastSelectedText, "selection")
        }
    }

    private fun onMenuItemClick(item: MenuItem, selectedText: String): Boolean {
        val menuItemText = menuOptions[item.itemId]
        sendSelectionEvent(selectedText, menuItemText)
        return true
    }

    private fun sendSelectionEvent(selectedText: String, eventType: String) {
        val reactContext = context as ReactContext
        val event = Arguments.createMap().apply {
            putString("selectedText", selectedText)
            putString("event", eventType)
        }
        reactContext.getJSModule(RCTEventEmitter::class.java)
            .receiveEvent(id, "onSelection", event)
    }

    private inner class WordClickableSpan(
        private val wordIndex: Int,
        private val word: String
    ) : ClickableSpan() {
        override fun onClick(widget: View) {
            sendWordPressEvent(word, wordIndex)
        }

        override fun updateDrawState(ds: TextPaint) {
            super.updateDrawState(ds)
            ds.isUnderlineText = false
        }
    }

    private inner class IndicatorSpan : ClickableSpan() {
        override fun onClick(widget: View) {

        }

        override fun updateDrawState(ds: TextPaint) {
            super.updateDrawState(ds)
            ds.color = Color.RED
            ds.isFakeBoldText = true
            ds.isUnderlineText = false
        }
    }

    private fun sendWordPressEvent(word: String, index: Int) {
        val reactContext = context as ReactContext
        val event = Arguments.createMap().apply {
            putString("word", word)
            putInt("index", index)
        }
        reactContext.getJSModule(RCTEventEmitter::class.java)
            .receiveEvent(id, "onWordPress", event)
    }

    fun clearSelection() {
        val spannable = this.text as? android.text.Spannable ?: return
        Selection.removeSelection(spannable)
    }
}

data class HighlightedWord(
    val index: Int,
    val highlightColor: String
)
