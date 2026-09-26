package com.shadowbody.app.domain.morning

import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.ceil

/** Lifecycle of a morning step countdown. FINISHED is terminal until reset(). */
enum class MorningTimerPhase {
    IDLE,
    RUNNING,
    PAUSED,
    FINISHED,
}

data class MorningTimerState(
    val totalSec: Int = 0,
    val remainingSec: Int = 0,
    val phase: MorningTimerPhase = MorningTimerPhase.IDLE,
)

/**
 * Everything needed to rebuild a running countdown, including the monotonic
 * deadline it was counting towards. Persisting the deadline (not just the
 * remaining seconds) is what stops a recreated screen from handing out free
 * time or losing it.
 */
data class MorningTimerSnapshot(
    val totalSec: Int = 0,
    val remainingSec: Int = 0,
    val phase: MorningTimerPhase = MorningTimerPhase.IDLE,
    val endsAtElapsedMs: Long = 0L,
)

/**
 * Phase 5 step timer.
 *
 * Semantics differ from the Phase 3 rest timer on purpose: a morning step is a
 * *countdown to act*, it is bound to one routine step, and it has to survive
 * recreation. So remaining time is always derived from a monotonic deadline
 * rather than from a decrementing counter, and [snapshot]/[restore] rebuild a
 * running countdown exactly.
 *
 * Still a single coroutine owned by [scope] (the ViewModel scope in
 * production, a TestScope in tests): no service, no background loop, no
 * wakelock. Cancel the scope and the timer dies.
 */
class MorningTimer(
    private val scope: CoroutineScope,
    private val elapsedRealtime: () -> Long = { SystemClock.elapsedRealtime() },
) {
    private val _state = MutableStateFlow(MorningTimerState())
    val state: StateFlow<MorningTimerState> = _state.asStateFlow()

    private var endsAtMs: Long = 0L
    private var job: Job? = null

    fun start(seconds: Int) {
        require(seconds > 0) { "A timed step needs a positive duration" }
        job?.cancel()
        endsAtMs = elapsedRealtime() + seconds * 1_000L
        _state.value = MorningTimerState(
            totalSec = seconds,
            remainingSec = seconds,
            phase = MorningTimerPhase.RUNNING,
        )
        launchTicker()
    }

    fun pause() {
        if (_state.value.phase != MorningTimerPhase.RUNNING) return
        job?.cancel()
        job = null
        val left = remainingFromClock()
        endsAtMs = 0L
        _state.update { it.copy(remainingSec = left, phase = MorningTimerPhase.PAUSED) }
    }

    fun resume() {
        val current = _state.value
        if (current.phase != MorningTimerPhase.PAUSED) return
        endsAtMs = elapsedRealtime() + current.remainingSec * 1_000L
        _state.update { it.copy(phase = MorningTimerPhase.RUNNING) }
        launchTicker()
    }

    fun reset() {
        job?.cancel()
        job = null
        endsAtMs = 0L
        _state.value = MorningTimerState()
    }

    /** Extends a running or paused countdown, e.g. "+30s" on a long step. */
    fun addSeconds(extra: Int) {
        val phase = _state.value.phase
        if (phase != MorningTimerPhase.RUNNING && phase != MorningTimerPhase.PAUSED) return
        if (extra <= 0) return
        if (phase == MorningTimerPhase.RUNNING) endsAtMs += extra * 1_000L
        _state.update {
            it.copy(
                totalSec = it.totalSec + extra,
                remainingSec = it.remainingSec + extra,
            )
        }
    }

    fun snapshot(): MorningTimerSnapshot = MorningTimerSnapshot(
        totalSec = _state.value.totalSec,
        remainingSec = _state.value.remainingSec,
        phase = _state.value.phase,
        endsAtElapsedMs = endsAtMs,
    )

    /**
     * Rebuilds a countdown after recreation. A running timer keeps its original
     * deadline, so time that passed while the UI was gone still counts; a
     * countdown that expired in the meantime comes back FINISHED, not reset.
     */
    fun restore(snapshot: MorningTimerSnapshot) {
        job?.cancel()
        job = null
        if (snapshot.phase == MorningTimerPhase.RUNNING && snapshot.endsAtElapsedMs > 0L) {
            endsAtMs = snapshot.endsAtElapsedMs
            val left = remainingFromClock()
            if (left <= 0) {
                endsAtMs = 0L
                _state.value = MorningTimerState(
                    totalSec = snapshot.totalSec,
                    remainingSec = 0,
                    phase = MorningTimerPhase.FINISHED,
                )
            } else {
                _state.value = MorningTimerState(
                    totalSec = snapshot.totalSec,
                    remainingSec = left,
                    phase = MorningTimerPhase.RUNNING,
                )
                launchTicker()
            }
            return
        }
        endsAtMs = 0L
        _state.value = MorningTimerState(
            totalSec = snapshot.totalSec,
            remainingSec = snapshot.remainingSec,
            phase = snapshot.phase,
        )
    }

    private fun remainingFromClock(): Int {
        val left = endsAtMs - elapsedRealtime()
        if (left <= 0L) return 0
        return ceil(left / 1_000.0).toInt()
    }

    private fun launchTicker() {
        job = scope.launch {
            while (true) {
                delay(1_000)
                val left = remainingFromClock()
                if (left <= 0) {
                    endsAtMs = 0L
                    _state.update { it.copy(remainingSec = 0, phase = MorningTimerPhase.FINISHED) }
                    break
                }
                _state.update { if (it.remainingSec == left) it else it.copy(remainingSec = left) }
            }
        }
    }
}
