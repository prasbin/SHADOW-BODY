package com.shadowbody.app.domain.model

/** Exercise modality. Determines which targets are meaningful (reps vs time). */
enum class ExerciseCategory(val label: String) {
    STRENGTH("Strength"),
    BODYWEIGHT("Bodyweight"),
    CARDIO("Cardio"),
    CORE("Core"),
}
