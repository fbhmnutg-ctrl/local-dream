package io.github.xororz.localdream.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.ImageDecoder
import android.graphics.Rect as AndroidRect
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.PickMultipleVisualMedia
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.graphics.scale
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.AsyncImage
import coil.request.ImageRequest
import io.github.xororz.localdream.BuildConfig
import io.github.xororz.localdream.R
import io.github.xororz.localdream.data.GenerationDefaults
import io.github.xororz.localdream.data.GenerationMode
import io.github.xororz.localdream.data.GenerationPreferences
import io.github.xororz.localdream.data.HistoryFilter
import io.github.xororz.localdream.data.HistoryItem
import io.github.xororz.localdream.data.HistoryManager
import io.github.xororz.localdream.data.ModelRepository
import io.github.xororz.localdream.data.ModelStorage
import io.github.xororz.localdream.data.PatchScanner
import io.github.xororz.localdream.data.RemoteRepository
import io.github.xororz.localdream.data.Resolution
import io.github.xororz.localdream.data.TagAutocompleteRepository
import io.github.xororz.localdream.data.TagMatchType
import io.github.xororz.localdream.data.TagSuggestion
import io.github.xororz.localdream.data.UpscalerRepository
import io.github.xororz.localdream.service.BackendService
import io.github.xororz.localdream.service.BackgroundGenerationService
import io.github.xororz.localdream.service.BackgroundGenerationService.GenerationState
import io.github.xororz.localdream.ui.components.BlockingProgressOverlay
import io.github.xororz.localdream.ui.components.GenerationParamsDialog
import io.github.xororz.localdream.ui.components.ImportParametersDialog
import io.github.xororz.localdream.ui.components.OverlayIconButton
import io.github.xororz.localdream.ui.components.ReproduceParametersDialog
import io.github.xororz.localdream.ui.components.ShareParamsFlow
import io.github.xororz.localdream.ui.components.SmoothLinearWavyProgressIndicator
import io.github.xororz.localdream.ui.components.ZoomableImageOverlay
import io.github.xororz.localdream.ui.theme.Motion
import io.github.xororz.localdream.utils.ImportedParams
import io.github.xororz.localdream.utils.LogCapture
import io.github.xororz.localdream.utils.ParamShare
import io.github.xororz.localdream.utils.ParamShareField
import io.github.xororz.localdream.utils.performUpscale
import io.github.xororz.localdream.utils.reportImage
import io.github.xororz.localdream.utils.saveImage
import io.github.xororz.localdream.utils.saveImageFromFile
import java.io.File
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray

private data class EditReferenceSelection(
    val uri: Uri,
    val bitmap: Bitmap,
    val base64: String,
)

