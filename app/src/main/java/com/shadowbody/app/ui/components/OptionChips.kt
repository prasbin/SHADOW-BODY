package com.shadowbody.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.shadowbody.app.ui.theme.LocalShadowSpacing

/** Single-choice chips (fitness level, session length). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> SingleChoiceChips(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    labelOf: (T) -> String,
    tagPrefix: String,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalShadowSpacing.current
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.xs),
        verticalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        options.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = { Text(labelOf(option)) },
                modifier = Modifier.testTag("$tagPrefix:${labelOf(option)}"),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    selectedLabelColor = MaterialTheme.colorScheme.primary,
                ),
            )
        }
    }
}

/** Multi-choice chips (equipment, goals, training days). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> MultiChoiceChips(
    options: List<T>,
    selected: Set<T>,
    onToggle: (T) -> Unit,
    labelOf: (T) -> String,
    tagPrefix: String,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalShadowSpacing.current
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.xs),
        verticalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        options.forEach { option ->
            FilterChip(
                selected = selected.contains(option),
                onClick = { onToggle(option) },
                label = { Text(labelOf(option)) },
                modifier = Modifier.testTag("$tagPrefix:${labelOf(option)}"),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    selectedLabelColor = MaterialTheme.colorScheme.primary,
                ),
            )
        }
    }
}
