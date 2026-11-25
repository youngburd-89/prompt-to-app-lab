package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class ServicePackage(
    val title: String,
    val subtitle: String,
    val badge: String
) {
    PACK("Pack for You", "2-person crew + boxes & protective wrap", "PACKING"),
    MOVE("Move for You", "Just-in-time truck + loading, transit & unload", "MOVING"),
    CLEAN("Moving Out Clean-Up", "Deep move-out kitchen, bath & floor turnover", "CLEANING"),
    FULL_BUNDLE("Full Reinforcement", "Pack + Move + Move-Out Clean-Up bundle", "ALL-IN-1");

    companion object {
        fun fromWire(raw: String?): ServicePackage =
            entries.firstOrNull { it.name == raw } ?: FULL_BUNDLE
    }
}

enum class HomeSize(
    val label: String,
    val defaultSqFt: Int,
    val defaultRooms: Int,
    val baseHours: Double,
    val defaultBoxes: Int
) {
    STUDIO("Studio", 500, 2, 2.5, 12),
    `1BR`("1 Bed", 750, 3, 3.5, 20),
    `2BR`("2 Bed", 1100, 5, 5.0, 32),
    `3BR`("3 Bed", 1650, 7, 7.0, 48),
    `4BR_PLUS`("4+ Bed", 2400, 9, 9.5, 70);

    companion object {
        fun fromWire(raw: String?): HomeSize =
            entries.firstOrNull { it.name == raw } ?: `2BR`
    }
}

enum class TruckSize(
    val label: String,
    val capacityHint: String
) {
    CARGO_VAN("Cargo Van (10ft)", "Studio / partial load"),
    TRUCK_15FT("15ft Box Truck", "1-2 Bedroom home"),
    TRUCK_20FT("20ft Box Truck", "2-3 Bedroom home"),
    TRUCK_26FT("26ft Box Truck", "4+ Bedroom full load");

    companion object {
        fun fromWire(raw: String?): TruckSize =
            entries.firstOrNull { it.name == raw } ?: TRUCK_15FT
    }
}

enum class ValuationTier(
    val title: String,
    val subtitle: String
) {
    RELEASED_VALUE(
        "Released Value ($0.60/lb)",
        "Standard statutory carrier coverage included at $0 extra"
    ),
    FULL_VALUE(
        "Full Value Protection",
        "Repair or replacement coverage for declared & high-value items"
    );

    companion object {
        fun fromWire(raw: String?): ValuationTier =
            entries.firstOrNull { it.name == raw } ?: RELEASED_VALUE
    }
}

enum class RiskLevel(val label: String) {
    LOW("Low Risk"),
    MODERATE("Moderate Risk"),
    ELEVATED("Elevated Risk");

    companion object {
        fun fromWire(raw: String?): RiskLevel =
            entries.firstOrNull { it.name == raw } ?: LOW
    }
}

enum class JobStatus(val label: String) {
    CONFIRMED("Confirmed • Truck Queued"),
    EN_ROUTE("Crew En Route"),
    ON_SITE_PRE_MOVE("On-Site Pre-Move Audit"),
    IN_PROGRESS("Move & Service Active"),
    AWAITING_FINAL_PAYMENT("Ready for 70% Final Balance"),
    COMPLETED("Completed & Signed Off");

    companion object {
        fun fromWire(raw: String?): JobStatus =
            entries.firstOrNull { it.name == raw } ?: CONFIRMED
    }
}

enum class ItemCategory(val label: String) {
    STANDARD("Standard"),
    FRAGILE("Fragile"),
    HEAVY("Heavy / Bulky"),
    HIGH_VALUE("High-Value");

    companion object {
        fun fromWire(raw: String?): ItemCategory =
            entries.firstOrNull { it.name == raw } ?: STANDARD
    }
}

enum class ItemRisk(val label: String) {
    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High");

    companion object {
        fun fromWire(raw: String?): ItemRisk =
            entries.firstOrNull { it.name == raw } ?: LOW
    }
}

enum class ConditionSource(val label: String) {
    AI_WALKTHROUGH("AI Walkthrough"),
    CREW_PRE_MOVE("Crew Pre-Move"),
    DAMAGE_LOG("Damage Log");

    companion object {
        fun fromWire(raw: String?): ConditionSource =
            entries.firstOrNull { it.name == raw } ?: AI_WALKTHROUGH
    }
}

