package com.example.memekeyboard.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.memekeyboard.data.Meme
import com.example.memekeyboard.data.MemeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MemeViewModel(private val repository: MemeRepository) : ViewModel() {

    // 1) Το κείμενο της αναζήτησης (π.χ. "funny,cat")
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _filteredMemes = MutableStateFlow<List<Meme>>(emptyList())
    val filteredMemes: StateFlow<List<Meme>> = _filteredMemes

    init {
        viewModelScope.launch {
            _searchQuery.collectLatest { query ->
                if (query.isBlank()) {
                    withContext(Dispatchers.IO) {
                        val all = repository.getAllMemes()
                        Log.d("VM_DEBUG", "Fetched ALL memes: ${all.size}")
                        _filteredMemes.value = all
                    }
                } else {
                    withContext(Dispatchers.IO) {
                        val tags = query.split(",").map { it.trim().lowercase() }
                            .filter { it.isNotEmpty() }
                        val resultSet = mutableSetOf<Meme>()
                        for (tag in tags) {
                            val found = repository.getMemesByTags(tag)
                            Log.d("VM_DEBUG", "Found ${found.size} memes for tag '$tag'")
                            resultSet.addAll(found)
                        }
                        _filteredMemes.value = resultSet.toList()
                    }
                }
            }
        }
    }


    /** Καλείται όταν θέλουμε να αλλάξουμε το query (π.χ. άγγιγμα κουμπιού στο IME). */
    fun setSearchQuery(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun forceReload() {
        _searchQuery.value = _searchQuery.value
    }

    /** Προστίθεται ένα νέο meme στη βάση, μετά γίνεται refresh. */
    fun insertMeme(meme: Meme) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertMeme(meme)
            // Αν ο χρήστης δεν έχει φιλτράρει (query == ""), μπορούμε να ξαναφορτώσουμε όλα
            if (_searchQuery.value.isBlank()) {
                val allMemes = repository.getAllMemes()
                _filteredMemes.value = allMemes
            } else {
                // Αν υπάρχει query, ξαναβάζουμε ίδιο query για να τρέξει ξανά το collectLatest
                _searchQuery.value = _searchQuery.value
            }
        }
    }

    /** Διαγράφει ένα meme και κάνει ανανέωση. */
    fun deleteMeme(meme: Meme) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteMeme(meme)
            if (_searchQuery.value.isBlank()) {
                val allMemes = repository.getAllMemes()
                _filteredMemes.value = allMemes
            } else {
                _searchQuery.value = _searchQuery.value
            }
        }
    }
}

class MemeViewModelFactory(private val repository: MemeRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MemeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MemeViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
