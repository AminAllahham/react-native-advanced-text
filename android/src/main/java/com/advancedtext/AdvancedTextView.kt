package com.advancedtext

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Point
import android.graphics.RectF
import android.os.Build
import android.text.Layout
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
import android.view.ViewConfiguration
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
    // First and last word index the selection touches, -1 for none.
    private var lastSelectedWords: Pair<Int, Int> = -1 to -1
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

    // Reused on every draw for the highlight backgrounds.
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rangePath = Path()
    private val lineClipPath = Path()
    private val rangeBounds = RectF()
    private val underlineBounds = RectF()

    // Double taps are only told apart while the app handles them
    // (onWordDoublePress); otherwise a tap fires onWordPress at once.
    private var wordDoublePressEnabled = false
    private val doubleTapTimeout = ViewConfiguration.getDoubleTapTimeout().toLong()
    private var doubleTapSlopSquare = 0
    private var lastTapUpTime = 0L
    private var lastTapUpX = 0f
    private var lastTapUpY = 0f
    // From a double tap's second touch down to its up. The TextView never sees
    // those events, so it can't select the word as it would on a double tap.
    private var handlingDoubleTap = false
    // A tap's onWordPress, held back while a second tap could still come.
    private var pendingWordPress: Runnable? = null
    private var lastDownX = 0f
    private var lastDownY = 0f
    private var longPressed = false

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

        val slop = ViewConfiguration.get(context).scaledDoubleTapSlop
        doubleTapSlopSquare = slop * slop

        // Reports the word, then returns false so the TextView still starts a
        // selection as usual.
        setOnLongClickListener {
            longPressed = true
            wordAt(lastDownX, lastDownY)?.let {
                sendWordEvent("onWordLongPress", it.word, it.index)
            }
            false
        }

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
                    lastSelectedWords = wordIndexRange(selectionStart, selectionEnd)

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

        highlightedWords.forEach { highlightedWord ->
            val wordPos = wordPositions.find { it.index == highlightedWord.index } ?: return@forEach
            if (highlightedWord.underline) {
                // The word, but not the space after it.
                drawRangeUnderline(
                    canvas,
                    currentLayout,
                    wordPos.start,
                    wordPos.end.coerceAtMost(currentText.length),
                    parseColor(highlightedWord.highlightColor)
                )
            } else {
                drawRangeBackground(
                    canvas,
                    currentLayout,
                    wordPos.start,
                    wordPos.extendedEnd.coerceAtMost(currentText.length),
                    parseColor(highlightedWord.highlightColor),
                    highlightedWord.borderRadius * density
                )
            }
        }

        // Draw indicator background only in Highlight mode
        if (indicatorMode != "fill" && indicatorWordIndex >= 0 && indicatorColor.isNotEmpty()) {
            val wordPos = wordPositions.find { it.index == indicatorWordIndex }
            if (wordPos != null) {
                drawRangeBackground(
                    canvas,
                    currentLayout,
                    wordPos.start,
                    wordPos.end.coerceAtMost(currentText.length),
                    parseColor(indicatorColor),
                    0f
                )
            }
        }
    }

    // Fills the background of text[start, end), one box per line.
    private fun drawRangeBackground(
        canvas: Canvas,
        layout: Layout,
        start: Int,
        end: Int,
        color: Int,
        radius: Float
    ) {
        highlightPaint.color = color
        forEachLineOfRange(layout, start, end) {
            canvas.drawRoundRect(rangeBounds, radius, radius, highlightPaint)
        }
    }

    // Draws a line under text[start, end), one per line it spans, just below
    // the baseline like a text underline.
    private fun drawRangeUnderline(
        canvas: Canvas,
        layout: Layout,
        start: Int,
        end: Int,
        color: Int
    ) {
        val density = resources.displayMetrics.density
        val thickness = UNDERLINE_THICKNESS_DP * density
        val gap = UNDERLINE_GAP_DP * density
        highlightPaint.color = color
        forEachLineOfRange(layout, start, end) { line ->
            val top = layout.getLineBaseline(line) + totalPaddingTop + gap
            underlineBounds.set(rangeBounds.left, top, rangeBounds.right, top + thickness)
            canvas.drawRoundRect(underlineBounds, thickness / 2, thickness / 2, highlightPaint)
        }
    }

    // Calls draw for each line text[start, end) spans, with rangeBounds set to
    // that line's part of the range in view coordinates. The edges come from
    // Layout.getSelectionPath, which follows the text's direction: a
    // right-to-left word (Arabic) starts at its right edge, so treating
    // getPrimaryHorizontal(start) as the left edge put boxes in the wrong place.
    private inline fun forEachLineOfRange(
        layout: Layout,
        start: Int,
        end: Int,
        draw: (line: Int) -> Unit
    ) {
        if (start >= end) return

        val startLine = layout.getLineForOffset(start)
        val endLine = layout.getLineForOffset(end - 1)
        for (line in startLine..endLine) {
            val segmentStart = maxOf(start, layout.getLineStart(line))
            val segmentEnd = minOf(end, layout.getLineEnd(line))
            if (segmentStart >= segmentEnd) continue

            rangePath.reset()
            layout.getSelectionPath(segmentStart, segmentEnd, rangePath)

            // A range reaching the end of a line counts as running onto the
            // next one, so the selection path fills out to the view's edge.
            // Clip to this line's text to stop the box at the last word.
            lineClipPath.reset()
            lineClipPath.addRect(
                layout.getLineLeft(line),
                layout.getLineTop(line).toFloat(),
                layout.getLineRight(line),
                layout.getLineBottom(line).toFloat(),
                Path.Direction.CW
            )
            rangePath.op(lineClipPath, Path.Op.INTERSECT)
            rangePath.computeBounds(rangeBounds, true)
            if (rangeBounds.isEmpty) continue

            rangeBounds.offset(totalPaddingLeft.toFloat(), totalPaddingTop.toFloat())
            draw(line)
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
                putInt("startWordIndex", lastSelectedWords.first)
                putInt("endWordIndex", lastSelectedWords.second)
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
            if (!wordDoublePressEnabled) {
                sendWordEvent("onWordPress", word, wordIndex)
                return
            }
            // Wait to see whether this is the first tap of a double tap.
            cancelPendingWordPress()
            val press = Runnable {
                pendingWordPress = null
                sendWordEvent("onWordPress", word, wordIndex)
            }
            pendingWordPress = press
            postDelayed(press, doubleTapTimeout)
        }

        // Skips super, which would paint the word in the link color and
        // underline it. The word's color comes from its ForegroundColorSpan.
        override fun updateDrawState(ds: TextPaint) {
            ds.isUnderlineText = false
        }
    }

    // onWordPress, onWordDoublePress or onWordLongPress.
    private fun sendWordEvent(eventName: String, word: String, index: Int) {
        try {
            val reactContext = context as? ReactContext ?: return
            val event = Arguments.createMap().apply {
                putString("word", word)
                putInt("index", index)
            }

            reactContext.getJSModule(RCTEventEmitter::class.java)
                .receiveEvent(id, eventName, event)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending $eventName event", e)
        }
    }

    fun setWordDoublePressEnabled(enabled: Boolean) {
        if (wordDoublePressEnabled == enabled) return
        wordDoublePressEnabled = enabled
        lastTapUpTime = 0L
        // A tap held back for a double tap fires now instead of being lost.
        pendingWordPress?.let {
            removeCallbacks(it)
            it.run()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastDownX = event.x
                lastDownY = event.y
                longPressed = false
                if (wordDoublePressEnabled && isSecondTap(event)) {
                    cancelPendingWordPress()
                    handlingDoubleTap = true
                    return true
                }
            }
            MotionEvent.ACTION_UP -> if (handlingDoubleTap) {
                handlingDoubleTap = false
                lastTapUpTime = 0L
                wordAt(event.x, event.y)?.let {
                    sendWordEvent("onWordDoublePress", it.word, it.index)
                }
                return true
            }
            MotionEvent.ACTION_CANCEL -> if (handlingDoubleTap) {
                handlingDoubleTap = false
                lastTapUpTime = 0L
                return true
            }
        }
        if (handlingDoubleTap) return true

        val handled = super.onTouchEvent(event)
        if (event.actionMasked == MotionEvent.ACTION_UP) {
            // A long press isn't the first tap of a double tap.
            if (longPressed) {
                lastTapUpTime = 0L
            } else {
                lastTapUpTime = event.eventTime
                lastTapUpX = event.x
                lastTapUpY = event.y
            }
        }
        return handled
    }

    override fun onDetachedFromWindow() {
        cancelPendingWordPress()
        super.onDetachedFromWindow()
    }

    private fun isSecondTap(down: MotionEvent): Boolean {
        if (lastTapUpTime == 0L || down.eventTime - lastTapUpTime > doubleTapTimeout) {
            return false
        }
        val dx = down.x - lastTapUpX
        val dy = down.y - lastTapUpY
        return dx * dx + dy * dy < doubleTapSlopSquare
    }

    private fun cancelPendingWordPress() {
        pendingWordPress?.let { removeCallbacks(it) }
        pendingWordPress = null
    }

    // The word under a point in view coordinates, or null for blank space,
    // including the space past the end of a line.
    private fun wordAt(x: Float, y: Float): WordPosition? {
        val currentLayout = layout ?: return null
        val line = currentLayout.getLineForVertical((y - totalPaddingTop + scrollY).toInt())
        val lineX = x - totalPaddingLeft + scrollX
        if (lineX < currentLayout.getLineLeft(line) || lineX > currentLayout.getLineRight(line)) {
            return null
        }
        val offset = getOffsetForPosition(x, y)
        return wordPositions.find { offset >= it.start && offset <= it.end }
    }

    // First and last word text[start, end) touches, or -1 to -1 for none.
    private fun wordIndexRange(start: Int, end: Int): Pair<Int, Int> {
        val touched = wordPositions.filter { it.start < end && it.end > start }
        if (touched.isEmpty()) return -1 to -1
        return touched.first().index to touched.last().index
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
    val borderRadius: Float = 0f,
    // Draws a line under the word instead of filling its background.
    val underline: Boolean = false
)

private const val UNDERLINE_THICKNESS_DP = 1.5f
private const val UNDERLINE_GAP_DP = 2.5f
