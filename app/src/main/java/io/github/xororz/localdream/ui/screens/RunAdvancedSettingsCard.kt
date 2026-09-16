package io.github.xororz.localdream.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.xororz.localdream.R
import io.github.xororz.localdream.data.GenerationDefaults
import io.github.xororz.localdream.data.Resolution
import io.github.xororz.localdream.utils.schedulerDisplayName
import kotlin.math.roundToInt

/**
 * The collapsible "Advanced Settings" card shown above the prompts card on the
 * run screen (replaces the former AdvancedSettingsDialog). Pure UI: every
 * state mutation is routed back through callbacks so the screen keeps
 * ownership of parameter state and persistence.
 *
 * Batch/seed interplay: while an explicit seed is set, the batch slider is
 * disabled and reads 1 (the engine already ignores batch counts when a seed
 * is fixed); its previous position is retained and restored when the seed is
 * cleared. Tapping the disabled slider explains why and offers to clear the
 * seed.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun RunAdvancedSettingsCard(
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    generationMode: String,
    onGenerationModeChange: (String) -> Unit,
    sweepInterval: Int,
    onSweepIntervalChange: (Int) -> Unit,
    pauseAt: Int,
    pauseAtAuto: Boolean,
    onPauseAtChange: (Int) -> Unit,
    isSdxl: Boolean,
    runOnCpu: Boolean,
    useImg2img: Boolean,
    isRunning: Boolean,
    aspectRatio: String,
    availableResolutions: List<Resolution>,
    currentWidth: Int,
    currentHeight: Int,
    scheduler: String,
    steps: Float,
    cfg: Float,
    useOpenCL: Boolean,
    batchCounts: Int,
    fineCfg: Boolean,
    denoiseStrength: Float,
    denoiseApplicable: Boolean,
    seed: String,
    returnedSeed: Long?,
    onAspectRatioSelected: (String) -> Unit,
    onCustomAspectRatioClick: () -> Unit,
    onResolutionSelected: (Resolution) -> Unit,
    onSchedulerChange: (String) -> Unit,
    onStepsChange: (Float) -> Unit,
    onCfgChange: (Float) -> Unit,
    onSizeChange: (Float) -> Unit,
    onCpuSelected: () -> Unit,
    onGpuSelected: () -> Unit,
    onBatchCountsChange: (Float) -> Unit,
    onDenoiseStrengthChange: (Float) -> Unit,
    onSeedChange: (String) -> Unit,
    onUseLastSeed: () -> Unit,
    onImportFromClipboard: () -> Unit,
    onShare: () -> Unit,
    onReset: () -> Unit,
) {
    val seedSet = seed.isNotBlank()
    val focusManager = LocalFocusManager.current

    // Local mirror of the persisted generation mode: updates immediately on
    // tap (pref writes don't trigger recomposition) and re-syncs when the
    // prop changes (model switch re-reads the pref).
    var selectedMode by remember(generationMode) { mutableStateOf(generationMode) }
    val activeMode = selectedMode.ifBlank { generationMode }

    // Local mirror of the persisted stepping pause point (see selectedMode):
    // the card-face label and the slider must track a drag immediately and
    // pref writes don't trigger recomposition. Re-syncs when the props change
    // (step-count edits move the auto default or clamp a manual value;
    // reproduce/restore re-reads the pref).
    var pausePoint by remember(steps.roundToInt(), pauseAt, pauseAtAuto) {
        mutableIntStateOf(
            steppingPausePoint(steps.roundToInt(), pauseAt, pauseAtAuto),
        )
    }

    // Local mirror of the persisted sweep interval (see selectedMode): the
    // slider must track a drag immediately and pref writes don't recompose.
    var sweepIntervalMirror by remember(sweepInterval) {
        mutableIntStateOf(sweepInterval)
    }

    // Last free (seed-less) batch position, restored when the seed is cleared.
    var retainedBatch by rememberSaveable { mutableFloatStateOf(1f) }
    var showBatchSeedDialog by remember { mutableStateOf(false) }

    // Kept at card level (outside the collapsed content) so a seed arriving
    // from a param import while the card is collapsed still normalizes batch.
    LaunchedEffect(seedSet) {
        when {
            seedSet -> if (batchCounts != 1) onBatchCountsChange(1f)

            batchCounts == 1 && retainedBatch > 1f -> onBatchCountsChange(retainedBatch)
        }
    }

    if (showBatchSeedDialog) {
        ModelRunConfirmDialog(
            title = stringResource(R.string.seed_set_title),
            text = stringResource(R.string.batch_seed_incompatible),
            subText = stringResource(R.string.batch_seed_hint),
            confirmText = stringResource(R.string.clear_seed),
            dismissText = stringResource(R.string.acknowledge),
            onConfirm = {
                // Drop focus first so clearing the seed doesn't wake the
                // keyboard on the (now empty) seed field.
                focusManager.clearFocus(force = true)
                onSeedChange("")
                showBatchSeedDialog = false
            },
            onDismiss = { showBatchSeedDialog = false },
        )
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.advanced_settings_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onImportFromClipboard) {
                    Icon(
                        imageVector = Icons.Default.ContentPaste,
                        contentDescription = stringResource(R.string.import_from_clipboard),
                    )
                }
                IconButton(onClick = onShare) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = stringResource(R.string.share),
                    )
                }
                IconButton(onClick = onToggleExpanded) {
                    Icon(
                        imageVector = if (expanded) {
                            Icons.Default.ExpandLess
                        } else {
                            Icons.Default.ExpandMore
                        },
                        contentDescription = stringResource(R.string.advanced_settings_title),
                    )
                }
            }

            // Steps and CFG are the least advanced of the advanced settings,
            // so they stay on the card face (visible while collapsed) for
            // quick adjustment; the rest lives behind the expander.
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column {
                    Text(
                        text = when (activeMode) {
                            "sweep" -> {
                                // Sweep: show the checkpoint step counts the
                                // interval produces (target, +interval, +2*interval).
                                val t = steps.roundToInt()
                                stringResource(
                                    R.string.steps_range,
                                    "$t",
                                    "${t + sweepInterval}",
                                    "${t + 2 * sweepInterval}",
                                )
                            }

                            "stepping" -> {
                                // Stepping: the schedule is the user's step
                                // count; the engine pauses at the pause point
                                // (auto default: steps minus the reserve) and
                                // after every later step (see startGeneration).
                                // pausePoint is the card-level mirror so the
                                // label tracks a slider drag (see selectedMode).
                                val t = steps.roundToInt()
                                val pause = pausePoint
                                if (t >= 3) {
                                    stringResource(
                                        R.string.steps_pause,
                                        t,
                                        pause,
                                        t - pause,
                                    )
                                } else {
                                    stringResource(R.string.steps, t)
                                }
                            }

                            else -> stringResource(R.string.steps, steps.roundToInt())
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Slider(
                        value = steps,
                        onValueChange = onStepsChange,
                        valueRange = GenerationDefaults.STEPS_RANGE,
                        steps = 48,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (activeMode == "stepping" && steps.roundToInt() >= 3) {
                    // Pause point: where the engine starts pausing (and the
                    // reserve begins). Auto-managed unless the user moves it;
                    // the reserve is whatever remains up to the step count.
                    // pausePoint is the card-level mirror (see selectedMode):
                    // it tracks the drag immediately and the pref write below
                    // makes the value manual for later runs.
                    val t = steps.roundToInt()
                    Column {
                        Text(
                            stringResource(R.string.pause_point, pausePoint),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Slider(
                            value = pausePoint.toFloat(),
                            onValueChange = { value ->
                                val manual = value.roundToInt().coerceIn(2, t - 1)
                                pausePoint = manual
                                onPauseAtChange(manual)
                            },
                            valueRange = 2f..(t - 1).coerceAtLeast(2).toFloat(),
                            steps = (t - 4).coerceAtLeast(0),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                if (activeMode == "sweep") {
                    // Sweep interval: how many steps between checkpoint
                    // decodes after the target is reached (1..5).
                    // sweepIntervalMirror is the card-level mirror (see
                    // selectedMode): it tracks the drag immediately.
                    Column {
                        Text(
                            stringResource(R.string.sweep_interval, sweepIntervalMirror),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Slider(
                            value = sweepIntervalMirror.toFloat(),
                            onValueChange = { value ->
                                val interval = value.roundToInt()
                                sweepIntervalMirror = interval
                                onSweepIntervalChange(interval)
                            },
                            valueRange = 1f..5f,
                            steps = 3,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                Column {
                    Text(
                        "CFG Scale: %.1f".format(cfg),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    // DMD2 checkpoints get a narrowed 0.1-increment range
                    // when the "finer CFG" setting is on; everything else
                    // uses the standard full range.
                    Slider(
                        value = cfg,
                        onValueChange = onCfgChange,
                        valueRange = if (fineCfg) {
                            GenerationDefaults.DMD2_CFG_RANGE
                        } else {
                            GenerationDefaults.CFG_RANGE
                        },
                        steps = if (fineCfg) {
                            GenerationDefaults.DMD2_CFG_SLIDER_STEPS
                        } else {
                            57
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    // Generation mode: standard (default), stepping (watch the
                    // composition form step by step, then keep / extend /
                    // discard — an experimental tool, hence the muted flask
                    // styling), or sweep (the same seed rendered at three step
                    // counts). Styled after the dark mode selection.
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            stringResource(R.string.generation_mode),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(
                                ButtonGroupDefaults.ConnectedSpaceBetween,
                            ),
                        ) {
                            val modeOptions = listOf(
                                "standard" to R.string.gen_mode_standard,
                                "stepping" to R.string.gen_mode_stepping,
                                "sweep" to R.string.gen_mode_sweep,
                            )
                            modeOptions.forEachIndexed { index, (id, label) ->
                                val shapes = when (index) {
                                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()

                                    modeOptions.lastIndex ->
                                        ButtonGroupDefaults.connectedTrailingButtonShapes()

                                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                                }
                                ToggleButton(
                                    checked = activeMode == id,
                                    onCheckedChange = { checked ->
                                        if (checked) {
                                            selectedMode = id
                                            onGenerationModeChange(id)
                                        }
                                    },
                                    shapes = shapes,
                                    // Stepping is an experimental tool: keep
                                    // it visually muted so it never reads as
                                    // a peer of the standard workflow.
                                    modifier = if (id == "stepping") {
                                        Modifier
                                            .weight(1f)
                                            .alpha(0.75f)
                                    } else {
                                        Modifier.weight(1f)
                                    },
                                ) {
                                    if (id == "stepping") {
                                        Icon(
                                            imageVector = Icons.Default.Science,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(16.dp)
                                                .padding(end = 2.dp),
                                        )
                                        Text(
                                            stringResource(label),
                                            textDecoration =
                                                TextDecoration.LineThrough,
                                        )
                                    } else {
                                        Text(stringResource(label))
                                    }
                                }
                            }
                        }
                    }
                    // Aspect ratio needs the VAE encoder (inpaint-based padding),
                    // which --no_img2img does not load.
                    if (isSdxl && useImg2img) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                stringResource(R.string.aspect_ratio),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            val presets = listOf("1:1", "3:4", "4:3")
                            val isCustom = aspectRatio !in presets
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(
                                    ButtonGroupDefaults.ConnectedSpaceBetween,
                                ),
                            ) {
                                presets.forEachIndexed { index, ratio ->
                                    val shapes = if (index == 0) {
                                        ButtonGroupDefaults.connectedLeadingButtonShapes()
                                    } else {
                                        ButtonGroupDefaults.connectedMiddleButtonShapes()
                                    }
                                    ToggleButton(
                                        checked = aspectRatio == ratio,
                                        onCheckedChange = { checked ->
                                            if (checked) onAspectRatioSelected(ratio)
                                        },
                                        shapes = shapes,
                                        enabled = !isRunning,
                                    ) {
                                        Text(ratio)
                                    }
                                }
                                ToggleButton(
                                    checked = isCustom,
                                    onCheckedChange = { onCustomAspectRatioClick() },
                                    shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(),
                                    enabled = !isRunning,
                                ) {
                                    Text(
                                        if (isCustom) {
                                            aspectRatio
                                        } else {
                                            stringResource(R.string.aspect_ratio_custom)
                                        },
                                    )
                                }
                            }
                        }
                    }
                    if (!runOnCpu && !isSdxl && availableResolutions.isNotEmpty()) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                stringResource(R.string.resolution),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(
                                    ButtonGroupDefaults.ConnectedSpaceBetween,
                                ),
                            ) {
                                availableResolutions.forEachIndexed { index, resolution ->
                                    val shapes = when (index) {
                                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()

                                        availableResolutions.lastIndex ->
                                            ButtonGroupDefaults.connectedTrailingButtonShapes()

                                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                                    }
                                    ToggleButton(
                                        checked = currentWidth == resolution.width &&
                                            currentHeight == resolution.height,
                                        onCheckedChange = { checked ->
                                            if (checked) onResolutionSelected(resolution)
                                        },
                                        shapes = shapes,
                                        enabled = !isRunning,
                                    ) {
                                        Text(resolution.toString())
                                    }
                                }
                            }
                        }
                    }

                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Split scheduler id into base + Karras flag so the UI
                        // can offer one base chip per family plus a single
                        // Karras switch, instead of listing every combination.
                        val baseId = scheduler.removeSuffix("_karras")
                        val karras = scheduler.endsWith("_karras")
                        val karrasSupported = baseId != "lcm"
                        val baseOptions = listOf(
                            "dpm" to "DPM++ 2M",
                            "dpm_sde" to "DPM++ 2M SDE",
                            "euler_a" to "Euler A",
                            "euler" to "Euler",
                            "lcm" to "LCM",
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            val context = LocalContext.current
                            val msgKarrasUnavailable = stringResource(
                                R.string.karras_unavailable,
                                schedulerDisplayName(baseId),
                            )
                            Text(
                                stringResource(R.string.scheduler),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                "Karras",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .alpha(if (karrasSupported) 1f else 0.4f),
                            )
                            CompositionLocalProvider(
                                LocalMinimumInteractiveComponentSize provides Dp.Unspecified,
                            ) {
                                // Always pressable: enabling Karras under an
                                // incompatible scheduler explains itself via a
                                // toast instead of being a silent no-op.
                                Switch(
                                    checked = karras && karrasSupported,
                                    onCheckedChange = { enable ->
                                        if (enable && !karrasSupported) {
                                            Toast.makeText(
                                                context,
                                                msgKarrasUnavailable,
                                                Toast.LENGTH_SHORT,
                                            ).show()
                                        } else {
                                            onSchedulerChange(
                                                if (enable) "${baseId}_karras" else baseId,
                                            )
                                        }
                                    },
                                    modifier = Modifier.scale(0.8f),
                                )
                            }
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(
                                ButtonGroupDefaults.ConnectedSpaceBetween,
                            ),
                        ) {
                            baseOptions.forEachIndexed { index, (id, label) ->
                                val shapes = when (index) {
                                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()

                                    baseOptions.lastIndex ->
                                        ButtonGroupDefaults.connectedTrailingButtonShapes()

                                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                                }
                                ToggleButton(
                                    checked = baseId == id,
                                    onCheckedChange = { checked ->
                                        if (checked) {
                                            val nextKarras = karras && id != "lcm"
                                            onSchedulerChange(
                                                if (nextKarras) "${id}_karras" else id,
                                            )
                                        }
                                    },
                                    shapes = shapes,
                                ) {
                                    Text(label)
                                }
                            }
                        }
                    }

                    if (runOnCpu) {
                        Column {
                            Text(
                                stringResource(
                                    R.string.image_size,
                                    currentWidth,
                                    currentHeight,
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Slider(
                                value = currentWidth.toFloat(),
                                onValueChange = onSizeChange,
                                valueRange = 128f..512f,
                                steps = 5,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(
                                ButtonGroupDefaults.ConnectedSpaceBetween,
                            ),
                        ) {
                            Text(
                                "Runtime",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(end = 4.dp),
                            )
                            ToggleButton(
                                checked = !useOpenCL,
                                onCheckedChange = { checked ->
                                    if (checked) onCpuSelected()
                                },
                                shapes = ButtonGroupDefaults.connectedLeadingButtonShapes(),
                                modifier = Modifier.weight(1f),
                            ) {
                                Text("CPU")
                            }
                            ToggleButton(
                                checked = useOpenCL,
                                onCheckedChange = { checked ->
                                    if (checked) onGpuSelected()
                                },
                                shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(),
                                modifier = Modifier.weight(1f),
                            ) {
                                Text("GPU")
                            }
                        }
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedTextField(
                            value = seed,
                            onValueChange = onSeedChange,
                            label = { Text(stringResource(R.string.random_seed)) },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            trailingIcon = {
                                if (seed.isNotEmpty()) {
                                    IconButton(onClick = { onSeedChange("") }) {
                                        Icon(
                                            Icons.Default.Clear,
                                            contentDescription = "clear",
                                        )
                                    }
                                }
                            },
                        )

                        if (returnedSeed != null) {
                            FilledTonalButton(
                                onClick = onUseLastSeed,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = stringResource(R.string.use_last_seed),
                                    modifier = Modifier
                                        .size(20.dp)
                                        .padding(end = 4.dp),
                                )
                                Text(
                                    stringResource(
                                        R.string.use_last_seed,
                                        returnedSeed.toString(),
                                    ),
                                )
                            }
                        }
                    }

                    // Batch sits below the seed and is forced to 1 while a
                    // seed is set (the engine ignores batch counts for fixed
                    // seeds); tapping it explains and offers to clear the seed.
                    // Stepping/sweep modes also lock batch to 1 (sweep drives
                    // its own multi-image run plan).
                    val batchLocked = seedSet || activeMode != "standard"
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (seedSet) {
                                    Modifier.pointerInput(Unit) {
                                        detectTapGestures {
                                            showBatchSeedDialog = true
                                        }
                                    }
                                } else {
                                    Modifier
                                },
                            ),
                    ) {
                        Column {
                            Text(
                                stringResource(
                                    R.string.batch_count,
                                    if (batchLocked) 1 else batchCounts,
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Slider(
                                value = if (batchLocked) 1f else batchCounts.toFloat(),
                                onValueChange = { value ->
                                    retainedBatch = value
                                    onBatchCountsChange(value)
                                },
                                valueRange = 1f..10f,
                                steps = 8,
                                enabled = !batchLocked,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                    if (useImg2img) {
                        // Greyed (but still adjustable) while the next run
                        // won't be img2img: purely a visual cue that denoise
                        // has no effect for txt2img.
                        Column(
                            modifier = Modifier.alpha(
                                if (denoiseApplicable) 1f else 0.4f,
                            ),
                        ) {
                            Text(
                                "[img2img]Denoise Strength: %.2f".format(denoiseStrength),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Slider(
                                value = denoiseStrength,
                                onValueChange = onDenoiseStrengthChange,
                                valueRange = 0f..1f,
                                steps = 99,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(
                            onClick = onReset,
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error,
                            ),
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.reset),
                                modifier = Modifier
                                    .size(20.dp)
                                    .padding(end = 4.dp),
                            )
                            Text(stringResource(R.string.reset))
                        }
                    }




                }
            }
        }
    }
}
