package com.shadowbody.app.ui.progression

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.shadowbody.app.ShadowBodyApp
import com.shadowbody.app.data.repository.ProgressionRepository
import com.shadowbody.app.domain.progression.ProgressionEngine
import com.shadowbody.app.domain.progression.ProgressionEngine.ProgressionSummary
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProgressionViewModel(
    private val repository: ProgressionRepository,
) : ViewModel() {

    private val _refreshTrigger = kotlinx.coroutines.flow.MutableStateFlow(0)

    val summary: StateFlow<ProgressionSummary> = _refreshTrigger
        .flatMapLatest {
            repository.observeSummary()
        }
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(), ProgressionEngine.defaultSummary())

    fun refresh() {
        _refreshTrigger.value += 1
    }

    fun forceProcessProgression() {
        viewModelScope.launch {
            repository.processProgression()
            refresh()
        }
    }

    companion object {
        fun defaultSummary() = ProgressionEngine.ProgressionSummary(
            totalXp = 0,
            level = 1,
            xpInCurrentLevel = 0,
            xpToNextLevel = 100,
            levelProgress = 0f,
            attribute = com.shadowbody.app.data.local.Attribute(),
            streak = com.shadowbody.app.data.local.Streak(),
            achievements = emptyList(),
            recentTransactions = emptyList(),
        )
    }

    class Factory(private val app: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val db = (app as ShadowBodyApp).database
            val repo = ProgressionRepository(
                db.xpTransactionDao(),
                db.attributeDao(),
                db.streakDao(),
                db.achievementDao(),
            )
            return ProgressionViewModel(repo) as T
        }
    }
}