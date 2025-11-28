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
    private var isSelectionEnabled: Boolean = true
    private var customActionMode: ActionMode? = null

    constructor(context: Context?) : super(context) { init() }
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) { init() }
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) { init() }

        private fun init() {
        Log.d(TAG, "AdvancedTextView initialized")

        // Set default text appearance - DON'T set black color here
        textSize = 16f
        setPadding(16, 16, 16, 16)

        movementMethod = LinkMovementMethod.getInstance()
        setTextIsSelectable(true)

        customSelectionActionModeCallback = object : ActionMode.Callback {
            override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                Log.d(TAG, "onCreateActionMode triggered")
                customActionMode = mode
                return true
            }

            override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                Log.d(TAG, "onPrepareActionMode triggered")
                menu?.clear()

                val selectionStart = selectionStart
                val selectionEnd = selectionEnd

                if (selectionStart >= 0 && selectionEnd >= 0 && selectionStart != selectionEnd) {
                    lastSelectedText = text.subSequence(selectionStart, selectionEnd).toString()
                    Log.d(TAG, "User selected text: '$lastSelectedText'")
                    Log.d(TAG, "Menu options available: $menuOptions")

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
                    Log.d(TAG, "Menu item clicked: $menuItemText")
                    sendSelectionEvent(lastSelectedText, menuItemText)
                    mode?.finish()
                    return true
                }
                return false
            }

            override fun onDestroyActionMode(mode: ActionMode?) {
                Log.d(TAG, "onDestroyActionMode")
                customActionMode = null
            }
        }
    }

    fun setAdvancedText(text: String) {
        Log.d(TAG, "setAdvancedText: $text (length=${text.length})")

        // Set the text first
        super.setText(text, BufferType.SPANNABLE)

        // Then apply highlights
        updateTextWithHighlights()

        // Force layout update
        requestLayout()
        invalidate()
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
        val textValue = this.text?.toString() ?: ""
        Log.d(TAG, "updateTextWithHighlights called")
        Log.d(TAG, "Current text: $textValue")
        Log.d(TAG, "Highlighted words: $highlightedWords")
        Log.d(TAG, "Indicator index: $indicatorWordIndex")

        if (textValue.isEmpty()) {
            Log.d(TAG, "No text available, skipping")
            return
        }

        val spannableString = SpannableString(textValue)

        // Split words while preserving spaces for accurate indexing
        val words = textValue.split("\\s+".toRegex()).filter { it.isNotEmpty() }

        var currentIndex = 0
        words.forEachIndexed { wordIndex, word ->

            // Find the actual position of the word in the text
            val wordStart = textValue.indexOf(word, currentIndex)
            if (wordStart >= 0) {
                val wordEnd = wordStart + word.length

                Log.d(TAG, "Processing word '$word' at position $wordStart-$wordEnd, index $wordIndex")

                // Apply clickable span FIRST (this is important)
                spannableString.setSpan(
                    WordClickableSpan(wordIndex, word),
                    wordStart,
                    wordEnd,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )

                // Then apply background color for highlighted words
                highlightedWords.find { it.index == wordIndex }?.let { highlightedWord ->
                    val color = try {
                        Color.parseColor(highlightedWord.highlightColor)
                    } catch (e: IllegalArgumentException) {
                        Log.e(TAG, "Invalid color: ${highlightedWord.highlightColor}, using yellow")
                        Color.YELLOW
                    }
                    Log.d(TAG, "Applying highlight to word '$word' at index $wordIndex with color ${highlightedWord.highlightColor}")

                    spannableString.setSpan(
                        BackgroundColorSpan(color),
                        wordStart,
                        wordEnd,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }

                // Then apply indicator span
                if (wordIndex == indicatorWordIndex) {
                    Log.d(TAG, "Applying indicator span to word '$word' at index $wordIndex")

                    spannableString.setSpan(
                        IndicatorSpan(),
                        wordStart,
                        wordEnd,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }

                currentIndex = wordEnd
            }
        }

        // Set the spannable text
        setText(spannableString, BufferType.SPANNABLE)

        // Ensure movement method is still set
        movementMethod = LinkMovementMethod.getInstance()

        Log.d(TAG, "Text updated with spans, total spans")
    }



    private fun onMenuItemClick(item: MenuItem, selectedText: String): Boolean {
        val menuItemText = menuOptions[item.itemId]
        Log.d(TAG, "onMenuItemClick: menuOption='$menuItemText', selectedText='$selectedText'")
        sendSelectionEvent(selectedText, menuItemText)
        return true
    }

    private fun sendSelectionEvent(selectedText: String, eventType: String) {
        Log.d(TAG, "sendSelectionEvent -> eventType='$eventType' selectedText='$selectedText'")

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
            Log.d(TAG, "WordClickableSpan onClick triggered: '$word' (index=$wordIndex)")

            // Small delay to ensure the click is processed
            widget.post {
                sendWordPressEvent(word, wordIndex)
            }
        }

        override fun updateDrawState(ds: TextPaint) {
            // Don't call super to avoid default link styling (blue color, underline)
            // Keep the original text appearance
            ds.color = currentTextColor
            ds.isUnderlineText = false
            ds.bgColor = Color.TRANSPARENT
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
        Log.d(TAG, "clearSelection called")
        val spannable = this.text as? android.text.Spannable ?: return
        Selection.removeSelection(spannable)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        Log.d(TAG, "onMeasure: width=${measuredWidth}, height=${measuredHeight}")
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        Log.d(TAG, "onLayout: changed=$changed, bounds=[$left,$top,$right,$bottom]")
    }
}

data class HighlightedWord(
    val index: Int,
    val highlightColor: String
)
