package com.example.memekeyboard

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.room.Room
import com.example.memekeyboard.data.Meme
import com.example.memekeyboard.data.MemeDatabase
import com.example.memekeyboard.data.MemeRepository
import com.example.memekeyboard.ui.MainScreen
import com.example.memekeyboard.ui.theme.MemeKeyboardTheme
import com.example.memekeyboard.viewmodel.MemeViewModel
import com.example.memekeyboard.viewmodel.MemeViewModelFactory

class MainActivity : ComponentActivity() {

    private val memeViewModel: MemeViewModel by lazy {
        val db = Room.databaseBuilder(
            applicationContext,
            MemeDatabase::class.java, "meme-database"
        ).build()
        val repository = MemeRepository(db.memeDao())
        MemeViewModelFactory(repository).create(MemeViewModel::class.java)
    }

    private lateinit var pickImageLauncher: ActivityResultLauncher<String>
    private val pendingImageUri = mutableStateOf<Uri?>(null)
    private val showTagDialog = mutableStateOf(false)
    private val tagInput = mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                pendingImageUri.value = it
                showTagDialog.value = true
            }
        }

        setContent {
            MemeKeyboardTheme {

                if (showTagDialog.value && pendingImageUri.value != null) {
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = {
                            showTagDialog.value = false
                            pendingImageUri.value = null
                            tagInput.value = ""
                        },
                        confirmButton = {
                            androidx.compose.material3.TextButton(onClick = {
                                val tags = tagInput.value.ifBlank { "untagged" }
                                memeViewModel.insertMeme(
                                    Meme(imagePath = pendingImageUri.value.toString(), tags = tags)
                                )
                                showTagDialog.value = false
                                pendingImageUri.value = null
                                tagInput.value = ""
                            }) {
                                androidx.compose.material3.Text("Save")
                            }
                        },
                        dismissButton = {
                            androidx.compose.material3.TextButton(onClick = {
                                showTagDialog.value = false
                                pendingImageUri.value = null
                                tagInput.value = ""
                            }) {
                                androidx.compose.material3.Text("Cancel")
                            }
                        },
                        title = { androidx.compose.material3.Text("Add Tags") },
                        text = {
                            androidx.compose.material3.OutlinedTextField(
                                value = tagInput.value,
                                onValueChange = { tagInput.value = it },
                                label = { androidx.compose.material3.Text("Tags (comma-separated)") }
                            )
                        }
                    )
                }

                MainScreen(
                    viewModel = memeViewModel,
                    onPickImage = { pickImageLauncher.launch("image/*") }
                )
            }
        }
    }
}
