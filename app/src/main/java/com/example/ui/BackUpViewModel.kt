package com.example.ui

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AiWalkthroughRepository
import com.example.data.BackUpRepository
import com.example.domain.QuoteCalculator
import com.example.model.AiWalkthroughAnalysis
import com.example.model.ConditionRecordItem
import com.example.model.ConditionSource
import com.example.model.EstimatorInputs
import com.example.model.HomeSize
import com.example.model.ItemCategory
import com.example.model.ItemRisk
import com.example.model.JobStatus
import com.example.model.MoveBooking
import com.example.model.PricingConfig
import com.example.model.QuoteBreakdown
import com.example.model.RiskLevel
import com.example.model.ServicePackage
import com.example.model.TruckSize
import com.example.model.ValuationTier
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val TAG = "BackUpVM"
private const val STOP_TIMEOUT_MS = 5000L

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

sealed interface AiUiState {
    data object Idle : AiUiState
    data object Analyzing : AiUiState
    data class Ready(val analysis: AiWalkthroughAnalysis) : AiUiState
    data class Error(val message: String) : AiUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
class BackUpViewModel(
    private val repository: BackUpRepository,
    private val aiRepository: AiWalkthroughRepository,
    private val currentUserId: String,
    private val userDisplayName: String,
    private val userEmail: String?
) : ViewModel() {

    private val _estimatorInputs = MutableStateFlow(EstimatorInputs())
    val estimatorInputs: StateFlow<EstimatorInputs> = _estimatorInputs.asStateFlow()

    private val _selectedPhotoUris = MutableStateFlow<List<Uri>>(emptyList())
    val selectedPhotoUris: StateFlow<List<Uri>> = _selectedPhotoUris.asStateFlow()

    private val _selectedVideoUri = MutableStateFlow<Uri?>(null)
    val selectedVideoUri: StateFlow<Uri?> = _selectedVideoUri.asStateFlow()

    private val _aiState = MutableStateFlow<AiUiState>(AiUiState.Idle)
    val aiState: StateFlow<AiUiState> = _aiState.asStateFlow()

    private val _selectedBookingId = MutableStateFlow<String?>(null)
    val selectedBookingId: StateFlow<String?> = _selectedBookingId.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    private val _isBusyAction = MutableStateFlow(false)
    val isBusyAction: StateFlow<Boolean> = _isBusyAction.asStateFlow()

    val pricingState: StateFlow<UiState<PricingConfig>> = repository.observePricingConfig(currentUserId)
        .map<PricingConfig, UiState<PricingConfig>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing pricing config", rootError)
            emit(UiState.Error(rootError.message ?: "Failed to load pricing rules"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = UiState.Loading
        )

    val bookingsState: StateFlow<UiState<List<MoveBooking>>> = repository.observeBookings(currentUserId)
        .map<List<MoveBooking>, UiState<List<MoveBooking>>> { list ->
            if (_selectedBookingId.value == null && list.isNotEmpty()) {
                _selectedBookingId.value = list.first().id
            }
            UiState.Success(list)
        }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing bookings", rootError)
            emit(UiState.Error(rootError.message ?: "Failed to load bookings"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = UiState.Loading
        )

    val activeConditionItemsState: StateFlow<UiState<List<ConditionRecordItem>>> = _selectedBookingId
        .flatMapLatest { bookingId ->
            if (bookingId.isNullOrBlank()) {
                flowOf(UiState.Success(emptyList()))
            } else {
                repository.observeConditionItems(currentUserId, bookingId)
                    .map<List<ConditionRecordItem>, UiState<List<ConditionRecordItem>>> {
                        UiState.Success(it)
                    }
                    .catch { error ->
                        val firestoreError = generateSequence(error) { it.cause }
                            .filterIsInstance<FirebaseFirestoreException>()
                            .firstOrNull()
                        if (firestoreError == null && error is CancellationException) throw error
                        val rootError = firestoreError ?: error
                        Log.w(TAG, "Error observing condition items", rootError)
                        emit(UiState.Error(rootError.message ?: "Failed to load condition record"))
                    }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = UiState.Loading
        )

    val livePricingConfig: StateFlow<PricingConfig> = pricingState
        .map { state ->
            (state as? UiState.Success)?.data ?: PricingConfig(userId = currentUserId)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = PricingConfig(userId = currentUserId)
        )

    val liveQuote: StateFlow<QuoteBreakdown> = combine(_estimatorInputs, livePricingConfig) { inputs, pricing ->
        QuoteCalculator.calculate(inputs, pricing)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = QuoteCalculator.calculate(EstimatorInputs(), PricingConfig(userId = currentUserId))
    )

    init {
        viewModelScope.launch {
            repository.ensureUserProfile(currentUserId, userDisplayName, userEmail)
        }
    }

    fun clearActionMessage() {
        _actionMessage.value = null
    }

    fun updateServicePackage(pkg: ServicePackage) {
        _estimatorInputs.value = _estimatorInputs.value.copy(servicePackage = pkg)
    }

    fun updateHomeSize(size: HomeSize) {
        val recommendedTruck = when (size) {
            HomeSize.STUDIO -> TruckSize.CARGO_VAN
            HomeSize.`1BR`, HomeSize.`2BR` -> TruckSize.TRUCK_15FT
            HomeSize.`3BR` -> TruckSize.TRUCK_20FT
            HomeSize.`4BR_PLUS` -> TruckSize.TRUCK_26FT
        }
        _estimatorInputs.value = _estimatorInputs.value.copy(
            homeSize = size,
            squareFootage = size.defaultSqFt,
            roomCount = size.defaultRooms,
            estimatedHours = size.baseHours,
            boxCount = size.defaultBoxes,
            truckSize = recommendedTruck
        )
    }

    fun updateSquareFootage(sqFt: Int) {
        _estimatorInputs.value = _estimatorInputs.value.copy(squareFootage = sqFt.coerceIn(250, 8000))
    }

    fun updateRoomCount(rooms: Int) {
        _estimatorInputs.value = _estimatorInputs.value.copy(roomCount = rooms.coerceIn(1, 20))
    }

    fun updateEstimatedHours(hours: Double) {
        _estimatorInputs.value = _estimatorInputs.value.copy(estimatedHours = hours.coerceIn(1.5, 40.0))
    }

    fun updateCrewSize(crew: Int) {
        _estimatorInputs.value = _estimatorInputs.value.copy(crewSize = crew.coerceIn(2, 8))
    }

    fun updateMileage(miles: Int) {
        _estimatorInputs.value = _estimatorInputs.value.copy(mileage = miles.coerceIn(0, 500))
    }

    fun updateTruckSize(truck: TruckSize) {
        _estimatorInputs.value = _estimatorInputs.value.copy(truckSize = truck)
    }

    fun updateStairsFlights(flights: Int) {
        _estimatorInputs.value = _estimatorInputs.value.copy(stairsFlights = flights.coerceIn(0, 20))
    }

    fun updateHeavyItemCount(count: Int) {
        _estimatorInputs.value = _estimatorInputs.value.copy(heavyItemCount = count.coerceIn(0, 30))
    }

    fun updateHighValueItemCount(count: Int) {
        _estimatorInputs.value = _estimatorInputs.value.copy(highValueItemCount = count.coerceIn(0, 30))
    }

    fun updateDeclaredValue(valueUsd: Double) {
        _estimatorInputs.value = _estimatorInputs.value.copy(declaredItemValue = valueUsd.coerceIn(0.0, 500_000.0))
    }

    fun updateSuppliesIncluded(included: Boolean) {
        _estimatorInputs.value = _estimatorInputs.value.copy(suppliesIncluded = included)
    }

    fun updateBoxCount(boxes: Int) {
        _estimatorInputs.value = _estimatorInputs.value.copy(boxCount = boxes.coerceIn(0, 300))
    }

    fun updateValuationTier(tier: ValuationTier) {
        _estimatorInputs.value = _estimatorInputs.value.copy(valuationTier = tier)
    }

    fun addPhotoUris(uris: List<Uri>) {
        if (uris.isEmpty()) return
        val merged = (_selectedPhotoUris.value + uris).distinct().take(5)
        _selectedPhotoUris.value = merged
    }

    fun removePhotoUri(uri: Uri) {
        _selectedPhotoUris.value = _selectedPhotoUris.value.filterNot { it == uri }
    }

    fun setVideoUri(uri: Uri?) {
        _selectedVideoUri.value = uri
    }

    fun clearWalkthroughMedia() {
        _selectedPhotoUris.value = emptyList()
        _selectedVideoUri.value = null
        _aiState.value = AiUiState.Idle
    }

    fun runAiWalkthroughAnalysis() {
        val photos = _selectedPhotoUris.value
        val video = _selectedVideoUri.value
        if (photos.isEmpty() && video == null) {
            _aiState.value = AiUiState.Error("Add at least one room photo or walkthrough video first.")
            return
        }
        viewModelScope.launch {
            _aiState.value = AiUiState.Analyzing
            try {
                val hint = _estimatorInputs.value
                val analysis = aiRepository.analyzeWalkthrough(
                    photoUris = photos,
                    videoUri = video,
                    squareFootageHint = hint.squareFootage,
                    roomCountHint = hint.roomCount
                )
                _aiState.value = AiUiState.Ready(analysis)
                applyAiAnalysisToInputs(analysis)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _aiState.value = AiUiState.Error(e.localizedMessage ?: "AI walkthrough analysis failed.")
            }
        }
    }

    fun applyAiAnalysisToInputs(analysis: AiWalkthroughAnalysis) {
        val heavyCount = analysis.items.count { it.category == ItemCategory.HEAVY }
        val highValueCount = analysis.items.count {
            it.category == ItemCategory.HIGH_VALUE || it.category == ItemCategory.FRAGILE
        }
        _estimatorInputs.value = _estimatorInputs.value.copy(
            truckSize = analysis.recommendedTruckSize,
            crewSize = analysis.estimatedCrewSize.coerceAtLeast(2),
            estimatedHours = analysis.estimatedHours,
            boxCount = analysis.recommendedBoxes,
            suppliesIncluded = true,
            heavyItemCount = heavyCount,
            highValueItemCount = highValueCount,
            declaredItemValue = analysis.estimatedTotalItemValueUsd.toDouble(),
            overallRiskLevel = analysis.overallRiskLevel,
            valuationTier = analysis.recommendedValuationTier,
            aiSummary = analysis.summaryNote
        )
        _actionMessage.value = "AI walkthrough estimate & pre-move condition items applied."
    }

    fun selectBooking(bookingId: String) {
        _selectedBookingId.value = bookingId
    }

    fun confirmBookingWithDeposit(
        customerName: String,
        pickupAddress: String,
        dropoffAddress: String,
        signerName: String,
        onBooked: (String) -> Unit
    ) {
        val inputs = _estimatorInputs.value
        val quote = liveQuote.value
        val aiReady = _aiState.value as? AiUiState.Ready

        viewModelScope.launch {
            _isBusyAction.value = true
            val booking = MoveBooking(
                userId = currentUserId,
                customerName = customerName,
                pickupAddress = pickupAddress,
                dropoffAddress = dropoffAddress,
                servicePackage = inputs.servicePackage.name,
                homeSize = inputs.homeSize.name,
                squareFootage = inputs.squareFootage,
                roomCount = inputs.roomCount,
                estimatedHours = inputs.estimatedHours,
                crewSize = inputs.crewSize,
                mileage = inputs.mileage,
                truckSize = inputs.truckSize.name,
                stairsFlights = inputs.stairsFlights,
                heavyItemCount = inputs.heavyItemCount,
                highValueItemCount = inputs.highValueItemCount,
                declaredItemValue = inputs.declaredItemValue,
                suppliesIncluded = inputs.suppliesIncluded,
                boxCount = inputs.boxCount,
                valuationTier = inputs.valuationTier.name,
                overallRiskLevel = inputs.overallRiskLevel.name,
                totalQuote = quote.totalQuote,
                depositPercent = quote.depositPercent,
                depositAmount = quote.depositAmount,
                finalBalanceAmount = quote.finalBalanceAmount,
                depositPaid = true,
                finalBalancePaid = false,
                contractAccepted = true,
                contractSignerName = signerName,
                status = JobStatus.CONFIRMED.name,
                truckReserved = true,
                suppliesPacked = false,
                cleanKitReady = false,
                preMoveWalkthroughDone = aiReady != null,
                crewNotes = "",
                aiSummary = inputs.aiSummary.ifBlank {
                    "Standard pre-move profile (${inputs.homeSize.label}, ${inputs.truckSize.label})."
                },
                completionSignOffName = ""
            )

            val conditionItems = aiReady?.analysis?.items?.map { draft ->
                ConditionRecordItem(
                    userId = currentUserId,
                    bookingId = "",
                    itemName = draft.name,
                    roomName = draft.room,
                    category = draft.category.name,
                    estimatedValueUsd = draft.estimatedValueUsd,
                    riskLevel = draft.riskLevel.name,
                    handlingNote = draft.handlingNote,
                    source = ConditionSource.AI_WALKTHROUGH.name,
                    crewInspected = false,
                    damageNoted = false,
                    damageDescription = "",
                    thumbnailBase64 = draft.thumbnailBase64
                )
            } ?: emptyList()

            val result = repository.createBookingWithConditions(
                userId = currentUserId,
                booking = booking,
                conditionItems = conditionItems
            )
            _isBusyAction.value = false
            result.onSuccess { newId ->
                _selectedBookingId.value = newId
                _actionMessage.value = "30% deposit authorized! Rental truck & supplies queued."
                onBooked(newId)
            }.onFailure { err ->
                _actionMessage.value = err.localizedMessage ?: "Failed to save booking."
            }
        }
    }

    fun collectFinalBalance(bookingId: String, signOffName: String) {
        viewModelScope.launch {
            _isBusyAction.value = true
            val result = repository.collectFinalBalance(currentUserId, bookingId, signOffName)
            _isBusyAction.value = false
            result.onSuccess {
                _actionMessage.value = "Remaining 70% balance paid & job signed off!"
            }.onFailure { err ->
                _actionMessage.value = err.localizedMessage ?: "Failed to collect final balance."
            }
        }
    }

    fun updateDispatchJob(
        bookingId: String,
        status: JobStatus,
        truckReserved: Boolean,
        suppliesPacked: Boolean,
        cleanKitReady: Boolean,
        preMoveWalkthroughDone: Boolean,
        crewNotes: String,
        completionSignOffName: String
    ) {
        viewModelScope.launch {
            _isBusyAction.value = true
            val result = repository.updateCrewChecklistAndStatus(
                userId = currentUserId,
                bookingId = bookingId,
                status = status,
                truckReserved = truckReserved,
                suppliesPacked = suppliesPacked,
                cleanKitReady = cleanKitReady,
                preMoveWalkthroughDone = preMoveWalkthroughDone,
                crewNotes = crewNotes,
                completionSignOffName = completionSignOffName
            )
            _isBusyAction.value = false
            result.onSuccess {
                _actionMessage.value = "Dispatch status & checklist synced."
            }.onFailure { err ->
                _actionMessage.value = err.localizedMessage ?: "Failed to update dispatch job."
            }
        }
    }

    fun addCrewConditionOrDamageLog(
        bookingId: String,
        itemName: String,
        roomName: String,
        category: ItemCategory,
        damageNoted: Boolean,
        damageDescription: String,
        handlingNote: String,
        photoUri: Uri?
    ) {
        viewModelScope.launch {
            _isBusyAction.value = true
            val thumb = photoUri?.let { aiRepository.decodeThumbnailBase64FromUri(it) }
            val item = ConditionRecordItem(
                userId = currentUserId,
                bookingId = bookingId,
                itemName = itemName,
                roomName = roomName,
                category = category.name,
                estimatedValueUsd = 0,
                riskLevel = if (damageNoted) ItemRisk.HIGH.name else ItemRisk.LOW.name,
                handlingNote = handlingNote.ifBlank { "Crew on-site pre-move inspection" },
                source = if (damageNoted) ConditionSource.DAMAGE_LOG.name else ConditionSource.CREW_PRE_MOVE.name,
                crewInspected = true,
                damageNoted = damageNoted,
                damageDescription = damageDescription,
                thumbnailBase64 = thumb
            )
            val result = repository.addConditionLogItem(currentUserId, bookingId, item)
            _isBusyAction.value = false
            result.onSuccess {
                _actionMessage.value = if (damageNoted) "Damage report logged to condition record." else "Pre-move item logged."
            }.onFailure { err ->
                _actionMessage.value = err.localizedMessage ?: "Failed to add condition item."
            }
        }
    }

    fun updateConditionItemInspection(
        bookingId: String,
        item: ConditionRecordItem,
        crewInspected: Boolean,
        damageNoted: Boolean,
        damageDescription: String
    ) {
        viewModelScope.launch {
            val result = repository.updateConditionInspection(
                userId = currentUserId,
                bookingId = bookingId,
                itemId = item.id,
                crewInspected = crewInspected,
                damageNoted = damageNoted,
                damageDescription = damageDescription,
                handlingNote = item.handlingNote,
                riskLevel = if (damageNoted) ItemRisk.HIGH.name else item.riskLevel
            )
            result.onFailure { err ->
                _actionMessage.value = err.localizedMessage ?: "Failed to update condition item."
            }
        }
    }

    fun savePricingConfig(config: PricingConfig) {
        viewModelScope.launch {
            _isBusyAction.value = true
            val result = repository.savePricingConfig(currentUserId, config.copy(userId = currentUserId))
            _isBusyAction.value = false
            result.onSuccess {
                _actionMessage.value = "Pricing rules saved & live quotes updated."
            }.onFailure { err ->
                _actionMessage.value = err.localizedMessage ?: "Failed to save pricing rules."
            }
        }
    }
}
