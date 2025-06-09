package com.example.memekeyboard

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.inputmethodservice.InputMethodService
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.inputmethod.EditorInfoCompat
import androidx.core.view.inputmethod.InputConnectionCompat
import androidx.core.view.inputmethod.InputContentInfoCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.memekeyboard.data.DatabaseProvider
import com.example.memekeyboard.data.Meme
import com.example.memekeyboard.data.MemeRepository
import com.example.memekeyboard.viewmodel.MemeViewModel
import com.example.memekeyboard.viewmodel.MemeViewModelFactory
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import java.util.Locale

class MemeKeyboardService : InputMethodService() {

    private lateinit var rootView: View
    private var isCaps = false
    private val searchBuffer = StringBuilder()
    private var currentLayer = 0

    private lateinit var lettersContainer: View
    private lateinit var numbersContainer: View
    private lateinit var symbolsContainer: View

    private lateinit var memeRecycler: RecyclerView
    private lateinit var memeAdapter: MemeThumbnailAdapter

    private lateinit var memeViewModel: MemeViewModel

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private lateinit var sharedPreferences: SharedPreferences

    private val selectedLanguagesKey = "selected_languages"
    private var currentLanguageIndex = 0
    private var selectedLanguages = listOf("en")

    private val allLanguages = Locale.getAvailableLocales()
        .map { it.language }
        .filter { it.isNotEmpty() }
        .distinct()
        .sorted()

