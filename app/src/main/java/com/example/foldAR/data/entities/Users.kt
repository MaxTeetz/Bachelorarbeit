package com.example.foldAR.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Users(
    @PrimaryKey(autoGenerate = true) val UserID: Int = 0,
    val Username: String
)
