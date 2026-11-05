package com.dcnm.carapp.core.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [SymptomEventEntity::class, MaintenanceRecordEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun symptomEventDao(): SymptomEventDao
    abstract fun maintenanceRecordDao(): MaintenanceRecordDao

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "carapp.db")
                .fallbackToDestructiveMigration()
                .build()
    }
}