    override fun onCreateInputView(): View {

        if (!Settings.canDrawOverlays(applicationContext)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            Toast.makeText(this, "Please enable overlay permission to use long press menu", Toast.LENGTH_LONG).show()
        }

        rootView = layoutInflater.inflate(R.layout.meme_keyboard_layout, null)

        rootView.findViewById<Button>(R.id.btn_add_meme).setOnClickListener {
            if (!Settings.canDrawOverlays(applicationContext)) {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                Toast.makeText(this, "Please allow overlay permission", Toast.LENGTH_LONG).show()
                startActivity(intent)
                return@setOnClickListener
            }

            val intent = Intent(this, TransparentAddMemeActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            Toast.makeText(this, "Opening meme picker...", Toast.LENGTH_SHORT).show()
            safeStartActivity(intent)
        }

        val db = DatabaseProvider.getDatabase(applicationContext)
        val repository = MemeRepository(db.memeDao())
        memeViewModel = MemeViewModelFactory(repository).create(MemeViewModel::class.java)

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

        memeRecycler = rootView.findViewById(R.id.meme_recycler_view)
        memeRecycler.layoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
        memeAdapter = MemeThumbnailAdapter(
            emptyList(),
            onMemeClick = { selectedUri -> commitOrCopy(selectedUri) },
            onMemeLongClick = { meme -> showLongPressMenu(meme) }
        )
        memeRecycler.adapter = memeAdapter

        lettersContainer = rootView.findViewById(R.id.keyboard_letters)
        numbersContainer = rootView.findViewById(R.id.keyboard_numbers)
        symbolsContainer = rootView.findViewById(R.id.keyboard_symbols)

        val fakeBox = fakeSearchBox
        listOf("q","w","e","r","t","y","u","i","o","p","a","s","d","f","g","h","j","k","l","z","x","c","v","b","n","m").forEach { key ->
            rootView.findViewById<Button>(resources.getIdentifier("key_$key", "id", packageName))?.setOnClickListener {
                val ch = if (isCaps) key.uppercase() else key
                searchBuffer.append(ch)
                fakeBox.text = searchBuffer.toString()
                memeViewModel.setSearchQuery(searchBuffer.toString())
            }
        }

        rootView.findViewById<Button>(R.id.key_caps).setOnClickListener {
            isCaps = !isCaps
            updateKeysCase()
        }

        rootView.findViewById<Button>(R.id.key_delete).setOnClickListener {
            if (searchBuffer.isNotEmpty()) {
                searchBuffer.deleteCharAt(searchBuffer.lastIndex)
                fakeBox.text = searchBuffer.toString()
                memeViewModel.setSearchQuery(searchBuffer.toString())
            }
        }

        rootView.findViewById<Button>(R.id.key_space).setOnClickListener {
            searchBuffer.append(" ")
            fakeBox.text = searchBuffer.toString()
            memeViewModel.setSearchQuery(searchBuffer.toString())
        }

        rootView.findViewById<Button>(R.id.key_enter).setOnClickListener {
            currentInputConnection.commitText(searchBuffer.toString(), 1)
        }

        rootView.findViewById<Button>(R.id.key_to_numbers).setOnClickListener { switchLayer(1) }
        rootView.findViewById<Button>(R.id.key_to_symbols).setOnClickListener { switchLayer(2) }

        (1..9).forEach { num ->
            rootView.findViewById<Button>(resources.getIdentifier("key_$num", "id", packageName))?.setOnClickListener {
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
        rootView.findViewById<Button>(R.id.key_to_letters).setOnClickListener { switchLayer(0) }

        listOf(".",",","!","?","@","#","$","%","&","*").forEachIndexed { idx, sym ->
            rootView.findViewById<Button>(resources.getIdentifier("key_sym_${idx+1}", "id", packageName))?.setOnClickListener {
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
        rootView.findViewById<Button>(R.id.key_to_letters_from_sym).setOnClickListener { switchLayer(0) }

        switchLayer(0)

        serviceScope.launch {
            memeViewModel.filteredMemes.collect { list ->
                memeAdapter.submitList(list)
            }
        }

        rootView.findViewById<Button>(R.id.btn_settings).setOnClickListener {
            Toast.makeText(this, "Opening language settings...", Toast.LENGTH_SHORT).show()
            val intent = Intent(Settings.ACTION_INPUT_METHOD_SUBTYPE_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Could not open settings: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        sharedPreferences = getSharedPreferences("meme_keyboard_prefs", Context.MODE_PRIVATE)
        selectedLanguages = sharedPreferences.getStringSet(selectedLanguagesKey, setOf("English"))!!.toList()

        // Settings button
        rootView.findViewById<Button>(R.id.btn_settings).setOnClickListener {
            showLanguageSelectionDialog()
        }

        // Language switch button
        rootView.findViewById<Button>(R.id.btn_switch_language).setOnClickListener {
            currentLanguageIndex = (currentLanguageIndex + 1) % selectedLanguages.size
            Toast.makeText(this, "Switched to ${selectedLanguages[currentLanguageIndex]}", Toast.LENGTH_SHORT).show()
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

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        searchBuffer.clear()
        rootView.findViewById<TextView>(R.id.search_input_fake).text = ""
        memeViewModel.setSearchQuery("")
        memeViewModel.forceReload()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    private fun commitOrCopy(uri: Uri) {
        val editorInfo = currentInputEditorInfo
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

    private fun copyUriToClipboard(uri: Uri, toastMsg: String) {
        val clip = ClipData.newUri(contentResolver, "Meme URI", uri)
        (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(clip)
        Toast.makeText(this, toastMsg, Toast.LENGTH_SHORT).show()
    }

    private fun showLongPressMenu(meme: Meme) {
        if (!Settings.canDrawOverlays(applicationContext)) {
            Toast.makeText(this, "Overlay permission required to show options", Toast.LENGTH_SHORT).show()
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
        val letters = listOf("q","w","e","r","t","y","u","i","o","p","a","s","d","f","g","h","j","k","l","z","x","c","v","b","n","m")
        letters.forEach { key ->
            val btn = rootView.findViewById<Button>(resources.getIdentifier("key_$key", "id", packageName))
            btn?.text = if (isCaps) key.uppercase() else key
        }
        val capsBtn = rootView.findViewById<Button>(R.id.key_caps)
        capsBtn.setBackgroundColor(
            if (isCaps) resources.getColor(android.R.color.holo_blue_light)
            else resources.getColor(android.R.color.transparent)
        )
    }

    private fun showLanguageSelectionDialog() {
        val availableLocales = Locale.getAvailableLocales()
            .map { it.displayName }
            .distinct()
            .sorted()

        val selectedItems = BooleanArray(availableLocales.size) { i ->
            selectedLanguages.contains(availableLocales[i])
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("Select Supported Languages")
            .setMultiChoiceItems(availableLocales.toTypedArray(), selectedItems) { _, which, isChecked ->
                selectedItems[which] = isChecked
            }
            .setPositiveButton("Save") { _, _ ->
                selectedLanguages = availableLocales.filterIndexed { index, _ -> selectedItems[index] }
                sharedPreferences.edit()
                    .putStringSet(selectedLanguagesKey, selectedLanguages.toSet())
                    .apply()
                currentLanguageIndex = 0
                Toast.makeText(this, "Saved ${selectedLanguages.size} languages", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.window?.setType(android.view.WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
        dialog.show()
    }

    private fun getLettersForLocale(localeCode: String): List<String> {
        return when (localeCode.lowercase()) {
            "el", "greek" -> listOf("ς","ε","ρ","τ","υ","θ","ι","ο","π","α","σ","δ","φ","γ","η","ξ","κ","λ","ζ","χ","ψ","ω","β","ν","μ")
            "fr", "french" -> listOf("a","z","e","r","t","y","u","i","o","p","q","s","d","f","g","h","j","k","l","m","w","x","c","v","b","n")
            "de", "german" -> listOf("q","w","e","r","t","z","u","i","o","p","a","s","d","f","g","h","j","k","l","y","x","c","v","b","n","m")
            else -> listOf("q","w","e","r","t","y","u","i","o","p","a","s","d","f","g","h","j","k","l","z","x","c","v","b","n","m") // fallback
        }
    }

    private fun updateKeyboardKeysForLanguage(language: String) {
        val letters = getLettersForLocale(language)
        val keyIds = listOf(
            "q","w","e","r","t","y","u","i","o","p",
            "a","s","d","f","g","h","j","k","l",
            "z","x","c","v","b","n","m"
        )

        keyIds.forEachIndexed { index, keyId ->
            val resId = resources.getIdentifier("key_$keyId", "id", packageName)
            val btn = rootView.findViewById<Button>(resId)
            if (index < letters.size) {
                btn?.text = letters[index]
            }
        }
    }

}
