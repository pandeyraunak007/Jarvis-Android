package com.voxn.ai.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 2,
    exportSchema = true,
)
abstract class VoxnDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun noteDao(): NoteDao

    companion object {
        @Volatile
        private var INSTANCE: VoxnDatabase? = null

        // Migration from v1 to v2: add frequency columns to habits table
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE habits ADD COLUMN frequencyRaw TEXT NOT NULL DEFAULT 'Daily'")
                db.execSQL("ALTER TABLE habits ADD COLUMN targetCount INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE habits ADD COLUMN weeklyDaysRaw TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getInstance(context: Context): VoxnDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    VoxnDatabase::class.java,
                    "jarvis_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
