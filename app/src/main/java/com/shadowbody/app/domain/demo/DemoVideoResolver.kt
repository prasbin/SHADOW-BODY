package com.shadowbody.app.domain.demo

import java.net.URLEncoder

/**
 * Feature A: exercise demonstration video resolver.
 *
 * HONEST CAPABILITY STATE: SHADOW BODY bundles no video files and holds no
 * verified per-exercise video URLs (adding unverifiable URLs would risk
 * opening unrelated content). For every exercise it instead deterministically
 * builds a YouTube *search* query URL for "<exercise> exercise proper form
 * tutorial" and labels it as an external, online video. Opening it hands off
 * to the YouTube app / browser — the SHADOW BODY process itself neither
 * streams nor needs the INTERNET permission. When there is no network a
 * viewer simply sees the external app's error — the app-side label always
 * says "OFFLINE / ONLINE" honestly.
 */
object DemoVideoResolver {

    const val PROVIDER = "YouTube (external search)"
    const val ATTRIBUTION = "Search results are served by YouTube; SHADOW BODY opens the search — it does not select or verify any individual video."

    private const val SEARCH_URL = "https://www.youtube.com/results?search_query="

    /** Deterministic demo metadata for an exercise name; null when the name is blank. */
    fun forExercise(exerciseName: String): ExerciseDemo? {
        val name = exerciseName.trim()
        if (name.isEmpty()) return null
        val query = URLEncoder.encode("$name exercise proper form tutorial", "UTF-8")
            .replace("+", "%20")
        return ExerciseDemo(
            exerciseName = name,
            title = "$name — form demonstration",
            provider = PROVIDER,
            url = SEARCH_URL + query,
            attribution = ATTRIBUTION,
            requiresInternet = true,
            verifiedVideo = false,
        )
    }

    /**
     * Database-facing video metadata fields for one exercise, expressed as a
     * value object (title / source / url / attribution map 1:1 onto the
     * requirement's metadata fields without inventing specific video ids).
     */
    data class ExerciseDemo(
        val exerciseName: String,
        val title: String,
        val provider: String,
        val url: String,
        val attribution: String,
        /** True — playback needs connectivity; UI must label this. */
        val requiresInternet: Boolean,
        /** Always false: no individual video has been verified by this app. */
        val verifiedVideo: Boolean,
    )
}
