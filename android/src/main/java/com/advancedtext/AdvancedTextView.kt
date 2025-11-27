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
import android.util.Log
import android.view.ContextMenu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReactContext
import com.facebook.react.uimanager.events.RCTEventEmitter
import android.text.Selection

class AdvancedTextView : TextView, View.OnCreateContextMenuListener {

    private val TAG = "AdvancedTextView"

    private var highlightedWords: List<HighlightedWord> = emptyList()
    private var menuOptions: List<String> = emptyList()
    private var indicatorWordIndex: Int = -1
    private var lastSelectedText: String = ""
    private var isSelectionEnabled: Boolean = true

    constructor(context: Context?) : super(context) { init() }
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) { init() }
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) { init() }

    private fun init() {
        Log.d(TAG, "AdvancedTextView initialized")
        movementMethod = LinkMovementMethod.getInstance()
        setTextIsSelectable(true)
        setOnCreateContextMenuListener(this)
    }

    fun setAdvancedText(text: String) {
        Log.d(TAG, "setAdvancedText: $text")
        this.text = text
        updateTextWithHighlights()
    }

    fun setMenuOptions(menuOptions: List<String>) {
        Log.d(TAG, "setMenuOptions received from RN: $menuOptions")
        this.menuOptions = menuOptions
    }

    fun setHighlightedWords(highlightedWords: List<HighlightedWord>) {
        Log.d(TAG, "setHighlightedWords received from RN: $highlightedWords")
        this.highlightedWords = highlightedWords
        updateTextWithHighlights()
    }

    fun setIndicatorWordIndex(index: Int) {
        Log.d(TAG, "setIndicatorWordIndex received: $index")
        this.indicatorWordIndex = index
        updateTextWithHighlights()
    }

    private fun updateTextWithHighlights() {
        val textValue = this.text.toString()
        Log.d(TAG, "updateTextWithHighlights called")
        Log.d(TAG, "Current text: $textValue")
        Log.d(TAG, "Highlighted words: $highlightedWords")
        Log.d(TAG, "Indicator index: $indicatorWordIndex")

        if (textValue.isEmpty()) {
            Log.d(TAG, "No text available, skipping")
            return
        }

        val spannableString = SpannableString(textValue)
        val words = textValue.split("\\s+".toRegex())

        var currentIndex = 0
        words.forEachIndexed { wordIndex, word ->

            if (word.isNotEmpty()) {
                val wordStart = textValue.indexOf(word, currentIndex)
                if (wordStart >= 0) {
                    val wordEnd = wordStart + word.length

                    highlightedWords.find { it.index == wordIndex }?.let { highlightedWord ->
                        val color = Color.parseColor(highlightedWord.highlightColor)
                        Log.d(TAG, "Applying highlight to word '$word' at index $wordIndex with color ${highlightedWord.highlightColor}")

                        spannableString.setSpan(
                            BackgroundColorSpan(color),
                            wordStart,
                            wordEnd,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    }

                    if (wordIndex == indicatorWordIndex) {
                        Log.d(TAG, "Applying indicator span to word '$word' at index $wordIndex")

                        spannableString.setSpan(
                            IndicatorSpan(),
                            wordStart,
                            wordEnd,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    }

                    // clickable span
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

        Log.d(TAG, "onCreateContextMenu triggered. selectionStart=$selectionStart selectionEnd=$selectionEnd")

        if (selectionStart >= 0 && selectionEnd >= 0 && selectionStart != selectionEnd) {
            lastSelectedText = text.subSequence(selectionStart, selectionEnd).toString()

            Log.d(TAG, "User selected text: '$lastSelectedText'")
            Log.d(TAG, "Menu options available: $menuOptions")

            menu?.clear()

            menuOptions.forEachIndexed { index, option ->
                menu?.add(0, index, index, option)?.setOnMenuItemClickListener {
                    Log.d(TAG, "Menu item clicked: $option")
                    onMenuItemClick(it, lastSelectedText)
                    true
                }
            }

            sendSelectionEvent(lastSelectedText, "selection")
        }
    }

    private fun onMenuItemClick(item: MenuItem, selectedText: String): Boolean {
        val menuItemText = menuOptions[item.itemId]
        Log.d(TAG, "onMenuItemClick: menuOption='$menuItemText', selectedText='$selectedText'")
        sendSelectionEvent(selectedText, menuItemText)
        return true
    }

    private fun sendSelectionEvent(selectedText: String, eventType: String) {
        Log.d(TAG, "sendSelectionEvent -> eventType='$eventType' selectedText='$selectedText'")

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
            Log.d(TAG, "Word clicked: '$word' (index=$wordIndex)")
            sendWordPressEvent(word, wordIndex)
        }

        override fun updateDrawState(ds: TextPaint) {
            ds.isUnderlineText = false
        }
    }

    private inner class IndicatorSpan : ClickableSpan() {
        override fun onClick(widget: View) {
            Log.d(TAG, "IndicatorSpan clicked (shouldn't trigger action)")
        }

        override fun updateDrawState(ds: TextPaint) {
            ds.color = Color.RED
            ds.isFakeBoldText = true
            ds.isUnderlineText = false
        }
    }

    private fun sendWordPressEvent(word: String, index: Int) {
        Log.d(TAG, "sendWordPressEvent -> word='$word', index=$index")

        val reactContext = context as ReactContext
        val event = Arguments.createMap().apply {
            putString("word", word)
            putInt("index", index)
        }

        reactContext.getJSModule(RCTEventEmitter::class.java)
            .receiveEvent(id, "onWordPress", event)
    }

    fun clearSelection() {
        Log.d(TAG, "clearSelection called")
        val spannable = this.text as? android.text.Spannable ?: return
        Selection.removeSelection(spannable)
    }
}

data class HighlightedWord(
    val index: Int,
    val highlightColor: String
)
