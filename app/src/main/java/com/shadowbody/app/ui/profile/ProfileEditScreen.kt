package com.shadowbody.app.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.shadowbody.app.data.local.UserProfile
import com.shadowbody.app.domain.model.Equipment
import com.shadowbody.app.domain.model.FitnessLevel
import com.shadowbody.app.domain.model.Goal
import com.shadowbody.app.domain.validation.ProfileValidator
import com.shadowbody.app.ui.components.MeasureField
import com.shadowbody.app.ui.components.MultiChoiceChips
import com.shadowbody.app.ui.components.SectionHeader
import com.shadowbody.app.ui.components.SingleChoiceChips
import com.shadowbody.app.ui.components.SystemPanel
import com.shadowbody.app.ui.theme.LocalShadowSpacing

/** Profile create/edit form. Saves only when validation passes. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditScreen(
    state: ProfileEditUiState,
    onChange: (ProfileEditUiState) -> Unit,
    onSave: () -> Unit,
    onSaved: () -> Unit,
    onBack: () -> Unit,
) {
    val spacing = LocalShadowSpacing.current
    if (state.saved) {
        LaunchedEffect(Unit) { onSaved() }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (state.isEdit) "EDIT PROFILE" else "CONFIGURE PROFILE",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        val input = state.input
        val errors = state.errors
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = spacing.md)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            SystemPanel {
                SectionHeader(title = "Vitals")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                ) {
                    MeasureField(
                        label = "Age", value = input.age,
                        onValueChange = { onChange(state.updateAge(it)) },
                        unit = "yrs", error = errors[ProfileValidator.FIELD_AGE],
                        testTag = "profileAge", modifier = Modifier.weight(1f),
                    )
                    MeasureField(
                        label = "Height", value = input.heightCm,
                        onValueChange = { onChange(state.updateHeight(it)) },
                        unit = "cm", error = errors[ProfileValidator.FIELD_HEIGHT],
                        testTag = "profileHeight", modifier = Modifier.weight(1f),
                    )
                    MeasureField(
                        label = "Weight", value = input.weightKg,
                        onValueChange = { onChange(state.updateWeight(it)) },
                        unit = "kg", error = errors[ProfileValidator.FIELD_WEIGHT],
                        testTag = "profileWeight", modifier = Modifier.weight(1f),
                    )
                }
            }

            SystemPanel {
                SectionHeader(title = "Rank")
                SingleChoiceChips(
                    options = FitnessLevel.entries,
                    selected = input.fitnessLevel,
                    onSelect = { onChange(state.updateLevel(it)) },
                    labelOf = { it.label },
                    tagPrefix = "level",
                )
            }

            SystemPanel {
                SectionHeader(title = "Equipment")
                MultiChoiceChips(
                    options = Equipment.entries,
                    selected = input.equipment,
                    onToggle = { onChange(state.toggleEquipment(it)) },
                    labelOf = { it.label },
                    tagPrefix = "equip",
                )
                errors[ProfileValidator.FIELD_EQUIPMENT]?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }

            SystemPanel {
                SectionHeader(title = "Goals")
                MultiChoiceChips(
                    options = Goal.entries,
                    selected = input.goals,
                    onToggle = { onChange(state.toggleGoal(it)) },
                    labelOf = { it.label },
                    tagPrefix = "goal",
                )
                errors[ProfileValidator.FIELD_GOALS]?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }

            SystemPanel {
                SectionHeader(title = "Schedule")
                MultiChoiceChips(
                    options = (1..7).toList(),
                    selected = input.trainingDays,
                    onToggle = { onChange(state.toggleDay(it)) },
                    labelOf = { UserProfile.DAY_LABELS[it] ?: it.toString() },
                    tagPrefix = "day",
                )
                errors[ProfileValidator.FIELD_DAYS]?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
                Spacer(modifier = Modifier.height(spacing.xs))
                Text(
                    text = "SESSION LENGTH (MIN)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SingleChoiceChips(
                    options = UserProfile.ALLOWED_SESSION_MINUTES.sorted(),
                    selected = input.sessionMinutes,
                    onSelect = { onChange(state.updateSession(it)) },
                    labelOf = { it.toString() },
                    tagPrefix = "session",
                )
            }

            Button(
                onClick = onSave,
                enabled = !state.saving,
                modifier = Modifier.fillMaxWidth().testTag("profileSave"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text(if (state.saving) "SEALING…" else "SEAL PROFILE")
            }
            Spacer(modifier = Modifier.height(spacing.md))
        }
    }
}
