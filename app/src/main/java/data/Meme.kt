package com.example.memekeyboard.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memes")
data class Meme(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    // path to image (URI as String). To cinema pickeri exei epistrepsei Uri.toString()
    val imagePath: String,

    // tags se ena pedio, comma-separated (p.x. "funny,cat,logo")
    val tags: String
)
