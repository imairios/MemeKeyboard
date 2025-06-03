package com.example.memekeyboard.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Meme::class], version = 1)
abstract class MemeDatabase : RoomDatabase() {
    abstract fun memeDao(): MemeDao
}
