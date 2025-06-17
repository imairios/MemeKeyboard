package com.example.memekeyboard

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import com.example.memekeyboard.data.DatabaseProvider
import com.example.memekeyboard.data.Meme
import com.example.memekeyboard.data.MemeDatabase
import com.example.memekeyboard.data.MemeRepository
import com.example.memekeyboard.viewmodel.MemeViewModel
import com.example.memekeyboard.viewmodel.MemeViewModelFactory

class AddMemeActivity : ComponentActivity() {

    private lateinit var pickImageLauncher: ActivityResultLauncher<String>
    private var pendingUri by mutableStateOf<Uri?>(null)
    private var showTagDialog by mutableStateOf(false)
    private var tagInput by mutableStateOf("")

    private lateinit var memeViewModel: MemeViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Build Room & ViewModel
        val db = DatabaseProvider.getDatabase(applicationContext)




        val repository = MemeRepository(db.memeDao())
        memeViewModel = ViewModelProvider(
            this,
            MemeViewModelFactory(repository)
        )[MemeViewModel::class.java]

        // Register image‐picker
        pickImageLauncher = registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            uri?.let {
                pendingUri = it
                showTagDialog = true
            }
        }

        setContent {
            // If the user has not yet picked an image, show a “Pick Meme” button
            if (!showTagDialog && pendingUri == null) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Button(onClick = { pickImageLauncher.launch("image/*") }) {
                        Text("Pick Meme Image")
                    }
                }
            }

            // Once an image is picked, ask for tags
            if (showTagDialog && pendingUri != null) {
                AlertDialog(
                    onDismissRequest = {
                        showTagDialog = false
                        pendingUri = null
                        finish() // user cancelled, close the activity
                    },
                    title = { Text("Add Tags to Meme") },
                    text = {
                        OutlinedTextField(
                            value = tagInput,
                            onValueChange = { tagInput = it },
                            label = { Text("Tags (comma-separated)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            val finalTags = tagInput.ifBlank { "untagged" }
                            pendingUri?.let { chosenUri ->
                                memeViewModel.insertMeme(
                                    Meme(
                                        name = "meme_${System.currentTimeMillis()}", // or derive from URI
                                        imagePath = chosenUri.toString(),
                                        tags = finalTags
                                    )
                                )
                            }
                            showTagDialog = false
                            pendingUri = null
                            finish()
                        }) {
                            Text("Save")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            showTagDialog = false
                            pendingUri = null
                            finish()
                        }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}
