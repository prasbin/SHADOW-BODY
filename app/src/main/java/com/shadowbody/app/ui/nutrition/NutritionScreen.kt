package com.shadowbody.app.ui.nutrition

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.shadowbody.app.ui.components.SectionHeader
import com.shadowbody.app.ui.components.StatCard
import com.shadowbody.app.ui.components.SystemPanel
import com.shadowbody.app.ui.theme.LocalShadowSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionScreen(
    viewModel: NutritionViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val spacing = LocalShadowSpacing.current
    val summary = state.summary

    val goal = summary?.goal
    val calConsumed = summary?.totalCalories ?: 0
    val proConsumed = summary?.totalProtein ?: 0.0
    val carbConsumed = summary?.totalCarbs ?: 0.0
    val fatConsumed = summary?.totalFat ?: 0.0
    val hydConsumed = summary?.totalHydrationMl ?: 0

    val calTarget = goal?.calorieTarget ?: 2000
    val hydTarget = goal?.hydrationMlTarget ?: 2500

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "NUTRITION & FUEL",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.testTag("nutritionTitle"),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("nutritionBack")) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::openGoalDialog, modifier = Modifier.testTag("nutritionGoalButton")) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit Goals")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
            )
        },
        floatingActionButton = {
            Row(
                modifier = Modifier.padding(horizontal = spacing.md),
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                FloatingActionButton(
                    onClick = viewModel::openAddWater,
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                    modifier = Modifier.testTag("addWaterFab"),
                ) {
                    Icon(Icons.Filled.Favorite, contentDescription = "Add Water")
                }
                FloatingActionButton(
                    onClick = viewModel::openAddFood,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("addFoodFab"),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add Food")
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = spacing.md)
                .testTag("nutritionList"),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            item {
                Spacer(modifier = Modifier.height(spacing.xs))
                SystemPanel(accentBorder = true) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                text = "ACTIVE DAY: ${state.currentDayKey}",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.testTag("nutritionDayHeader"),
                            )
                            Text(
                                text = if (goal != null) "Goal: ${goal.goalType} (${goal.calorieTarget} kcal)" else "No nutrition goal set (defaults shown)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            item {
                SectionHeader(title = "Energy & Macros")
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                ) {
                    StatCard(
                        label = "Calories",
                        value = "$calConsumed",
                        footnote = "Target: $calTarget",
                        progress = if (calTarget > 0) (calConsumed.toFloat() / calTarget).coerceIn(0f, 1f) else 0f,
                        modifier = Modifier.weight(1f),
                    )
                    StatCard(
                        label = "Hydration",
                        value = "${hydConsumed}ml",
                        footnote = "Target: ${hydTarget}ml",
                        progress = if (hydTarget > 0) (hydConsumed.toFloat() / hydTarget).coerceIn(0f, 1f) else 0f,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                ) {
                    StatCard(
                        label = "Protein",
                        value = "${proConsumed.toInt()}g",
                        footnote = "Target: ${goal?.proteinGrams ?: 150}g",
                        modifier = Modifier.weight(1f),
                    )
                    StatCard(
                        label = "Carbs",
                        value = "${carbConsumed.toInt()}g",
                        footnote = "Target: ${goal?.carbGrams ?: 200}g",
                        modifier = Modifier.weight(1f),
                    )
                    StatCard(
                        label = "Fat",
                        value = "${fatConsumed.toInt()}g",
                        footnote = "Target: ${goal?.fatGrams ?: 70}g",
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            item {
                SectionHeader(title = "Food Logs", trailing = "${summary?.foodLogs?.size ?: 0} items")
            }

            val foods = summary?.foodLogs.orEmpty()
            if (foods.isEmpty()) {
                item {
                    SystemPanel {
                        Text(
                            text = "No food logged for this day.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag("nutritionEmptyFood"),
                        )
                    }
                }
            } else {
                items(foods, key = { "food-${it.id}" }) { food ->
                    SystemPanel {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = food.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.testTag("foodItemName:${food.id}"),
                                )
                                Text(
                                    text = "${food.servingText} • ${food.calories} kcal | P:${food.proteinGrams}g C:${food.carbGrams}g F:${food.fatGrams}g",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                if (food.notes.isNotBlank()) {
                                    Text(
                                        text = food.notes,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.tertiary,
                                    )
                                }
                            }
                            IconButton(
                                onClick = { viewModel.deleteFood(food.id) },
                                modifier = Modifier.testTag("deleteFood:${food.id}"),
                            ) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = "Delete Food",
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                }
            }

            item {
                SectionHeader(title = "Hydration Logs", trailing = "${summary?.hydrationLogs?.size ?: 0} entries")
            }

            val hydrations = summary?.hydrationLogs.orEmpty()
            if (hydrations.isEmpty()) {
                item {
                    SystemPanel {
                        Text(
                            text = "No water logged for this day.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag("nutritionEmptyWater"),
                        )
                    }
                }
            } else {
                items(hydrations, key = { "water-${it.id}" }) { water ->
                    SystemPanel {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${water.amountMl} ml Water",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.testTag("waterItem:${water.id}"),
                                )
                            }
                            IconButton(
                                onClick = { viewModel.deleteHydration(water.id) },
                                modifier = Modifier.testTag("deleteWater:${water.id}"),
                            ) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = "Delete Water",
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    // Add Food Dialog
    if (state.showAddFoodDialog) {
        AlertDialog(
            onDismissRequest = viewModel::closeAddFood,
            title = { Text("LOG FOOD") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(spacing.xs),
                    modifier = Modifier.testTag("addFoodDialog"),
                ) {
                    OutlinedTextField(
                        value = state.foodName,
                        onValueChange = { viewModel.updateFoodInput(name = it) },
                        label = { Text("Food Name") },
                        isError = state.foodName.trim().isEmpty() && !state.foodValidation.isValid,
                        modifier = Modifier.fillMaxWidth().testTag("inputFoodName"),
                    )
                    OutlinedTextField(
                        value = state.foodServing,
                        onValueChange = { viewModel.updateFoodInput(serving = it) },
                        label = { Text("Serving Size (e.g. 1 bowl, 200g)") },
                        modifier = Modifier.fillMaxWidth().testTag("inputFoodServing"),
                    )
                    OutlinedTextField(
                        value = state.foodCalories,
                        onValueChange = { viewModel.updateFoodInput(calories = it) },
                        label = { Text("Calories (kcal)") },
                        modifier = Modifier.fillMaxWidth().testTag("inputFoodCalories"),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                        OutlinedTextField(
                            value = state.foodProtein,
                            onValueChange = { viewModel.updateFoodInput(protein = it) },
                            label = { Text("Protein (g)") },
                            modifier = Modifier.weight(1f).testTag("inputFoodProtein"),
                        )
                        OutlinedTextField(
                            value = state.foodCarbs,
                            onValueChange = { viewModel.updateFoodInput(carbs = it) },
                            label = { Text("Carbs (g)") },
                            modifier = Modifier.weight(1f).testTag("inputFoodCarbs"),
                        )
                        OutlinedTextField(
                            value = state.foodFat,
                            onValueChange = { viewModel.updateFoodInput(fat = it) },
                            label = { Text("Fat (g)") },
                            modifier = Modifier.weight(1f).testTag("inputFoodFat"),
                        )
                    }
                    OutlinedTextField(
                        value = state.foodNotes,
                        onValueChange = { viewModel.updateFoodInput(notes = it) },
                        label = { Text("Notes (optional)") },
                        modifier = Modifier.fillMaxWidth().testTag("inputFoodNotes"),
                    )
                    if (!state.foodValidation.isValid) {
                        val err = state.foodValidation.nameError
                            ?: state.foodValidation.caloriesError
                            ?: state.foodValidation.proteinError
                            ?: state.foodValidation.carbError
                            ?: state.foodValidation.fatError
                            ?: state.foodValidation.servingError
                            ?: "Invalid input."
                        Text(
                            text = err,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.testTag("foodValidationError"),
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = viewModel::saveFood,
                    modifier = Modifier.testTag("saveFoodButton"),
                ) {
                    Text("SAVE")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = viewModel::closeAddFood) {
                    Text("CANCEL")
                }
            },
        )
    }

    // Add Water Dialog
    if (state.showAddWaterDialog) {
        AlertDialog(
            onDismissRequest = viewModel::closeAddWater,
            title = { Text("LOG HYDRATION") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(spacing.xs),
                    modifier = Modifier.testTag("addWaterDialog"),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                        Button(
                            onClick = { viewModel.saveWater(250) },
                            modifier = Modifier.weight(1f).testTag("water250"),
                        ) {
                            Text("+250ml")
                        }
                        Button(
                            onClick = { viewModel.saveWater(500) },
                            modifier = Modifier.weight(1f).testTag("water500"),
                        ) {
                            Text("+500ml")
                        }
                        Button(
                            onClick = { viewModel.saveWater(750) },
                            modifier = Modifier.weight(1f).testTag("water750"),
                        ) {
                            Text("+750ml")
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                OutlinedButton(onClick = viewModel::closeAddWater) {
                    Text("CLOSE")
                }
            },
        )
    }

    // Goal Configuration Dialog
    if (state.showGoalDialog) {
        AlertDialog(
            onDismissRequest = viewModel::closeGoalDialog,
            title = { Text("NUTRITION GOALS") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(spacing.xs),
                    modifier = Modifier.testTag("goalDialog"),
                ) {
                    OutlinedTextField(
                        value = state.goalType,
                        onValueChange = { viewModel.updateGoalInput(type = it) },
                        label = { Text("Goal Type (MAINTENANCE, BULK, CUT)") },
                        modifier = Modifier.fillMaxWidth().testTag("inputGoalType"),
                    )
                    OutlinedTextField(
                        value = state.goalCalories,
                        onValueChange = { viewModel.updateGoalInput(calories = it) },
                        label = { Text("Daily Calories Target") },
                        modifier = Modifier.fillMaxWidth().testTag("inputGoalCalories"),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                        OutlinedTextField(
                            value = state.goalProtein,
                            onValueChange = { viewModel.updateGoalInput(protein = it) },
                            label = { Text("Protein (g)") },
                            modifier = Modifier.weight(1f).testTag("inputGoalProtein"),
                        )
                        OutlinedTextField(
                            value = state.goalCarbs,
                            onValueChange = { viewModel.updateGoalInput(carbs = it) },
                            label = { Text("Carbs (g)") },
                            modifier = Modifier.weight(1f).testTag("inputGoalCarbs"),
                        )
                        OutlinedTextField(
                            value = state.goalFat,
                            onValueChange = { viewModel.updateGoalInput(fat = it) },
                            label = { Text("Fat (g)") },
                            modifier = Modifier.weight(1f).testTag("inputGoalFat"),
                        )
                    }
                    OutlinedTextField(
                        value = state.goalHydration,
                        onValueChange = { viewModel.updateGoalInput(hydration = it) },
                        label = { Text("Hydration Target (ml)") },
                        modifier = Modifier.fillMaxWidth().testTag("inputGoalHydration"),
                    )
                    if (!state.goalValidation.isValid) {
                        val err = state.goalValidation.caloriesError
                            ?: state.goalValidation.proteinError
                            ?: state.goalValidation.carbError
                            ?: state.goalValidation.fatError
                            ?: state.goalValidation.hydrationError
                            ?: "Invalid goal input."
                        Text(
                            text = err,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.testTag("goalValidationError"),
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = viewModel::saveGoal,
                    modifier = Modifier.testTag("saveGoalButton"),
                ) {
                    Text("SAVE GOALS")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = viewModel::closeGoalDialog) {
                    Text("CANCEL")
                }
            },
        )
    }
}
