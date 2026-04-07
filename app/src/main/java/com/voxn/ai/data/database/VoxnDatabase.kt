package com.voxn.ai.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.voxn.ai.data.database.dao.ExpenseDao
import com.voxn.ai.data.database.dao.HabitDao
import com.voxn.ai.data.database.dao.NoteDao
import com.voxn.ai.data.database.entity.ExpenseEntity
import com.voxn.ai.data.database.entity.HabitCompletionEntity
import com.voxn.ai.data.database.entity.HabitEntity
import com.voxn.ai.data.database.entity.NoteEntity

@Database(
    entities = [
        HabitEntity::class,
        HabitCompletionEntity::class,
        ExpenseEntity::class,
        NoteEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class VoxnDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun noteDao(): NoteDao

    companion object {
        @Volatile
        private var INSTANCE: VoxnDatabase? = null

        fun getInstance(context: Context): VoxnDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    VoxnDatabase::class.java,
                    "jarvis_database"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
