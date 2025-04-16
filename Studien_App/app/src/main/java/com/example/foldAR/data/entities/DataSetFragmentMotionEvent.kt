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
data class DataSetFragmentMotionEvent(
    @PrimaryKey(autoGenerate = true) val MotionEventID: Int = 0,
    val TestCaseID: Int,
    val Time: Long,
    val Action: Int,
    val PointerCount: Int,
    val ActionIndex: Int,
    val FirstFingerX: Float?,
    val FirstFingerY: Float?,
    val SecondFingerX: Float?,
    val SecondFingerY: Float?
)
