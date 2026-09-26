package com.shadowbody.app.data.repository

import com.shadowbody.app.data.local.FoodLog
import com.shadowbody.app.data.local.FoodLogDao
import com.shadowbody.app.data.local.HydrationLog
import com.shadowbody.app.data.local.HydrationLogDao
import com.shadowbody.app.data.local.NutritionGoal
import com.shadowbody.app.data.local.NutritionGoalDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class DailyNutritionSummary(
    val dayKey: String,
    val totalCalories: Int,
    val totalProtein: Double,
    val totalCarbs: Double,
    val totalFat: Double,
    val totalHydrationMl: Int,
    val foodLogs: List<FoodLog>,
    val hydrationLogs: List<HydrationLog>,
    val goal: NutritionGoal?,
)

class NutritionRepository(
    private val goalDao: NutritionGoalDao,
    private val foodLogDao: FoodLogDao,
    private val hydrationLogDao: HydrationLogDao,
) {
    val goal: Flow<NutritionGoal?> = goalDao.getGoal()

    suspend fun getGoalSync(): NutritionGoal? = goalDao.getGoalSync()

    suspend fun saveGoal(goal: NutritionGoal) {
        goalDao.saveGoal(goal)
    }

    fun getFoodLogs(dayKey: String): Flow<List<FoodLog>> = foodLogDao.getLogsForDay(dayKey)

    fun getHydrationLogs(dayKey: String): Flow<List<HydrationLog>> = hydrationLogDao.getLogsForDay(dayKey)

    fun getDailySummary(dayKey: String): Flow<DailyNutritionSummary> {
        return combine(
            foodLogDao.getLogsForDay(dayKey),
            hydrationLogDao.getLogsForDay(dayKey),
            goalDao.getGoal(),
        ) { foods, hydrations, goal ->
            val cal = foods.sumOf { it.calories }
            val pro = foods.sumOf { it.proteinGrams }
            val carb = foods.sumOf { it.carbGrams }
            val fat = foods.sumOf { it.fatGrams }
            val hyd = hydrations.sumOf { it.amountMl }
            DailyNutritionSummary(
                dayKey = dayKey,
                totalCalories = cal,
                totalProtein = pro,
                totalCarbs = carb,
                totalFat = fat,
                totalHydrationMl = hyd,
                foodLogs = foods,
                hydrationLogs = hydrations,
                goal = goal,
            )
        }
    }

    suspend fun addFood(log: FoodLog): Long = foodLogDao.insert(log)

    suspend fun deleteFood(id: Long) = foodLogDao.delete(id)

    suspend fun addHydration(log: HydrationLog): Long = hydrationLogDao.insert(log)

    suspend fun deleteHydration(id: Long) = hydrationLogDao.delete(id)

    fun getAllLoggedDays(): Flow<List<String>> = foodLogDao.getAllLoggedDays()
}
