package com.advancedtext

import android.content.Context
import android.graphics.Color
import android.graphics.Point
import android.text.SpannableString
import android.text.Spannable
import android.text.Spanned
import android.text.TextPaint
import android.text.method.ArrowKeyMovementMethod
import android.text.style.ClickableSpan
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.util.AttributeSet
import android.util.Log
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.MotionEvent
import android.widget.TextView
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReactContext
import com.facebook.react.uimanager.events.RCTEventEmitter
import android.text.Selection
import android.graphics.Typeface
import androidx.core.text.getSpans

class AdvancedTextView : TextView {

    private val TAG = "AdvancedTextView"

    private var highlightedWords: List<HighlightedWord> = emptyList()
    private var menuOptions: List<String> = emptyList()
    private var indicatorWordIndex: Int = -1
    private var lastSelectedText: String = ""
    private var customActionMode: ActionMode? = null
    private var currentText: String = ""
    private var textColor: String = "#000000"
    private var fontSize: Float = 16f
    private var fontWeight: String = "normal"
    private var textAlign: String = "left"
    private var fontFamily: String = "sans-serif"

    private var wordPositions: List<WordPosition> = emptyList()

    constructor(context: Context?) : super(context) { init() }
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) { init() }
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) { init() }

    private fun init() {
        Log.d(TAG, "AdvancedTextView initialized")

        textSize = 16f
        setPadding(16, 16, 16, 16)
        setTextIsSelectable(true)

        movementMethod = SmartMovementMethod

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

    fun setAdvancedTextColor(colorInt: Int) {
          textColor = String.format("#%06X", 0xFFFFFF and colorInt)
          updateTextWithHighlights()
    }

    fun setAdvancedTextSize(size: Float) {
        if (fontSize == size) return
        fontSize = size
        updateTextWithHighlights()
    }

    fun setAdvancedFontWeight(weight: String) {
        if (fontWeight == weight) return
        fontWeight = weight
        updateTextWithHighlights()
    }

    fun setAdvancedTextAlign(align: String) {
        if (textAlign == align) return
        textAlign = align
        when (align) {
            "left" -> textAlignment = View.TEXT_ALIGNMENT_TEXT_START
            "center" -> textAlignment = View.TEXT_ALIGNMENT_CENTER
            "right" -> textAlignment = View.TEXT_ALIGNMENT_TEXT_END
            else -> textAlignment = View.TEXT_ALIGNMENT_TEXT_START
        }
    }

    fun setAdvancedFontFamily(family: String) {
        if (fontFamily == family) return
        fontFamily = family
        typeface = Typeface.create(family, Typeface.NORMAL)
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

        wordPositions.forEach { wordPos ->
            highlightedWords.find { it.index == wordPos.index }?.let { highlightedWord ->
                val color = parseColor(highlightedWord.highlightColor)
                spannableString.setSpan(
                    BackgroundColorSpan(color),
                    wordPos.start,
                    wordPos.end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }

            if (wordPos.index == indicatorWordIndex) {
                spannableString.setSpan(
                    ForegroundColorSpan(Color.parseColor(textColor)),
                    wordPos.start,
                    wordPos.end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }

            // Add clickable span for word clicks
            spannableString.setSpan(
                WordClickableSpan(wordPos.index, wordPos.word),
                wordPos.start,
                wordPos.end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

         textAlignment = when (textAlign) {
            "left" -> View.TEXT_ALIGNMENT_TEXT_START
            "center" -> View.TEXT_ALIGNMENT_CENTER
            "right" -> View.TEXT_ALIGNMENT_TEXT_END
            else -> View.TEXT_ALIGNMENT_TEXT_START
        }

        setTextSize(fontSize)

        typeface = when (fontWeight) {
            "bold" -> Typeface.create(fontFamily, Typeface.BOLD)
            "italic" -> Typeface.create(fontFamily, Typeface.ITALIC)
            else -> Typeface.create(fontFamily, Typeface.NORMAL)
        }

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
            sendWordPressEvent(word, wordIndex)
        }

        override fun updateDrawState(ds: TextPaint) {
            super.updateDrawState(ds)
            ds.isUnderlineText = false
            ds.color = Color.parseColor(textColor)
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


    private object SmartMovementMethod : ArrowKeyMovementMethod() {

        override fun onTouchEvent(widget: TextView?, buffer: Spannable?, event: MotionEvent?): Boolean {
            if (event != null && widget != null && buffer != null) {
                if (handleMotion(event, widget, buffer)) {
                    return true
                }
            }
            return super.onTouchEvent(widget, buffer, event)
        }

        private fun handleMotion(event: MotionEvent, widget: TextView, buffer: Spannable): Boolean {
            var handled = false

            if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_UP) {
                val target = Point().apply {
                    x = event.x.toInt() - widget.totalPaddingLeft + widget.scrollX
                    y = event.y.toInt() - widget.totalPaddingTop + widget.scrollY
                }

                val line = widget.layout.getLineForVertical(target.y)
                val offset = widget.layout.getOffsetForHorizontal(line, target.x.toFloat())

                if (event.action == MotionEvent.ACTION_DOWN) {
                    handled = handled || buffer.execute<ClickableSpan>(offset) {
                        Selection.setSelection(buffer, buffer.getSpanStart(it), buffer.getSpanEnd(it))
                    }
                }

                if (event.action == MotionEvent.ACTION_UP) {
                    handled = handled || buffer.execute<ClickableSpan>(offset) {
                        it.onClick(widget)
                    }
                }
            }

            return handled
        }

        private inline fun <reified T : Any> Spannable.execute(offset: Int, fn: (T) -> Unit): Boolean {
            val spans = this.getSpans<T>(offset, offset)
            if (spans.isNotEmpty()) {
                spans.forEach(fn)
                return true
            }
            return false
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
