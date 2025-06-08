package com.example.memekeyboard.ui

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.memekeyboard.data.Meme
import com.example.memekeyboard.viewmodel.MemeViewModel

/**
 * H MainScreen emfanizei:
 * 1) ena koumpi gia to pick image (pickImage())
 * 2) ena horizontal scroll me ta memes (thumbnails)
 *    - otan pataei meme, kalei to onMemeClick(pedi imageUriString)
 */
@Composable
fun MainScreen(
    viewModel: MemeViewModel,
    onPickImage: () -> Unit
) {
    val context = LocalContext.current

    // collected state apo viewmodel
    val memesState = viewModel.filteredMemes.collectAsState()

    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        // Koumpi pou kanei pickImageLauncher.launch("image/*")
        Button(onClick = { onPickImage() }) {
            Text(text = "Pick Meme Image")
        }

        // Spasi ligo
        Box(modifier = Modifier.size(0.dp, 8.dp))

        // Emfanizo ta memes se LazyRow
        if (memesState.value.isEmpty()) {
            Text(text = "No memes yet", style = MaterialTheme.typography.bodyMedium)
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .padding(top = 8.dp)
            ) {
                items(memesState.value) { meme ->
                    MemeThumbnail(meme = meme, onClick = {
                        // Otan pataei o xristis: grafoume to imagePath sto clipboard h to sthn keim el?
                        // P.x. kanoume intent copy to clipboard, ala edo apla to file μεταφέρουμε.
                        // Kathe IME kalei commitText me to URI string
                        viewModel.setSearchQuery("") // καθαρίζουμε
                    }) {
                        // se service/IME tha το διαχειριστούμε
                    }
                }
            }
        }
    }
}

@Composable
fun MemeThumbnail(
    meme: Meme,
    onClick: (Uri) -> Unit = {},
    onTemporary: () -> Unit = {}
) {
    // Coil painter
    val painter = rememberAsyncImagePainter(model = Uri.parse(meme.imagePath))

    Card(
        modifier = Modifier
            .size(80.dp)
            .clickable {
                onClick(Uri.parse(meme.imagePath))
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Image(
            painter = painter,
            contentDescription = "Meme thumbnail",
            modifier = Modifier.size(80.dp)
        )
    }
}
