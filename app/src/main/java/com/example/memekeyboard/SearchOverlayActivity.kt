// SearchOverlayActivity.kt
package com.example.memekeyboard

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.WindowManager
import android.widget.EditText
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.memekeyboard.data.DatabaseProvider
import com.example.memekeyboard.data.MemeRepository
import com.example.memekeyboard.viewmodel.MemeViewModel
import com.example.memekeyboard.viewmodel.MemeViewModelFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SearchOverlayActivity : Activity() {

    private lateinit var searchInput: EditText
    private lateinit var recycler: RecyclerView
    private lateinit var memeViewModel: MemeViewModel
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Use existing layout
        setContentView(R.layout.activity_search_overlay)

        // Optional: make transparent background
        window.setBackgroundDrawableResource(android.R.color.transparent)
        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)

        // Position above the keyboard
        val params = window.attributes
        params.gravity = Gravity.BOTTOM
        window.attributes = params

        searchInput = findViewById(R.id.edit_search)
        recycler = findViewById(R.id.search_recycler)
        recycler.layoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)

        val db = DatabaseProvider.getDatabase(this)
        val repository = MemeRepository(db.memeDao())
        memeViewModel = MemeViewModelFactory(repository).create(MemeViewModel::class.java)

        val adapter = MemeThumbnailAdapter(
            emptyList(),
            onMemeClick = { uri ->
                val intent = android.content.Intent("com.example.memekeyboard.ACTION_INSERT_MEME")
                intent.putExtra("meme_uri", uri.toString())
                sendBroadcast(intent)
                finish()
            },
            onMemeLongClick = {}
        )
        recycler.adapter = adapter

        scope.launch {
            memeViewModel.filteredMemes.collectLatest {
                adapter.submitList(it)
            }
        }

        searchInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                memeViewModel.setSearchQuery(s?.toString().orEmpty())
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // no-op
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // no-op
            }
        })

    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
