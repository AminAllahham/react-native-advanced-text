package com.advancedtext

import android.content.res.AssetManager
import android.content.res.Resources
import android.graphics.Typeface
import android.os.Build
import android.text.Layout
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextPaint
import android.text.style.MetricAffectingSpan
import android.util.TypedValue
import com.facebook.react.common.assets.ReactFontManager

/**
 * JNI bridge so the Fabric ShadowNode (C++ `measureContent()`) can ask
 * Android's own `StaticLayout` -- the exact class `AdvancedTextView`'s real
 * rendering is built on -- for this text's real height, instead of going
 * through React Native's own text-measurement engine (`TextLayoutManager`).
 *
 * RN's own engine and Android's native `TextView` rendering are two
 * independently-implemented paths that can drift apart by a few px per line
 * (verified: RN's `TextLayoutManager` takes a different code path --
 * `BoringLayout` vs `StaticLayout` -- for short/simple text than for real,
 * multi-line paragraphs, and even StaticLayout-to-StaticLayout the two
 * engines' resolved `Paint`/`Typeface` don't perfectly agree). Since this
 * component doesn't reuse RN's own prepared text layout for drawing anyway
 * (it re-renders through its own `TextView`), measuring with the same
 * `StaticLayout` call the view itself is built on removes the drift
 * entirely, rather than approximating it from the C++ side.
 *
 * Every parameter below is chosen to mirror exactly what `AdvancedTextView`
 * itself configures on its own `TextView`/`Paint` in `updateTextWithHighlights()`
 * -- font resolution, text size (SP, not raw px), `setLineSpacing`, letter
 * and word spacing, line breaking and hyphenation. Spans that only color or
 * highlight are omitted: they don't affect font metrics or line breaking.
 */
object NativeTextMeasurement {

    /** App assets, so fonts bundled under `assets/fonts/` resolve here too. */
    @Volatile
    var assetManager: AssetManager? = null

    /**
     * Resolves a font family the way React Native's own <Text> does: fonts
     * registered at runtime (expo-font), then `assets/fonts/<family>.ttf|otf`,
     * then system families. `AdvancedTextView` uses this too, so measuring and
     * rendering always pick the same typeface.
     */
    @JvmStatic
    fun resolveTypeface(fontFamily: String, style: Int): Typeface =
        ReactFontManager.getInstance().getTypeface(fontFamily, style, assetManager)

    /**
     * Widens every space in `spannable` by `extraEm` of the font size, on top
     * of the paragraph's letter spacing. Letter spacing applies to a space
     * like any other character, so a span on each space adds word spacing
     * without changing the text: word indexes, selections and the offsets
     * the app keeps stay as they were. React Native's <Text> spaces a nested
     * span the same way, so a copy laid out there breaks lines alike.
     */
    @JvmStatic
    fun applyWordSpacing(spannable: Spannable, extraEm: Float) {
        if (extraEm == 0f) return
        val text = spannable.toString()
        for (i in text.indices) {
            if (text[i] == ' ') {
                spannable.setSpan(
                    WordSpacingSpan(extraEm),
                    i,
                    i + 1,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }
    }

    @JvmStatic
    fun measureHeight(
        text: String,
        fontFamily: String,
        typefaceStyle: Int,
        fontSizeSp: Float,
        lineHeightMultiplier: Float,
        letterSpacingDp: Float,
        wordSpacingDp: Float,
        widthPx: Float
    ): Float {
        if (text.isEmpty() || widthPx <= 0f) {
            return 0f
        }

        val displayMetrics = Resources.getSystem().displayMetrics
        val fontSizePx =
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, fontSizeSp, displayMetrics)

        val paint = TextPaint(TextPaint.ANTI_ALIAS_FLAG)
        paint.typeface = resolveTypeface(fontFamily, typefaceStyle)
        paint.textSize = fontSizePx
        // Mirrors AdvancedTextView's own letterSpacing conversion: dp/points
        // prop -> em, scale-invariant so no density conversion is needed.
        if (letterSpacingDp != 0f && fontSizeSp > 0f) {
            paint.letterSpacing = letterSpacingDp / fontSizeSp
        }

        val content: CharSequence = if (wordSpacingDp != 0f && fontSizeSp > 0f) {
            SpannableString(text).also { applyWordSpacing(it, wordSpacingDp / fontSizeSp) }
        } else {
            text
        }

        val width = widthPx.toInt().coerceAtLeast(1)
        val builder = StaticLayout.Builder.obtain(content, 0, content.length, paint, width)
        // `setLineSpacing(0, multiplier)` is exactly what AdvancedTextView
        // itself applies for rendering -- see updateTextWithHighlights().
        builder.setLineSpacing(0f, if (lineHeightMultiplier > 0f) lineHeightMultiplier else 1f)
        // StaticLayout's own defaults differ from TextView's (simple breaking,
        // no fallback line spacing), so they're set to what the view uses.
        // A different break could make a paragraph a line taller or shorter
        // than the view draws it.
        builder.setBreakStrategy(Layout.BREAK_STRATEGY_HIGH_QUALITY)
        builder.setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            builder.setUseLineSpacingFromFallbacks(true)
        }
        val layout = builder.build()
        return layout.height.toFloat()
    }
}

/** Adds `extraEm` to the letter spacing of the text it covers: a space. */
class WordSpacingSpan(private val extraEm: Float) : MetricAffectingSpan() {
    // Each run starts from a fresh copy of the view's paint, so adding here
    // never stacks up.
    override fun updateMeasureState(textPaint: TextPaint) {
        textPaint.letterSpacing += extraEm
    }

    override fun updateDrawState(textPaint: TextPaint) {
        textPaint.letterSpacing += extraEm
    }
}
