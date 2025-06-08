package com.example.memekeyboard.data

class MemeRepository(private val dao: MemeDao) {

    /** Get *all* memes. */
    suspend fun getAllMemes(): List<Meme> =
        dao.getAllMemes()

    /** Search the DB for any meme whose tags string contains `search`. */
    suspend fun getMemesByTags(search: String): List<Meme> =
        dao.getMemesByTags(search)

    /** (NEW) Search the DB for any meme whose tags string contains exactly one tag. */
    suspend fun getMemesByTag(tag: String): List<Meme> =
        dao.getMemesByTags(tag)

    /** Insert a new meme. */
    suspend fun insertMeme(meme: Meme) =
        dao.insertMeme(meme)

    /** Delete an existing meme. */
    suspend fun deleteMeme(meme: Meme) =
        dao.deleteMeme(meme)
}
