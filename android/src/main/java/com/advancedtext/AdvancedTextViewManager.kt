package com.advancedtext

import android.view.ViewGroup
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.module.annotations.ReactModule
import com.facebook.react.uimanager.SimpleViewManager
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.ViewManagerDelegate
import com.facebook.react.uimanager.annotations.ReactProp
import com.facebook.react.viewmanagers.AdvancedTextViewManagerInterface
import com.facebook.react.viewmanagers.AdvancedTextViewManagerDelegate

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
        return view
    }

    @ReactProp(name = "text")
    override fun setText(view: AdvancedTextView?, text: String?) {
        view?.setAdvancedText(text ?: "")
    }

    @ReactProp(name = "fontSize", defaultFloat = 16f)
    override fun setFontSize(view: AdvancedTextView?, fontSize: Float) {
        view?.setFontSize(fontSize)
    }

    @ReactProp(name = "color")
    override fun setColor(view: AdvancedTextView?, color: String?) {
        view?.setTextColorProp(color)
    }

    @ReactProp(name = "highlightedWords")
    override fun setHighlightedWords(view: AdvancedTextView?, highlightedWords: ReadableArray?) {
        val words = mutableListOf<HighlightedWord>()
        highlightedWords?.let {
            for (i in 0 until it.size()) {
                val map = it.getMap(i)
                map?.let { wordMap ->
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
        val options = mutableListOf<String>()
        menuOptions?.let {
            for (i in 0 until it.size()) {
                it.getString(i)?.let { option ->
                    options.add(option)
                }
            }
        }
        view?.setMenuOptions(options)
    }

    @ReactProp(name = "indicatorWordIndex")
    override fun setIndicatorWordIndex(view: AdvancedTextView?, index: Int) {
        view?.setIndicatorWordIndex(index)
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
