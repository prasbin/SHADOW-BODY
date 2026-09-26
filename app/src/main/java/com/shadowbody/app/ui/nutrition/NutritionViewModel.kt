package com.shadowbody.app.ui.nutrition

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.shadowbody.app.ShadowBodyApp
import com.shadowbody.app.data.local.FoodLog
import com.shadowbody.app.data.local.HydrationLog
import com.shadowbody.app.data.local.NutritionGoal
import com.shadowbody.app.data.repository.DailyNutritionSummary
import com.shadowbody.app.data.repository.NutritionRepository
import com.shadowbody.app.domain.nutrition.NutritionDayKey
import com.shadowbody.app.domain.validation.NutritionGoalValidationResult
import com.shadowbody.app.domain.validation.NutritionValidationResult
import com.shadowbody.app.domain.validation.NutritionValidator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class NutritionUiState(
    val currentDayKey: String = NutritionDayKey.today(),
    val summary: DailyNutritionSummary? = null,
    val loggedDays: List<String> = emptyList(),
    // Add food dialog state
    val showAddFoodDialog: Boolean = false,
    val foodName: String = "",
    val foodCalories: String = "",
    val foodProtein: String = "",
    val foodCarbs: String = "",
    val foodFat: String = "",
    val foodServing: String = "",
    val foodNotes: String = "",
    val foodValidation: NutritionValidationResult = NutritionValidationResult(true),
    // Add water dialog state
    val showAddWaterDialog: Boolean = false,
    val waterAmountMl: String = "250",
    // Goal edit dialog state
    val showGoalDialog: Boolean = false,
    val goalCalories: String = "2000",
    val goalProtein: String = "150",
    val goalCarbs: String = "200",
    val goalFat: String = "70",
    val goalHydration: String = "2500",
    val goalType: String = "MAINTENANCE",
    val goalValidation: NutritionGoalValidationResult = NutritionGoalValidationResult(true),
)

