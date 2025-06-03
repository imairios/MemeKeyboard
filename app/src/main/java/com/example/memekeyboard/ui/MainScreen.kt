// ✅ MainScreen.kt
package com.example.memekeyboard.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.memekeyboard.data.Meme
import com.example.memekeyboard.viewmodel.MemeViewModel

@Composable
fun MainScreen(
    viewModel: MemeViewModel,
    modifier: Modifier = Modifier,
    onPickImage: () -> Unit
) {
    val context = LocalContext.current
    val memeList by viewModel.memes.collectAsState()
    var searchQuery by remember { mutableStateOf(TextFieldValue("")) }
    var confirmDelete by remember { mutableStateOf<Meme?>(null) }

    val filteredList = memeList.filter {
        searchQuery.text.isBlank() || it.tags.contains(searchQuery.text, ignoreCase = true)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 48.dp) // fix buttons too high
    ) {
        Button(onClick = onPickImage, modifier = Modifier.fillMaxWidth()) {
            Text("Pick Image from Gallery")
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search by tag") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn {
            items(filteredList) { meme ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .clickable { confirmDelete = meme }
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(meme.imagePath),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    )
                    Text(text = "Tags: ${meme.tags}")
                }
            }
        }

        confirmDelete?.let { meme ->
            AlertDialog(
                onDismissRequest = { confirmDelete = null },
                title = { Text("Delete meme?") },
                text = { Text("Are you sure you want to delete this meme?") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteMeme(meme)
                        confirmDelete = null
                    }) { Text("Yes") }
                },
                dismissButton = {
                    TextButton(onClick = { confirmDelete = null }) { Text("No") }
                }
            )
        }
    }
}