@SuppressLint("DefaultLocale")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ModelRunScreen(
    modelId: String,
    navController: NavController,
    modifier: Modifier = Modifier,
    isRemote: Boolean = false,
) {
    val serviceState by BackgroundGenerationService.generationState.collectAsState()
    val backendState by BackendService.backendState.collectAsState()
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    val generationPreferences = remember { GenerationPreferences(context) }
    val coroutineScope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val modelRepository = remember { ModelRepository.getInstance(context) }
    val remoteRepository = remember { RemoteRepository.getInstance(context) }
    // Control client for the host device; null in local mode. The generation
    // host feeds every backend HTTP call (generate/tokenize/health).
    val remoteClient = remember(remoteRepository.connection) {
        if (isRemote) remoteRepository.client() else null
    }
    val backendHost = remoteClient?.generationHost
        ?: BackgroundGenerationService.LOCAL_BACKEND_HOST

    // String resources hoisted to composable scope (lint: LocalContextGetResourceValueCall).
    val msgMediaPermissionHint = stringResource(R.string.media_permission_hint)
    val msgBackendFailed = stringResource(R.string.backend_failed)
    val msgImportNoParams = stringResource(R.string.import_no_params)
    val msgImageSaved = stringResource(R.string.image_saved)
    val msgDeleted = stringResource(R.string.deleted)
    val msgDeleteFailedMessage = stringResource(R.string.delete_failed_message)
    val msgImportApplied = stringResource(R.string.import_applied)
    val msgUpscaleFailed = stringResource(R.string.upscale_failed)
    val msgUltrafixFailed = stringResource(R.string.ultrafix_failed)
    val msgSavedCountWithFailed = stringResource(R.string.saved_count_with_failed)
    val msgDeletedCountWithFailed = stringResource(R.string.deleted_count_with_failed)
    val msgUnknownError = stringResource(R.string.unknown_error)
    val msgSaveFailed = stringResource(R.string.save_failed_detail)
    val msgImg2imgFailed = stringResource(R.string.img2img_failed_detail)
    val msgPleaseCropFirst = stringResource(R.string.please_crop_first)
    val msgReportSuccess = stringResource(R.string.report_success)
    val msgReportFailed = stringResource(R.string.report_failed)
    val msgNoImageAvailable = stringResource(R.string.no_image_available)
    val msgImageLoadFailed = stringResource(R.string.image_load_failed)
    val msgGenerationInterrupted = stringResource(R.string.generation_interrupted)
    val msgRemoteSelectFailed = stringResource(R.string.remote_select_failed)
    // Reaches the screen with the repository already loaded on the normal
    // navigation path; resolves asynchronously after process recreation.
    val model = if (isRemote) {
        remember(remoteRepository.models) { remoteRepository.models.find { it.id == modelId } }
    } else {
        remember(modelRepository.models) { modelRepository.models.find { it.id == modelId } }
    }
    val supportsReferenceEditing = model?.ditKind == "klein" ||
        model?.ditKind == "qwen21"
    LaunchedEffect(Unit) {
        if (!isRemote) {
            modelRepository.ensureLoaded()
        }
    }
    val historyManager = remember { HistoryManager(context) }
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val focusManager = LocalFocusManager.current
    val interactionSource = remember { MutableInteractionSource() }

    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var showOpenCLWarningDialog by remember { mutableStateOf(false) }
    var showInterruptDialog by remember { mutableStateOf(false) }

    var currentBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var intermediateBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var imageVersion by remember { mutableIntStateOf(0) }
    var generationParams by remember { mutableStateOf<GenerationParameters?>(null) }
    var generationParamsModelId by remember { mutableStateOf(modelId) }

    // History state
    var historyFilter by remember(modelId) {
        mutableStateOf(HistoryFilter(modelIds = setOf(modelId)))
    }
    val pagedHistory = remember(historyFilter) { historyManager.pager(historyFilter) }
        .collectAsLazyPagingItems()
    val historyTotalCount by remember(historyFilter) { historyManager.observeCount(historyFilter) }
        .collectAsState(initial = 0)
    // Bounded newest-first feed for the result-page thumbnail strip and the
    // seed-on-open effect; avoids materializing the full history.
    val recentHistory by remember(historyFilter) { historyManager.observeRecent(historyFilter, 20) }
        .collectAsState(initial = emptyList())
    val knownModelIds by remember { historyManager.observeKnownModelIds() }
        .collectAsState(initial = emptyList())
    val knownSchedulers by remember { historyManager.observeKnownSchedulers() }
        .collectAsState(initial = emptyList())
    val knownSizes by remember { historyManager.observeKnownSizes() }
        .collectAsState(initial = emptyList())
    var showHistoryFilterSheet by remember { mutableStateOf(false) }
    var selectedHistoryItem by remember { mutableStateOf<HistoryItem?>(null) }
    var showHistoryDetailDialog by remember { mutableStateOf(false) }
    var showHistoryParametersDialog by remember { mutableStateOf(false) }
    var showDeleteHistoryDialog by remember { mutableStateOf(false) }
    var showReproduceParamsDialog by remember { mutableStateOf(false) }
    var pendingReproduceParams by remember { mutableStateOf<GenerationParameters?>(null) }

    // Parameter share state
    var shareSourceParams by remember { mutableStateOf<GenerationParameters?>(null) }
    var shareSourceModelId by remember { mutableStateOf<String?>(null) }
    var pendingImport by remember { mutableStateOf<ImportedParams?>(null) }
    var clipboardImportChecked by remember { mutableStateOf(false) }
    val shareUseBase64 by remember { generationPreferences.observeShareUseBase64() }
        .collectAsState(initial = true)
    val shareClearClipboardOnImport by remember {
        generationPreferences.observeShareClearClipboardOnImport()
    }.collectAsState(initial = true)

    // Selection mode state
    var isSelectionMode by remember { mutableStateOf(false) }
    val selectedIds = remember { mutableStateListOf<Long>() }
    var showBatchDeleteDialog by remember { mutableStateOf(false) }
    var showBatchSaveDialog by remember { mutableStateOf(false) }
    var isBatchSaving by remember { mutableStateOf(false) }
    var batchSaveTotal by remember { mutableIntStateOf(0) }
    var batchSaveCurrent by remember { mutableIntStateOf(0) }
    var batchSaveFailed by remember { mutableIntStateOf(0) }

    var generationParamsTmp by remember {
        mutableStateOf(
            GenerationParameters(
                steps = 0,
                cfg = 0f,
                seed = 0,
                prompt = "",
                negativePrompt = "",
                generationTime = "",
                width = defaultGenerationSize(
                    model?.usesFixedCanvas == true,
                    model?.runOnCpu == true,
                    model?.isDit == true,
                ),
                height = defaultGenerationSize(
                    model?.usesFixedCanvas == true,
                    model?.runOnCpu == true,
                    model?.isDit == true,
                ),
                runOnCpu = model?.runOnCpu ?: false,
            ),
        )
    }
    var cfg by remember { mutableFloatStateOf(GenerationDefaults.GLOBAL.cfg) }
    var steps by remember { mutableFloatStateOf(GenerationDefaults.GLOBAL.steps) }
    var seed by remember { mutableStateOf(GenerationDefaults.GLOBAL.seed) }
    var denoiseStrength by remember { mutableFloatStateOf(GenerationDefaults.GLOBAL.denoiseStrength) }
    var useOpenCL by remember { mutableStateOf(false) }
    var batchCounts by remember { mutableIntStateOf(GenerationDefaults.GLOBAL.batchCounts) }
    var scheduler by remember { mutableStateOf(GenerationDefaults.GLOBAL.scheduler) }
    var aspectRatio by remember { mutableStateOf(GenerationDefaults.GLOBAL.aspectRatio) }
    var showCustomAspectRatioDialog by remember { mutableStateOf(false) }
    var currentBatchIndex by remember { mutableIntStateOf(0) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    val editReferenceImages = remember { mutableStateListOf<EditReferenceSelection>() }
    // Native-edit references are independent of the base image, so they track their
    // own decode state instead of sharing base64EncodeDone.
    var editReferencesLoading by remember { mutableStateOf(false) }
    var showReferenceEditMenu by remember { mutableStateOf(false) }
    var base64EncodeDone by remember { mutableStateOf(false) }
    var returnedSeed by remember { mutableStateOf<Long?>(null) }
    var isRunning by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isCheckingBackend by remember { mutableStateOf(true) }

    // True only after a health check succeeded (and reset when a restart
    // begins). Gates tokenizer calls: "checking finished" alone also covers
    // the failure case, where firing tokenize requests is pointless.
    var backendReady by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (isRemote) {
            // After process recreation the remote catalog is empty; re-fetch it
            // from the saved host so the model resolves again.
            remoteRepository.restore()
            if (remoteRepository.models.isEmpty()) {
                remoteRepository.refresh()
            }
            if (remoteRepository.models.none { it.id == modelId }) {
                // Host unreachable or the model is gone from it: drop the
                // loading overlay so the offline body below is visible.
                isCheckingBackend = false
            }
        }
    }
    var showParametersDialog by remember { mutableStateOf(false) }
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 3 })
    var generationStartTime by remember { mutableStateOf<Long?>(null) }
    var hasInitialized by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }

    // The prompt fields live on page 0. When the user swipes to the result or
    // history page the suggestion popup is anchored absolutely and would linger,
    // so drop focus (which clears the suggestions) as soon as we leave page 0.
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != 0) {
            focusManager.clearFocus()
        }
    }

    var currentWidth by remember {
        mutableIntStateOf(
            defaultGenerationSize(
                model?.usesFixedCanvas == true,
                model?.runOnCpu == true,
                model?.isDit == true,
            ),
        )
    }
    var currentHeight by remember {
        mutableIntStateOf(
            defaultGenerationSize(
                model?.usesFixedCanvas == true,
                model?.runOnCpu == true,
                model?.isDit == true,
            ),
        )
    }
    var availableResolutions by remember { mutableStateOf<List<Resolution>>(emptyList()) }
    var showResolutionChangeDialog by remember { mutableStateOf(false) }
    var pendingResolution by remember { mutableStateOf<Resolution?>(null) }
    var backendRestartTrigger by remember { mutableIntStateOf(0) }
    var showAdvancedSettings by remember { mutableStateOf(false) }

    var isPreviewMode by remember { mutableStateOf(false) }
    val preferences = remember {
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    }
    // Both settings can only change on the model list screen, so a snapshot
    // taken once per screen entry is enough; re-reading on every recomposition
    // would hit SharedPreferences in the hottest path of this composable.
    // In remote mode the img2img capability is the host's, not this device's.
    val localUseImg2img = remember { preferences.getBoolean("use_img2img", true) }
    val useImg2img = if (isRemote) remoteRepository.useImg2img else localUseImg2img
    val enableTagAutocomplete = remember { preferences.getBoolean("enable_tag_autocomplete", true) }
    val tagSuggestionCount = 128
    val tagAutocompleteRepository = remember { TagAutocompleteRepository.getInstance(context) }
    val tagDictState by tagAutocompleteRepository.state.collectAsState()
    val tagAutocompleteAvailable = enableTagAutocomplete && tagDictState.mainImported

    LaunchedEffect(tagAutocompleteAvailable) {
        if (tagAutocompleteAvailable) {
            tagAutocompleteRepository.warmUp()
        }
    }

    // Names of imported textual-inversion embeddings (filename stems). Refreshed
    // when either prompt field gains focus so newly-imported embeddings show up
    // without re-entering the screen.
    var embeddingNames by remember { mutableStateOf<List<String>>(emptyList()) }

    // Build embedding TagSuggestion rows for the current query. Returns at most
    // `limit` entries: prefix matches first, then contains matches. Comparison
    // normalizes spaces/dashes to underscores so users typing either form match.
    fun embeddingSuggestionsFor(query: String, limit: Int = 5): List<TagSuggestion> {
        if (embeddingNames.isEmpty()) return emptyList()
        val q = query.trim()
            .lowercase()
            .replace(' ', '_')
            .replace('-', '_')
        if (q.isEmpty()) return emptyList()
        val prefix = mutableListOf<TagSuggestion>()
        val contains = mutableListOf<TagSuggestion>()
        for (name in embeddingNames) {
            val normalized = name.lowercase().replace(' ', '_').replace('-', '_')
            val idx = normalized.indexOf(q)
            if (idx < 0) continue
            val suggestion = TagSuggestion(
                replacementTag = name,
                primaryText = name,
                secondaryText = null,
                matchType = TagMatchType.Embedding,
                category = 0,
                postCount = 0,
                score = 0,
            )
            if (idx == 0) prefix += suggestion else contains += suggestion
        }
        return (prefix + contains).take(limit)
    }

    val promptField = rememberPromptFieldController(
        repository = tagAutocompleteRepository,
        suggestionCount = tagSuggestionCount,
        embeddingSuggestionsFor = { embeddingSuggestionsFor(it) },
    )
    val negativePromptField = rememberPromptFieldController(
        repository = tagAutocompleteRepository,
        suggestionCount = tagSuggestionCount,
        embeddingSuggestionsFor = { embeddingSuggestionsFor(it) },
    )
    promptField.autocompleteAvailable = tagAutocompleteAvailable
    negativePromptField.autocompleteAvailable = tagAutocompleteAvailable

    LaunchedEffect(promptField.isFocused, negativePromptField.isFocused) {
        if (!promptField.isFocused && !negativePromptField.isFocused) return@LaunchedEffect
        val names = withContext(Dispatchers.IO) {
            ModelStorage.embeddingsDir(context)
                .takeIf { it.isDirectory }
                ?.listFiles()
                ?.asSequence()
                ?.filter { it.isFile && it.extension.equals("safetensors", ignoreCase = true) }
                ?.map { it.nameWithoutExtension }
                ?.sortedBy { it.lowercase() }
                ?.toList()
                .orEmpty()
        }
        embeddingNames = names
    }

    var showCropScreen by remember { mutableStateOf(false) }
    var imageUriForCrop by remember { mutableStateOf<Uri?>(null) }
    var croppedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    var showInpaintScreen by remember { mutableStateOf(false) }
    var maskBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isInpaintMode by remember { mutableStateOf(false) }
    var savedPathHistory by remember { mutableStateOf<List<PathData>?>(null) }
    var cropRect by remember { mutableStateOf<AndroidRect?>(null) }

    var showDrawScreen by remember { mutableStateOf(false) }
    // Transparent edits are also retained for full-image inpaint exports.
    var drawingOverlayBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var snapshotDrawingOverlayBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // True only when selectedImageUri points to a real source image from the gallery picker.
    // False when img2img was seeded from a result/history bitmap (selectedImageUri is a
    // synthetic tmp.txt path that holds base64, not a decodable image).
    var hasOriginalImageForStitch by remember { mutableStateOf(false) }

    var snapshotIsInpaintMode by remember { mutableStateOf(false) }
    var snapshotSelectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var snapshotCropRect by remember { mutableStateOf<AndroidRect?>(null) }
    var snapshotMaskBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var snapshotHasOriginalImage by remember { mutableStateOf(false) }
    // History-item ids whose bitmaps may be stitched back into the inpaint source:
    // the just-completed inpaint generation plus any upscaled copies derived from
    // it. Compared against currentDisplayedHistoryId so saving only stitches when
    // the bitmap on screen really is one of those (regardless of whether it's the
    // original Bitmap object or a fresh decode from clicking the thumbnail again).
    var stitchableHistoryIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var currentDisplayedHistoryId by remember { mutableStateOf<Long?>(null) }
    // Favorite flag of the image currently on the result page, looked up by id
    // so it stays correct even when the row isn't in a loaded history page.
    val displayedFavorite by remember(currentDisplayedHistoryId) {
        currentDisplayedHistoryId
            ?.let { historyManager.observeFavorite(it) }
            ?: flowOf(null)
    }.collectAsState(initial = null)

    var saveAllJob: Job? by remember { mutableStateOf(null) }
    var batchGenerationJob: Job? by remember { mutableStateOf(null) }

    // Upscaler related states
    var showUpscalerDialog by remember { mutableStateOf(false) }
    var isUpscaling by remember { mutableStateOf(false) }

    // Ultrafix (tiled img2img repair of an upscaled image) states.
    // pendingUltrafix marks the in-flight generation as an ultrafix run so the
    // completion handler can record the right mode.
    var showUltrafixConfirmDialog by remember { mutableStateOf(false) }
    var showUltrafixImportDialog by remember { mutableStateOf(false) }
    var pendingUltrafix by remember { mutableStateOf(false) }
    var isUltrafixPreparing by remember { mutableStateOf(false) }
    // UltraFix runs with its own steps/denoise (defaults 10 / 0.4), persisted
    // globally and kept independent of the main generation params so tweaking
    // them in the UltraFix dialog never touches the prompt-page settings.
    var ultrafixSteps by remember { mutableFloatStateOf(GenerationDefaults.GLOBAL.ultrafixSteps) }
    // Denoise is controlled as a step count (0..min(10, steps)); the backend
    // strength is derived from it at run time.
    var ultrafixDenoiseSteps by remember {
        mutableIntStateOf(GenerationDefaults.GLOBAL.ultrafixDenoiseSteps)
    }
    // On (default): UltraFix runs on neutral quality tags instead of the
    // prompt-page prompt. Off: uses the prompt-page prompt (legacy behavior).
    var ultrafixQualityDenoise by remember {
        mutableStateOf(GenerationDefaults.GLOBAL.ultrafixQualityDenoise)
    }
    var ultrafixSaveJob: Job? by remember { mutableStateOf(null) }
    LaunchedEffect(Unit) {
        ultrafixSteps = generationPreferences.observeUltrafixSteps(modelId).first()
        val maxDenoiseSteps =
            minOf(GenerationDefaults.ULTRAFIX_DENOISE_STEPS_MAX, ultrafixSteps.roundToInt())
        ultrafixDenoiseSteps =
            generationPreferences.observeUltrafixDenoiseSteps(modelId).first().coerceIn(0, maxDenoiseSteps)
        ultrafixQualityDenoise = generationPreferences.observeUltrafixQualityDenoise(modelId).first()
    }
    val upscalerRepository = remember { UpscalerRepository.getInstance(context) }
    val upscalerPreferences =
        remember { context.getSharedPreferences("upscaler_prefs", Context.MODE_PRIVATE) }

    // (effectiveWidth, effectiveHeight) is the size of the visible result.
    // For SDXL with non-1:1 aspect_ratio it equals the centered target_w/target_h
    // inside the 1024x1024 generation canvas; otherwise it equals the canvas itself.
    val effectiveSize = remember(model?.usesFixedCanvas, aspectRatio, currentWidth, currentHeight) {
        computeAspectTargetSize(model?.usesFixedCanvas == true, aspectRatio)
            ?: Pair(currentWidth, currentHeight)
    }
    val effectiveWidth = effectiveSize.first
    val effectiveHeight = effectiveSize.second

    fun clearImg2imgState() {
        selectedImageUri = null
        editReferenceImages.clear()
        File(context.filesDir, "dit_references.json").delete()
        croppedBitmap = null
        drawingOverlayBitmap = null
        maskBitmap = null
        isInpaintMode = false
        cropRect = null
        savedPathHistory = null
        base64EncodeDone = false
        hasOriginalImageForStitch = false
    }

    fun saveAllFields() {
        saveAllJob?.cancel()
        saveAllJob = scope.launch(Dispatchers.IO) {
            delay(1000)
            generationPreferences.saveAllFields(
                modelId = modelId,
                prompt = promptField.text,
                negativePrompt = negativePromptField.text,
                steps = steps,
                cfg = cfg,
                seed = seed,
                width = currentWidth,
                height = currentHeight,
                denoiseStrength = denoiseStrength,
                useOpenCL = useOpenCL,
                batchCounts = batchCounts,
                scheduler = scheduler,
                aspectRatio = aspectRatio,
            )
        }
    }

    fun saveUltrafixParams() {
        ultrafixSaveJob?.cancel()
        ultrafixSaveJob = scope.launch(Dispatchers.IO) {
            delay(500)
            generationPreferences.saveUltrafixParams(
                modelId,
                ultrafixSteps,
                ultrafixDenoiseSteps,
                ultrafixQualityDenoise,
            )
        }
    }

    val onStepsChange = remember {
        { value: Float ->
            // Clamp to centralized bounds so UI and persistence agree
            steps = value.coerceIn(GenerationDefaults.STEPS_MIN, GenerationDefaults.STEPS_MAX)
            saveAllFields()
        }
    }
    val onCfgChange = remember {
        { value: Float ->
            cfg = value
            saveAllFields()
        }
    }
    val onSizeChange = remember {
        { value: Float ->
            val rounded = (value / 64).roundToInt() * 64
            val newSize = rounded.coerceIn(128, 512)
            currentWidth = newSize
            currentHeight = newSize
            saveAllFields()
        }
    }
    // DiT models take the requested size directly, so width and height are two
    // plain controls; aspectRatio is kept in step for the img2img crop and for
    // whatever reads it back from history.
    val onDitWidthChange = remember {
        { value: Float ->
            currentWidth = snapDitSize(value)
            aspectRatio = inferAspectRatioString(currentWidth, currentHeight)
            saveAllFields()
        }
    }
    val onDitHeightChange = remember {
        { value: Float ->
            currentHeight = snapDitSize(value)
            aspectRatio = inferAspectRatioString(currentWidth, currentHeight)
            saveAllFields()
        }
    }
    val onDenoiseStrengthChange =
        remember {
            { value: Float ->
                denoiseStrength = value
                saveAllFields()
            }
        }
    val onSeedChange = remember {
        { value: String ->
            seed = value
            saveAllFields()
        }
    }
    promptField.onTextCommitted = { saveAllFields() }
    negativePromptField.onTextCommitted = { saveAllFields() }

    PromptTokenCountEffect(promptField, backendReady = backendReady, backendHost = backendHost)
    PromptTokenCountEffect(negativePromptField, backendReady = backendReady, backendHost = backendHost)

    val onBatchCountsChange = remember {
        { value: Float ->
            batchCounts = value.roundToInt().coerceIn(1, 10)
            saveAllFields()
        }
    }

    fun processSelectedImage(uri: Uri) {
        imageUriForCrop = uri
        showCropScreen = true
    }

    @Suppress("UnusedParameter") // base64String from cropify callback is re-derived later
    fun handleCropComplete(base64String: String, bitmap: Bitmap, rect: AndroidRect) {
        showCropScreen = false
        val sourceUri = imageUriForCrop
        selectedImageUri = sourceUri
        imageUriForCrop = null
        hasOriginalImageForStitch = true

        // CropImageScreen returns the cropped bitmap via cropify, whose output
        // can carry a sub-pixel offset relative to the cropRect we computed
        // from frameRect / imageRect. That's invisible when the patch is later
        // pasted back as a whole (SD1.5 / SDXL 1:1), but in SDXL aspect-pad
        // mode the patch goes through scale → pad → backend center-crop →
        // scale-back, and the per-step rounding leaks the offset as a
        // few-pixel stitch misalignment.
        //
        // Fix: re-crop directly from the original image using BitmapRegionDecoder
        // so the bitmap content is *strictly* the cropRect region in the
        // original's coordinate space. cropRect is also clamped to the original
        // bounds, and the clamped value is saved so stitch later paints to the
        // exact same pixel range we cropped from.
        scope.launch(Dispatchers.IO) {
            try {
                base64EncodeDone = false
                val aspectTarget =
                    computeAspectTargetSize(model?.usesFixedCanvas == true, aspectRatio)
                val targetW = aspectTarget?.first ?: currentWidth
                val targetH = aspectTarget?.second ?: currentHeight

                var clampedRect = rect
                val freshCropped: Bitmap? = try {
                    sourceUri?.let { uri ->
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            @Suppress("DEPRECATION")
                            val decoder = BitmapRegionDecoder.newInstance(input, false)
                                ?: throw IllegalStateException(
                                    "BitmapRegionDecoder.newInstance returned null",
                                )
                            try {
                                val safeLeft = rect.left.coerceAtLeast(0)
                                val safeTop = rect.top.coerceAtLeast(0)
                                val safeRight = rect.right.coerceAtMost(decoder.width)
                                val safeBottom = rect.bottom.coerceAtMost(decoder.height)
                                if (safeRight > safeLeft && safeBottom > safeTop) {
                                    val region = snapInpaintCropRect(
                                        rect = AndroidRect(safeLeft, safeTop, safeRight, safeBottom),
                                        imageWidth = decoder.width,
                                        imageHeight = decoder.height,
                                        tolerance = (2f * context.resources.displayMetrics.density)
                                            .roundToInt().coerceAtLeast(1),
                                    )
                                    clampedRect = region
                                    decoder.decodeRegion(region, BitmapFactory.Options())
                                } else {
                                    null
                                }
                            } finally {
                                decoder.recycle()
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(
                        "ModelRunScreen",
                        "BitmapRegionDecoder failed, fall back to cropify bitmap: ${e.message}",
                    )
                    null
                }

                val sourceBitmap = freshCropped ?: bitmap

                val scaled = withContext(Dispatchers.Default) {
                    if (sourceBitmap.width != targetW || sourceBitmap.height != targetH) {
                        sourceBitmap.scale(targetW, targetH)
                    } else {
                        sourceBitmap
                    }
                }

                val needsPad =
                    scaled.width != currentWidth || scaled.height != currentHeight
                val payload = if (needsPad) {
                    bitmapToBase64Png(padBitmapToCanvas(scaled, currentWidth, currentHeight))
                } else {
                    bitmapToBase64Png(scaled)
                }

                withContext(Dispatchers.Main) {
                    cropRect = clampedRect
                    croppedBitmap = scaled
                    drawingOverlayBitmap = null
                }

                val tmpFile = File(context.filesDir, "tmp.txt")
                tmpFile.writeText(payload)
                base64EncodeDone = true
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        msgSaveFailed.format(e.message ?: msgUnknownError),
                        Toast.LENGTH_SHORT,
                    ).show()
                    selectedImageUri = null
                    croppedBitmap = null
                    drawingOverlayBitmap = null
                    cropRect = null
                    hasOriginalImageForStitch = false
                }
            }
        }
    }

    fun handleInpaintComplete(maskBase64: String, maskBmp: Bitmap, pathHistory: List<PathData>) {
        showInpaintScreen = false
        isInpaintMode = true
        maskBitmap = maskBmp
        savedPathHistory = pathHistory

        scope.launch(Dispatchers.IO) {
            try {
                // The mask comes back at target size (matching the cropped image fed
                // into InpaintScreen). For SDXL aspect-pad mode we re-encode after
                // padding to currentWidth x currentHeight so it lines up with the
                // padded image upload.
                val needsPad = maskBmp.width != currentWidth || maskBmp.height != currentHeight
                val payload = if (needsPad) {
                    bitmapToBase64Png(padBitmapToCanvas(maskBmp, currentWidth, currentHeight))
                } else {
                    maskBase64
                }
                val maskFile = File(context.filesDir, "mask.txt")
                maskFile.writeText(payload)

                withContext(Dispatchers.Main) {
                    base64EncodeDone = true
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        msgSaveFailed.format(e.message ?: msgUnknownError),
                        Toast.LENGTH_SHORT,
                    ).show()
                    isInpaintMode = false
                    maskBitmap = null
                    savedPathHistory = null
                }
            }
        }
    }

    fun sendBitmapToImg2img(bitmap: Bitmap) {
        scope.launch {
            val ready = try {
                base64EncodeDone = false
                val aspectTarget = computeAspectTargetSize(model?.usesFixedCanvas == true, aspectRatio)
                val targetW = aspectTarget?.first ?: currentWidth
                val targetH = aspectTarget?.second ?: currentHeight

                // 1) Center-crop+scale the source to (targetW, targetH).
                // 2) If aspect padding is in effect, pad up to (currentWidth, currentHeight).
                val resized = withContext(Dispatchers.Default) {
                    val srcRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
                    val dstRatio = targetW.toFloat() / targetH.toFloat()
                    val centerCropped = if (kotlin.math.abs(srcRatio - dstRatio) < 1e-3f) {
                        bitmap
                    } else {
                        val (cropW, cropH) = if (srcRatio > dstRatio) {
                            Pair((bitmap.height * dstRatio).toInt(), bitmap.height)
                        } else {
                            Pair(bitmap.width, (bitmap.width / dstRatio).toInt())
                        }
                        val cx = (bitmap.width - cropW) / 2
                        val cy = (bitmap.height - cropH) / 2
                        Bitmap.createBitmap(bitmap, cx, cy, cropW, cropH)
                    }
                    val scaled =
                        if (centerCropped.width != targetW || centerCropped.height != targetH) {
                            centerCropped.scale(targetW, targetH)
                        } else {
                            centerCropped.copy(Bitmap.Config.ARGB_8888, false)
                        }
                    scaled
                }

                val displayBitmap = resized
                val uploadBitmap =
                    if (resized.width != currentWidth || resized.height != currentHeight) {
                        padBitmapToCanvas(resized, currentWidth, currentHeight)
                    } else {
                        resized
                    }

                val base64String = withContext(Dispatchers.IO) {
                    bitmapToBase64Png(uploadBitmap)
                }

                withContext(Dispatchers.IO) {
                    File(context.filesDir, "tmp.txt").writeText(base64String)
                }

                croppedBitmap = displayBitmap
                drawingOverlayBitmap = null
                cropRect = AndroidRect(0, 0, displayBitmap.width, displayBitmap.height)
                selectedImageUri = Uri.fromFile(File(context.filesDir, "tmp.txt"))
                hasOriginalImageForStitch = false
                base64EncodeDone = true
                true
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    msgImg2imgFailed.format(e.message ?: msgUnknownError),
                    Toast.LENGTH_SHORT,
                ).show()
                base64EncodeDone = false
                selectedImageUri = null
                croppedBitmap = null
                drawingOverlayBitmap = null
                cropRect = null
                hasOriginalImageForStitch = false
                false
            }

            if (ready) {
                try {
                    pagerState.animateScrollToPage(0)
                } catch (_: kotlinx.coroutines.CancellationException) {
                    // Animation interrupted by another scroll — img2img data is already set, ignore
                }
            }
        }
    }

    // Runs tiled img2img ("ultrafix") over the currently displayed (upscaled)
    // bitmap at its full resolution, using the prompt-page parameters. The
    // base image goes through its own file so a pending img2img selection in
    // tmp.txt is left untouched.
    fun startUltrafix() {
        val bmp = currentBitmap ?: return
        val tileSize = maxOf(currentWidth, currentHeight)
        val totalSteps = ultrafixSteps.roundToInt()
        // Derive the strength that makes the backend run exactly the chosen
        // number of denoise steps (clamped to the total).
        val ultrafixDenoiseStrength = ultrafixDenoiseStrength(ultrafixDenoiseSteps, totalSteps)
        // When quality-denoise is on (default), UltraFix runs on neutral quality
        // tags instead of the prompt-page prompt; off uses the box prompt.
        val ultrafixPrompt =
            if (ultrafixQualityDenoise) GenerationDefaults.ULTRAFIX_QUALITY_PROMPT else promptField.text
        isUltrafixPreparing = true
        generationParamsTmp = GenerationParameters(
            steps = totalSteps,
            cfg = cfg,
            seed = 0,
            // Record the prompt UltraFix actually ran with (quality tags when
            // the toggle is on), so history reflects what produced the result.
            prompt = ultrafixPrompt,
            negativePrompt = negativePromptField.text,
            generationTime = "",
            width = bmp.width,
            height = bmp.height,
            runOnCpu = false,
            denoiseStrength = ultrafixDenoiseStrength,
            useOpenCL = false,
            scheduler = scheduler,
        )
        batchGenerationJob = coroutineScope.launch {
            // The progress card lives on the prompt page; bring it into view.
            try {
                pagerState.animateScrollToPage(0)
            } catch (_: kotlinx.coroutines.CancellationException) {
                // Scroll interrupted by another gesture; the run still starts.
            }
        }
    }
}
