// MemeKeyboardService.kt

package com.example.memekeyboard

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.core.view.inputmethod.EditorInfoCompat
import androidx.core.view.inputmethod.InputConnectionCompat
import androidx.core.view.inputmethod.InputContentInfoCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.inputmethodservice.InputMethodService
import android.content.ClipDescription
import android.os.Handler
import android.os.Looper
import android.widget.PopupWindow
import android.widget.TextView
import com.example.memekeyboard.data.DatabaseProvider
import com.example.memekeyboard.data.MemeRepository
import com.example.memekeyboard.viewmodel.MemeViewModel
import com.example.memekeyboard.viewmodel.MemeViewModelFactory
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import android.view.MotionEvent
import android.view.ViewGroup


class MemeKeyboardService : InputMethodService() {

    private lateinit var rootView: View
    private lateinit var memeViewModel: MemeViewModel
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var searchDialog: AlertDialog? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateInputView(): View {
        rootView = layoutInflater.inflate(R.layout.meme_keyboard_layout, null)

        val db = DatabaseProvider.getDatabase(applicationContext)
        val repository = MemeRepository(db.memeDao())
        memeViewModel = MemeViewModelFactory(repository).create(MemeViewModel::class.java)

        // Setup keyboard keys
        val keyIds = listOf(
            R.id.key_q, R.id.key_w, R.id.key_e, R.id.key_r, R.id.key_t, R.id.key_y, R.id.key_u, R.id.key_i, R.id.key_o, R.id.key_p,
            R.id.key_a, R.id.key_s, R.id.key_d, R.id.key_f, R.id.key_g, R.id.key_h, R.id.key_j, R.id.key_k, R.id.key_l,
            R.id.key_z, R.id.key_x, R.id.key_c, R.id.key_v, R.id.key_b, R.id.key_n, R.id.key_m,
            R.id.key_space, R.id.key_enter, R.id.key_delete
        )

        keyIds.forEach { id ->
            rootView.findViewById<Button>(id)?.setOnTouchListener { v, event ->
                val button = v as? Button ?: return@setOnTouchListener false
                val key = button.text.toString()

                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        showKeyPreview(button, key)
                        currentInputConnection.commitText(key, 1)
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        hideKeyPreview()
                    }
                }
                true // Consume the touch
            }
        }


        rootView.findViewById<Button>(R.id.btn_add_meme)?.setOnClickListener {
            val intent = Intent(this, TransparentAddMemeActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }

        rootView.findViewById<Button>(R.id.btn_open_search)?.setOnClickListener {
            showSearchDialog()
        }

        return rootView
    }

//    fun onKeyClicked(view: View) {
////        val ic = currentInputConnection ?: return
////        val key = (view as? Button)?.text?.toString() ?: return
////        Log.d("Keyboard", "Key pressed: $key")
////
////        when (view.id) {
////            R.id.key_space -> ic.commitText(" ", 1)
////            R.id.key_enter -> ic.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_ENTER))
////            R.id.key_delete -> ic.deleteSurroundingText(1, 0)
////            else -> ic.commitText(key, 1)
////        }
////    }

    fun onKeyClicked(view: View) {
        val key = (view as Button).text.toString()

        // Show popup preview
        showKeyPreview(view, key)

        // Send key to input
        currentInputConnection.commitText(key, 1)

        // Hide preview after short delay
        Handler(Looper.getMainLooper()).postDelayed({ hideKeyPreview() }, 150)
    }


    private fun showSearchDialog() {
        val dialogView = layoutInflater.inflate(R.layout.activity_search_overlay, null)

        val searchInput = dialogView.findViewById<EditText>(R.id.edit_search)
        val recycler = dialogView.findViewById<RecyclerView>(R.id.search_recycler)
        recycler.layoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)

        val adapter = MemeThumbnailAdapter(
            emptyList(),
            onMemeClick = { uri ->
                commitOrCopy(uri)
                searchDialog?.dismiss()
            },
            onMemeLongClick = {}
        )
        recycler.adapter = adapter

        serviceScope.launch {
            memeViewModel.filteredMemes.collectLatest {
                adapter.submitList(it)
            }
        }

        searchInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                memeViewModel.setSearchQuery(s?.toString().orEmpty())
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        searchDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        searchDialog?.window?.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
        searchDialog?.show()
    }

    private fun commitOrCopy(uri: Uri) {
        val editorInfo = currentInputEditorInfo ?: return
        val mimeTypes = EditorInfoCompat.getContentMimeTypes(editorInfo)
        val supportsImage = mimeTypes.any { ClipDescription.compareMimeTypes(it, "image/*") }

        if (supportsImage) {
            val desc = ClipDescription("meme", arrayOf("image/*"))
            val content = InputContentInfoCompat(uri, desc, null)
            val flags = InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION
            val opts = Bundle()
            val ok = InputConnectionCompat.commitContent(currentInputConnection, editorInfo, content, flags, opts)
            if (!ok) copyUriToClipboard(uri, "Editor did not accept image. URI copied.")
        } else {
            copyUriToClipboard(uri, "Copied meme URI to clipboard")
        }
    }

    private fun copyUriToClipboard(uri: Uri, message: String) {
        val clip = ClipData.newUri(contentResolver, "Meme URI", uri)
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    private var popupWindow: PopupWindow? = null

    @SuppressLint("InflateParams")
    private fun showKeyPreview(view: View, text: String) {
        val inflater = LayoutInflater.from(view.context)
        val previewView = inflater.inflate(R.layout.view_key_preview, null)
        previewView.findViewById<TextView>(R.id.preview_text).text = text

        popupWindow?.dismiss()
        popupWindow = PopupWindow(
            previewView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            false
        ).apply {
            isOutsideTouchable = false
            isTouchable = false
            elevation = 10f
        }

        view.post {
            // Measure preview to center it horizontally
            previewView.measure(
                View.MeasureSpec.UNSPECIFIED,
                View.MeasureSpec.UNSPECIFIED
            )

            val xOffset = (view.width - previewView.measuredWidth) / 2
            val yOffset = -view.height - previewView.measuredHeight + 8 // move it above

            popupWindow?.showAsDropDown(view, xOffset, yOffset)
        }
    }




    private fun hideKeyPreview() {
        popupWindow?.dismiss()
    }

}
