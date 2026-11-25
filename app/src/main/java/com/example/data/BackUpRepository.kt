package com.example.data

import android.content.Context
import com.example.R
import com.example.firebase.OperationType
import com.example.firebase.handleFirestoreError
import com.example.model.ConditionRecordItem
import com.example.model.JobStatus
import com.example.model.MoveBooking
import com.example.model.PricingConfig
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

private const val PRICING_DOC_ID = "pricing_config"

class BackUpRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(expectedUid: String? = null): String {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
        if (expectedUid != null) {
            check(uid == expectedUid) { "Signed-in user changed; refusing cross-user write." }
        }
        return uid
    }

    suspend fun ensureUserProfile(userId: String, displayName: String, email: String?): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId(userId)
            val docRef = db.collection("users").document(uid)
            val snap = docRef.get().await()
            val safeName = displayName.ifBlank { "Back Up Operator" }.take(100)
            if (!snap.exists()) {
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to safeName,
                    "email" to email?.take(120),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.set(payload).await()
            } else {
                val updatePayload = mapOf(
                    "displayName" to safeName,
                    "email" to email?.take(120),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.update(updatePayload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    fun observePricingConfig(userId: String): Flow<PricingConfig> = flow {
        val uid = requireUserId(userId)
        val docRef = db.collection("users").document(uid).collection("settings").document(PRICING_DOC_ID)
        val path = docRef.path
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    snapshot.toObject(
                        PricingConfig::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ) ?: PricingConfig(userId = uid)
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(toLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    suspend fun savePricingConfig(userId: String, config: PricingConfig): Result<Unit> {
        val path = "users/$userId/settings/$PRICING_DOC_ID"
        return try {
            val uid = requireUserId(userId)
            val docRef = db.collection("users").document(uid).collection("settings").document(PRICING_DOC_ID)
            val existing = docRef.get().await()

            val coercedHourly = config.hourlyLaborRate.coerceIn(10.0, 500.0)
            val coercedPack = config.packPackageFee.coerceIn(0.0, 5000.0)
            val coercedMove = config.movePackageFee.coerceIn(0.0, 5000.0)
            val coercedClean = config.cleanPackageFee.coerceIn(0.0, 5000.0)
            val coercedFull = config.fullBundleFee.coerceIn(0.0, 10000.0)
            val coercedMileage = config.mileageRate.coerceIn(0.0, 50.0)
            val coercedVan = config.cargoVanFee.coerceIn(0.0, 1000.0)
            val coerced15 = config.truck15FtFee.coerceIn(0.0, 1000.0)
            val coerced20 = config.truck20FtFee.coerceIn(0.0, 2000.0)
            val coerced26 = config.truck26FtFee.coerceIn(0.0, 2000.0)
            val coercedSupplies = config.suppliesBaseFee.coerceIn(0.0, 1000.0)
            val coercedBox = config.perBoxFee.coerceIn(0.0, 100.0)
            val coercedStairs = config.stairsFlightFee.coerceIn(0.0, 500.0)
            val coercedHeavy = config.heavyItemFee.coerceIn(0.0, 1000.0)
            val coercedHighValue = config.highValueItemFee.coerceIn(0.0, 1000.0)
            val coercedDeposit = config.depositPercent.coerceIn(10, 80)
            val coercedFullPct = config.fullValueRatePercent.coerceIn(0.0, 10.0)
            val coercedFullMin = config.fullValueMinFee.coerceIn(0.0, 500.0)

            if (!existing.exists()) {
                val payload = mapOf(
                    "userId" to uid,
                    "hourlyLaborRate" to coercedHourly,
                    "packPackageFee" to coercedPack,
                    "movePackageFee" to coercedMove,
                    "cleanPackageFee" to coercedClean,
                    "fullBundleFee" to coercedFull,
                    "mileageRate" to coercedMileage,
                    "cargoVanFee" to coercedVan,
                    "truck15FtFee" to coerced15,
                    "truck20FtFee" to coerced20,
                    "truck26FtFee" to coerced26,
                    "suppliesBaseFee" to coercedSupplies,
                    "perBoxFee" to coercedBox,
                    "stairsFlightFee" to coercedStairs,
                    "heavyItemFee" to coercedHeavy,
                    "highValueItemFee" to coercedHighValue,
                    "depositPercent" to coercedDeposit,
                    "fullValueRatePercent" to coercedFullPct,
                    "fullValueMinFee" to coercedFullMin,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            } else {
                val updates = mapOf(
                    "hourlyLaborRate" to coercedHourly,
                    "packPackageFee" to coercedPack,
                    "movePackageFee" to coercedMove,
                    "cleanPackageFee" to coercedClean,
                    "fullBundleFee" to coercedFull,
                    "mileageRate" to coercedMileage,
                    "cargoVanFee" to coercedVan,
                    "truck15FtFee" to coerced15,
                    "truck20FtFee" to coerced20,
                    "truck26FtFee" to coerced26,
                    "suppliesBaseFee" to coercedSupplies,
                    "perBoxFee" to coercedBox,
                    "stairsFlightFee" to coercedStairs,
                    "heavyItemFee" to coercedHeavy,
                    "highValueItemFee" to coercedHighValue,
                    "depositPercent" to coercedDeposit,
                    "fullValueRatePercent" to coercedFullPct,
                    "fullValueMinFee" to coercedFullMin,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updates).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    fun observeBookings(userId: String): Flow<List<MoveBooking>> = flow {
        val uid = requireUserId(userId)
        val colRef = db.collection("users").document(uid).collection("bookings")
        val path = colRef.path
        emitAll(
            colRef.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        MoveBooking::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(toLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeConditionItems(userId: String, bookingId: String): Flow<List<ConditionRecordItem>> = flow {
        val uid = requireUserId(userId)
        require(bookingId.isNotBlank()) { "Booking ID cannot be blank" }
        val colRef = db.collection("users").document(uid)
            .collection("bookings").document(bookingId)
            .collection("condition_items")
        val path = colRef.path
        emitAll(
            colRef.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ConditionRecordItem::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(toLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun createBookingWithConditions(
        userId: String,
        booking: MoveBooking,
        conditionItems: List<ConditionRecordItem>
    ): Result<String> {
        var docPath = "users/$userId/bookings"
        return try {
            val uid = requireUserId(userId)
            val bookingRef = db.collection("users").document(uid).collection("bookings").document()
            docPath = bookingRef.path

            val safeCustomer = booking.customerName.trim().ifBlank { "Customer" }.take(100)
            val safePickup = booking.pickupAddress.trim().take(200)
            val safeDropoff = booking.dropoffAddress.trim().take(200)
            val safeSigner = booking.contractSignerName.trim().take(100)
            require(safePickup.isNotBlank()) { "Pickup address is required" }
            require(safeDropoff.isNotBlank()) { "Dropoff address is required" }
            require(booking.contractAccepted && safeSigner.isNotBlank()) {
                "Digital contract acceptance and signature are required"
            }

            val bookingPayload = mapOf(
                "userId" to uid,
                "customerName" to safeCustomer,
                "pickupAddress" to safePickup,
                "dropoffAddress" to safeDropoff,
                "servicePackage" to booking.servicePackage,
                "homeSize" to booking.homeSize,
                "squareFootage" to booking.squareFootage.coerceIn(100, 20000),
                "roomCount" to booking.roomCount.coerceIn(1, 30),
                "estimatedHours" to booking.estimatedHours.coerceIn(1.0, 100.0),
                "crewSize" to booking.crewSize.coerceIn(2, 10),
                "mileage" to booking.mileage.coerceIn(0, 3000),
                "truckSize" to booking.truckSize,
                "stairsFlights" to booking.stairsFlights.coerceIn(0, 30),
                "heavyItemCount" to booking.heavyItemCount.coerceIn(0, 50),
                "highValueItemCount" to booking.highValueItemCount.coerceIn(0, 50),
                "declaredItemValue" to booking.declaredItemValue.coerceIn(0.0, 1_000_000.0),
                "suppliesIncluded" to booking.suppliesIncluded,
                "boxCount" to booking.boxCount.coerceIn(0, 500),
                "valuationTier" to booking.valuationTier,
                "overallRiskLevel" to booking.overallRiskLevel,
                "totalQuote" to booking.totalQuote.coerceIn(0.0, 500_000.0),
                "depositPercent" to booking.depositPercent.coerceIn(10, 80),
                "depositAmount" to booking.depositAmount.coerceIn(0.0, 500_000.0),
                "finalBalanceAmount" to booking.finalBalanceAmount.coerceIn(0.0, 500_000.0),
                "depositPaid" to true,
                "finalBalancePaid" to false,
                "contractAccepted" to true,
                "contractSignerName" to safeSigner,
                "status" to JobStatus.CONFIRMED.name,
                "truckReserved" to true,
                "suppliesPacked" to false,
                "cleanKitReady" to false,
                "preMoveWalkthroughDone" to conditionItems.isNotEmpty(),
                "crewNotes" to booking.crewNotes.take(1000),
                "aiSummary" to booking.aiSummary.take(1500),
                "completionSignOffName" to "",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            bookingRef.set(bookingPayload).await()

            for (item in conditionItems.take(25)) {
                val itemRef = bookingRef.collection("condition_items").document()
                val itemPayload = mapOf(
                    "userId" to uid,
                    "bookingId" to bookingRef.id,
                    "itemName" to item.itemName.trim().ifBlank { "Item" }.take(120),
                    "roomName" to item.roomName.trim().ifBlank { "Room" }.take(80),
                    "category" to item.category,
                    "estimatedValueUsd" to item.estimatedValueUsd.coerceIn(0, 1_000_000),
                    "riskLevel" to item.riskLevel,
                    "handlingNote" to item.handlingNote.take(500),
                    "source" to item.source,
                    "crewInspected" to item.crewInspected,
                    "damageNoted" to item.damageNoted,
                    "damageDescription" to item.damageDescription.take(500),
                    "thumbnailBase64" to item.thumbnailBase64?.takeIf { it.isNotBlank() && it.length <= 120_000 },
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                itemRef.set(itemPayload).await()
            }

            Result.success(bookingRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateCrewChecklistAndStatus(
        userId: String,
        bookingId: String,
        status: JobStatus,
        truckReserved: Boolean,
        suppliesPacked: Boolean,
        cleanKitReady: Boolean,
        preMoveWalkthroughDone: Boolean,
        crewNotes: String,
        completionSignOffName: String
    ): Result<Unit> {
        val path = "users/$userId/bookings/$bookingId"
        return try {
            val uid = requireUserId(userId)
            require(bookingId.isNotBlank()) { "Booking ID cannot be blank" }
            val safeStatus = if (status == JobStatus.COMPLETED) JobStatus.AWAITING_FINAL_PAYMENT else status
            val docRef = db.collection("users").document(uid).collection("bookings").document(bookingId)
            docRef.update(
                mapOf(
                    "status" to safeStatus.name,
                    "truckReserved" to truckReserved,
                    "suppliesPacked" to suppliesPacked,
                    "cleanKitReady" to cleanKitReady,
                    "preMoveWalkthroughDone" to preMoveWalkthroughDone,
                    "crewNotes" to crewNotes.take(1000),
                    "completionSignOffName" to completionSignOffName.take(100),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun collectFinalBalance(
        userId: String,
        bookingId: String,
        signOffName: String
    ): Result<Unit> {
        val path = "users/$userId/bookings/$bookingId"
        return try {
            val uid = requireUserId(userId)
            require(bookingId.isNotBlank()) { "Booking ID cannot be blank" }
            val docRef = db.collection("users").document(uid).collection("bookings").document(bookingId)
            docRef.update(
                mapOf(
                    "finalBalancePaid" to true,
                    "status" to JobStatus.COMPLETED.name,
                    "completionSignOffName" to signOffName.trim().ifBlank { "Customer Approved" }.take(100),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun addConditionLogItem(
        userId: String,
        bookingId: String,
        item: ConditionRecordItem
    ): Result<Unit> {
        var path = "users/$userId/bookings/$bookingId/condition_items"
        return try {
            val uid = requireUserId(userId)
            require(bookingId.isNotBlank()) { "Booking ID cannot be blank" }
            val itemRef = db.collection("users").document(uid)
                .collection("bookings").document(bookingId)
                .collection("condition_items").document()
            path = itemRef.path
            val payload = mapOf(
                "userId" to uid,
                "bookingId" to bookingId,
                "itemName" to item.itemName.trim().ifBlank { "Logged Item" }.take(120),
                "roomName" to item.roomName.trim().ifBlank { "On-Site" }.take(80),
                "category" to item.category,
                "estimatedValueUsd" to item.estimatedValueUsd.coerceIn(0, 1_000_000),
                "riskLevel" to item.riskLevel,
                "handlingNote" to item.handlingNote.take(500),
                "source" to item.source,
                "crewInspected" to item.crewInspected,
                "damageNoted" to item.damageNoted,
                "damageDescription" to item.damageDescription.take(500),
                "thumbnailBase64" to item.thumbnailBase64?.takeIf { it.isNotBlank() && it.length <= 120_000 },
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            itemRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateConditionInspection(
        userId: String,
        bookingId: String,
        itemId: String,
        crewInspected: Boolean,
        damageNoted: Boolean,
        damageDescription: String,
        handlingNote: String,
        riskLevel: String
    ): Result<Unit> {
        val path = "users/$userId/bookings/$bookingId/condition_items/$itemId"
        return try {
            val uid = requireUserId(userId)
            require(bookingId.isNotBlank() && itemId.isNotBlank()) { "IDs must not be blank" }
            val itemRef = db.collection("users").document(uid)
                .collection("bookings").document(bookingId)
                .collection("condition_items").document(itemId)
            itemRef.update(
                mapOf(
                    "crewInspected" to crewInspected,
                    "damageNoted" to damageNoted,
                    "damageDescription" to damageDescription.take(500),
                    "handlingNote" to handlingNote.take(500),
                    "riskLevel" to riskLevel,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }
}
