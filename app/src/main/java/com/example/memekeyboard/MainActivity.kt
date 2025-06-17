package com.example.memekeyboard

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.example.memekeyboard.data.DatabaseProvider
import com.example.memekeyboard.data.Meme
import com.example.memekeyboard.data.MemeDatabase
import com.example.memekeyboard.data.MemeRepository
import com.example.memekeyboard.ui.MainScreen
import com.example.memekeyboard.ui.theme.MemeKeyboardTheme
import com.example.memekeyboard.viewmodel.MemeViewModel
import com.example.memekeyboard.viewmodel.MemeViewModelFactory
import kotlinx.coroutines.launch
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.view.inputmethod.InputMethodInfo

class MainActivity : ComponentActivity() {

    // 1) Δημιουργία ViewModel με lazy
    private val memeViewModel: MemeViewModel by lazy {
        val db = DatabaseProvider.getDatabase(applicationContext);

        val repository = MemeRepository(db.memeDao())
        MemeViewModelFactory(repository).create(MemeViewModel::class.java)
    }

    private lateinit var pickImageLauncher: ActivityResultLauncher<String>
    private var pendingImageUri by mutableStateOf<Uri?>(null)
    private var showTagDialog by mutableStateOf(false)
    private var tagInput by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 2) Ορισμός του launcher για επιλογή εικόνας (ActivityResult API)
        pickImageLauncher =
            registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
                uri?.let {
                    pendingImageUri = it
                    showTagDialog = true
                }
            }

        setContent {
            MemeKeyboardTheme {

                // ✅ Show this button if keyboard is NOT enabled
                if (!isKeyboardEnabled(this)) {
                    TextButton(onClick = { openKeyboardSettings(this) }) {
                        Text("Enable MemeKeyboard in Settings")
                    }
                }

                // Show tag dialog
                if (showTagDialog && pendingImageUri != null) {
                    AlertDialog(
                        onDismissRequest = {
                            showTagDialog = false
                            pendingImageUri = null
                            tagInput = ""
                        },
                        title = { Text("Add Tags") },
                        text = {
                            OutlinedTextField(
                                value = tagInput,
                                onValueChange = { tagInput = it },
                                label = { Text("Tags (comma-separated)") }
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                val tags = tagInput.ifBlank { "untagged" }
                                pendingImageUri?.let { uri ->
                                    memeViewModel.insertMeme(
                                        Meme(
                                            name = "meme_${System.currentTimeMillis()}",
                                            imagePath = uri.toString(),
                                            tags = tags
                                        )
                                    )
                                }
                                showTagDialog = false
                                pendingImageUri = null
                                tagInput = ""
                            }) { Text("Save") }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                showTagDialog = false
                                pendingImageUri = null
                                tagInput = ""
                            }) { Text("Cancel") }
                        }
                    )
                }

                // Main screen
                MainScreen(
                    viewModel = memeViewModel,
                    onPickImage = { pickImageLauncher.launch("image/*") }
                )
            }
        }
    }
}

fun isKeyboardEnabled(context: Context): Boolean {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    val enabledMethods = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_INPUT_METHODS)
    return enabledMethods?.contains(context.packageName) == true
}

fun openKeyboardSettings(context: Context) {
    val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
    context.startActivity(intent)
}
