package com.advancedtext

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Point
import android.graphics.RectF
import android.os.Build
import android.text.SpannableString
import android.text.Spannable
import android.text.Spanned
import android.text.TextPaint
import android.text.method.ArrowKeyMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.util.AttributeSet
import android.util.Log
import android.view.ActionMode
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.MotionEvent
import android.view.accessibility.AccessibilityNodeInfo
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
    private var lastSelectedText: String = ""
    private var customActionMode: ActionMode? = null
    private var currentText: String = ""
    private var textColor: String = "#000000"
    private var fontSize: Float = 16f
    private var fontWeight: String = "normal"
    // "" = unset: natural alignment (start of the text's own direction).
    private var textAlign: String = ""
    private var fontFamily: String = "sans-serif"
    private var lineHeightMultiplier: Float = 1.0f
    // Value from JS, in dp/points (0 = unset). Converted to em for TextView.
    private var letterSpacingDp: Float = 0f

    private var indicatorWordIndex: Int = -1
    private var indicatorColor: String = ""
    private var indicatorMode: String = "Highlight"

    private var wordPositions: List<WordPosition> = emptyList()
    // Each word's color span, kept so color changes can be applied to the live
    // text. setText() would clear any selection the user is making.
    private var wordColorSpans: Array<ForegroundColorSpan?> = emptyArray()

    constructor(context: Context?) : super(context) { init() }
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) { init() }
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) { init() }

    private fun init() {
        Log.d(TAG, "AdvancedTextView initialized")

        textSize = 16f
        // Zero intrinsic padding so the rendered text matches the size reported
        // by the Fabric ShadowNode's measureContent(). Consumers add padding via
        // React Native `style` instead, exactly like <Text>.
        setPadding(0, 0, 0, 0)
        setTextIsSelectable(true)
        // Fabric measures this component's text via RN's own TextLayoutManager,
        // which explicitly builds its StaticLayout with
        // `setUseLineSpacingFromFallbacks(true)`. TextView's own equivalent
        // (`setFallbackLineSpacing`) defaults differently depending on
        // targetSdkVersion, so without this the two engines can compute
        // slightly different natural line heights for the same font/text.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            setFallbackLineSpacing(true)
        }

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
        val color = String.format("#%06X", 0xFFFFFF and colorInt)
        if (textColor == color) return
        textColor = color
        updateWordColors()
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
        applyTextAlign()
    }

    // "left"/"right" are absolute edges, as on iOS. TEXT_START/TEXT_END follow
    // the paragraph's direction, so RTL text (Arabic) with "right" would land
    // on the left. Only TEXT_ALIGNMENT_GRAVITY with Gravity.LEFT/RIGHT is absolute.
    private fun applyTextAlign() {
        val vertical = gravity and Gravity.VERTICAL_GRAVITY_MASK
        when (textAlign) {
            "left" -> {
                gravity = vertical or Gravity.LEFT
                textAlignment = View.TEXT_ALIGNMENT_GRAVITY
            }
            "right" -> {
                gravity = vertical or Gravity.RIGHT
                textAlignment = View.TEXT_ALIGNMENT_GRAVITY
            }
            "center" -> textAlignment = View.TEXT_ALIGNMENT_CENTER
            else -> textAlignment = View.TEXT_ALIGNMENT_TEXT_START
        }
    }

    fun setAdvancedLineHeight(multiplier: Float) {
        if (lineHeightMultiplier == multiplier) return
        lineHeightMultiplier = multiplier
        updateTextWithHighlights()
    }

    fun setAdvancedLetterSpacing(spacingDp: Float) {
        if (letterSpacingDp == spacingDp) return
        letterSpacingDp = spacingDp
        updateTextWithHighlights()
    }


    fun setAdvancedFontFamily(family: String) {
        if (fontFamily == family) return
        fontFamily = family
        typeface = NativeTextMeasurement.resolveTypeface(family, Typeface.NORMAL)
    }

    fun setMenuOptions(menuOptions: List<String>) {
        if (this.menuOptions == menuOptions) return
        this.menuOptions = menuOptions
    }

    fun setIndicatorWordIndex(index: Int) {
        if (indicatorWordIndex == index) return
        indicatorWordIndex = index
        if (indicatorMode == "fill") {
            updateWordColors()
        } else {
            invalidate()
        }
    }

    fun setIndicatorColor(color: String) {
        if (indicatorColor == color) return
        indicatorColor = color
        if (indicatorMode == "fill") {
            updateWordColors()
        } else {
            invalidate()
        }
    }

    fun setIndicatorMode(mode: String) {
        if (indicatorMode == mode) return
        indicatorMode = mode
        updateWordColors()
        invalidate()
    }

    fun setHighlightedWords(highlightedWords: List<HighlightedWord>) {
        if (this.highlightedWords == highlightedWords) return
        this.highlightedWords = highlightedWords
        updateTextWithHighlights()
    }

    private fun calculateWordPositions(text: String) {
        if (text.isEmpty()) {
            wordPositions = emptyList()
            return
        }

        val positions = mutableListOf<WordPosition>()
        val regex = "\\S+".toRegex()
        val matches = regex.findAll(text).toList()

        matches.forEachIndexed { index, match ->
            val wordEnd = match.range.last + 1
            val extendedEnd = if (index + 1 < matches.size) matches[index + 1].range.first else text.length
            positions.add(
                WordPosition(
                    index = index,
                    start = match.range.first,
                    end = wordEnd,
                    extendedEnd = extendedEnd,
                    word = match.value
                )
            )
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
            spannableString.setSpan(
                WordClickableSpan(
                    wordIndex = wordPos.index,
                    word = wordPos.word
                ),
                wordPos.start,
                wordPos.end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        wordColorSpans = arrayOfNulls(wordPositions.size)
        applyWordColors(spannableString)

        applyTextAlign()

        setTextSize(fontSize)

        typeface = when (fontWeight) {
            "bold" -> NativeTextMeasurement.resolveTypeface(fontFamily, Typeface.BOLD)
            "italic" -> NativeTextMeasurement.resolveTypeface(fontFamily, Typeface.ITALIC)
            else -> NativeTextMeasurement.resolveTypeface(fontFamily, Typeface.NORMAL)
        }


        setLineSpacing(0f, lineHeightMultiplier)

        // Android's letterSpacing is in em; the prop is in dp/points. The ratio
        // dp / fontSize is scale-invariant, so no density conversion is needed.
        letterSpacing = if (letterSpacingDp != 0f && fontSize > 0f) {
            letterSpacingDp / fontSize
        } else {
            0f
        }


        // Set synchronously, not via `post {}`: in a virtualized list (e.g.
        // FlashList) this view gets recycled for a new row on every scroll
        // tick. A posted `setText` races the *next* recycle -- if this view
        // is reused again before the queued runnable fires, whichever row's
        // runnable happens to run last wins, and a row can end up rendering
        // blank (with its correctly-measured height still reserved by
        // Fabric) while its own `setText` was dropped. `setText` doesn't
        // need to wait for anything here, so there's nothing to defer for.
        setText(spannableString, BufferType.SPANNABLE)
        Log.d(TAG, "Text updated with ${wordPositions.size} spans")
    }

    // Recolors words on the live text instead of rebuilding it, so the
    // indicator can advance while the user is selecting text. Span changes go
    // through TextView's ChangeWatcher, which redraws just the changed region
    // and keeps the selection and its action mode.
    private fun updateWordColors() {
        if (currentText.isEmpty()) return
        val spannable = text as? Spannable
        if (spannable == null ||
            spannable.length != currentText.length ||
            wordColorSpans.size != wordPositions.size) {
            updateTextWithHighlights()
            return
        }
        applyWordColors(spannable)
    }

    // Only words whose color changed get a new span, so an indicator step
    // touches one or two words rather than the whole paragraph.
    private fun applyWordColors(spannable: Spannable) {
        val baseColor = parseColor(textColor)
        val fillColor = if (indicatorMode == "fill" &&
                            indicatorWordIndex >= 0 &&
                            indicatorColor.isNotEmpty()) {
            parseColor(indicatorColor)
        } else {
            null
        }

        wordPositions.forEach { wordPos ->
            val color = if (fillColor != null && wordPos.index <= indicatorWordIndex) {
                fillColor
            } else {
                baseColor
            }
            val current = wordColorSpans[wordPos.index]
            if (current?.foregroundColor == color) return@forEach

            current?.let { spannable.removeSpan(it) }
            val span = ForegroundColorSpan(color)
            spannable.setSpan(span, wordPos.start, wordPos.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            wordColorSpans[wordPos.index] = span
        }
    }

    // Every word is a ClickableSpan so a tap can report it, but TalkBack treats
    // each span as a link: it reads the text one word at a time and lists every
    // word under Actions. Plain text keeps it reading as prose; granularity
    // navigation still works since the characters are identical.
    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        info.text = text?.toString()
    }

    override fun onDraw(canvas: Canvas) {
        drawHighlightedWordBackgrounds(canvas)
        super.onDraw(canvas)
    }

    private fun drawHighlightedWordBackgrounds(canvas: Canvas) {
        val currentLayout = layout ?: return
        val density = resources.displayMetrics.density
        val originalColor = paint.color

        highlightedWords.forEach { highlightedWord ->
            val wordPos = wordPositions.find { it.index == highlightedWord.index } ?: return@forEach
            val start = wordPos.start
            val end = wordPos.extendedEnd.coerceAtMost(currentText.length)

            if (start >= end) return@forEach

            val startLine = currentLayout.getLineForOffset(start)
            val safeEndOffset = (end - 1).coerceAtLeast(start)
            val endLine = currentLayout.getLineForOffset(safeEndOffset)

            paint.color = parseColor(highlightedWord.highlightColor)

            for (line in startLine..endLine) {
                val lineStartOffset = maxOf(start, currentLayout.getLineStart(line))
                val lineEndOffset = minOf(end, currentLayout.getLineEnd(line))
                if (lineStartOffset >= lineEndOffset) continue

                val left = currentLayout.getPrimaryHorizontal(lineStartOffset)
                val isLastWordOnLine = lineEndOffset >= currentLayout.getLineEnd(line)
                val right = if (isLastWordOnLine) {
                    currentLayout.getLineRight(line)
                } else {
                    currentLayout.getPrimaryHorizontal(lineEndOffset)
                }
                val top = currentLayout.getLineTop(line).toFloat()
                val bottom = currentLayout.getLineBottom(line).toFloat()
                val rect = RectF(
                    totalPaddingLeft + left,
                    totalPaddingTop + top,
                    totalPaddingLeft + right,
                    totalPaddingTop + bottom
                )
                val radius = highlightedWord.borderRadius * density
                canvas.drawRoundRect(rect, radius, radius, paint)
            }
        }

        // Draw indicator background only in Highlight mode
        if (indicatorMode != "fill" && indicatorWordIndex >= 0 && indicatorColor.isNotEmpty()) {
            val wordPos = wordPositions.find { it.index == indicatorWordIndex }
            if (wordPos != null) {
                val start = wordPos.start
                val end = wordPos.end.coerceAtMost(currentText.length)

                if (start < end) {
                    val startLine = currentLayout.getLineForOffset(start)
                    val safeEndOffset = (end - 1).coerceAtLeast(start)
                    val endLine = currentLayout.getLineForOffset(safeEndOffset)

                    paint.color = parseColor(indicatorColor)

                    for (line in startLine..endLine) {
                        val lineStartOffset = maxOf(start, currentLayout.getLineStart(line))
                        val lineEndOffset = minOf(end, currentLayout.getLineEnd(line))
                        if (lineStartOffset >= lineEndOffset) continue

                        val left = currentLayout.getPrimaryHorizontal(lineStartOffset)
                        val isLastWordOnLine = lineEndOffset >= currentLayout.getLineEnd(line)
                        val right = if (isLastWordOnLine) {
                            currentLayout.getLineRight(line)
                        } else {
                            currentLayout.getPrimaryHorizontal(lineEndOffset)
                        }
                        val top = currentLayout.getLineTop(line).toFloat()
                        val bottom = currentLayout.getLineBottom(line).toFloat()
                        val rect = RectF(
                            totalPaddingLeft + left,
                            totalPaddingTop + top,
                            totalPaddingLeft + right,
                            totalPaddingTop + bottom
                        )
                        canvas.drawRoundRect(rect, 0f, 0f, paint)
                    }
                }
            }
        }

        paint.color = originalColor
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

        // Skips super, which would paint the word in the link color and
        // underline it. The word's color comes from its ForegroundColorSpan.
        override fun updateDrawState(ds: TextPaint) {
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
        val extendedEnd: Int,
        val word: String
    )
}

data class HighlightedWord(
    val index: Int,
    val highlightColor: String,
    val borderRadius: Float = 0f
)
