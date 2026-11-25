package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.model.HomeSize
import com.example.model.ServicePackage
import com.example.model.TruckSize
import com.example.model.ValuationTier
import com.example.ui.AiUiState
import com.example.ui.BackUpViewModel
import com.example.ui.components.CategoryChip
import com.example.ui.components.QuoteBreakdownCard
import com.example.ui.components.RiskChip
import com.example.ui.components.SectionHeader
import com.example.ui.components.StepperRow
import com.example.ui.components.formatUsd
import com.example.ui.theme.MonoMetricStyle
import com.example.ui.theme.SignalOrange
import java.io.File
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EstimatorScreen(
    viewModel: BackUpViewModel,
    onProceedToBooking: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val inputs by viewModel.estimatorInputs.collectAsStateWithLifecycle()
    val quote by viewModel.liveQuote.collectAsStateWithLifecycle()
    val photoUris by viewModel.selectedPhotoUris.collectAsStateWithLifecycle()
    val videoUri by viewModel.selectedVideoUri.collectAsStateWithLifecycle()
    val aiState by viewModel.aiState.collectAsStateWithLifecycle()

    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var cameraNotice by remember { mutableStateOf<String?>(null) }

    val multiPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 5)
    ) { uris ->
        viewModel.addPhotoUris(uris)
    }

    val videoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) viewModel.setVideoUri(uri)
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = pendingCameraUri
        if (success && uri != null) {
            viewModel.addPhotoUris(listOf(uri))
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val uri = createTempCaptureUri(context)
            pendingCameraUri = uri
            takePictureLauncher.launch(uri)
        } else {
            cameraNotice = "Camera permission denied. Use Pick Photos or Pick Video instead."
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp)
            ) {
                Column {
                    Image(
                        painter = painterResource(id = R.drawable.img_hero_reinforcements),
                        contentDescription = "Back Up reinforcement crew",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        contentScale = ContentScale.Crop
                    )
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Call for Reinforcements!",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Surface(
                                color = SignalOrange,
                                shape = RoundedCornerShape(999.dp)
                            ) {
                                Text(
                                    text = "$0 UPFRONT FLEET",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Text(
                            text = "2-person+ crews • Just-in-time rental trucks • ${quote.depositPercent}% upfront deposit, ${100 - quote.depositPercent}% at job completion.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.88f)
                        )
                    }
                }
            }
        }

        // 1. AI Photo & Video Walkthrough Estimator
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SectionHeader(
                        title = "AI Walkthrough Estimator",
                        subtitle = "Upload room photos or a walkthrough video to auto-size your truck, boxes, risk, and pre-move condition record.",
                        badge = "GEMINI AI"
                    )

                    Image(
                        painter = painterResource(id = R.drawable.img_ai_walkthrough_banner),
                        contentDescription = "AI room inventory scanning preview",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(118.dp)
                            .clip(RoundedCornerShape(14.dp)),
                        contentScale = ContentScale.Crop
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                multiPhotoLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("pick_photos_button")
                        ) {
                            Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Room Photos")
                        }

                        OutlinedButton(
                            onClick = {
                                videoLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                )
                            },
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("pick_video_button")
                        ) {
                            Icon(Icons.Filled.Videocam, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Walkthrough Video")
                        }

                        OutlinedButton(
                            onClick = {
                                cameraNotice = null
                                val hasCam = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.CAMERA
                                ) == PackageManager.PERMISSION_GRANTED
                                if (hasCam) {
                                    val uri = createTempCaptureUri(context)
                                    pendingCameraUri = uri
                                    takePictureLauncher.launch(uri)
                                } else {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            },
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("capture_photo_button")
                        ) {
                            Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Take Photo")
                        }
                    }

                    if (cameraNotice != null) {
                        Text(
                            text = cameraNotice!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    if (photoUris.isNotEmpty() || videoUri != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            photoUris.forEach { uri ->
                                Box(modifier = Modifier.size(74.dp)) {
                                    AsyncImage(
                                        model = uri,
                                        contentDescription = "Selected room photo",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(12.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    IconButton(
                                        onClick = { viewModel.removePhotoUri(uri) },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .size(26.dp)
                                            .background(
                                                MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                                RoundedCornerShape(999.dp)
                                            )
                                    ) {
                                        Icon(
                                            Icons.Filled.Close,
                                            contentDescription = "Remove photo",
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }

                            if (videoUri != null) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            Icons.Filled.Videocam,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Text(
                                            text = "Walkthrough Video Ready",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        IconButton(
                                            onClick = { viewModel.setVideoUri(null) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Filled.Close, contentDescription = "Remove video", modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Button(
                        onClick = { viewModel.runAiWalkthroughAnalysis() },
                        enabled = (photoUris.isNotEmpty() || videoUri != null) && aiState !is AiUiState.Analyzing,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("analyze_walkthrough_button")
                    ) {
                        if (aiState is AiUiState.Analyzing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onSecondary,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(10.dp))
                            Text("Inspecting Rooms & Risk Flags...")
                        } else {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Analyze Walkthrough & Save Condition Record")
                        }
                    }

                    AnimatedVisibility(visible = aiState is AiUiState.Error) {
                        val msg = (aiState as? AiUiState.Error)?.message.orEmpty()
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    AnimatedVisibility(visible = aiState is AiUiState.Ready) {
                        val ready = aiState as? AiUiState.Ready
                        if (ready != null) {
                            val analysis = ready.analysis
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Surface(
                                    color = MaterialTheme.colorScheme.tertiaryContainer,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                Icons.Filled.CheckCircle,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = "Pre-Move Condition Record Ready (${analysis.items.size} items)",
                                                style = MaterialTheme.typography.titleSmall,
                                                color = MaterialTheme.colorScheme.onTertiaryContainer
                                            )
                                        }
                                        Text(
                                            text = analysis.summaryNote,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                        Text(
                                            text = "Rec: ${analysis.recommendedTruckSize.label} • ${analysis.estimatedCrewSize} Crew • ${analysis.estimatedHours}h • ${analysis.recommendedBoxes} Boxes • ${analysis.recommendedWrapRolls} Wrap Rolls • Est. Value ${formatUsd(analysis.estimatedTotalItemValueUsd.toDouble())}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                    }
                                }

                                analysis.items.forEach { item ->
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "${item.name} (${item.room})",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Text(
                                                    text = formatUsd(item.estimatedValueUsd.toDouble()),
                                                    style = MonoMetricStyle,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                CategoryChip(item.category)
                                                RiskChip(item.riskLevel)
                                            }
                                            Text(
                                                text = item.handlingNote,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
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

        // 2. Service Package & Home Scope
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SectionHeader(
                        title = "Service Package & Scope",
                        subtitle = "Choose your reinforcement crew package and home dimensions."
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ServicePackage.entries.forEach { pkg ->
                            val selected = inputs.servicePackage == pkg
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(
                                    width = if (selected) 2.dp else 1.dp,
                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.updateServicePackage(pkg) }
                                    .testTag("package_${pkg.name.lowercase()}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = pkg.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = pkg.subtitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Surface(
                                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = pkg.badge,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Text("Home Size", style = MaterialTheme.typography.titleSmall)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        HomeSize.entries.forEach { size ->
                            FilterChip(
                                selected = inputs.homeSize == size,
                                onClick = { viewModel.updateHomeSize(size) },
                                label = { Text(size.label) }
                            )
                        }
                    }

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Square Footage", style = MaterialTheme.typography.bodyMedium)
                            Text("${inputs.squareFootage} sq ft", style = MonoMetricStyle)
                        }
                        Slider(
                            value = inputs.squareFootage.toFloat(),
                            onValueChange = { viewModel.updateSquareFootage((it / 50f).roundToInt() * 50) },
                            valueRange = 350f..4000f
                        )
                    }

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Estimated Mileage", style = MaterialTheme.typography.bodyMedium)
                            Text("${inputs.mileage} miles", style = MonoMetricStyle)
                        }
                        Slider(
                            value = inputs.mileage.toFloat(),
                            onValueChange = { viewModel.updateMileage(it.roundToInt()) },
                            valueRange = 0f..150f
                        )
                    }

                    StepperRow(
                        label = "Rooms",
                        valueText = "${inputs.roomCount}",
                        onDecrement = { viewModel.updateRoomCount(inputs.roomCount - 1) },
                        onIncrement = { viewModel.updateRoomCount(inputs.roomCount + 1) },
                        canDecrement = inputs.roomCount > 1
                    )
                    StepperRow(
                        label = "Crew Size (2-person base)",
                        valueText = "${inputs.crewSize} crew",
                        onDecrement = { viewModel.updateCrewSize(inputs.crewSize - 1) },
                        onIncrement = { viewModel.updateCrewSize(inputs.crewSize + 1) },
                        canDecrement = inputs.crewSize > 2
                    )
                    StepperRow(
                        label = "Estimated Hours",
                        valueText = "${inputs.estimatedHours} hrs",
                        onDecrement = { viewModel.updateEstimatedHours(inputs.estimatedHours - 0.5) },
                        onIncrement = { viewModel.updateEstimatedHours(inputs.estimatedHours + 0.5) },
                        canDecrement = inputs.estimatedHours > 1.5
                    )

                    Text("Just-in-Time Rental Truck", style = MaterialTheme.typography.titleSmall)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        TruckSize.entries.forEach { truck ->
                            FilterChip(
                                selected = inputs.truckSize == truck,
                                onClick = { viewModel.updateTruckSize(truck) },
                                label = { Text(truck.label) }
                            )
                        }
                    }
                }
            }
        }

        // 3. Access Add-Ons, Supplies & Valuation Tier
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SectionHeader(
                        title = "Add-Ons, Supplies & Valuation",
                        subtitle = "Configure stairs, bulky/high-value items, and protection tier."
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Packing & Cleaning Supplies Kit", style = MaterialTheme.typography.titleSmall)
                            Text("Boxes, tape, wrap & move-out cleaners", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = inputs.suppliesIncluded,
                            onCheckedChange = { viewModel.updateSuppliesIncluded(it) }
                        )
                    }

                    if (inputs.suppliesIncluded) {
                        StepperRow(
                            label = "Boxes & Supply Bundles",
                            valueText = "${inputs.boxCount} boxes",
                            onDecrement = { viewModel.updateBoxCount(inputs.boxCount - 4) },
                            onIncrement = { viewModel.updateBoxCount(inputs.boxCount + 4) },
                            canDecrement = inputs.boxCount >= 4
                        )
                    }

                    StepperRow(
                        label = "Stairs (Flights)",
                        valueText = "${inputs.stairsFlights}",
                        onDecrement = { viewModel.updateStairsFlights(inputs.stairsFlights - 1) },
                        onIncrement = { viewModel.updateStairsFlights(inputs.stairsFlights + 1) },
                        canDecrement = inputs.stairsFlights > 0
                    )
                    StepperRow(
                        label = "Heavy / Bulky Items",
                        valueText = "${inputs.heavyItemCount}",
                        onDecrement = { viewModel.updateHeavyItemCount(inputs.heavyItemCount - 1) },
                        onIncrement = { viewModel.updateHeavyItemCount(inputs.heavyItemCount + 1) },
                        canDecrement = inputs.heavyItemCount > 0
                    )
                    StepperRow(
                        label = "High-Value / Fragile Declarations",
                        valueText = "${inputs.highValueItemCount}",
                        onDecrement = { viewModel.updateHighValueItemCount(inputs.highValueItemCount - 1) },
                        onIncrement = { viewModel.updateHighValueItemCount(inputs.highValueItemCount + 1) },
                        canDecrement = inputs.highValueItemCount > 0
                    )

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Declared Total Item Value", style = MaterialTheme.typography.bodyMedium)
                            Text(formatUsd(inputs.declaredItemValue), style = MonoMetricStyle)
                        }
                        Slider(
                            value = inputs.declaredItemValue.toFloat(),
                            onValueChange = { viewModel.updateDeclaredValue(((it / 500f).roundToInt() * 500).toDouble()) },
                            valueRange = 1000f..50000f
                        )
                    }

                    Text("Valuation / Insurance Tier", style = MaterialTheme.typography.titleSmall)
                    ValuationTier.entries.forEach { tier ->
                        val selected = inputs.valuationTier == tier
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (selected) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                width = if (selected) 2.dp else 1.dp,
                                color = if (selected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.updateValuationTier(tier) }
                                .testTag("valuation_${tier.name.lowercase()}")
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Shield,
                                    contentDescription = null,
                                    tint = if (selected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(tier.title, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        tier.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Live Quote Card & Proceed CTA
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuoteBreakdownCard(
                    quote = quote,
                    crewSize = inputs.crewSize,
                    hours = inputs.estimatedHours,
                    mileage = inputs.mileage
                )

                Button(
                    onClick = onProceedToBooking,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("proceed_to_booking_button")
                ) {
                    Text(
                        text = "Book Now • Pay ${quote.depositPercent}% Deposit (${formatUsd(quote.depositAmount)})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun createTempCaptureUri(context: Context): Uri {
    val dir = File(context.cacheDir, "captures").apply { mkdirs() }
    val file = File(dir, "room_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}
