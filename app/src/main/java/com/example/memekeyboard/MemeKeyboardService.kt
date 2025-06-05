package com.example.memekeyboard.ui

import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import com.example.memekeyboard.R

class MemeKeyboardService : InputMethodService() {

    private var isCaps = false
    private lateinit var searchInput: EditText

    override fun onCreateInputView(): View {
        val view = layoutInflater.inflate(R.layout.meme_keyboard_layout, null)

        searchInput = view.findViewById(R.id.search_input)

        // Show system keyboard when EditText gets focus
        searchInput.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(searchInput, InputMethodManager.SHOW_IMPLICIT)
            }
        }

        searchInput.requestFocus()

        // Handle key presses
        val keys = listOf(
            "a", "b", "c", "d", "e", "f", "g", "h", "i", "j",
            "k", "l", "m", "n", "o", "p", "q", "r", "s", "t",
            "u", "v", "w", "x", "y", "z"
        )

        keys.forEach { key ->
            val buttonId = resources.getIdentifier("key_$key", "id", packageName)
            val button = view.findViewById<Button>(buttonId)
            button?.setOnClickListener {
                val char = if (isCaps) key.uppercase() else key
                searchInput.append(char)
            }
        }

        // Handle space
        view.findViewById<Button>(R.id.key_space)?.setOnClickListener {
            searchInput.append(" ")
        }

        // Handle delete
        view.findViewById<Button>(R.id.key_delete)?.setOnClickListener {
            val currentText = searchInput.text.toString()
            if (currentText.isNotEmpty()) {
                searchInput.setText(currentText.dropLast(1))
                searchInput.setSelection(searchInput.text.length)
            }
        }

        // Handle caps lock
        val capsButton = view.findViewById<Button>(R.id.key_caps)
        capsButton?.setOnClickListener {
            isCaps = !isCaps
            capsButton.alpha = if (isCaps) 1.0f else 0.5f
        }

        // 🔹 Launch FloatingInputActivity for system keyboard
        val openInputBtn = view.findViewById<Button>(R.id.open_input_button)
        openInputBtn?.setOnClickListener {
            val intent = Intent(this, FloatingInputActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }

        return view
    }
}
