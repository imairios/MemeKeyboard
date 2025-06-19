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
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import android.inputmethodservice.InputMethodService
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.view.inputmethod.EditorInfoCompat
import androidx.core.view.inputmethod.InputConnectionCompat
import androidx.core.view.inputmethod.InputContentInfoCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.memekeyboard.data.DatabaseProvider
import com.example.memekeyboard.data.MemeRepository
import com.example.memekeyboard.viewmodel.MemeViewModel
import com.example.memekeyboard.viewmodel.MemeViewModelFactory
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import android.content.ClipDescription
import android.util.Log
import android.util.TypedValue


class MemeKeyboardService : InputMethodService() {

    private lateinit var rootView: View
    private lateinit var memeViewModel: MemeViewModel
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var searchDialog: AlertDialog? = null
    private var popupWindow: PopupWindow? = null
    private var capsOn = false
    private var currentLanguageIndex = 0
    private val languages = listOf("EN", "GR")
    private var currentLayer: String = "letters"
    private lateinit var letters: View
    private lateinit var numbers: View
    private lateinit var symbols: View


    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateInputView(): View {
        rootView = layoutInflater.inflate(R.layout.meme_keyboard_layout, null)
        val testDynamicLeft = rootView.findViewById<Button>(R.id.key_dynamic_left)
        android.util.Log.d("INIT", "key_dynamic_left is ${if (testDynamicLeft == null) "null" else "found"}")

        val db = DatabaseProvider.getDatabase(applicationContext)
        val repository = MemeRepository(db.memeDao())
        memeViewModel = MemeViewModelFactory(repository).create(MemeViewModel::class.java)

        val allKeys = listOf(
            R.id.key_q, R.id.key_w, R.id.key_e, R.id.key_r, R.id.key_t, R.id.key_y, R.id.key_u, R.id.key_i, R.id.key_o, R.id.key_p,
            R.id.key_a, R.id.key_s, R.id.key_d, R.id.key_f, R.id.key_g, R.id.key_h, R.id.key_j, R.id.key_k, R.id.key_l,
            R.id.key_z, R.id.key_x, R.id.key_c, R.id.key_v, R.id.key_b, R.id.key_n, R.id.key_m,
            R.id.key_0, R.id.key_1, R.id.key_2, R.id.key_3, R.id.key_4, R.id.key_5, R.id.key_6, R.id.key_7, R.id.key_8, R.id.key_9,
            R.id.key_sym_1, R.id.key_sym_2, R.id.key_sym_3, R.id.key_sym_4, R.id.key_sym_5,
            R.id.key_sym_6, R.id.key_sym_7, R.id.key_sym_8, R.id.key_sym_9, R.id.key_sym_10,
            R.id.key_delete, R.id.key_delete_sym, R.id.key_delete_num,
            R.id.key_space, R.id.key_dynamic_left,
            R.id.key_to_numbers, R.id.key_to_symbols,
//            R.id.key_to_letters,
            R.id.key_to_letters_from_sym,
            R.id.btn_switch_language, R.id.btn_settings
        )

        allKeys.forEach { id ->
            val btn = rootView.findViewById<Button>(id)

            // Preview on press
            btn?.setOnTouchListener { v, event ->
                val button = v as Button
                val key = button.text.toString()

                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        android.util.Log.d("KEY_EVENT", "Pressed key: $key")
                        showKeyPreview(button, key)
                        // Allow system to draw pressed state
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        android.util.Log.d("KEY_EVENT", "Released key: $key")
                        hideKeyPreview()
                    }
                }
                false // <--- Allow default pressed state drawable to work
            }

