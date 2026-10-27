package com.shadowbody.app.ui.outfit

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.shadowbody.app.ShadowBodyApp
import com.shadowbody.app.data.local.OutfitDailyDecision
import com.shadowbody.app.data.local.WardrobePhotoCombination
import com.shadowbody.app.data.repository.OutfitDailyRepository
import com.shadowbody.app.domain.outfit.OutfitDayKey
import com.shadowbody.app.domain.outfit.OutfitRecommenderEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Feature C ViewModel: daily outfit decision over real photographed options.
 * The engine is rule-based and its explanations plainly say so.
 */
class OutfitDailyViewModel(
    private val repository: OutfitDailyRepository,
) : ViewModel() {

    /** All photographed options from the wardrobe photo combinations. */
    val options: StateFlow<List<WardrobePhotoCombination>> =
        MutableStateFlow<List<WardrobePhotoCombination>>(emptyList())
            .also { base ->
                viewModelScope.launch {
                    base.value = repository.photographedOptions()
                }
            }
            .asStateFlow()

    /** Past and present daily decisions. */
    val decisions: StateFlow<List<OutfitDailyDecision>> = repository.observeDecisions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _todayDecision = MutableStateFlow<OutfitDailyDecision?>(null)
    val todayDecision: StateFlow<OutfitDailyDecision?> = _todayDecision.asStateFlow()

    private val _selectedOccasion = MutableStateFlow(OutfitRecommenderEngine.Occasions.SOCIAL_CASUAL)
    val selectedOccasion: StateFlow<String> = _selectedOccasion.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _recommendLock = MutableStateFlow(false)
    val recommendLock: StateFlow<Boolean> = _recommendLock.asStateFlow()

    init {
        refreshToday()
    }

    fun refreshToday() {
        viewModelScope.launch {
            _todayDecision.value = repository.todayDecision()
            _recommendLock.value = false
        }
    }

    fun selectOccasion(occasion: String) {
        _selectedOccasion.value = occasion
    }

    fun recommendToday() {
        viewModelScope.launch {
            _recommendLock.value = true
            val decision = repository.recommendForToday(_selectedOccasion.value)
            if (decision == null) {
                _message.value = "No photographed outfit options yet — add photos in 'Wardrobe photos' first."
            }
            _todayDecision.value = decision
            _recommendLock.value = false
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    /** Maps a decision's selected id onto the label of the option. */
    fun labelFor(optionId: Long, options: List<WardrobePhotoCombination>): String =
        options.firstOrNull { it.id == optionId }?.label
            ?: "Option #$optionId (removed)"

    class Factory(private val app: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val shadow = app as ShadowBodyApp
            return OutfitDailyViewModel(shadow.outfitDailyRepository) as T
        }
    }
}
