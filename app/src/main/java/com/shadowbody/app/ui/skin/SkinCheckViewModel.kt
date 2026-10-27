package com.shadowbody.app.ui.skin

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.shadowbody.app.ShadowBodyApp
import com.shadowbody.app.data.local.SkinCheckRecord
import com.shadowbody.app.data.repository.PhotoFileOps
import com.shadowbody.app.data.repository.PrivatePhotoStorage
import com.shadowbody.app.data.repository.SkinCheckRepository
import com.shadowbody.app.domain.skincheck.SkinCheckDayKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

/**
 * Feature B ViewModel. Turns successful camera results / gallery picks into
 * repository calls, exposes today's record plus full history, and keeps
 * user-facing messages free of technical detail.
 */
class SkinCheckViewModel(
    private val repository: SkinCheckRepository,
    private val photoOps: PhotoFileOps,
) : ViewModel() {

    val history: StateFlow<List<SkinCheckRecord>> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _today = MutableStateFlow<SkinCheckRecord?>(null)
    val today: StateFlow<SkinCheckRecord?> = _today.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        refreshToday()
    }

    fun refreshToday() {
        viewModelScope.launch {
            _today.value = repository.today()
        }
    }

    /** Creates the private camera target file for one cheek. */
    fun newCameraFile(cheek: SkinCheckRepository.Cheek): File =
        photoOps.newCameraFile(
            PrivatePhotoStorage.DIR_SKIN,
            when (cheek) {
                SkinCheckRepository.Cheek.LEFT -> "left_cheek"
                SkinCheckRepository.Cheek.RIGHT -> "right_cheek"
            },
        )

    fun onCameraComplete(cheek: SkinCheckRepository.Cheek, file: File, captured: Boolean) {
        viewModelScope.launch {
            if (!captured) {
                photoOps.deleteFileIfInside(file.absolutePath)
                return@launch
            }
            val dayKey = SkinCheckDayKey.today()
            if (!photoOps.exists(file.absolutePath) || file.length() == 0L) {
                _message.value = "Camera returned no image — please try again."
                photoOps.deleteFileIfInside(file.absolutePath)
                refreshToday()
                return@launch
            }
            repository.setCheekFromCamera(dayKey, cheek, file)
            _message.value = when (cheek) {
                SkinCheckRepository.Cheek.LEFT -> "Left cheek photo saved (camera)."
                SkinCheckRepository.Cheek.RIGHT -> "Right cheek photo saved (camera)."
            }
            refreshToday()
        }
    }

    fun onGalleryResult(cheek: SkinCheckRepository.Cheek, uri: Uri?) {
        if (uri == null) return
        viewModelScope.launch {
            val dayKey = SkinCheckDayKey.today()
            val saved = repository.setCheekFromGallery(dayKey, cheek, uri)
            _message.value = if (saved != null) {
                when (cheek) {
                    SkinCheckRepository.Cheek.LEFT -> "Left cheek photo saved (gallery)."
                    SkinCheckRepository.Cheek.RIGHT -> "Right cheek photo saved (gallery)."
                }
            } else {
                "Could not read that image — please try another one."
            }
            refreshToday()
        }
    }

    fun removeCheek(cheek: SkinCheckRepository.Cheek) {
        viewModelScope.launch {
            repository.removeCheek(SkinCheckDayKey.today(), cheek)
            _message.value = "Photo removed. The file was deleted."
            refreshToday()
        }
    }

    fun updateNotes(notes: String) {
        viewModelScope.launch {
            repository.updateNotes(SkinCheckDayKey.today(), notes)
            refreshToday()
        }
    }

    fun deleteDay(dayKey: String) {
        viewModelScope.launch {
            repository.deleteDay(dayKey)
            _message.value = "Deleted that day and both photo files."
            refreshToday()
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    class Factory(private val app: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val shadow = app as ShadowBodyApp
            return SkinCheckViewModel(
                repository = shadow.skinCheckRepository,
                photoOps = shadow.photoStorage,
            ) as T
        }
    }
}
