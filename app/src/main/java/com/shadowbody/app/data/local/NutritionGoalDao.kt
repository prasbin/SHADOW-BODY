package com.shadowbody.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NutritionGoalDao {
    @Query("SELECT * FROM nutrition_goal WHERE id = 1")
    fun getGoal(): Flow<NutritionGoal?>

    @Query("SELECT * FROM nutrition_goal WHERE id = 1")
    suspend fun getGoalSync(): NutritionGoal?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveGoal(goal: NutritionGoal)
}
