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

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _filteredMemes = MutableStateFlow<List<Meme>>(emptyList())
    val filteredMemes: StateFlow<List<Meme>> = _filteredMemes

    init {
        viewModelScope.launch {
            _searchQuery.collectLatest { query ->
                withContext(Dispatchers.IO) {
                    val result = if (query.isBlank()) {
                        repository.getAllMemes()
                    } else {
                        repository.searchMemes(query)
                    }
                    Log.d("VM_DEBUG", "Found ${result.size} memes for query '$query'")
                    _filteredMemes.value = result
                }
            }
        }
    }

    fun setSearchQuery(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun forceReload() {
        _searchQuery.value = _searchQuery.value // Trigger re-collect
    }

    fun insertMeme(meme: Meme) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertMeme(meme)
            forceReload()
        }
    }

    fun deleteMeme(meme: Meme) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteMeme(meme)
            forceReload()
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
