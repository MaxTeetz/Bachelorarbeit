package com.example.foldAR.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    foreignKeys = [ForeignKey(
        entity = Scenario::class,
        parentColumns = ["ScenarioID"],
        childColumns = ["ScenarioID"],
        onDelete = ForeignKey.CASCADE
    )], indices = [Index(value = ["ScenarioID"])]
)
data class TestCase(
    @PrimaryKey(autoGenerate = true) val TestCaseID: Int = 0,
    val ScenarioID: Int,
    val TestCaseName: Int,
    val StartTime: String,
    val EndTime: String?
)
