package io.github.xororz.localdream.ui.screens

import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import io.github.xororz.localdream.R
import io.github.xororz.localdream.ui.components.SmoothLinearWavyProgressIndicator
import io.github.xororz.localdream.data.GenerationMode
import io.github.xororz.localdream.data.Model
import io.github.xororz.localdream.utils.ParamShare
import kotlin.math.roundToInt

/**
 * The prompt page of [ModelRunScreen]: the settings card (img2img picker,
 * advanced settings), the prompt/negative-prompt fields, the generate
 * button, the error banner, the live progress card, and the img2img source
 * thumbnail strip.
 *
 * All state is read from the holders *inside* this composable so its
 * recompositions (progress ticks, prompt edits) never reach the screen
 * orchestrator. Start/persistence/source-screen operations stay with the
 * screen and are passed in as callbacks.
 */
@Composable
internal fun RunPromptPage(
    runState: RunGenerationState,
    resultState: RunResultState,
    setupState: RunSetupState,
    upscaleState: RunUpscaleState,
    ultrafixState: RunUltrafixState,
    img2ImgState: RunImg2ImgState,
    shareState: RunShareImportState,
    promptField: PromptFieldController,
    negativePromptField: PromptFieldController,
    model: Model?,
    modelId: String,
    useImg2img: Boolean,
    tagAutocompleteAvailable: Boolean,
    onStepsChange: (Float) -> Unit,
    onCfgChange: (Float) -> Unit,
    onSizeChange: (Float) -> Unit,
    onBatchCountsChange: (Float) -> Unit,
    onDenoiseStrengthChange: (Float) -> Unit,
    onSeedChange: (String) -> Unit,
    onSelectImageClick: () -> Unit,
    onClearImg2imgState: () -> Unit,
    onSaveAllFields: () -> Unit,
    onGenerateClick: () -> Unit,
    onSteppingDecision: (String) -> Unit,
) {
    val context = LocalContext.current
    // String resources hoisted to composable scope (lint: LocalContextGetResourceValueCall).
    val msgImportNoParams = stringResource(R.string.import_no_params)
    val msgPleaseCropFirst = stringResource(R.string.please_crop_first)


        Column(
            modifier = Modifier
                .fillMaxSize()
                // Reserve the IME area so the scroll viewport ends above the
                // keyboard; the focused prompt field is then scrolled above the IME
                // (which also keeps its window position accurate for the popup).
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            RunAdvancedSettingsCard(
                expanded = setupState.showAdvancedSettings,
                onToggleExpanded = {
                    setupState.showAdvancedSettings = !setupState.showAdvancedSettings
                },
                generationMode = context.getSharedPreferences(
                    "app_prefs",
                    Context.MODE_PRIVATE,
                ).getString("generation_mode", "standard") ?: "standard",
                onGenerationModeChange = { mode ->
                    context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                        .edit()
                        .putString("generation_mode", mode)
                        .apply()
                },
                sweepInterval = context.getSharedPreferences(
                    "app_prefs",
                    Context.MODE_PRIVATE,
                ).getInt("sweep_interval", 2),
                onSweepIntervalChange = { value ->
                    context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                        .edit()
                        .putInt("sweep_interval", value)
                        .apply()
                },
                isSdxl = model?.usesFixedCanvas == true,
                runOnCpu = model?.runOnCpu ?: false,
                useImg2img = useImg2img,
                isRunning = runState.isRunning,
                aspectRatio = runState.aspectRatio,
                availableResolutions = setupState.availableResolutions,
                currentWidth = setupState.currentWidth,
                currentHeight = setupState.currentHeight,
                scheduler = runState.scheduler,
                steps = runState.steps,
                cfg = runState.cfg,
                useOpenCL = runState.useOpenCL,
                batchCounts = runState.batchCounts,
                fineCfg = model?.name?.contains("dmd2", ignoreCase = true) == true &&
                    context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                        .getBoolean("dmd2_fine_cfg", true),
                denoiseStrength = runState.denoiseStrength,
                denoiseApplicable = img2ImgState.isInpaintMode ||
                    runState.selectedImageUri != null,
                seed = runState.seed,
                returnedSeed = runState.returnedSeed,
                onAspectRatioSelected = { ratio ->
                    if (!runState.isRunning && runState.aspectRatio != ratio) {
                        runState.aspectRatio = ratio
                        onClearImg2imgState()
                        onSaveAllFields()
                    }
                },
                onCustomAspectRatioClick = {
                    if (!runState.isRunning) {
                        runState.showCustomAspectRatioDialog = true
                    }
                },
                onResolutionSelected = { resolution ->
                    if (!runState.isRunning &&
                        (
                            resolution.width != setupState.currentWidth ||
                                resolution.height != setupState.currentHeight
                            )
                    ) {
                        setupState.pendingResolution = resolution
                        setupState.showResolutionChangeDialog = true
                    }
                },
                onSchedulerChange = { value ->
                    runState.scheduler = value
                    onSaveAllFields()
                },
                onStepsChange = onStepsChange,
                onCfgChange = onCfgChange,
                onSizeChange = onSizeChange,
                onCpuSelected = {
                    runState.useOpenCL = false
                    onSaveAllFields()
                },
                onGpuSelected = { setupState.showOpenCLWarningDialog = true },
                onBatchCountsChange = onBatchCountsChange,
                onDenoiseStrengthChange = onDenoiseStrengthChange,
                onSeedChange = onSeedChange,
                onUseLastSeed = {
                    runState.seed = runState.returnedSeed.toString()
                    onSaveAllFields()
                },
                onImportFromClipboard = {
                    val clipboard =
                        context.getSystemService(
                            Context.CLIPBOARD_SERVICE,
                        ) as? ClipboardManager
                    val raw = clipboard?.primaryClip
                        ?.takeIf { it.itemCount > 0 }
                        ?.getItemAt(0)
                        ?.coerceToText(context)
                        ?.toString()
                    val imported = ParamShare.tryDecode(raw)
                    if (imported != null) {
                        shareState.pendingImport = imported
                        shareState.clipboardImportChecked = true
                    } else {
                        Toast.makeText(
                            context,
                            msgImportNoParams,
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                },
                onShare = {
                    val currentMode = when {
                        img2ImgState.isInpaintMode -> GenerationMode.INPAINT
                        runState.selectedImageUri != null -> GenerationMode.IMG2IMG
                        else -> GenerationMode.TXT2IMG
                    }
                    shareState.shareSourceParams = GenerationParameters(
                        steps = runState.steps.toInt(),
                        cfg = runState.cfg,
                        seed = runState.seed.toLongOrNull(),
                        prompt = promptField.text,
                        negativePrompt = negativePromptField.text,
                        generationTime = null,
                        width = setupState.currentWidth,
                        height = setupState.currentHeight,
                        runOnCpu = model?.runOnCpu ?: false,
                        denoiseStrength = runState.denoiseStrength,
                        useOpenCL = runState.useOpenCL,
                        scheduler = runState.scheduler,
                        mode = currentMode,
                    )
                    shareState.shareSourceModelId = modelId
                },
                onReset = { setupState.showResetConfirmDialog = true },
            )

            AnimatedVisibility(
                visible = resultState.intermediateBitmap == null,
                enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
                exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top),
            ) {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                stringResource(R.string.prompt_settings),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (useImg2img) {
                                    TextButton(
                                        onClick = { onSelectImageClick() },
                                        contentPadding = PaddingValues(
                                            horizontal = 8.dp,
                                            vertical = 8.dp,
                                        ),
                                    ) {
                                        Text(
                                            "img2img",
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.padding(end = 4.dp),
                                        )
                                        Icon(
                                            Icons.Default.Image,
                                            contentDescription = "select image",
                                            modifier = Modifier.size(20.dp),
                                        )
                                    }
                                }
                            }
                        }

                        ControlledPromptTagTextField(
                            controller = promptField,
                            autocompleteAvailable = tagAutocompleteAvailable,
                            modifier = Modifier.fillMaxWidth(),
                            label = {
                                PromptCountLabel(
                                    label = stringResource(R.string.image_prompt),
                                    count = promptField.tokenCount,
                                    max = promptField.tokenMax,
                                    showCount = promptField.text.isNotEmpty(),
                                )
                            },
                        )

                        ControlledPromptTagTextField(
                            controller = negativePromptField,
                            autocompleteAvailable = tagAutocompleteAvailable,
                            modifier = Modifier.fillMaxWidth(),
                            label = {
                                PromptCountLabel(
                                    label = stringResource(R.string.negative_prompt),
                                    count = negativePromptField.tokenCount,
                                    max = negativePromptField.tokenMax,
                                    showCount = negativePromptField.text.isNotEmpty(),
                                )
                            },
                        )

                        RunGenerateButton(
                            runState = runState,
                            upscaleState = upscaleState,
                            ultrafixState = ultrafixState,
                            modifier = Modifier.fillMaxWidth(),
                            onGenerateClick = { onGenerateClick() },
                        )
                    }
                }
            }
            AnimatedVisibility(
                visible = runState.errorMessage != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                runState.errorMessage?.let { msg ->
                    Card(
                        onClick = { runState.errorMessage = null },
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                        ),
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                            )
                            Text(
                                msg,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
            AnimatedVisibility(
                visible = runState.isRunning,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = if (runState.currentBatchIndex > 0) {
                                "${
                                    stringResource(
                                        R.string.generating,
                                    )
                                } (${runState.currentBatchIndex}/${runState.batchCounts})…"
                            } else {
                                stringResource(
                                    R.string.generating,
                                )
                            },
                            style = MaterialTheme.typography.titleMedium,
                        )
                        SmoothLinearWavyProgressIndicator(
                            progress = runState.progress,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            "${(runState.progress * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        resultState.intermediateBitmap?.let { bitmap ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f),
                            ) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "Generation Preview",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit,
                                )
                            }
                        }
                        if (runState.paused && runState.steppingScheduleSteps > 0) {
                            // Stepping mode: the engine pauses after every
                            // step from the user's count onward (the reserve
                            // plan — see startGeneration). The schedule runs a
                            // few steps past the user's count, so every
                            // continued step is a real schedule step that
                            // strictly refines the estimate; there is no
                            // past-the-end degradation path.
                            val schedule = runState.steppingScheduleSteps
                            val pauseAt = runState.steppingPauseAt
                            val currentStep =
                                (runState.progress * schedule).roundToInt()
                            val reserveTotal = schedule - pauseAt
                            val reserveDone =
                                (currentStep - pauseAt).coerceIn(0, reserveTotal)
                            val atScheduleEnd = currentStep >= schedule

                            // Reserve bar: subtle tracker under the preview
                            // showing the position through the extra steps.
                            SmoothLinearWavyProgressIndicator(
                                progress = if (reserveTotal > 0) {
                                    reserveDone.toFloat() / reserveTotal
                                } else {
                                    1f
                                },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Text(
                                stringResource(
                                    R.string.stepping_step_position,
                                    currentStep,
                                    schedule,
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (atScheduleEnd) {
                                // Schedule end: Continue disappears — Finish
                                // with a short auto-confirm countdown.
                                SteppingAutoFinish(onSteppingDecision)
                            } else {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Button(
                                        onClick = { onSteppingDecision("confirm") },
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Text(stringResource(R.string.stepping_finish))
                                    }
                                    OutlinedButton(
                                        onClick = { onSteppingDecision("next") },
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Text(stringResource(R.string.stepping_continue))
                                    }
                                    OutlinedButton(
                                        onClick = { onSteppingDecision("abort") },
                                    ) {
                                        Text(stringResource(R.string.stepping_cancel))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = runState.selectedImageUri != null && runState.base64EncodeDone,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start,
                    ) {
                        Card(
                            modifier = Modifier
                                .size(100.dp),
                            shape = MaterialTheme.shapes.small,
                        ) {
                            Box {
                                img2ImgState.croppedBitmap?.let { bitmap ->
                                    AsyncImage(
                                        model = ImageRequest.Builder(
                                            LocalContext.current,
                                        )
                                            .data(bitmap)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "Cropped Image",
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                } ?: runState.selectedImageUri?.let { uri ->
                                    AsyncImage(
                                        model = ImageRequest.Builder(
                                            LocalContext.current,
                                        )
                                            .data(uri)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "Selected Image",
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        runState.selectedImageUri = null
                                        img2ImgState.croppedBitmap = null
                                        img2ImgState.drawingOverlayBitmap = null
                                        img2ImgState.maskBitmap = null
                                        img2ImgState.isInpaintMode = false
                                        img2ImgState.cropRect = null
                                        img2ImgState.savedPathHistory = null
                                        img2ImgState.hasOriginalImageForStitch = false
                                    },
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(
                                            color = MaterialTheme.colorScheme.surface.copy(
                                                alpha = 0.7f,
                                            ),
                                            shape = CircleShape,
                                        )
                                        .align(Alignment.TopEnd),
                                ) {
                                    Icon(
                                        Icons.Default.Clear,
                                        contentDescription = "Remove Image",
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        img2ImgState.showDrawScreen = true
                                    },
                                    enabled = !runState.isRunning && img2ImgState.croppedBitmap != null,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(
                                            color = MaterialTheme.colorScheme.surface.copy(
                                                alpha = 0.7f,
                                            ),
                                            shape = CircleShape,
                                        )
                                        .align(Alignment.TopStart),
                                ) {
                                    Icon(
                                        Icons.Default.Draw,
                                        contentDescription = "Draw Image",
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                        }

                        AnimatedVisibility(
                            visible = img2ImgState.croppedBitmap != null && !img2ImgState.isInpaintMode,
                            enter = fadeIn() + expandHorizontally(),
                            exit = fadeOut() + shrinkHorizontally(),
                        ) {
                            Row {
                                Spacer(modifier = Modifier.width(12.dp))
                                SmallFloatingActionButton(
                                    onClick = {
                                        if (img2ImgState.croppedBitmap != null) {
                                            img2ImgState.showInpaintScreen = true
                                        } else {
                                            Toast.makeText(
                                                context,
                                                msgPleaseCropFirst,
                                                Toast.LENGTH_SHORT,
                                            ).show()
                                        }
                                    },
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Brush,
                                        contentDescription = "Set Mask",
                                    )
                                }
                            }
                        }

                        AnimatedVisibility(
                            visible = img2ImgState.isInpaintMode && img2ImgState.maskBitmap != null,
                            enter = fadeIn() + expandHorizontally(),
                            exit = fadeOut() + shrinkHorizontally(),
                        ) {
                            Row {
                                Spacer(modifier = Modifier.width(8.dp))
                                Card(
                                    onClick = {
                                        if (img2ImgState.croppedBitmap != null && img2ImgState.maskBitmap != null) {
                                            img2ImgState.showInpaintScreen = true
                                        }
                                    },
                                    modifier = Modifier.size(100.dp),
                                    shape = MaterialTheme.shapes.small,
                                ) {
                                    Box {
                                        img2ImgState.maskBitmap?.let { mb ->
                                            AsyncImage(
                                                model = ImageRequest.Builder(
                                                    LocalContext.current,
                                                )
                                                    .data(mb)
                                                    .crossfade(true)
                                                    .build(),
                                                contentDescription = "Mask Image",
                                                modifier = Modifier.fillMaxSize(),
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                img2ImgState.maskBitmap = null
                                                img2ImgState.isInpaintMode = false
                                                img2ImgState.savedPathHistory = null
                                            },
                                            modifier = Modifier
                                                .size(24.dp)
                                                .background(
                                                    color = MaterialTheme.colorScheme.surface.copy(
                                                        alpha = 0.7f,
                                                    ),
                                                    shape = CircleShape,
                                                )
                                                .align(Alignment.TopEnd),
                                        ) {
                                            Icon(
                                                Icons.Default.Clear,
                                                contentDescription = "Clear Mask",
                                                modifier = Modifier.size(16.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
}

@Composable
private fun PromptCountLabel(label: String, count: Int, max: Int, showCount: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label)
        if (showCount) {
            Spacer(Modifier.width(6.dp))
            Text("$count/$max")
        }
    }
}

/**
 * Stepping mode, schedule end: Finish plus a ~2 second countdown that
 * auto-confirms the final pause, so reaching the schedule end flows straight
 * to the results unless the user taps Finish (same outcome) or Cancel.
 */
@Composable
private fun SteppingAutoFinish(onSteppingDecision: (String) -> Unit) {
    var countdown by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val durationNanos = 2_000_000_000f
        var last = withFrameNanos { it }
        while (countdown < 1f) {
            val now = withFrameNanos { it }
            countdown = (countdown + (now - last) / durationNanos).coerceIn(0f, 1f)
            last = now
        }
        onSteppingDecision("confirm")
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            stringResource(R.string.stepping_auto_finish),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SmoothLinearWavyProgressIndicator(
            progress = countdown,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
