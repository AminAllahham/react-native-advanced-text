package com.advancedtext

import android.graphics.Paint
import android.text.style.LineHeightSpan
import kotlin.math.ceil
import kotlin.math.floor

/**
 * A [LineHeightSpan] that forces every line's box to exactly [lineHeightPx],
 * following CSS `line-height` semantics: the difference between the target
 * height and the font's natural ascent+descent ("leading") is split evenly
 * above and below, and the very first line's top / very last line's bottom
 * are pinned back to the font's own ascent/descent so the half-leading at
 * the block's own edges doesn't inflate its total height.
 *
 * This exists because `AdvancedTextView`'s Fabric ShadowNode measures the
 * `lineHeight` prop through React Native's `TextLayoutManager`, which applies
 * `lineHeight` using this exact CSS-style algorithm internally -- a different
 * one from Android's own `TextView.setLineSpacing(0, multiplier)`. Rendering
 * with `setLineSpacing` instead of this span made the real height diverge
 * from the measured one by a per-line amount, which is why the discrepancy
 * grew with the number of wrapped lines.
 *
 * `start`/`end` here are the offsets of the *current line*, not the span's
 * own range, so the first/last checks below correctly fire only once each
 * per paragraph. Mirrors React Native's own
 * `com.facebook.react.views.text.internal.span.CustomLineHeightSpan`
 * (used internally for `<Text lineHeight={}>`) verbatim, so it inherits that
 * implementation's on-device track record instead of a new one.
 *
 * See: https://www.w3.org/TR/css-inline-3/#inline-height
 */
internal class CssLineHeightSpan(private val lineHeightPx: Int) : LineHeightSpan {

    override fun chooseHeight(
        text: CharSequence,
        start: Int,
        end: Int,
        spanstartv: Int,
        v: Int,
        fm: Paint.FontMetricsInt
    ) {
        val leading = lineHeightPx - ((-fm.ascent) + fm.descent)
        fm.ascent -= ceil(leading / 2.0f).toInt()
        fm.descent += floor(leading / 2.0f).toInt()

        if (start == 0) {
            fm.top = fm.ascent
        }
        if (end == text.length) {
            fm.bottom = fm.descent
        }
    }
}
