package com.shadowbody.app.ui.profile

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.shadowbody.app.ShadowBodyApp
import com.shadowbody.app.data.local.BaselineRecord
import com.shadowbody.app.data.local.UserProfile
import com.shadowbody.app.data.repository.BaselineRepository
import com.shadowbody.app.data.repository.ProfileRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class ProfileUiState(
    val profile: UserProfile? = null,
    val latestBaseline: BaselineRecord? = null,
    val baselineCount: Int = 0,
)

class ProfileViewModel(
    profileRepository: ProfileRepository,
    baselineRepository: BaselineRepository,
) : ViewModel() {

    val uiState: StateFlow<ProfileUiState> = combine(
        profileRepository.profile,
        baselineRepository.latest(),
        baselineRepository.history(),
    ) { profile, latest, history ->
        ProfileUiState(profile, latest, history.size)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ProfileUiState())

    class Factory(private val app: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val shadow = app as ShadowBodyApp
            return ProfileViewModel(shadow.profileRepository, shadow.baselineRepository) as T
        }
    }
}
