package io.github.xororz.localdream.data

import androidx.compose.runtime.Immutable

/**
 * Fully resolved default generation parameters. The constructor defaults are
 * the app-wide global fallbacks: this is the single place they are defined.
 *
 * Per-model defaults are resolved field by field in [Model.defaults] with the
 * priority: built-in code defaults > config.json in the model directory >
 * these global values.
 */
@Immutable
data class GenerationDefaults(
    val prompt: String = "",
    val negativePrompt: String = "",
    val steps: Float = 20f,
    val cfg: Float = 7f,
    val scheduler: String = "dpm",
    val seed: String = "",
    val denoiseStrength: Float = 0.6f,
    val batchCounts: Int = 1,
    val aspectRatio: String = "1:1",
    // UltraFix runs with its own steps/denoise, independent of the main params
    // above: a few-step, low-denoise pass tuned for the tiled repair regime.
    // Denoise is expressed as a step count (how many of the total steps actually
    // run) rather than a strength, so the user controls the count directly; the
    // default 4 is ceil(10 * 0.4).
    val ultrafixSteps: Float = 10f,
    val ultrafixDenoiseSteps: Int = 4,
    // When on (default), UltraFix runs on the neutral quality tags
    // (ULTRAFIX_QUALITY_PROMPT) instead of the prompt-page prompt; off uses the
    // box prompt as before.
    val ultrafixQualityDenoise: Boolean = true,
) {
    companion object {
        val GLOBAL = GenerationDefaults()

        // Slider/value ranges for user-facing generation parameters. Single
        // source of truth on the Kotlin side: model config.json defaults are
        // clamped to these (ModelConfig), the settings sliders use them, and
        // parameter-share imports coerce into them. The native engine
        // independently enforces the same limits (see
        // app/src/main/cpp/src/RequestParser.hpp) — keep the two in sync.
        const val STEPS_RANGE_MIN = 1f
        const val STEPS_RANGE_MAX = 50f
        val STEPS_RANGE = STEPS_RANGE_MIN..STEPS_RANGE_MAX
        const val CFG_RANGE_MIN = 1f
        const val CFG_RANGE_MAX = 30f
        val CFG_RANGE = CFG_RANGE_MIN..CFG_RANGE_MAX

        // DMD2 (distilled) checkpoints are tuned for low guidance. With the
        // "finer CFG for dmd2 models" toggle on (app_prefs dmd2_fine_cfg,
        // default enabled) and a DMD2 model active, the CFG slider uses this
        // narrowed 0.1-increment range instead of CFG_RANGE. 0.5 is within
        // the engine's accepted 0-30 band (RequestParser.hpp).
        const val DMD2_CFG_RANGE_MIN = 0.5f
        const val DMD2_CFG_RANGE_MAX = 10f
        val DMD2_CFG_RANGE = DMD2_CFG_RANGE_MIN..DMD2_CFG_RANGE_MAX
        // Slider's discrete intermediate points: (MAX-MIN)/0.1 - 1.
        const val DMD2_CFG_SLIDER_STEPS = 94
        const val DENOISE_RANGE_MIN = 0f
        const val DENOISE_RANGE_MAX = 1f
        val DENOISE_RANGE = DENOISE_RANGE_MIN..DENOISE_RANGE_MAX

        // UltraFix slider bounds (kept here so the persistence layer and the
        // dialog agree on the clamp range). Denoise steps are additionally
        // capped at the current total step count at use time.
        const val ULTRAFIX_STEPS_MIN = 1f
        const val ULTRAFIX_STEPS_MAX = 20f
        const val ULTRAFIX_DENOISE_STEPS_MAX = 10

        // Neutral quality tags UltraFix runs on when quality-denoise is on.
        // Subject-free on purpose so tiles don't re-stage the subject; not
        // localized -- it is a model prompt, not UI copy.
        const val ULTRAFIX_QUALITY_PROMPT = "masterpiece, best quality, 4k resolution"
    }
}
