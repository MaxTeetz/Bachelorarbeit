package com.example.foldAR.data

import android.app.Application

class DatabaseApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
}