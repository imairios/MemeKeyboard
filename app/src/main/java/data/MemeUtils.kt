// File: MemeUtils.kt
package com.example.memekeyboard

import android.content.*
import android.net.Uri
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.core.view.inputmethod.EditorInfoCompat
import androidx.core.view.inputmethod.InputConnectionCompat
import androidx.core.view.inputmethod.InputContentInfoCompat

object MemeUtils {
    fun commitOrCopy(
        context: Context,
        contentResolver: ContentResolver,
        inputConnection: android.view.inputmethod.InputConnection,
        editorInfo: EditorInfo,
        uri: Uri
    ) {
        val mimeTypes = EditorInfoCompat.getContentMimeTypes(editorInfo)
        val supportsImage = mimeTypes.any { ClipDescription.compareMimeTypes(it, "image/*") }
        if (supportsImage) {
            val desc = ClipDescription("meme", arrayOf("image/*"))
            val content = InputContentInfoCompat(uri, desc, null)
            val flags = InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION
            val opts = Bundle()
            val ok = InputConnectionCompat.commitContent(inputConnection, editorInfo, content, flags, opts)
            if (!ok) {
                copyToClipboard(context, contentResolver, uri, "Editor did not accept image. URI copied.")
            }
        } else {
            copyToClipboard(context, contentResolver, uri, "Copied meme URI to clipboard")
        }
    }

    private fun copyToClipboard(context: Context, contentResolver: ContentResolver, uri: Uri, msg: String) {
        val clip = ClipData.newUri(contentResolver, "Meme URI", uri)
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }
}