@Keep
@IgnoreExtraProperties
data class PricingConfig(
    @DocumentId val id: String = "",
    val userId: String = "",
    val hourlyLaborRate: Double = 55.0,
    val packPackageFee: Double = 180.0,
    val movePackageFee: Double = 220.0,
    val cleanPackageFee: Double = 160.0,
    val fullBundleFee: Double = 460.0,
    val mileageRate: Double = 2.25,
    val cargoVanFee: Double = 49.0,
    val truck15FtFee: Double = 89.0,
    val truck20FtFee: Double = 129.0,
    val truck26FtFee: Double = 169.0,
    val suppliesBaseFee: Double = 45.0,
    val perBoxFee: Double = 4.5,
    val stairsFlightFee: Double = 35.0,
    val heavyItemFee: Double = 65.0,
    val highValueItemFee: Double = 45.0,
    val depositPercent: Int = 30,
    val fullValueRatePercent: Double = 1.5,
    val fullValueMinFee: Double = 49.0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class MoveBooking(
    @DocumentId val id: String = "",
    val userId: String = "",
    val customerName: String = "",
    val pickupAddress: String = "",
    val dropoffAddress: String = "",
    val servicePackage: String = ServicePackage.FULL_BUNDLE.name,
    val homeSize: String = HomeSize.`2BR`.name,
    val squareFootage: Int = 1100,
    val roomCount: Int = 5,
    val estimatedHours: Double = 5.0,
    val crewSize: Int = 2,
    val mileage: Int = 15,
    val truckSize: String = TruckSize.TRUCK_15FT.name,
    val stairsFlights: Int = 0,
    val heavyItemCount: Int = 0,
    val highValueItemCount: Int = 0,
    val declaredItemValue: Double = 5000.0,
    val suppliesIncluded: Boolean = true,
    val boxCount: Int = 30,
    val valuationTier: String = ValuationTier.RELEASED_VALUE.name,
    val overallRiskLevel: String = RiskLevel.LOW.name,
    val totalQuote: Double = 0.0,
    val depositPercent: Int = 30,
    val depositAmount: Double = 0.0,
    val finalBalanceAmount: Double = 0.0,
    val depositPaid: Boolean = false,
    val finalBalancePaid: Boolean = false,
    val contractAccepted: Boolean = false,
    val contractSignerName: String = "",
    val status: String = JobStatus.CONFIRMED.name,
    val truckReserved: Boolean = false,
    val suppliesPacked: Boolean = false,
    val cleanKitReady: Boolean = false,
    val preMoveWalkthroughDone: Boolean = false,
    val crewNotes: String = "",
    val aiSummary: String = "",
    val completionSignOffName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ConditionRecordItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val bookingId: String = "",
    val itemName: String = "",
    val roomName: String = "",
    val category: String = ItemCategory.STANDARD.name,
    val estimatedValueUsd: Int = 0,
    val riskLevel: String = ItemRisk.LOW.name,
    val handlingNote: String = "",
    val source: String = ConditionSource.AI_WALKTHROUGH.name,
    val crewInspected: Boolean = false,
    val damageNoted: Boolean = false,
    val damageDescription: String = "",
    val thumbnailBase64: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class EstimatorInputs(
    val servicePackage: ServicePackage = ServicePackage.FULL_BUNDLE,
    val homeSize: HomeSize = HomeSize.`2BR`,
    val squareFootage: Int = 1100,
    val roomCount: Int = 5,
    val estimatedHours: Double = 5.0,
    val crewSize: Int = 2,
    val mileage: Int = 15,
    val truckSize: TruckSize = TruckSize.TRUCK_15FT,
    val stairsFlights: Int = 1,
    val heavyItemCount: Int = 1,
    val highValueItemCount: Int = 0,
    val declaredItemValue: Double = 5000.0,
    val suppliesIncluded: Boolean = true,
    val boxCount: Int = 32,
    val valuationTier: ValuationTier = ValuationTier.RELEASED_VALUE,
    val overallRiskLevel: RiskLevel = RiskLevel.LOW,
    val aiSummary: String = ""
)

data class QuoteBreakdown(
    val laborCost: Double,
    val packageFee: Double,
    val truckBaseFee: Double,
    val mileageFee: Double,
    val suppliesFee: Double,
    val stairsFee: Double,
    val heavyItemFee: Double,
    val highValueItemFee: Double,
    val valuationFee: Double,
    val totalQuote: Double,
    val depositPercent: Int,
    val depositAmount: Double,
    val finalBalanceAmount: Double
)

data class AiDetectedItemDraft(
    val name: String,
    val room: String,
    val category: ItemCategory,
    val estimatedValueUsd: Int,
    val riskLevel: ItemRisk,
    val handlingNote: String,
    val thumbnailBase64: String? = null
)

data class AiWalkthroughAnalysis(
    val items: List<AiDetectedItemDraft>,
    val recommendedTruckSize: TruckSize,
    val estimatedCrewSize: Int,
    val estimatedHours: Double,
    val recommendedBoxes: Int,
    val recommendedWrapRolls: Int,
    val estimatedTotalItemValueUsd: Int,
    val overallRiskLevel: RiskLevel,
    val recommendedValuationTier: ValuationTier,
    val summaryNote: String,
    val capturedThumbnailsBase64: List<String>
)
