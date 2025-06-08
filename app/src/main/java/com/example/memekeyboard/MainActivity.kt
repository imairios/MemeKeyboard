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
        pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                pendingImageUri = it
                showTagDialog = true
            }
        }

        setContent {
            MemeKeyboardTheme {
                // 3) Αν πρέπει να δείξουμε διάλογο για tags
                if (showTagDialog && pendingImageUri != null) {
                    AlertDialog(
                        onDismissRequest = {
                            showTagDialog = false
                            pendingImageUri = null
                            tagInput = ""
                        },
                        title = {
                            Text("Add Tags")
                        },
                        text = {
                            OutlinedTextField(
                                value = tagInput,
                                onValueChange = { tagInput = it },
                                label = { Text("Tags (comma-separated)") }
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                // Όταν πατάμε Save:
                                val tags = tagInput.ifBlank { "untagged" }
                                // Καλούμε το ViewModel να κάνει insert
                                memeViewModel.insertMeme(
                                    Meme(
                                        imagePath = pendingImageUri.toString(),
                                        tags = tags
                                    )
                                )
                                // Κλείνουμε τον διάλογο
                                showTagDialog = false
                                pendingImageUri = null
                                tagInput = ""
                            }) {
                                Text("Save")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                showTagDialog = false
                                pendingImageUri = null
                                tagInput = ""
                            }) {
                                Text("Cancel")
                            }
                        }
                    )
                }

                // 4) Καλούμε το MainScreen, περνώντας viewModel & onPickImage
                MainScreen(
                    viewModel = memeViewModel,
                    onPickImage = { pickImageLauncher.launch("image/*") }
                )
            }
        }
    }
}
