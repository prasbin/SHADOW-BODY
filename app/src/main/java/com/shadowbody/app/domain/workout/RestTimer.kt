package com.shadowbody.app.domain.workout

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Lifecycle of the rest timer. FINISHED is terminal until start()/reset(). */
enum class RestPhase {
    IDLE,
    RUNNING,
    PAUSED,
    FINISHED,
}

data class RestState(
    val totalSec: Int = 0,
    val remainingSec: Int = 0,
    val phase: RestPhase = RestPhase.IDLE,
)

/**
 * Reusable rest timer. Single coroutine job, no background loops: ticks are
 * one-second delays owned by [scope] (the caller's ViewModel scope in
 * production, a TestScope in tests). Cancel the scope and the timer dies.
 */
class RestTimer(
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(RestState())
    val state: StateFlow<RestState> = _state.asStateFlow()

    private var job: Job? = null

    fun start(seconds: Int) {
        require(seconds > 0) { "Rest must be positive" }
        job?.cancel()
        _state.update { RestState(totalSec = seconds, remainingSec = seconds, phase = RestPhase.RUNNING) }
        job = scope.launch {
            while (true) {
                delay(1_000)
                val next = _state.value.remainingSec - 1
                if (next <= 0) {
                    _state.update { it.copy(remainingSec = 0, phase = RestPhase.FINISHED) }
                    break
                }
                _state.update { it.copy(remainingSec = next) }
            }
        }
    }

    fun pause() {
        if (_state.value.phase != RestPhase.RUNNING) return
        job?.cancel()
        job = null
        _state.update { it.copy(phase = RestPhase.PAUSED) }
    }

    fun resume() {
        val current = _state.value
        if (current.phase != RestPhase.PAUSED) return
        _state.update { it.copy(phase = RestPhase.RUNNING) }
        job = scope.launch {
            while (true) {
                delay(1_000)
                val next = _state.value.remainingSec - 1
                if (next <= 0) {
                    _state.update { it.copy(remainingSec = 0, phase = RestPhase.FINISHED) }
                    break
                }
                _state.update { it.copy(remainingSec = next) }
            }
        }
    }

    fun reset() {
        job?.cancel()
        job = null
        _state.update { RestState() }
    }

    /** Adds time mid-rest (e.g. +15s). Ignored unless running/paused. */
    fun addSeconds(extra: Int) {
        val phase = _state.value.phase
        if (phase != RestPhase.RUNNING && phase != RestPhase.PAUSED) return
        _state.update { it.copy(remainingSec = it.remainingSec + extra, totalSec = it.totalSec + extra) }
    }
}
