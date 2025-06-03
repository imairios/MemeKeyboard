package com.example.memekeyboard.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MemeRepository(private val memeDao: MemeDao) {

    suspend fun insertMeme(meme: Meme) {
        withContext(Dispatchers.IO) {
            memeDao.insertMeme(meme)
        }
    }

    suspend fun getAllMemes(): List<Meme> {
        return withContext(Dispatchers.IO) {
            memeDao.getAllMemes()
        }
    }

    suspend fun searchMemesByTag(query: String): List<Meme> {
        return withContext(Dispatchers.IO) {
            memeDao.searchMemesByTag(query)
        }
    }

    suspend fun deleteMeme(meme: Meme) {
        memeDao.deleteMeme(meme)
    }
}
