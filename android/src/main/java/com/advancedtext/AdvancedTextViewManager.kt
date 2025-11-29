// File: AdvancedTextViewManager.kt
package com.advancedtext

import android.graphics.Color
import android.util.TypedValue
import android.view.ViewGroup
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.module.annotations.ReactModule
import com.facebook.react.uimanager.SimpleViewManager
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.ViewManagerDelegate
import com.facebook.react.uimanager.annotations.ReactProp
import com.facebook.react.viewmanagers.AdvancedTextViewManagerInterface
import com.facebook.react.viewmanagers.AdvancedTextViewManagerDelegate
import com.facebook.react.uimanager.PixelUtil

@ReactModule(name = AdvancedTextViewManager.NAME)
class AdvancedTextViewManager : SimpleViewManager<AdvancedTextView>(),
    AdvancedTextViewManagerInterface<AdvancedTextView> {

    private val mDelegate: ViewManagerDelegate<AdvancedTextView>

    init {
        mDelegate = AdvancedTextViewManagerDelegate(this)
    }

    override fun getDelegate(): ViewManagerDelegate<AdvancedTextView>? {
        return mDelegate
    }

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
        return view
    }

    @ReactProp(name = "text")
    override fun setText(view: AdvancedTextView?, text: String?) {
        android.util.Log.d("AdvancedTextViewManager", "setText called with: '$text'")
        view?.setAdvancedText(text ?: "")
    }

    @ReactProp(name = "highlightedWords")
    override fun setHighlightedWords(view: AdvancedTextView?, highlightedWords: ReadableArray?) {
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
        view?.setHighlightedWords(words)
    }

    @ReactProp(name = "menuOptions")
    override fun setMenuOptions(view: AdvancedTextView?, menuOptions: ReadableArray?) {
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
        view?.setMenuOptions(options)
    }

    @ReactProp(name = "indicatorWordIndex")
    override fun setIndicatorWordIndex(view: AdvancedTextView?, index: Int) {
        view?.setIndicatorWordIndex(if (index >= 0) index else -1)
    }

    @ReactProp(name = "color", customType = "Color")
    fun setColor(view: AdvancedTextView?, color: Int?) {
        android.util.Log.d("AdvancedTextViewManager", "setColor called with: $color")
        view?.setTextColor(color ?: Color.BLACK)
    }

    @ReactProp(name = "fontSize")
    fun setFontSize(view: AdvancedTextView?, fontSize: Float) {
        android.util.Log.d("AdvancedTextViewManager", "setFontSize called with: $fontSize")
        if (fontSize > 0) {
            // Convert from React Native points to Android pixels
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
