package com.example.memekeyboard.ui

import android.app.Activity
import android.os.Bundle
import android.view.WindowManager
import android.widget.EditText
import com.example.memekeyboard.R

class FloatingInputActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_floating_input)

        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        )

        val editText = findViewById<EditText>(R.id.floating_input)
        editText.requestFocus()
    }
}
