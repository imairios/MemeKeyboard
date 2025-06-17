package com.example.memekeyboard.data

class MemeRepository(private val dao: MemeDao) {

    /** Get all memes. */
    suspend fun getAllMemes(): List<Meme> =
        dao.getAllMemes()

    /** Search memes by both name and tags. */
    suspend fun searchMemes(query: String): List<Meme> =
        dao.searchMemes(query)

    /** Insert a new meme. */
    suspend fun insertMeme(meme: Meme) =
        dao.insertMeme(meme)

    /** Delete an existing meme. */
    suspend fun deleteMeme(meme: Meme) =
        dao.deleteMeme(meme)
}