            // Action on click
            btn?.setOnClickListener {
                handleKeyPress(it as Button)
            }
        }

        // Put this above the loop
        val keysWithPreview = setOf(
            R.id.key_q, R.id.key_w, R.id.key_e, R.id.key_r, R.id.key_t, R.id.key_y, R.id.key_u, R.id.key_i, R.id.key_o, R.id.key_p,
            R.id.key_a, R.id.key_s, R.id.key_d, R.id.key_f, R.id.key_g, R.id.key_h, R.id.key_j, R.id.key_k, R.id.key_l,
            R.id.key_z, R.id.key_x, R.id.key_c, R.id.key_v, R.id.key_b, R.id.key_n, R.id.key_m,
            R.id.key_0, R.id.key_1, R.id.key_2, R.id.key_3, R.id.key_4, R.id.key_5, R.id.key_6, R.id.key_7, R.id.key_8, R.id.key_9,
            R.id.key_sym_1, R.id.key_sym_2, R.id.key_sym_3, R.id.key_sym_4, R.id.key_sym_5,
            R.id.key_sym_6, R.id.key_sym_7, R.id.key_sym_8, R.id.key_sym_9, R.id.key_sym_10
        )

        allKeys.forEach { id ->
            val btn = rootView.findViewById<Button>(id)

            btn?.setOnTouchListener { v, event ->
                val button = v as Button
                val key = button.text.toString()

                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        android.util.Log.d("KEY_EVENT", "Pressed key: $key")
                        if (id in keysWithPreview) {
                            showKeyPreview(button, key)
                        }
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        android.util.Log.d("KEY_EVENT", "Released key: $key")
                        hideKeyPreview()
                    }
                }
                false
            }

            btn?.setOnClickListener {
                handleKeyPress(it as Button)
            }
        }


        rootView.findViewById<Button>(R.id.btn_add_meme)?.setOnClickListener {
            val intent = Intent(this, TransparentAddMemeActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }

        rootView.findViewById<Button>(R.id.btn_open_search)?.setOnClickListener {
            val intent = Intent(this, SearchOverlayActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }

        return rootView
    }

    private fun handleKeyPress(button: Button) {
        when (button.id) {
            R.id.key_delete, R.id.key_delete_sym, R.id.key_delete_num ->
                currentInputConnection.deleteSurroundingText(1, 0)

            R.id.key_space ->
                currentInputConnection.commitText(" ", 1)

            R.id.key_dynamic_left -> {
                Log.d("KEY_ACTION", "Pressed key_dynamic_left, currentLayer=$currentLayer")
                when (currentLayer) {
                    "letters" -> {
                        capsOn = !capsOn
                        updateKeyCase()
                    }
                    "numbers" -> toggleMoreSymbols() // ✅ This toggles your more symbols row
                    "symbols" -> switchKeyboardLayer("numbers")
                }
            }



            R.id.key_to_numbers -> {
                if (currentLayer == "letters") {
                    switchKeyboardLayer("numbers")
                } else {
                    switchKeyboardLayer("letters")
                }
            }

            R.id.key_to_symbols -> toggleMoreSymbols()

            R.id.key_to_letters_from_sym -> switchKeyboardLayer("letters")

            R.id.btn_switch_language -> cycleLanguage()

            R.id.btn_settings -> {
                val intent = Intent(this, TransparentAddMemeActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }

            else -> {
                val key = button.text.toString()
                val output = if (capsOn) key.uppercase() else key.lowercase()
                currentInputConnection.commitText(output, 1)
            }
        }
    }

    private fun toggleMoreSymbols() {
        val moreSymbolsRow = rootView.findViewById<View>(R.id.more_symbols_row)
        val dynamicLeftKey = rootView.findViewById<Button>(R.id.key_dynamic_left)

        val isVisible = moreSymbolsRow?.visibility == View.VISIBLE
        moreSymbolsRow?.visibility = if (isVisible) View.GONE else View.VISIBLE

        // update the key label
        dynamicLeftKey?.text = if (isVisible) "=\u003C" else "?123"
        Log.d("KEY_ACTION", "Toggled moreSymbolsRow to ${if (isVisible) "GONE" else "VISIBLE"}")
    }



    private fun updateKeyCase() {
        val letterKeys = listOf(
            R.id.key_q, R.id.key_w, R.id.key_e, R.id.key_r, R.id.key_t, R.id.key_y, R.id.key_u, R.id.key_i, R.id.key_o, R.id.key_p,
            R.id.key_a, R.id.key_s, R.id.key_d, R.id.key_f, R.id.key_g, R.id.key_h, R.id.key_j, R.id.key_k, R.id.key_l,
            R.id.key_z, R.id.key_x, R.id.key_c, R.id.key_v, R.id.key_b, R.id.key_n, R.id.key_m
        )
        letterKeys.forEach {
            val btn = rootView.findViewById<Button>(it)
            val original = btn.text.toString()
            btn.text = if (capsOn) original.uppercase() else original.lowercase()
        }
    }

    private fun switchKeyboardLayer(layer: String) {
        val letters = rootView.findViewById<View>(R.id.keyboard_letters)
        val numbers = rootView.findViewById<View>(R.id.keyboard_numbers)
        val symbols = rootView.findViewById<View>(R.id.keyboard_symbols)
        val dynamicLeftKey = rootView.findViewById<Button>(R.id.key_dynamic_left)
        val toggleKey = rootView.findViewById<Button>(R.id.key_to_numbers)

        val keysWithPreview = setOf(
            // existing previewable keys...
            R.id.key_more_1, R.id.key_more_2, R.id.key_more_3, R.id.key_more_4, R.id.key_more_5,
            R.id.key_more_6, R.id.key_more_7, R.id.key_more_8, R.id.key_more_9
        )

        // Show the correct layer
        letters.visibility = if (layer == "letters") View.VISIBLE else View.GONE
        numbers.visibility = if (layer == "numbers") View.VISIBLE else View.GONE
        symbols.visibility = if (layer == "symbols") View.VISIBLE else View.GONE

        // Update left key and toggle label (visuals only, no click handlers here)
        when (layer) {
            "letters" -> {
                dynamicLeftKey?.text = "⇧"      // Caps icon
                toggleKey?.text = "?123"        // Switch to numbers
            }
            "numbers" -> {
                dynamicLeftKey?.text = "=\u003C" // =<
                toggleKey?.text = "ABC"          // Switch to letters
            }
            "symbols" -> {
                dynamicLeftKey?.text = "?123"    // Switch back to numbers
                toggleKey?.text = "ABC"          // Switch to letters
            }
        }

        currentLayer = layer
    }

    private fun cycleLanguage() {
        currentLanguageIndex = (currentLanguageIndex + 1) % languages.size
        val lang = languages[currentLanguageIndex]
        Toast.makeText(this, "Switched to $lang", Toast.LENGTH_SHORT).show()
    }

    private var searchPopup: PopupWindow? = null

    @SuppressLint("InflateParams")
    private fun showSearchDialog() {
        val container = rootView.findViewById<View>(R.id.search_bar_container)
        val searchInput = container.findViewById<EditText>(R.id.edit_search)
        val recycler = container.findViewById<RecyclerView>(R.id.search_recycler)

        val adapter = MemeThumbnailAdapter(
            emptyList(),
            onMemeClick = { uri ->
                commitOrCopy(uri)
                container.visibility = View.GONE
            },
            onMemeLongClick = {}
        )
        recycler.layoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
        recycler.adapter = adapter

        serviceScope.launch {
            memeViewModel.filteredMemes.collectLatest {
                adapter.submitList(it)
            }
        }

        searchInput.setText("") // clear previous query
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                memeViewModel.setSearchQuery(s?.toString().orEmpty())
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        container.visibility = View.VISIBLE
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

    @SuppressLint("InflateParams")
    private fun showKeyPreview(view: View, text: String) {
        val inflater = LayoutInflater.from(view.context)
        val previewView = inflater.inflate(R.layout.view_key_preview, null)
        previewView.findViewById<TextView>(R.id.preview_text).text = text

        val fixedWidth = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            60f,
            view.resources.displayMetrics
        ).toInt()

        popupWindow?.dismiss()
        popupWindow = PopupWindow(
            previewView,
            fixedWidth,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            false
        ).apply {
            isOutsideTouchable = false
            isTouchable = false
            elevation = 10f
        }

        view.post {
            previewView.measure(
                View.MeasureSpec.UNSPECIFIED,
                View.MeasureSpec.UNSPECIFIED
            )

            // Center horizontally above the key
            val xOffset = (view.width - fixedWidth) / 2

            // Push higher vertically above the key
            val yOffset = -view.height - previewView.measuredHeight - 40 // Increase offset

            popupWindow?.showAsDropDown(view, xOffset, yOffset)
        }
    }

    private fun hideKeyPreview() {
        popupWindow?.dismiss()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }




}
