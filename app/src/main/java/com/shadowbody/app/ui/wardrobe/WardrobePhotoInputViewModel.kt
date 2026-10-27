package com.shadowbody.app.ui.wardrobe

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.shadowbody.app.ShadowBodyApp
import com.shadowbody.app.data.local.WardrobePhotoCombination
import com.shadowbody.app.data.local.WardrobePhotoCombinationDao
import com.shadowbody.app.data.repository.PhotoFileOps
import com.shadowbody.app.data.repository.PrivatePhotoStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Feature C ViewModel: photographs of the user's actual outfit options.
 * Photos are stored under app-private `outfit_photos/`; a replaced or removed
 * photo deletes its previous file (no orphaned references).
 */
class WardrobePhotoInputViewModel(
    private val dao: WardrobePhotoCombinationDao,
    private val photoOps: PhotoFileOps,
) : ViewModel() {

    val combinations: StateFlow<List<WardrobePhotoCombination>> = dao.observeAll()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun newCameraFile(position: Int): java.io.File =
        photoOps.newCameraFile(PrivatePhotoStorage.DIR_OUTFIT, "combination$position")

    /** Stores a completed camera capture for the combination slot. */
    fun onCameraComplete(position: Int, file: java.io.File, captured: Boolean) {
        viewModelScope.launch {
            val path = file.absolutePath
            if (!captured || !photoOps.exists(path) || file.length() == 0L) {
                photoOps.deleteFileIfInside(path)
                if (captured) _message.value = "Camera returned no image — try again."
                return@launch
            }
            applyPhoto(position, path)
            _message.value = "Photo saved for Combination ${position + 1}."
        }
    }

    /** Copies a gallery pick into private storage and attaches it. */
    fun onGalleryResult(position: Int, uri: android.net.Uri?) {
        if (uri == null) return
        viewModelScope.launch {
            val copied = photoOps.copyFromUri(uri, PrivatePhotoStorage.DIR_OUTFIT)
            if (copied == null) {
                _message.value = "Could not read that image — please try another one."
                return@launch
            }
            applyPhoto(position, copied)
            _message.value = "Photo attached from gallery for Combination ${position + 1}."
        }
    }

    private suspend fun applyPhoto(position: Int, newPath: String) {
        val existing = dao.getByPosition(position)
        val oldPath = existing?.photoPath
        if (existing != null) {
            dao.update(
                existing.copy(
                    photoPath = newPath,
                    updatedAt = System.currentTimeMillis(),
                )
            )
        } else {
            dao.insert(
                WardrobePhotoCombination(
                    position = position,
                    label = "Combination ${position + 1}",
                    photoPath = newPath,
                )
            )
        }
        if (oldPath != null && oldPath != newPath) {
            photoOps.deleteFileIfInside(oldPath)
        }
    }

    fun replacePhoto(position: Int, photoPath: String) {
        viewModelScope.launch {
            val existing = dao.getByPosition(position)
            if (existing != null) {
                dao.update(
                    existing.copy(photoPath = photoPath, updatedAt = System.currentTimeMillis())
                )
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
                existing.photoPath?.let { photoOps.deleteFileIfInside(it) }
                dao.update(
                    existing.copy(photoPath = null, updatedAt = System.currentTimeMillis())
                )
                _message.value = "Photo removed and its file deleted."
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
            return WardrobePhotoInputViewModel(
                dao = shadow.database.wardrobePhotoCombinationDao(),
                photoOps = shadow.photoStorage,
            ) as T
        }
    }
}
