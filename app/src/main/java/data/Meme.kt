package com.example.memekeyboard.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memes")
data class Meme(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val imagePath: String,
    val tags: String // μπορείς να κρατήσεις τα tags σαν comma-separated string για αρχή
)
