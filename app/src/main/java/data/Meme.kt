package com.example.memekeyboard.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memes")
data class Meme(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,        // make sure this exists
    val tags: String,
    val imagePath: String
)
