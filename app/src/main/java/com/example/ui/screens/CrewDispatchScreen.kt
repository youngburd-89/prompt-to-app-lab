package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.model.ConditionRecordItem
import com.example.model.ConditionSource
import com.example.model.ItemCategory
import com.example.model.ItemRisk
import com.example.model.JobStatus
import com.example.model.MoveBooking
import com.example.model.ServicePackage
import com.example.model.TruckSize
import com.example.ui.BackUpViewModel
import com.example.ui.UiState
import com.example.ui.components.Base64Thumbnail
import com.example.ui.components.CategoryChip
import com.example.ui.components.JobStatusBadge
import com.example.ui.components.RiskChip
import com.example.ui.components.SectionHeader
import com.example.ui.components.formatUsd

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CrewDispatchScreen(
    viewModel: BackUpViewModel,
    onCreateBookingPrompt: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bookingsState by viewModel.bookingsState.collectAsStateWithLifecycle()
    val selectedBookingId by viewModel.selectedBookingId.collectAsStateWithLifecycle()
    val conditionState by viewModel.activeConditionItemsState.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusyAction.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp)
            ) {
                SectionHeader(
                    title = "Crew & Job Dispatch",
                    subtitle = "Manage rental truck/supply checklists, pre-move condition logs, and final sign-off.",
                    badge = "2-PERSON CREW"
                )
            }
        }

        when (val state = bookingsState) {
            UiState.Loading -> {
                item { CircularProgressIndicator() }
            }

            is UiState.Error -> {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 720.dp)
                    ) {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }

            is UiState.Success -> {
                val bookings = state.data
                if (bookings.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 720.dp)
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Filled.LocalShipping,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(40.dp)
                                )
                                Text("No Dispatched Jobs Yet", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = "Book a quote with a 30% deposit to dispatch a 2-person crew and reserve your just-in-time rental truck.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Button(
                                    onClick = onCreateBookingPrompt,
                                    modifier = Modifier.height(48.dp)
                                ) {
                                    Text("Build Quote & Book")
                                }
                            }
                        }
                    }
                } else {
                    val activeBooking = bookings.firstOrNull { it.id == selectedBookingId } ?: bookings.first()

                    if (bookings.size > 1) {
                        item {
                            FlowRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .widthIn(max = 720.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                bookings.forEachIndexed { idx, job ->
                                    FilterChip(
                                        selected = job.id == activeBooking.id,
                                        onClick = { viewModel.selectBooking(job.id) },
                                        label = {
                                            Text("Job #${bookings.size - idx}: ${job.customerName.take(14)}")
                                        }
                                    )
                                }
                            }
                        }
                    }

                    item {
                        DispatchJobDetailCard(
                            booking = activeBooking,
                            isBusy = isBusy,
                            onSaveDispatch = { status, truck, supplies, cleanKit, walkthrough, notes, signOff ->
                                viewModel.updateDispatchJob(
                                    bookingId = activeBooking.id,
                                    status = status,
                                    truckReserved = truck,
                                    suppliesPacked = supplies,
                                    cleanKitReady = cleanKit,
                                    preMoveWalkthroughDone = walkthrough,
                                    crewNotes = notes,
                                    completionSignOffName = signOff
                                )
                            }
                        )
                    }

                    item {
                        NewConditionLogCard(
                            bookingId = activeBooking.id,
                            isCompleted = activeBooking.status == JobStatus.COMPLETED.name,
                            isBusy = isBusy,
                            onAddLog = { itemName, room, category, damageNoted, damageDesc, note, photoUri ->
                                viewModel.addCrewConditionOrDamageLog(
                                    bookingId = activeBooking.id,
                                    itemName = itemName,
                                    roomName = room,
                                    category = category,
                                    damageNoted = damageNoted,
                                    damageDescription = damageDesc,
                                    handlingNote = note,
                                    photoUri = photoUri
                                )
                            }
                        )
                    }

                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 720.dp)
                        ) {
                            SectionHeader(
                                title = "Pre-Move Condition & Damage Record",
                                subtitle = "AI-detected items and crew inspection entries for this job."
                            )
                        }
                    }

                    when (val cond = conditionState) {
                        UiState.Loading -> item { CircularProgressIndicator() }
                        is UiState.Error -> item {
                            Text(cond.message, color = MaterialTheme.colorScheme.error)
                        }
                        is UiState.Success -> {
                            if (cond.data.isEmpty()) {
                                item {
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .widthIn(max = 720.dp)
                                    ) {
                                        Text(
                                            text = "No condition items logged yet. Use the form above to record pre-move items or damage photos.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(16.dp)
                                        )
                                    }
                                }
                            } else {
                                items(cond.data, key = { it.id }) { item ->
                                    ConditionRecordRowCard(
                                        item = item,
                                        isCompleted = activeBooking.status == JobStatus.COMPLETED.name,
                                        onToggleInspected = { checked ->
                                            viewModel.updateConditionItemInspection(
                                                bookingId = activeBooking.id,
                                                item = item,
                                                crewInspected = checked,
                                                damageNoted = item.damageNoted,
                                                damageDescription = item.damageDescription
                                            )
                                        },
                                        onFlagDamage = { desc ->
                                            viewModel.updateConditionItemInspection(
                                                bookingId = activeBooking.id,
                                                item = item,
                                                crewInspected = true,
                                                damageNoted = true,
                                                damageDescription = desc
                                            )
                                        }
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DispatchJobDetailCard(
    booking: MoveBooking,
    isBusy: Boolean,
    onSaveDispatch: (JobStatus, Boolean, Boolean, Boolean, Boolean, String, String) -> Unit
) {
    val currentStatus = JobStatus.fromWire(booking.status)
    val isLocked = currentStatus == JobStatus.COMPLETED
    val pkg = ServicePackage.fromWire(booking.servicePackage)
    val truck = TruckSize.fromWire(booking.truckSize)

    var selectedStatus by remember(booking.id, booking.status) { mutableStateOf(currentStatus) }
    var truckReserved by remember(booking.id, booking.truckReserved) { mutableStateOf(booking.truckReserved) }
    var suppliesPacked by remember(booking.id, booking.suppliesPacked) { mutableStateOf(booking.suppliesPacked) }
    var cleanKitReady by remember(booking.id, booking.cleanKitReady) { mutableStateOf(booking.cleanKitReady) }
    var walkthroughDone by remember(booking.id, booking.preMoveWalkthroughDone) { mutableStateOf(booking.preMoveWalkthroughDone) }
    var crewNotes by remember(booking.id, booking.crewNotes) { mutableStateOf(booking.crewNotes) }
    var signOffName by remember(booking.id, booking.completionSignOffName) { mutableStateOf(booking.completionSignOffName) }

    LaunchedEffect(booking.status) {
        selectedStatus = JobStatus.fromWire(booking.status)
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 720.dp)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("${booking.customerName} • ${pkg.title}", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "${booking.pickupAddress} → ${booking.dropoffAddress}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                JobStatusBadge(currentStatus)
            }

            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Dispatch Brief: ${truck.label} • ${booking.crewSize} Crew • ${booking.boxCount} Boxes • ${booking.stairsFlights} Stairs",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    if (booking.aiSummary.isNotBlank()) {
                        Text(
                            text = booking.aiSummary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Text("Just-in-Time Truck & Supply Checklist", style = MaterialTheme.typography.titleSmall)

            ChecklistRow("Rental Truck Reserved & Inspected (${truck.label})", truckReserved, !isLocked) {
                truckReserved = it
            }
            ChecklistRow("Packing Boxes, Tape & Wrap Loaded (${booking.boxCount} boxes)", suppliesPacked, !isLocked) {
                suppliesPacked = it
            }
            ChecklistRow("Move-Out Clean-Up Kit Ready", cleanKitReady, !isLocked) {
                cleanKitReady = it
            }
            ChecklistRow("Pre-Move Condition Walkthrough Verified", walkthroughDone, !isLocked) {
                walkthroughDone = it
            }

            HorizontalDivider()

            Text("Dispatch Status Progression", style = MaterialTheme.typography.titleSmall)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                JobStatus.entries.filter { it != JobStatus.COMPLETED }.forEach { step ->
                    FilterChip(
                        selected = selectedStatus == step,
                        enabled = !isLocked,
                        onClick = { selectedStatus = step },
                        label = { Text(step.label) }
                    )
                }
            }

            OutlinedTextField(
                value = crewNotes,
                onValueChange = { crewNotes = it },
                enabled = !isLocked,
                label = { Text("Crew Field Notes") },
                placeholder = { Text("Parking access, elevator pads, completion notes...") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = signOffName,
                onValueChange = { signOffName = it },
                enabled = !isLocked,
                label = { Text("Final Walkthrough Sign-Off Name") },
                placeholder = { Text("Required when marking ready for 70% final balance") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (!isLocked) {
                Button(
                    onClick = {
                        onSaveDispatch(
                            selectedStatus,
                            truckReserved,
                            suppliesPacked,
                            cleanKitReady,
                            walkthroughDone,
                            crewNotes,
                            signOffName
                        )
                    },
                    enabled = !isBusy,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("save_dispatch_button")
                ) {
                    Text("Update Dispatch Checklist & Status", fontWeight = FontWeight.Bold)
                }
            } else {
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                        Text(
                            text = "Job completed & signed off by ${booking.completionSignOffName.ifBlank { booking.customerName }}.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChecklistRow(
    label: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NewConditionLogCard(
    bookingId: String,
    isCompleted: Boolean,
    isBusy: Boolean,
    onAddLog: (String, String, ItemCategory, Boolean, String, String, Uri?) -> Unit
) {
    var itemName by remember(bookingId) { mutableStateOf("") }
    var roomName by remember(bookingId) { mutableStateOf("") }
    var category by remember(bookingId) { mutableStateOf(ItemCategory.FRAGILE) }
    var damageNoted by remember(bookingId) { mutableStateOf(false) }
    var damageDesc by remember(bookingId) { mutableStateOf("") }
    var handlingNote by remember(bookingId) { mutableStateOf("") }
    var photoUri by remember(bookingId) { mutableStateOf<Uri?>(null) }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) photoUri = uri
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 720.dp)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Log Pre-Move Item or Damage Photo", style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = itemName,
                onValueChange = { itemName = it },
                enabled = !isCompleted,
                label = { Text("Item Name") },
                placeholder = { Text("e.g., Oak Dresser, Wall Mirror") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = roomName,
                onValueChange = { roomName = it },
                enabled = !isCompleted,
                label = { Text("Room / Location") },
                placeholder = { Text("e.g., Primary Bedroom") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ItemCategory.entries.forEach { cat ->
                    FilterChip(
                        selected = category == cat,
                        enabled = !isCompleted,
                        onClick = { category = cat },
                        label = { Text(cat.label) }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Pre-existing or transit damage noted?", style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = damageNoted,
                    enabled = !isCompleted,
                    onCheckedChange = { damageNoted = it }
                )
            }

            if (damageNoted) {
                OutlinedTextField(
                    value = damageDesc,
                    onValueChange = { damageDesc = it },
                    enabled = !isCompleted,
                    label = { Text("Damage Description") },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                OutlinedTextField(
                    value = handlingNote,
                    onValueChange = { handlingNote = it },
                    enabled = !isCompleted,
                    label = { Text("Handling / Condition Note") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    enabled = !isCompleted,
                    modifier = Modifier.height(48.dp)
                ) {
                    Icon(Icons.Filled.AddAPhoto, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (photoUri == null) "Attach Photo" else "Change Photo")
                }
                if (photoUri != null) {
                    AsyncImage(
                        model = photoUri,
                        contentDescription = "Condition item photo",
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            Button(
                onClick = {
                    if (itemName.isNotBlank()) {
                        onAddLog(
                            itemName,
                            roomName.ifBlank { "On-Site" },
                            category,
                            damageNoted,
                            damageDesc,
                            handlingNote,
                            photoUri
                        )
                        itemName = ""
                        roomName = ""
                        damageDesc = ""
                        handlingNote = ""
                        photoUri = null
                    }
                },
                enabled = !isCompleted && !isBusy && itemName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("add_condition_log_button")
            ) {
                Text("Save to Pre-Move Condition Record")
            }
        }
    }
}

@Composable
private fun ConditionRecordRowCard(
    item: ConditionRecordItem,
    isCompleted: Boolean,
    onToggleInspected: (Boolean) -> Unit,
    onFlagDamage: (String) -> Unit
) {
    val category = ItemCategory.fromWire(item.category)
    val risk = ItemRisk.fromWire(item.riskLevel)
    val source = ConditionSource.fromWire(item.source)
    var showDamageInput by remember { mutableStateOf(false) }
    var damageInput by remember(item.id) { mutableStateOf(item.damageDescription) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 720.dp)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!item.thumbnailBase64.isNullOrBlank()) {
                    Base64Thumbnail(
                        base64 = item.thumbnailBase64,
                        contentDescription = item.itemName,
                        modifier = Modifier.size(64.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("${item.itemName} • ${item.roomName}", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        CategoryChip(category)
                        RiskChip(risk)
                    }
                    Text(
                        text = "${source.label}${if (item.estimatedValueUsd > 0) " • Est. ${formatUsd(item.estimatedValueUsd.toDouble())}" else ""}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (item.handlingNote.isNotBlank()) {
                Text(
                    text = item.handlingNote,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (item.damageNoted) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Filled.ReportProblem,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Damage Logged: ${item.damageDescription.ifBlank { "Flagged during crew inspection" }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            if (!isCompleted) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = item.crewInspected,
                            onCheckedChange = onToggleInspected
                        )
                        Text("Crew Inspected", style = MaterialTheme.typography.bodySmall)
                    }
                    OutlinedButton(
                        onClick = { showDamageInput = !showDamageInput },
                        modifier = Modifier.height(42.dp)
                    ) {
                        Text(if (showDamageInput) "Cancel" else "Flag Damage")
                    }
                }

                if (showDamageInput) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = damageInput,
                            onValueChange = { damageInput = it },
                            label = { Text("Describe scuff/damage") },
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                onFlagDamage(damageInput.ifBlank { "Pre-existing wear documented" })
                                showDamageInput = false
                            },
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }
}
