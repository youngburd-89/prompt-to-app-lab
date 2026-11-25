package com.example.domain

import com.example.model.EstimatorInputs
import com.example.model.PricingConfig
import com.example.model.QuoteBreakdown
import com.example.model.ServicePackage
import com.example.model.TruckSize
import com.example.model.ValuationTier
import kotlin.math.max
import kotlin.math.roundToInt

object QuoteCalculator {

    fun calculate(inputs: EstimatorInputs, pricing: PricingConfig): QuoteBreakdown {
        val crew = inputs.crewSize.coerceAtLeast(2)
        val hours = inputs.estimatedHours.coerceAtLeast(1.0)
        val laborCost = roundCents(crew * hours * pricing.hourlyLaborRate)

        val packageFee = roundCents(
            when (inputs.servicePackage) {
                ServicePackage.PACK -> pricing.packPackageFee
                ServicePackage.MOVE -> pricing.movePackageFee
                ServicePackage.CLEAN -> pricing.cleanPackageFee
                ServicePackage.FULL_BUNDLE -> pricing.fullBundleFee
            }
        )

        val includesTruck = inputs.servicePackage == ServicePackage.MOVE ||
            inputs.servicePackage == ServicePackage.FULL_BUNDLE

        val truckBaseFee = if (includesTruck) {
            roundCents(
                when (inputs.truckSize) {
                    TruckSize.CARGO_VAN -> pricing.cargoVanFee
                    TruckSize.TRUCK_15FT -> pricing.truck15FtFee
                    TruckSize.TRUCK_20FT -> pricing.truck20FtFee
                    TruckSize.TRUCK_26FT -> pricing.truck26FtFee
                }
            )
        } else {
            0.0
        }

        val mileageFee = if (includesTruck) {
            roundCents(inputs.mileage.coerceAtLeast(0) * pricing.mileageRate)
        } else {
            0.0
        }

        val suppliesFee = if (inputs.suppliesIncluded) {
            roundCents(pricing.suppliesBaseFee + inputs.boxCount.coerceAtLeast(0) * pricing.perBoxFee)
        } else {
            0.0
        }

        val stairsFee = roundCents(inputs.stairsFlights.coerceAtLeast(0) * pricing.stairsFlightFee)
        val heavyItemFee = roundCents(inputs.heavyItemCount.coerceAtLeast(0) * pricing.heavyItemFee)
        val highValueItemFee = roundCents(inputs.highValueItemCount.coerceAtLeast(0) * pricing.highValueItemFee)

        val valuationFee = when (inputs.valuationTier) {
            ValuationTier.RELEASED_VALUE -> 0.0
            ValuationTier.FULL_VALUE -> {
                val pctFee = inputs.declaredItemValue.coerceAtLeast(0.0) * (pricing.fullValueRatePercent / 100.0)
                roundCents(max(pricing.fullValueMinFee, pctFee))
            }
        }

        val totalQuote = roundCents(
            laborCost +
                packageFee +
                truckBaseFee +
                mileageFee +
                suppliesFee +
                stairsFee +
                heavyItemFee +
                highValueItemFee +
                valuationFee
        )

        val depositPct = pricing.depositPercent.coerceIn(10, 80)
        val depositAmount = roundCents(totalQuote * (depositPct / 100.0))
        val finalBalanceAmount = roundCents((totalQuote - depositAmount).coerceAtLeast(0.0))

        return QuoteBreakdown(
            laborCost = laborCost,
            packageFee = packageFee,
            truckBaseFee = truckBaseFee,
            mileageFee = mileageFee,
            suppliesFee = suppliesFee,
            stairsFee = stairsFee,
            heavyItemFee = heavyItemFee,
            highValueItemFee = highValueItemFee,
            valuationFee = valuationFee,
            totalQuote = totalQuote,
            depositPercent = depositPct,
            depositAmount = depositAmount,
            finalBalanceAmount = finalBalanceAmount
        )
    }

    private fun roundCents(value: Double): Double =
        (value * 100.0).roundToInt() / 100.0
}
