package com.jarvis.app.data.database.dao

import androidx.room.*
import com.jarvis.app.data.database.entity.HabitCompletionEntity
import com.jarvis.app.data.database.entity.HabitEntity
import com.jarvis.app.data.database.entity.HabitWithCompletions
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Transaction
    @Query("SELECT * FROM habits ORDER BY createdAt DESC")
    fun getAllWithCompletions(): Flow<List<HabitWithCompletions>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: HabitEntity)

    @Update
    suspend fun updateHabit(habit: HabitEntity)

    @Delete
    suspend fun deleteHabit(habit: HabitEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompletion(completion: HabitCompletionEntity)

    @Query("DELETE FROM habit_completions WHERE habitId = :habitId AND date >= :dayStart")
    suspend fun deleteCompletionForToday(habitId: String, dayStart: Long)
}
