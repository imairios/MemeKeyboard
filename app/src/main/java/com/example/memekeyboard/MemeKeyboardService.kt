package com.example.memekeyboard

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.compose.ui.text.font.FontVariation
import androidx.core.view.inputmethod.EditorInfoCompat
import androidx.core.view.inputmethod.InputConnectionCompat
import androidx.core.view.inputmethod.InputContentInfoCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.example.memekeyboard.data.DatabaseProvider
import com.example.memekeyboard.data.Meme
import com.example.memekeyboard.data.MemeDatabase
import com.example.memekeyboard.data.MemeRepository
import com.example.memekeyboard.viewmodel.MemeViewModel
import com.example.memekeyboard.viewmodel.MemeViewModelFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class MemeKeyboardService : InputMethodService() {

    private lateinit var rootView: View
    private var isCaps = false
    private val searchBuffer = StringBuilder()
    private var currentLayer = 0  // 0 = letters, 1 = numbers, 2 = symbols

    private lateinit var lettersContainer: View
    private lateinit var numbersContainer: View
    private lateinit var symbolsContainer: View

    private lateinit var memeRecycler: RecyclerView
    private lateinit var memeAdapter: MemeThumbnailAdapter

    private lateinit var memeViewModel: MemeViewModel

    // Since InputMethodService is not a LifecycleOwner, make our own CoroutineScope
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    override fun onCreateInputView(): View {


        if (!Settings.canDrawOverlays(applicationContext)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            Toast.makeText(this, "Please enable overlay permission to use long press menu", Toast.LENGTH_LONG).show()
        }

        // 1) Inflate keyboard + search UI
        rootView = layoutInflater.inflate(R.layout.meme_keyboard_layout, null)

        // 2) “Add Meme” button → launch transparent overlay
        rootView.findViewById<Button>(R.id.btn_add_meme).setOnClickListener {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M &&
                !android.provider.Settings.canDrawOverlays(applicationContext)) {

                val intent = Intent(
                    android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                Toast.makeText(this, "Please allow overlay permission", Toast.LENGTH_LONG).show()
                startActivity(intent)
                return@setOnClickListener
            }

            val intent = Intent(this, TransparentAddMemeActivity::class.java)
                .apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }

            Toast.makeText(this, "Opening meme picker...", Toast.LENGTH_SHORT).show()
            safeStartActivity(intent)
        }


        // 3) Setup Room & ViewModel (shared "meme-database")
        val db = DatabaseProvider.getDatabase(applicationContext)


        val repository = MemeRepository(db.memeDao())
        memeViewModel = MemeViewModelFactory(repository)
            .create(MemeViewModel::class.java)

        // 4) Fake “search” TextView
        val fakeSearchBox = rootView.findViewById<TextView>(R.id.search_input_fake)

        fakeSearchBox.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString()
                searchBuffer.clear()
                searchBuffer.append(query)
                memeViewModel.setSearchQuery(query)
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })


        // 5) RecyclerView + Adapter
        memeRecycler = rootView.findViewById(R.id.meme_recycler_view)
        memeRecycler.layoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
        memeAdapter = MemeThumbnailAdapter(
            emptyList(),
            onMemeClick = { selectedUri -> commitOrCopy(selectedUri) },
            onMemeLongClick = { meme -> showLongPressMenu(meme) }
        )
        memeRecycler.adapter = memeAdapter

        // 6) Keyboard layer containers
        lettersContainer = rootView.findViewById(R.id.keyboard_letters)
        numbersContainer = rootView.findViewById(R.id.keyboard_numbers)
        symbolsContainer = rootView.findViewById(R.id.keyboard_symbols)

        // 7) Letter keys
        val fakeBox = fakeSearchBox
        listOf(
            "q","w","e","r","t","y","u","i","o","p",
            "a","s","d","f","g","h","j","k","l",
            "z","x","c","v","b","n","m"
        ).forEach { key ->
            rootView.findViewById<Button>(
                resources.getIdentifier("key_$key", "id", packageName)
            )?.setOnClickListener {
                val ch = if (isCaps) key.uppercase() else key
                searchBuffer.append(ch)
                fakeBox.text = searchBuffer.toString()
                memeViewModel.setSearchQuery(searchBuffer.toString())
            }
        }

        // 8) Caps
        rootView.findViewById<Button>(R.id.key_caps).setOnClickListener {
            isCaps = !isCaps
            updateKeysCase()
        }
        // 9) Delete
        rootView.findViewById<Button>(R.id.key_delete).setOnClickListener {
            if (searchBuffer.isNotEmpty()) {
                searchBuffer.deleteCharAt(searchBuffer.lastIndex)
                fakeBox.text = searchBuffer.toString()
                memeViewModel.setSearchQuery(searchBuffer.toString())
            }
        }
        // 10) Space
        rootView.findViewById<Button>(R.id.key_space).setOnClickListener {
            searchBuffer.append(" ")
            fakeBox.text = searchBuffer.toString()
            memeViewModel.setSearchQuery(searchBuffer.toString())
        }
        // 11) Enter
        rootView.findViewById<Button>(R.id.key_enter).setOnClickListener {
            currentInputConnection.commitText(searchBuffer.toString(), 1)
        }
        // 12) Layers toggle
        rootView.findViewById<Button>(R.id.key_to_numbers).setOnClickListener { switchLayer(1) }
        rootView.findViewById<Button>(R.id.key_to_symbols).setOnClickListener { switchLayer(2) }

        // 13) Number keys
        (1..9).forEach { num ->
            rootView.findViewById<Button>(
                resources.getIdentifier("key_$num", "id", packageName)
            )?.setOnClickListener {
                searchBuffer.append(num)
                fakeBox.text = searchBuffer.toString()
                memeViewModel.setSearchQuery(searchBuffer.toString())
            }
        }
        rootView.findViewById<Button>(R.id.key_0).setOnClickListener {
            searchBuffer.append("0")
            fakeBox.text = searchBuffer.toString()
            memeViewModel.setSearchQuery(searchBuffer.toString())
        }
        rootView.findViewById<Button>(R.id.key_delete_num).setOnClickListener {
            if (searchBuffer.isNotEmpty()) {
                searchBuffer.deleteCharAt(searchBuffer.lastIndex)
                fakeBox.text = searchBuffer.toString()
                memeViewModel.setSearchQuery(searchBuffer.toString())
            }
        }
        rootView.findViewById<Button>(R.id.key_to_letters).setOnClickListener {
            switchLayer(0)
        }

        // 14) Symbol keys
        listOf(".",",","!","?","@","#","$","%","&","*").forEachIndexed { idx, sym ->
            rootView.findViewById<Button>(
                resources.getIdentifier("key_sym_${idx+1}", "id", packageName)
            )?.setOnClickListener {
                searchBuffer.append(sym)
                fakeBox.text = searchBuffer.toString()
                memeViewModel.setSearchQuery(searchBuffer.toString())
            }
        }
        rootView.findViewById<Button>(R.id.key_delete_sym).setOnClickListener {
            if (searchBuffer.isNotEmpty()) {
                searchBuffer.deleteCharAt(searchBuffer.lastIndex)
                fakeBox.text = searchBuffer.toString()
                memeViewModel.setSearchQuery(searchBuffer.toString())
            }
        }
        rootView.findViewById<Button>(R.id.key_to_letters_from_sym).setOnClickListener {
            switchLayer(0)
        }

        // 15) Default to letters
        switchLayer(0)

        // 16) Collect flow updates
        serviceScope.launch {
            memeViewModel.filteredMemes.collect { list ->
                android.util.Log.d("IME_MEMES", "Collected memes: ${list.size}")
                list.forEach {
                    android.util.Log.d("IME_MEME_ITEM", "Path: ${it.imagePath}, Tags: ${it.tags}")
                }
                memeAdapter.submitList(list)
            }
        }



        return rootView
    }

    private fun safeStartActivity(intent: Intent) {
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to open meme picker: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }


    /** Called every time the keyboard is shown. */
    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        searchBuffer.clear()
        rootView.findViewById<TextView>(R.id.search_input_fake).text = ""

        // force search query to trigger reload
        memeViewModel.setSearchQuery("") // already here

        // add this line to re-trigger collection
        memeViewModel.forceReload()
    }



    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    //--- helper methods below ---

    private fun commitOrCopy(uri: Uri) {
        val editorInfo = currentInputEditorInfo
        val mimeTypes = EditorInfoCompat.getContentMimeTypes(editorInfo)
        val supportsImage = mimeTypes.any { ClipDescription.compareMimeTypes(it, "image/*") }
        if (supportsImage) {
            val desc = ClipDescription("meme", arrayOf("image/*"))
            val content = InputContentInfoCompat(uri, desc, null)
            val flags = InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION
            val opts = Bundle()
            val ok = InputConnectionCompat.commitContent(
                currentInputConnection, editorInfo, content, flags, opts
            )
            if (!ok) copyUriToClipboard(uri, "Editor did not accept image. URI copied.")
        } else {
            copyUriToClipboard(uri, "Copied meme URI to clipboard")
        }
    }

    private fun copyUriToClipboard(uri: Uri, toastMsg: String) {
        val clip = ClipData.newUri(contentResolver, "Meme URI", uri)
        (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
            .setPrimaryClip(clip)
        Toast.makeText(this, toastMsg, Toast.LENGTH_SHORT).show()
    }


    private fun showLongPressMenu(meme: Meme) {


        if (!Settings.canDrawOverlays(applicationContext)) {
            Toast.makeText(this, "Overlay permission required to show options", Toast.LENGTH_SHORT).show()
            return
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M &&
            !android.provider.Settings.canDrawOverlays(applicationContext)) {
            Toast.makeText(this, "Overlay permission needed to show menu", Toast.LENGTH_LONG).show()
            return
        }

        val context = applicationContext
        val themedContext = android.view.ContextThemeWrapper(context, R.style.Theme_MemeKeyboard)

        val options = arrayOf("Copy URI", "Delete Meme", "Cancel")
        val dialog = AlertDialog.Builder(themedContext)
            .setTitle("Meme Options")
            .setItems(options) { dlg, idx ->
                when (idx) {
                    0 -> copyUriToClipboard(Uri.parse(meme.imagePath), "Copied meme URI")
                    1 -> memeViewModel.deleteMeme(meme)
                    else -> dlg.dismiss()
                }
            }
            .create()

        dialog.window?.setType(android.view.WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
        dialog.show()
    }



    private fun switchLayer(layer: Int) {
        currentLayer = layer
        lettersContainer.visibility = if (layer == 0) View.VISIBLE else View.GONE
        numbersContainer.visibility = if (layer == 1) View.VISIBLE else View.GONE
        symbolsContainer.visibility = if (layer == 2) View.VISIBLE else View.GONE
    }

    private fun updateKeysCase() {
        val letters = listOf(
            "q","w","e","r","t","y","u","i","o","p",
            "a","s","d","f","g","h","j","k","l",
            "z","x","c","v","b","n","m"
        )
        letters.forEach { key ->
            val btn = rootView.findViewById<Button>(
                resources.getIdentifier("key_$key", "id", packageName)
            )
            btn?.text = if (isCaps) key.uppercase() else key
        }
        val capsBtn = rootView.findViewById<Button>(R.id.key_caps)
        capsBtn.setBackgroundColor(
            if (isCaps) resources.getColor(android.R.color.holo_blue_light)
            else resources.getColor(android.R.color.transparent)
        )
    }

    private fun insertImageIntoCurrentInput(uri: Uri) {
        val ic = currentInputConnection
        if (ic != null) {
            // Insert content if supported
            val inputContentInfo = InputContentInfoCompat(uri, ClipDescription("Meme", arrayOf("image/*")), null)
            val flags = InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION
            InputConnectionCompat.commitContent(ic, currentInputEditorInfo, inputContentInfo, flags, null)
        } else {
            Toast.makeText(this, "Input connection unavailable", Toast.LENGTH_SHORT).show()
        }
    }



}
