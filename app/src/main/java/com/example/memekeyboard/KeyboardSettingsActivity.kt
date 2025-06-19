package com.example.memekeyboard

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat

class KeyboardSettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_keyboard_settings)

        val toggle = findViewById<SwitchCompat>(R.id.vibration_toggle)
        val prefs = getSharedPreferences("keyboard_prefs", MODE_PRIVATE)

        toggle.isChecked = prefs.getBoolean("vibration_enabled", true)

        toggle.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("vibration_enabled", isChecked).apply()
        }
    }
}
