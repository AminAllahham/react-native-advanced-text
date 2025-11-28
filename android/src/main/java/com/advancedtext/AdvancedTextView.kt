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
import android.util.TypedValue
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

    // Cache for base text color to avoid parsing multiple times
    private var baseTextColor: Int = Color.BLACK

    constructor(context: Context?) : super(context) { init() }
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) { init() }
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) { init() }

    private fun init() {
        Log.d(TAG, "AdvancedTextView initialized")

        // Set default text appearance
        textSize = 16f
        setPadding(16, 16, 16, 16)
        baseTextColor = currentTextColor

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
        super.setText(text, BufferType.SPANNABLE)
        updateTextWithHighlights()
        requestLayout()
        invalidate()
    }

    // Performance-optimized: Set base font size at view level
    fun setFontSize(size: Float) {
        Log.d(TAG, "setFontSize: $size")
        // Use SP units for accessibility
        setTextSize(TypedValue.COMPLEX_UNIT_SP, size)
    }

    // Performance-optimized: Set base color at view level
    fun setTextColorProp(colorString: String?) {
        try {
            val color = if (colorString != null) {
                Color.parseColor(colorString)
            } else {
                Color.BLACK
            }
            Log.d(TAG, "setTextColorProp: $colorString -> $color")
            baseTextColor = color
            setTextColor(color)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Invalid color: $colorString, using black", e)
            baseTextColor = Color.BLACK
            setTextColor(Color.BLACK)
        }
    }

    fun setMenuOptions(menuOptions: List<String>) {
        Log.d(TAG, "setMenuOptions received: $menuOptions")
        this.menuOptions = menuOptions
    }

    fun setHighlightedWords(highlightedWords: List<HighlightedWord>) {
        Log.d(TAG, "setHighlightedWords received: $highlightedWords")
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

        if (textValue.isEmpty()) {
            Log.d(TAG, "No text available, skipping")
            return
        }

        val spannableString = SpannableString(textValue)
        val words = textValue.split("\\s+".toRegex()).filter { it.isNotEmpty() }

        var currentIndex = 0
        words.forEachIndexed { wordIndex, word ->
            val wordStart = textValue.indexOf(word, currentIndex)
            if (wordStart >= 0) {
                val wordEnd = wordStart + word.length

                // Apply clickable span
                spannableString.setSpan(
                    WordClickableSpan(wordIndex, word),
                    wordStart,
                    wordEnd,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )

                // Apply background highlight
                highlightedWords.find { it.index == wordIndex }?.let { highlightedWord ->
                    val color = try {
                        Color.parseColor(highlightedWord.highlightColor)
                    } catch (e: IllegalArgumentException) {
                        Log.e(TAG, "Invalid color: ${highlightedWord.highlightColor}")
                        Color.YELLOW
                    }

                    spannableString.setSpan(
                        BackgroundColorSpan(color),
                        wordStart,
                        wordEnd,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }

                // Apply indicator span
                if (wordIndex == indicatorWordIndex) {
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

        setText(spannableString, BufferType.SPANNABLE)
        movementMethod = LinkMovementMethod.getInstance()
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
            Log.d(TAG, "WordClickableSpan onClick: '$word' (index=$wordIndex)")
            widget.post {
                sendWordPressEvent(word, wordIndex)
            }
        }

        override fun updateDrawState(ds: TextPaint) {
            // Preserve the base text color instead of forcing a color
            ds.color = baseTextColor
            ds.isUnderlineText = false
            ds.bgColor = Color.TRANSPARENT
        }
    }

    private inner class IndicatorSpan : ClickableSpan() {
        override fun onClick(widget: View) {
            Log.d(TAG, "IndicatorSpan clicked")
        }

        override fun updateDrawState(ds: TextPaint) {
            ds.color = baseTextColor
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
        Log.d(TAG, "onLayout: changed=$changed")
    }
}

data class HighlightedWord(
    val index: Int,
    val highlightColor: String
)
