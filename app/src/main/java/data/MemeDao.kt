package com.example.memekeyboard.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MemeDao {

    @Query("SELECT * FROM memes")
    suspend fun getAllMemes(): List<Meme>

    @Insert
    suspend fun insertMeme(meme: Meme)

    @Delete
    suspend fun deleteMeme(meme: Meme)

    // 🔍 Add this method for searching
    @Query("SELECT * FROM memes WHERE tags LIKE '%' || :query || '%'")
    suspend fun searchMemes(query: String): List<Meme>
}
