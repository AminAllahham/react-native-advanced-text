// File: AdvancedTextViewManager.kt
package com.advancedtext

import android.graphics.Color
import android.util.TypedValue
import android.view.ViewGroup
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.module.annotations.ReactModule
import com.facebook.react.uimanager.SimpleViewManager
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.annotations.ReactProp

@ReactModule(name = AdvancedTextViewManager.NAME)
class AdvancedTextViewManager : SimpleViewManager<AdvancedTextView>() {

    override fun getName(): String {
        return NAME
    }

    public override fun createViewInstance(context: ThemedReactContext): AdvancedTextView {
        val view = AdvancedTextView(context)
        view.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        // Set default text color to black to ensure visibility
        view.setTextColor(Color.BLACK)
        android.util.Log.d(NAME, "createViewInstance: View created")
        return view
    }

    @ReactProp(name = "text")
    fun setText(view: AdvancedTextView?, text: String?) {
        android.util.Log.d(NAME, "setText called with: '$text'")
        view?.setAdvancedText(text ?: "")
    }

    @ReactProp(name = "highlightedWords")
    fun setHighlightedWords(view: AdvancedTextView?, highlightedWords: ReadableArray?) {
        if (highlightedWords == null) {
            view?.setHighlightedWords(emptyList())
            return
        }

        val words = mutableListOf<HighlightedWord>()
        for (i in 0 until highlightedWords.size()) {
            val map = highlightedWords.getMap(i)
            map?.let { wordMap ->
                if (wordMap.hasKey("index") && wordMap.hasKey("highlightColor")) {
                    words.add(
                        HighlightedWord(
                            index = wordMap.getInt("index"),
                            highlightColor = wordMap.getString("highlightColor") ?: "#FFFF00"
                        )
                    )
                }
            }
        }
        android.util.Log.d(NAME, "setHighlightedWords: ${words.size} words")
        view?.setHighlightedWords(words)
    }

    @ReactProp(name = "menuOptions")
    fun setMenuOptions(view: AdvancedTextView?, menuOptions: ReadableArray?) {
        if (menuOptions == null) {
            view?.setMenuOptions(emptyList())
            return
        }

        val options = mutableListOf<String>()
        for (i in 0 until menuOptions.size()) {
            menuOptions.getString(i)?.let { option ->
                options.add(option)
            }
        }
        android.util.Log.d(NAME, "setMenuOptions: ${options.size} options")
        view?.setMenuOptions(options)
    }

    @ReactProp(name = "indicatorWordIndex")
    fun setIndicatorWordIndex(view: AdvancedTextView?, index: Int) {
        android.util.Log.d(NAME, "setIndicatorWordIndex: $index")
        view?.setIndicatorWordIndex(if (index >= 0) index else -1)
    }

    @ReactProp(name = "color", customType = "Color")
    fun setColor(view: AdvancedTextView?, color: Int?) {
        android.util.Log.d(NAME, "setColor called with: $color")
        if (color != null) {
            view?.setAdvancedTextColor(color)
        }
    }

    @ReactProp(name = "fontSize")
    fun setFontSize(view: AdvancedTextView?, fontSize: Float) {
        android.util.Log.d(NAME, "setFontSize called with: $fontSize")
        if (fontSize > 0) {
            view?.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize)
        }
    }

    override fun getExportedCustomDirectEventTypeConstants(): Map<String, Any> {
        return mapOf(
            "onWordPress" to mapOf("registrationName" to "onWordPress"),
            "onSelection" to mapOf("registrationName" to "onSelection")
        )
    }

    companion object {
        const val NAME = "AdvancedTextView"
    }
}
