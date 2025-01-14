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
data class DataSet(
    @PrimaryKey(autoGenerate = true) val DataSetID: Int = 0,
    val TestCaseID: Int,
    val Time: String,
    val CameraPositionX: Float,
    val CameraPositionY: Float,
    val CameraPositionZ: Float,
    val CameraRoatationX: Float,
    val CameraRoatationY: Float,
    val CameraRoatationZ: Float,
    val CameraRoatationW: Float,
    val Location_ManipulatedObjectX: Float,
    val Location_ManipulatedObjectY: Float,
    val Location_ManipulatedObjectZ: Float,
    val Location_TargetObjectX: Float,
    val Location_TargetObjectY: Float,
    val Location_TargetObjectZ: Float,
)
