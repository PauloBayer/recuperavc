package com.recuperavc.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "User")
data class User(
    @PrimaryKey(autoGenerate = false) val id: Int,
    val login: String,
    val password: String,
    val email: String,
    val wordsPerMinute: Float,
    val wordErrorRate: Float,
    val name: String
)