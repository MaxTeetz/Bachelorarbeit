package com.example.foldAR.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    foreignKeys = [ForeignKey(
        entity = User::class,
        parentColumns = ["UserID"],
        childColumns = ["UserID"],
        onDelete = ForeignKey.CASCADE
    )], indices = [Index(value = ["UserID"])]
)
data class Scenario(
    @PrimaryKey(autoGenerate = true) val ScenarioID: Int = 0,
    val UserID: Int,
    val ScenarioName: Int
)
