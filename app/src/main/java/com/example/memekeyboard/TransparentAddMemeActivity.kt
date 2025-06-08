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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import com.example.memekeyboard.data.Meme
import com.example.memekeyboard.data.MemeDatabase
import com.example.memekeyboard.data.MemeRepository
import com.example.memekeyboard.viewmodel.MemeViewModel
import com.example.memekeyboard.viewmodel.MemeViewModelFactory

/**
 * A fully transparent, no‐title Activity that:
 * 1) Immediately launches the system image picker.
 * 2) When a (URI) result arrives, shows a Compose AlertDialog to enter comma-separated tags.
 * 3) Inserts the new Meme(imagePath, tags) into Room.
 * 4) Calls finish(), revealing the keyboard again.
 */
class TransparentAddMemeActivity : ComponentActivity() {

    private lateinit var pickImageLauncher: ActivityResultLauncher<String>
    private var pendingUri by mutableStateOf<Uri?>(null)
    private var showTagDialog by mutableStateOf(false)
    private var tagInput by mutableStateOf("")

    private lateinit var memeViewModel: MemeViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1) Build Room & ViewModel (using same "meme-database" as the IME)
        val db = Room.databaseBuilder(
            applicationContext,
            MemeDatabase::class.java,
            "meme-database"
        ).build()
        val repository = MemeRepository(db.memeDao())
        memeViewModel = ViewModelProvider(
            this,
            MemeViewModelFactory(repository)
        )[MemeViewModel::class.java]

        // 2) Register for “pick image” result
        pickImageLauncher = registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            uri?.let {
                pendingUri = it
                showTagDialog = true
            }
        }

        // 3) Immediately launch the system picker (image/*)
        pickImageLauncher.launch("image/*")

        // 4) Compose UI: once we have a URI and need tags, show a dialog
        setContent {
            if (showTagDialog && pendingUri != null) {
                TagInputDialog(
                    onConfirm = { enteredTags ->
                        val finalTags = enteredTags.ifBlank { "untagged" }
                        pendingUri?.let { chosenUri ->
                            memeViewModel.insertMeme(
                                Meme(
                                    imagePath = chosenUri.toString(),
                                    tags = finalTags
                                )
                            )
                        }
                        // Reset and finish
                        showTagDialog = false
                        pendingUri = null
                        finish()
                    },
                    onCancel = {
                        showTagDialog = false
                        pendingUri = null
                        finish()
                    }
                )
            }
        }
    }

    /** A small Compose dialog to collect comma-separated tags. */
    @Composable
    private fun TagInputDialog(
        onConfirm: (String) -> Unit,
        onCancel: () -> Unit
    ) {
        var textState by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { onCancel() },
            title = { Text("Add Tags to Meme") },
            text = {
                Column(Modifier.padding(8.dp)) {
                    OutlinedTextField(
                        value = textState,
                        onValueChange = { textState = it },
                        label = { Text("Tags (comma-separated)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { onConfirm(textState) }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { onCancel() }) {
                    Text("Cancel")
                }
            }
        )
    }
}
