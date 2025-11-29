// File: AdvancedTextView.kt
package com.advancedtext

import android.content.Context
import android.graphics.Color
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.util.AttributeSet
import android.util.Log
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReactContext
import com.facebook.react.uimanager.events.RCTEventEmitter
import android.text.Selection

class AdvancedTextView : TextView {

    private val TAG = "AdvancedTextView"

    private var highlightedWords: List<HighlightedWord> = emptyList()
    private var menuOptions: List<String> = emptyList()
    private var indicatorWordIndex: Int = -1
    private var lastSelectedText: String = ""
    private var customActionMode: ActionMode? = null
    private var currentText: String = ""

    // Cache for word positions to avoid recalculating
    private var wordPositions: List<WordPosition> = emptyList()

    constructor(context: Context?) : super(context) { init() }
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) { init() }
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) { init() }

    private fun init() {
        Log.d(TAG, "AdvancedTextView initialized")

        // Set default properties
        textSize = 16f
        setPadding(16, 16, 16, 16)
        movementMethod = LinkMovementMethod.getInstance()
        setTextIsSelectable(true)

        customSelectionActionModeCallback = object : ActionMode.Callback {
            override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                customActionMode = mode
                return true
            }

            override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                menu?.clear()

                val selectionStart = selectionStart
                val selectionEnd = selectionEnd

                if (selectionStart >= 0 && selectionEnd >= 0 && selectionStart != selectionEnd) {
                    lastSelectedText = text.subSequence(selectionStart, selectionEnd).toString()

                    menuOptions.forEachIndexed { index, option ->
                        menu?.add(0, index, index, option)
                    }

                    sendSelectionEvent(lastSelectedText, "selection")
                    return true
                }
                return false
            }

            override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
                item?.let {
                    val menuItemText = it.title.toString()
                    sendSelectionEvent(lastSelectedText, menuItemText)
                    mode?.finish()
                    return true
                }
                return false
            }

            override fun onDestroyActionMode(mode: ActionMode?) {
                customActionMode = null
            }
        }
    }

    fun setAdvancedText(text: String) {
        if (currentText == text) {
            Log.d(TAG, "Text unchanged, skipping update")
            return
        }

        Log.d(TAG, "setAdvancedText: length=${text.length}")
        currentText = text
        calculateWordPositions(text)
        updateTextWithHighlights()
    }

    fun setMenuOptions(menuOptions: List<String>) {
        if (this.menuOptions == menuOptions) return
        this.menuOptions = menuOptions
    }

    fun setHighlightedWords(highlightedWords: List<HighlightedWord>) {
        if (this.highlightedWords == highlightedWords) return
        this.highlightedWords = highlightedWords
        updateTextWithHighlights()
    }

    fun setIndicatorWordIndex(index: Int) {
        if (this.indicatorWordIndex == index) return
        this.indicatorWordIndex = index
        updateTextWithHighlights()
    }

    private fun calculateWordPositions(text: String) {
        if (text.isEmpty()) {
            wordPositions = emptyList()
            return
        }

        val positions = mutableListOf<WordPosition>()
        val regex = "\\S+".toRegex()

        regex.findAll(text).forEachIndexed { index, match ->
            positions.add(WordPosition(
                index = index,
                start = match.range.first,
                end = match.range.last + 1,
                word = match.value
            ))
        }

        wordPositions = positions
        Log.d(TAG, "Calculated ${wordPositions.size} word positions")
    }

    private fun updateTextWithHighlights() {
        if (currentText.isEmpty()) {
            Log.d(TAG, "No text available, skipping")
            return
        }

        val spannableString = SpannableString(currentText)

        // Apply spans efficiently
        wordPositions.forEach { wordPos ->
            // Apply highlights
            highlightedWords.find { it.index == wordPos.index }?.let { highlightedWord ->
                val color = parseColor(highlightedWord.highlightColor)
                spannableString.setSpan(
                    BackgroundColorSpan(color),
                    wordPos.start,
                    wordPos.end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }

            // Apply indicator color
            if (wordPos.index == indicatorWordIndex) {
                spannableString.setSpan(
                    ForegroundColorSpan(Color.RED),
                    wordPos.start,
                    wordPos.end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }

            // Make words clickable
            spannableString.setSpan(
                WordClickableSpan(wordPos.index, wordPos.word),
                wordPos.start,
                wordPos.end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        // Use post to ensure UI thread and avoid layout issues
        post {
            setText(spannableString, BufferType.SPANNABLE)
            Log.d(TAG, "Text updated with ${wordPositions.size} spans")
        }
    }

    private fun parseColor(colorString: String): Int {
        return try {
            Color.parseColor(colorString)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Invalid color: $colorString, using yellow")
            Color.YELLOW
        }
    }

    private fun sendSelectionEvent(selectedText: String, eventType: String) {
        try {
            val reactContext = context as? ReactContext ?: return
            val event = Arguments.createMap().apply {
                putString("selectedText", selectedText)
                putString("event", eventType)
            }

            reactContext.getJSModule(RCTEventEmitter::class.java)
                .receiveEvent(id, "onSelection", event)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending selection event", e)
        }
    }

    private inner class WordClickableSpan(
        private val wordIndex: Int,
        private val word: String
    ) : ClickableSpan() {

        override fun onClick(widget: View) {
            Log.d(TAG, "Word clicked: '$word' (index=$wordIndex)")
            val spannable = widget as? TextView
            spannable?.text?.let {
                if (it is android.text.Spannable) {
                    Selection.removeSelection(it)
                }
            }
            sendWordPressEvent(word, wordIndex)
        }

        override fun updateDrawState(ds: TextPaint) {
            super.updateDrawState(ds)
            ds.isUnderlineText = false
        }
    }

    private fun sendWordPressEvent(word: String, index: Int) {
        try {
            val reactContext = context as? ReactContext ?: return
            val event = Arguments.createMap().apply {
                putString("word", word)
                putInt("index", index)
            }

            reactContext.getJSModule(RCTEventEmitter::class.java)
                .receiveEvent(id, "onWordPress", event)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending word press event", e)
        }
    }

    fun clearSelection() {
        (text as? android.text.Spannable)?.let {
            Selection.removeSelection(it)
        }
    }

    data class WordPosition(
        val index: Int,
        val start: Int,
        val end: Int,
        val word: String
    )
}

data class HighlightedWord(
    val index: Int,
    val highlightColor: String
)
