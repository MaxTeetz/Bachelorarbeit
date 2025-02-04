package com.example.foldAR.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    foreignKeys = [ForeignKey(
        entity = TestCase::class,
        parentColumns = ["TestCaseID"],
        childColumns = ["TestCaseID"],
        onDelete = ForeignKey.CASCADE
    )], indices = [Index(value = ["TestCaseID"])]
)
data class DataSetGlSurfaceViewMotionEvent(
    @PrimaryKey(autoGenerate = true) val MotionEventID: Int = 0,
    val TestCaseID : Int,
    val Time: Long,
    //TODO
    val Event: String?
)
