package io.github.xororz.localdream.ui.screens

import android.util.Log
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import io.github.xororz.localdream.R
import io.github.xororz.localdream.data.GenerationMode
import io.github.xororz.localdream.data.HistoryItem
import io.github.xororz.localdream.data.HistoryManager
import io.github.xororz.localdream.data.Model
import io.github.xororz.localdream.service.BackgroundGenerationService
import io.github.xororz.localdream.service.BackgroundGenerationService.GenerationState
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Generation-service state machine of [ModelRunScreen]: turns service state
 * emissions (progress / complete / error) into screen state updates, history
 * persistence, and the result-page hand-off.
 *
 * Collects the service flow *inside* this composable so progress ticks only
 * recompose this (invisible) scope, never the screen orchestrator. Launched
 * effect is keyed on the collected state, preserving the original
 * cancel-and-restart-per-emission semantics.
 */
@Composable
internal fun RunGenerationEffects(
    runState: RunGenerationState,
    resultState: RunResultState,
    setupState: RunSetupState,
    ultrafixState: RunUltrafixState,
    img2ImgState: RunImg2ImgState,
    model: Model?,
    modelId: String,
    historyManager: HistoryManager,
    pagerState: PagerState,
    coroutineScope: CoroutineScope,
) {
    val serviceState by BackgroundGenerationService.generationState.collectAsState()
    val msgGenerationCancelled = stringResource(R.string.generation_cancelled)
    LaunchedEffect(serviceState) {

        when (val state = serviceState) {
            is GenerationState.Progress -> {
                if (runState.generationStartTime == null) {
                    runState.generationStartTime = System.currentTimeMillis()
                }
                runState.progress = state.progress
                runState.isRunning = true
                runState.paused = state.paused
                state.intermediateImage?.let { resultState.intermediateBitmap = it }
            }

            is GenerationState.Complete -> {
                resultState.intermediateBitmap = null
                withContext(Dispatchers.Main) {
                    Log.d("ModelRunScreen", "update bitmap")

                    state.seed?.let { runState.returnedSeed = it }
                    runState.progress = 0f

                    val genTime = runState.generationStartTime?.let { startTime ->
                        val endTime = System.currentTimeMillis()
                        val duration = endTime - startTime
                        when {
                            duration < 1000 -> "${duration}ms"

                            duration < 60000 -> String.format(Locale.US, "%.1fs", duration / 1000.0)

                            else -> String.format(
                                Locale.US,
                                "%dm%ds",
                                duration / 60000,
                                (duration % 60000) / 1000,
                            )
                        }
                    }

                    val wasUltrafix = ultrafixState.pendingUltrafix
                    ultrafixState.pendingUltrafix = false
                    val currentGenerationMode = when {
                        wasUltrafix -> GenerationMode.ULTRAFIX
                        img2ImgState.isInpaintMode -> GenerationMode.INPAINT
                        runState.selectedImageUri != null -> GenerationMode.IMG2IMG
                        else -> GenerationMode.TXT2IMG
                    }

                    val newParams = GenerationParameters(
                        // The engine reports the steps it actually executed —
                        // the exit step for a stepping early exit (also a
                        // sweep checkpoint), the schedule length for a
                        // natural completion. Falls back to the plan for
                        // older engines.
                        steps = state.steps.takeIf { it > 0 }
                            ?: setupState.generationParamsTmp.steps,
                        cfg = setupState.generationParamsTmp.cfg,
                        seed = runState.returnedSeed,
                        prompt = setupState.generationParamsTmp.prompt,
                        negativePrompt = setupState.generationParamsTmp.negativePrompt,
                        generationTime = genTime,
                        width = if (model?.runOnCpu == true) setupState.generationParamsTmp.width else state.bitmap.width,
                        height = if (model?.runOnCpu == true) setupState.generationParamsTmp.height else state.bitmap.height,
                        runOnCpu = model?.runOnCpu ?: false,
                        denoiseStrength = setupState.generationParamsTmp.denoiseStrength,
                        useOpenCL = setupState.generationParamsTmp.useOpenCL,
                        scheduler = setupState.generationParamsTmp.scheduler,
                        mode = currentGenerationMode,
                        nsfwScore = state.nsfwScore,
                        scheduleSteps = setupState.generationParamsTmp.scheduleSteps,
                    )

                    // Save to disk and update history list. The saved item's id is
                    // forwarded to both the snapshot and the currently-displayed marker
                    // so handleSaveImage can later confirm the user is still looking at
                    // this generation (and not a different history thumbnail).
                    coroutineScope.launch(Dispatchers.IO) {
                        val toSave = buildList {
                            add(state.bitmap to newParams.steps)
                        }
                        var lastSaved: HistoryItem? = null
                        for ((bitmap, steps) in toSave) {
                            val savedItem = historyManager.saveGeneratedImage(
                                modelId = modelId,
                                bitmap = bitmap,
                                params = newParams.copy(steps = steps),
                                mode = currentGenerationMode,
                            )
                            if (savedItem != null) {
                                lastSaved = savedItem
                            }
                        }
                        if (lastSaved != null) {
                            withContext(Dispatchers.Main) {
                                // An ultrafix result is a standalone image, not a
                                // stitchable inpaint patch.
                                if (!wasUltrafix) {
                                    resultState.stitchableHistoryIds = setOf(lastSaved.id)
                                }
                                resultState.currentDisplayedHistoryId = lastSaved.id
                            }
                        }
                    }

                    resultState.currentBitmap = state.bitmap
                    resultState.generationParams = newParams
                    resultState.generationParamsModelId = modelId
                    resultState.imageVersion += 1

                    if (!wasUltrafix) {
                        img2ImgState.snapshotIsInpaintMode = img2ImgState.isInpaintMode
                        img2ImgState.snapshotSelectedImageUri = runState.selectedImageUri
                        img2ImgState.snapshotCropRect = img2ImgState.cropRect
                        img2ImgState.snapshotMaskBitmap = if (img2ImgState.isInpaintMode) img2ImgState.maskBitmap else null
                        img2ImgState.snapshotDrawingOverlayBitmap = img2ImgState.drawingOverlayBitmap
                        img2ImgState.snapshotHasOriginalImage = img2ImgState.hasOriginalImageForStitch
                    }
                    // resultState.stitchableHistoryIds / resultState.currentDisplayedHistoryId are set once
                    // the DB save above resolves.
                    resultState.stitchableHistoryIds = emptySet()
                    resultState.currentDisplayedHistoryId = null

                    Log.d(
                        "ModelRunScreen",
                        "params update: ${resultState.generationParams?.steps}, ${resultState.generationParams?.cfg}",
                    )

                    runState.generationStartTime = null

                    // Navigate to the results page when the run is over — but
                    // not mid-batch: sweep completes run-by-run and the
                    // remaining runs' progress lives on the prompt page.
                    // Device finding 2026-09-17: the old `currentBatchIndex
                    // <= 1` clause also matched sweep run 1 (the index is
                    // 1-based), bouncing to the results page after the
                    // first sweep run and back mid-plan. Stepping finishes
                    // as a single run, so its early exit (Finish) reaches
                    // the results page even with the advanced panel open
                    // (its controls live there).
                    val isLastRun = runState.batchTotal <= 1 ||
                        runState.currentBatchIndex >= runState.batchTotal
                    if (pagerState.currentPage == 0 && isLastRun) {
                        try {
                            pagerState.animateScrollToPage(1)
                        } finally {
                            BackgroundGenerationService.markBitmapConsumed()
                        }
                    } else {
                        BackgroundGenerationService.markBitmapConsumed()
                    }
                }
            }

            is GenerationState.Error -> {
                resultState.intermediateBitmap = null
                // A stepping abort is a deliberate user action (the engine
                // throws "Cancelled in stepping mode"), not a failure — show
                // the cancelled message instead of the raw error.
                runState.errorMessage =
                    if (runState.userRequestedAbort) {
                        msgGenerationCancelled
                    } else {
                        state.message
                    }
                runState.userRequestedAbort = false
                runState.isRunning = false
                runState.paused = false
                runState.progress = 0f
                runState.generationStartTime = null
                ultrafixState.pendingUltrafix = false
            }

            else -> {
                runState.isRunning = false
                runState.progress = 0f
            }
        }
    }
}
