// ✅ MemeViewModel.kt
package com.example.memekeyboard.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.memekeyboard.data.Meme
import com.example.memekeyboard.data.MemeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MemeViewModel(private val repository: MemeRepository) : ViewModel() {

    private val _memes = MutableStateFlow<List<Meme>>(emptyList())
    val memes: StateFlow<List<Meme>> = _memes

    init {
        refreshMemes()
    }

    private fun refreshMemes() {
        viewModelScope.launch {
            _memes.value = repository.getAllMemes()
        }
    }

    fun insertMeme(meme: Meme) {
        viewModelScope.launch {
            repository.insertMeme(meme)
            refreshMemes()
        }
    }

    fun deleteMeme(meme: Meme) {
        viewModelScope.launch {
            repository.deleteMeme(meme)
            refreshMemes()
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
