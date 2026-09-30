package com.shadowbody.app.ui.wardrobe

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.shadowbody.app.ShadowBodyApp
import com.shadowbody.app.data.local.OutfitRecord
import com.shadowbody.app.data.local.WardrobeItem
import com.shadowbody.app.data.repository.OutfitRepository
import com.shadowbody.app.data.repository.WardrobeRepository
import com.shadowbody.app.domain.wardrobe.OutfitGenerator
import com.shadowbody.app.domain.wardrobe.OutfitSuggestion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class OutfitViewModel(
    private val wardrobeRepository: WardrobeRepository,
    private val outfitRepository: OutfitRepository,
) : ViewModel() {

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _selectedOccasion = MutableStateFlow("CASUAL")
    val selectedOccasion: StateFlow<String> = _selectedOccasion.asStateFlow()

    private val _selectedSeason = MutableStateFlow("ALL_SEASON")
    val selectedSeason: StateFlow<String> = _selectedSeason.asStateFlow()

    private val _currentSuggestion = MutableStateFlow<OutfitSuggestion?>(null)
    val currentSuggestion: StateFlow<OutfitSuggestion?> = _currentSuggestion.asStateFlow()

    val enabledItems: StateFlow<List<WardrobeItem>> = wardrobeRepository.enabledItems
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val outfitHistory: StateFlow<List<OutfitRecord>> = outfitRepository.observeRecent(20)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun setOccasion(occasion: String) {
        _selectedOccasion.value = occasion
    }

    fun setSeason(season: String) {
        _selectedSeason.value = season
    }

    fun generateOutfit() {
        val items = enabledItems.value
        val suggestion = OutfitGenerator.generate(
            items = items,
            occasion = _selectedOccasion.value,
            season = _selectedSeason.value,
        )
        _currentSuggestion.value = suggestion
    }

    fun saveOutfit(name: String) {
        val suggestion = _currentSuggestion.value ?: return
        viewModelScope.launch {
            val record = OutfitRecord(
                name = name,
                topItemId = suggestion.top?.id,
                bottomItemId = suggestion.bottom?.id,
                footwearItemId = suggestion.footwear?.id,
                accessoryItemId = suggestion.accessory?.id,
                occasion = _selectedOccasion.value,
                season = _selectedSeason.value,
                explanation = suggestion.explanation,
            )
            outfitRepository.saveOutfit(record)
            _message.value = "Outfit saved."
        }
    }

    fun deleteOutfit(record: OutfitRecord) {
        viewModelScope.launch {
            outfitRepository.deleteOutfit(record)
            _message.value = "Outfit deleted."
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    class Factory(private val app: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val shadow = app as ShadowBodyApp
            return OutfitViewModel(shadow.wardrobeRepository, shadow.outfitRepository) as T
        }
    }
}
