package com.example.memekeyboard

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.WindowManager
import android.widget.EditText
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.memekeyboard.data.DatabaseProvider
import com.example.memekeyboard.data.MemeRepository
import com.example.memekeyboard.viewmodel.MemeViewModel
import com.example.memekeyboard.viewmodel.MemeViewModelFactory
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest

class SearchOverlayActivity : Activity() {

    private lateinit var searchInput: EditText
    private lateinit var memeRecycler: RecyclerView
    private lateinit var memeAdapter: MemeThumbnailAdapter
    private lateinit var viewModel: MemeViewModel
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Allow touches to pass through areas not covered by content
        window.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        )

        // Prevent limiting height to the decor bounds
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        // Set gravity to bottom and wrap content height
        val params = window.attributes
        params.gravity = Gravity.BOTTOM
        window.attributes = params

        window.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        window.setBackgroundDrawableResource(android.R.color.transparent)

        setContentView(R.layout.activity_search_overlay)

        searchInput = findViewById(R.id.edit_search)
        memeRecycler = findViewById(R.id.search_recycler)
        memeRecycler.layoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)

        val db = DatabaseProvider.getDatabase(this)
        val repo = MemeRepository(db.memeDao())
        viewModel = MemeViewModelFactory(repo).create(MemeViewModel::class.java)

        memeAdapter = MemeThumbnailAdapter(
            emptyList(),
            onMemeClick = { uri ->
                setResult(Activity.RESULT_OK, Intent().apply { data = uri })
                finish()
            },
            onMemeLongClick = {
                Toast.makeText(this, "Long-pressed meme: ${it.tags}", Toast.LENGTH_SHORT).show()
            }
        )

        memeRecycler.adapter = memeAdapter

        searchInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                viewModel.setSearchQuery(s?.toString().orEmpty())
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        scope.launch {
            viewModel.filteredMemes.collectLatest { memes ->
                memeAdapter.submitList(memes)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
