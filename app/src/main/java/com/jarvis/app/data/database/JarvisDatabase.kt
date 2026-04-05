package com.jarvis.app.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.jarvis.app.data.database.dao.ExpenseDao
import com.jarvis.app.data.database.dao.HabitDao
import com.jarvis.app.data.database.dao.NoteDao
import com.jarvis.app.data.database.entity.ExpenseEntity
import com.jarvis.app.data.database.entity.HabitCompletionEntity
import com.jarvis.app.data.database.entity.HabitEntity
import com.jarvis.app.data.database.entity.NoteEntity

@Database(
    entities = [
        HabitEntity::class,
        HabitCompletionEntity::class,
        ExpenseEntity::class,
        NoteEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class JarvisDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun noteDao(): NoteDao

    companion object {
        @Volatile
        private var INSTANCE: JarvisDatabase? = null

        fun getInstance(context: Context): JarvisDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    JarvisDatabase::class.java,
                    "jarvis_database"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
