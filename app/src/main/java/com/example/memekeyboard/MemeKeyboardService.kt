package com.example.memekeyboard.ui

import android.inputmethodservice.InputMethodService
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.example.memekeyboard.R

class MemeKeyboardService : InputMethodService() {

    private var isCaps = false
    private val searchBuffer = StringBuilder()

    override fun onCreateInputView(): View {
        val view = layoutInflater.inflate(R.layout.meme_keyboard_layout, null)
        val fakeSearchBox = view.findViewById<TextView>(R.id.search_input_fake)

        val keys = listOf(
            "a", "b", "c", "d", "e", "f", "g", "h", "i", "j",
            "k", "l", "m", "n", "o", "p", "q", "r", "s", "t",
            "u", "v", "w", "x", "y", "z"
        )

        // Handle letter keys
        keys.forEach { key ->
            val buttonId = resources.getIdentifier("key_$key", "id", packageName)
            val button = view.findViewById<Button>(buttonId)
            button?.setOnClickListener {
                val charToAdd = if (isCaps) key.uppercase() else key
                searchBuffer.append(charToAdd)
                fakeSearchBox.text = searchBuffer.toString()
                // TODO: Filter memes based on searchBuffer.toString()
            }
        }

        // Space key
        view.findViewById<Button>(R.id.key_space).setOnClickListener {
            searchBuffer.append(" ")
            fakeSearchBox.text = searchBuffer.toString()
        }

        // Delete key
        view.findViewById<Button>(R.id.key_delete).setOnClickListener {
            if (searchBuffer.isNotEmpty()) {
                searchBuffer.deleteCharAt(searchBuffer.length - 1)
                fakeSearchBox.text = searchBuffer.toString()
            }
        }

        // Caps lock toggle
        view.findViewById<Button>(R.id.key_caps).setOnClickListener {
            isCaps = !isCaps
        }

        return view
    }
}
