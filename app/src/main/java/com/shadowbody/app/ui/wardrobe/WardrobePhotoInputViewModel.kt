package com.shadowbody.app.ui.wardrobe

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.shadowbody.app.ShadowBodyApp
import com.shadowbody.app.data.local.WardrobePhotoCombination
import com.shadowbody.app.data.local.WardrobePhotoCombinationDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WardrobePhotoInputViewModel(
    private val dao: WardrobePhotoCombinationDao,
) : ViewModel() {

    val combinations: StateFlow<List<WardrobePhotoCombination>> = dao.observeAll()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun addPhoto(position: Int, photoPath: String) {
        viewModelScope.launch {
            val existing = dao.getByPosition(position)
            if (existing != null) {
                dao.update(existing.copy(photoPath = photoPath, updatedAt = System.currentTimeMillis()))
            } else {
                dao.insert(
                    WardrobePhotoCombination(
                        position = position,
                        label = "Combination ${position + 1}",
                        photoPath = photoPath,
                    )
                )
            }
            _message.value = "Photo added to Combination ${position + 1}."
        }
    }

    fun replacePhoto(position: Int, photoPath: String) {
        viewModelScope.launch {
            val existing = dao.getByPosition(position)
            if (existing != null) {
                dao.update(existing.copy(photoPath = photoPath, updatedAt = System.currentTimeMillis()))
                _message.value = "Photo replaced for Combination ${position + 1}."
            } else {
                _message.value = "No existing combination to replace."
            }
        }
    }

    fun removePhoto(position: Int) {
        viewModelScope.launch {
            val existing = dao.getByPosition(position)
            if (existing != null) {
                dao.update(existing.copy(photoPath = null, updatedAt = System.currentTimeMillis()))
                _message.value = "Photo removed from Combination ${position + 1}."
            } else {
                _message.value = "No photo to remove."
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    class Factory(private val app: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val shadow = app as ShadowBodyApp
            return WardrobePhotoInputViewModel(shadow.database.wardrobePhotoCombinationDao()) as T
        }
    }
}
