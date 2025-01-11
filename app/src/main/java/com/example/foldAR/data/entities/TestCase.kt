package com.example.foldAR.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    foreignKeys = [ForeignKey(
        entity = Scenario::class,
        parentColumns = ["ScenarioID"],
        childColumns = ["ScenarioID"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class TestCase(
    @PrimaryKey(autoGenerate = true) val TestCaseID: Int = 0,
    val ScenarioID: Int,
    val TestCaseName: String,
    val StartTime: String,
    val EndTime: String?
)
