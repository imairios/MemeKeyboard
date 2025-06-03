package com.example.memekeyboard.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Delete

@Dao
interface MemeDao {
    @Insert
    suspend fun insertMeme(meme: Meme)

    @Query("SELECT * FROM memes")
    suspend fun getAllMemes(): List<Meme>

    @Query("SELECT * FROM memes WHERE tags LIKE '%' || :query || '%'")
    suspend fun searchMemesByTag(query: String): List<Meme>

    @Delete
    suspend fun deleteMeme(meme: Meme)
}
