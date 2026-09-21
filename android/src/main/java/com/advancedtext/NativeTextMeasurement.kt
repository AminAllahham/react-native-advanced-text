package com.advancedtext

import android.content.res.Resources
import android.graphics.Typeface
import android.text.StaticLayout
import android.text.TextPaint
import android.util.TypedValue

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
 * -- font resolution, text size (SP, not raw px), `setLineSpacing`, and
 * letter spacing. Spans (highlight/click color) are omitted: they don't
 * affect font metrics or line breaking, only drawing.
 */
object NativeTextMeasurement {

    @JvmStatic
    fun measureHeight(
        text: String,
        fontFamily: String,
        typefaceStyle: Int,
        fontSizeSp: Float,
        lineHeightMultiplier: Float,
        letterSpacingDp: Float,
        widthPx: Float
    ): Float {
        if (text.isEmpty() || widthPx <= 0f) {
            return 0f
        }

        val displayMetrics = Resources.getSystem().displayMetrics
        val fontSizePx =
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, fontSizeSp, displayMetrics)

        val paint = TextPaint(TextPaint.ANTI_ALIAS_FLAG)
        paint.typeface = Typeface.create(fontFamily, typefaceStyle)
        paint.textSize = fontSizePx
        // Mirrors AdvancedTextView's own letterSpacing conversion: dp/points
        // prop -> em, scale-invariant so no density conversion is needed.
        if (letterSpacingDp != 0f && fontSizeSp > 0f) {
            paint.letterSpacing = letterSpacingDp / fontSizeSp
        }

        val width = widthPx.toInt().coerceAtLeast(1)
        val builder = StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
        // `setLineSpacing(0, multiplier)` is exactly what AdvancedTextView
        // itself applies for rendering -- see updateTextWithHighlights().
        builder.setLineSpacing(0f, if (lineHeightMultiplier > 0f) lineHeightMultiplier else 1f)
        val layout = builder.build()
        return layout.height.toFloat()
    }
}