class NutritionViewModel(
    private val repository: NutritionRepository,
) : ViewModel() {

    private val _selectedDay = MutableStateFlow(NutritionDayKey.today())

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<NutritionUiState> = _selectedDay
        .flatMapLatest { day ->
            repository.getDailySummary(day)
        }
        .let { summaryFlow ->
            // Combine with logged days and goal/form state via MutableStateFlow
            val mutableState = MutableStateFlow(NutritionUiState())
            // For simplicity, we expose summary and allow mutations via actions
            mutableState
        }

    // A simpler concrete StateFlow approach:
    private val _state = MutableStateFlow(NutritionUiState())
    val state: StateFlow<NutritionUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getDailySummary(_state.value.currentDayKey).collect { summary ->
                _state.value = _state.value.copy(summary = summary)
                summary.goal?.let { g ->
                    if (!_state.value.showGoalDialog) {
                        _state.value = _state.value.copy(
                            goalCalories = g.calorieTarget.toString(),
                            goalProtein = g.proteinGrams.toString(),
                            goalCarbs = g.carbGrams.toString(),
                            goalFat = g.fatGrams.toString(),
                            goalHydration = g.hydrationMlTarget.toString(),
                            goalType = g.goalType,
                        )
                    }
                }
            }
        }
        viewModelScope.launch {
            repository.getAllLoggedDays().collect { days ->
                _state.value = _state.value.copy(loggedDays = days)
            }
        }
    }

    fun selectDay(dayKey: String) {
        _state.value = _state.value.copy(currentDayKey = dayKey)
        viewModelScope.launch {
            repository.getDailySummary(dayKey).collect { summary ->
                _state.value = _state.value.copy(summary = summary)
            }
        }
    }

    fun openAddFood() {
        _state.value = _state.value.copy(
            showAddFoodDialog = true,
            foodName = "",
            foodCalories = "",
            foodProtein = "",
            foodCarbs = "",
            foodFat = "",
            foodServing = "1 serving",
            foodNotes = "",
            foodValidation = NutritionValidationResult(true),
        )
    }

    fun closeAddFood() {
        _state.value = _state.value.copy(showAddFoodDialog = false)
    }

    fun updateFoodInput(
        name: String = _state.value.foodName,
        calories: String = _state.value.foodCalories,
        protein: String = _state.value.foodProtein,
        carbs: String = _state.value.foodCarbs,
        fat: String = _state.value.foodFat,
        serving: String = _state.value.foodServing,
        notes: String = _state.value.foodNotes,
    ) {
        val validation = NutritionValidator.validateFoodLog(name, calories, protein, carbs, fat, serving)
        _state.value = _state.value.copy(
            foodName = name,
            foodCalories = calories,
            foodProtein = protein,
            foodCarbs = carbs,
            foodFat = fat,
            foodServing = serving,
            foodNotes = notes,
            foodValidation = validation,
        )
    }

    fun saveFood() {
        val st = _state.value
        val validation = NutritionValidator.validateFoodLog(
            st.foodName,
            st.foodCalories,
            st.foodProtein,
            st.foodCarbs,
            st.foodFat,
            st.foodServing,
        )
        if (!validation.isValid) {
            _state.value = st.copy(foodValidation = validation)
            return
        }
        viewModelScope.launch {
            repository.addFood(
                FoodLog(
                    dayKey = st.currentDayKey,
                    name = st.foodName.trim(),
                    calories = st.foodCalories.trim().toInt(),
                    proteinGrams = st.foodProtein.trim().toDouble(),
                    carbGrams = st.foodCarbs.trim().toDouble(),
                    fatGrams = st.foodFat.trim().toDouble(),
                    servingText = st.foodServing.trim(),
                    notes = st.foodNotes.trim(),
                    loggedAt = System.currentTimeMillis(),
                ),
            )
            closeAddFood()
        }
    }

    fun deleteFood(id: Long) {
        viewModelScope.launch {
            repository.deleteFood(id)
        }
    }

    fun openAddWater() {
        _state.value = _state.value.copy(showAddWaterDialog = true, waterAmountMl = "250")
    }

    fun closeAddWater() {
        _state.value = _state.value.copy(showAddWaterDialog = false)
    }

    fun updateWaterAmount(amount: String) {
        _state.value = _state.value.copy(waterAmountMl = amount)
    }

    fun saveWater(amountMl: Int) {
        viewModelScope.launch {
            repository.addHydration(
                HydrationLog(
                    dayKey = _state.value.currentDayKey,
                    amountMl = amountMl,
                    loggedAt = System.currentTimeMillis(),
                ),
            )
            closeAddWater()
        }
    }

    fun deleteHydration(id: Long) {
        viewModelScope.launch {
            repository.deleteHydration(id)
        }
    }

    fun openGoalDialog() {
        val g = _state.value.summary?.goal
        _state.value = _state.value.copy(
            showGoalDialog = true,
            goalCalories = (g?.calorieTarget ?: 2000).toString(),
            goalProtein = (g?.proteinGrams ?: 150).toString(),
            goalCarbs = (g?.carbGrams ?: 200).toString(),
            goalFat = (g?.fatGrams ?: 70).toString(),
            goalHydration = (g?.hydrationMlTarget ?: 2500).toString(),
            goalType = g?.goalType ?: "MAINTENANCE",
            goalValidation = NutritionGoalValidationResult(true),
        )
    }

    fun closeGoalDialog() {
        _state.value = _state.value.copy(showGoalDialog = false)
    }

    fun updateGoalInput(
        calories: String = _state.value.goalCalories,
        protein: String = _state.value.goalProtein,
        carbs: String = _state.value.goalCarbs,
        fat: String = _state.value.goalFat,
        hydration: String = _state.value.goalHydration,
        type: String = _state.value.goalType,
    ) {
        val validation = NutritionValidator.validateGoal(calories, protein, carbs, fat, hydration)
        _state.value = _state.value.copy(
            goalCalories = calories,
            goalProtein = protein,
            goalCarbs = carbs,
            goalFat = fat,
            goalHydration = hydration,
            goalType = type,
            goalValidation = validation,
        )
    }

    fun saveGoal() {
        val st = _state.value
        val validation = NutritionValidator.validateGoal(
            st.goalCalories,
            st.goalProtein,
            st.goalCarbs,
            st.goalFat,
            st.goalHydration,
        )
        if (!validation.isValid) {
            _state.value = st.copy(goalValidation = validation)
            return
        }
        viewModelScope.launch {
            repository.saveGoal(
                NutritionGoal(
                    id = 1L,
                    calorieTarget = st.goalCalories.trim().toInt(),
                    proteinGrams = st.goalProtein.trim().toInt(),
                    carbGrams = st.goalCarbs.trim().toInt(),
                    fatGrams = st.goalFat.trim().toInt(),
                    hydrationMlTarget = st.goalHydration.trim().toInt(),
                    goalType = st.goalType.trim(),
                    updatedAt = System.currentTimeMillis(),
                ),
            )
            closeGoalDialog()
        }
    }

    class Factory(private val app: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val db = (app as ShadowBodyApp).database
            val repo = NutritionRepository(db.nutritionGoalDao(), db.foodLogDao(), db.hydrationLogDao())
            return NutritionViewModel(repo) as T
        }
    }
}
