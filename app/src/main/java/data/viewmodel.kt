package com.example.memekeyboard.viewmodel

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

    // 2) Η λίστα memes που θα παρατηρεί UI (είτε όλα είτε filtered)
    private val _filteredMemes = MutableStateFlow<List<Meme>>(emptyList())
    val filteredMemes: StateFlow<List<Meme>> = _filteredMemes

    init {
        viewModelScope.launch {
            _searchQuery.collectLatest { query ->
                if (query.isBlank()) {
                    withContext(Dispatchers.IO) {
                        _filteredMemes.value = repository.getAllMemes()
                    }
                } else {
                    val tagsList = query
                        .split(",")
                        .map { it.trim().lowercase() }
                        .filter { it.isNotEmpty() }

                    if (tagsList.isEmpty()) {
                        withContext(Dispatchers.IO) {
                            _filteredMemes.value = repository.getAllMemes()
                        }
                    } else {
                        withContext(Dispatchers.IO) {
                            val resultSet = mutableSetOf<Meme>()
                            for (singleTag in tagsList) {
                                // ← corrected method name here:
                                val matching = repository.getMemesByTags(singleTag)
                                resultSet.addAll(matching)
                            }
                            _filteredMemes.value = resultSet.toList()
                        }
                    }
                }
            }
        }
    }


    /** Καλείται όταν θέλουμε να αλλάξουμε το query (π.χ. άγγιγμα κουμπιού στο IME). */
    fun setSearchQuery(newQuery: String) {
        _searchQuery.value = newQuery
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
