package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.PricingConfig
import com.example.ui.BackUpViewModel
import com.example.ui.UiState
import com.example.ui.components.SectionHeader
import com.example.ui.theme.MonoMetricStyle
import kotlin.math.roundToInt

@Composable
fun AdminPricingScreen(
    viewModel: BackUpViewModel,
    userDisplayName: String,
    userEmail: String?,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pricingState by viewModel.pricingState.collectAsStateWithLifecycle()
    val livePricing by viewModel.livePricingConfig.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusyAction.collectAsStateWithLifecycle()

    var hourlyLabor by remember { mutableStateOf(livePricing.hourlyLaborRate.toString()) }
    var packFee by remember { mutableStateOf(livePricing.packPackageFee.toString()) }
    var moveFee by remember { mutableStateOf(livePricing.movePackageFee.toString()) }
    var cleanFee by remember { mutableStateOf(livePricing.cleanPackageFee.toString()) }
    var fullBundleFee by remember { mutableStateOf(livePricing.fullBundleFee.toString()) }
    var mileageRate by remember { mutableStateOf(livePricing.mileageRate.toString()) }
    var cargoVanFee by remember { mutableStateOf(livePricing.cargoVanFee.toString()) }
    var truck15Fee by remember { mutableStateOf(livePricing.truck15FtFee.toString()) }
    var truck20Fee by remember { mutableStateOf(livePricing.truck20FtFee.toString()) }
    var truck26Fee by remember { mutableStateOf(livePricing.truck26FtFee.toString()) }
    var suppliesBase by remember { mutableStateOf(livePricing.suppliesBaseFee.toString()) }
    var perBoxFee by remember { mutableStateOf(livePricing.perBoxFee.toString()) }
    var stairsFee by remember { mutableStateOf(livePricing.stairsFlightFee.toString()) }
    var heavyFee by remember { mutableStateOf(livePricing.heavyItemFee.toString()) }
    var highValueFee by remember { mutableStateOf(livePricing.highValueItemFee.toString()) }
    var depositPercent by remember { mutableStateOf(livePricing.depositPercent) }
    var fullValuePct by remember { mutableStateOf(livePricing.fullValueRatePercent.toString()) }
    var fullValueMin by remember { mutableStateOf(livePricing.fullValueMinFee.toString()) }

    LaunchedEffect(livePricing) {
        hourlyLabor = livePricing.hourlyLaborRate.toString()
        packFee = livePricing.packPackageFee.toString()
        moveFee = livePricing.movePackageFee.toString()
        cleanFee = livePricing.cleanPackageFee.toString()
        fullBundleFee = livePricing.fullBundleFee.toString()
        mileageRate = livePricing.mileageRate.toString()
        cargoVanFee = livePricing.cargoVanFee.toString()
        truck15Fee = livePricing.truck15FtFee.toString()
        truck20Fee = livePricing.truck20FtFee.toString()
        truck26Fee = livePricing.truck26FtFee.toString()
        suppliesBase = livePricing.suppliesBaseFee.toString()
        perBoxFee = livePricing.perBoxFee.toString()
        stairsFee = livePricing.stairsFlightFee.toString()
        heavyFee = livePricing.heavyItemFee.toString()
        highValueFee = livePricing.highValueItemFee.toString()
        depositPercent = livePricing.depositPercent
        fullValuePct = livePricing.fullValueRatePercent.toString()
        fullValueMin = livePricing.fullValueMinFee.toString()
    }

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
                    title = "Admin Pricing Controls",
                    subtitle = "Configure live rates for labor, packages, just-in-time trucks, supplies, deposit %, and valuation tiers.",
                    badge = "LIVE ENGINE"
                )
            }
        }

        if (pricingState is UiState.Error) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 720.dp)
                ) {
                    Text(
                        text = (pricingState as UiState.Error).message,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        }

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
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Labor & Service Packages", style = MaterialTheme.typography.titleMedium)
                    RateFieldRow("Hourly Labor / Crew ($/hr)", hourlyLabor) { hourlyLabor = it }
                    RateFieldRow("Pack for You Base ($)", packFee) { packFee = it }
                    RateFieldRow("Move for You Base ($)", moveFee) { moveFee = it }
                    RateFieldRow("Moving Out Clean-Up Base ($)", cleanFee) { cleanFee = it }
                    RateFieldRow("Full Reinforcement Bundle ($)", fullBundleFee) { fullBundleFee = it }
                }
            }
        }

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
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Just-in-Time Rental Trucks & Mileage", style = MaterialTheme.typography.titleMedium)
                    RateFieldRow("Mileage Rate ($/mile)", mileageRate) { mileageRate = it }
                    RateFieldRow("Cargo Van 10ft Base ($)", cargoVanFee) { cargoVanFee = it }
                    RateFieldRow("15ft Box Truck Base ($)", truck15Fee) { truck15Fee = it }
                    RateFieldRow("20ft Box Truck Base ($)", truck20Fee) { truck20Fee = it }
                    RateFieldRow("26ft Box Truck Base ($)", truck26Fee) { truck26Fee = it }
                }
            }
        }

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
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Supplies, Special Items, Deposit & Valuation", style = MaterialTheme.typography.titleMedium)
                    RateFieldRow("Supplies Kit Base ($)", suppliesBase) { suppliesBase = it }
                    RateFieldRow("Per-Box Fee ($/box)", perBoxFee) { perBoxFee = it }
                    RateFieldRow("Stairs Carry ($/flight)", stairsFee) { stairsFee = it }
                    RateFieldRow("Heavy / Bulky Item ($/item)", heavyFee) { heavyFee = it }
                    RateFieldRow("High-Value Handling ($/item)", highValueFee) { highValueFee = it }

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Upfront Deposit %", style = MaterialTheme.typography.bodyMedium)
                            Text("$depositPercent% / ${100 - depositPercent}% split", style = MonoMetricStyle)
                        }
                        Slider(
                            value = depositPercent.toFloat(),
                            onValueChange = { depositPercent = it.roundToInt().coerceIn(10, 50) },
                            valueRange = 10f..50f
                        )
                    }

                    RateFieldRow("Full Value Protection (% of declared)", fullValuePct) { fullValuePct = it }
                    RateFieldRow("Full Value Minimum Fee ($)", fullValueMin) { fullValueMin = it }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                viewModel.savePricingConfig(PricingConfig())
                            },
                            enabled = !isBusy,
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("reset_pricing_button")
                        ) {
                            Icon(Icons.Filled.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Defaults")
                        }

                        Button(
                            onClick = {
                                val updated = livePricing.copy(
                                    hourlyLaborRate = hourlyLabor.toDoubleOrNull() ?: livePricing.hourlyLaborRate,
                                    packPackageFee = packFee.toDoubleOrNull() ?: livePricing.packPackageFee,
                                    movePackageFee = moveFee.toDoubleOrNull() ?: livePricing.movePackageFee,
                                    cleanPackageFee = cleanFee.toDoubleOrNull() ?: livePricing.cleanPackageFee,
                                    fullBundleFee = fullBundleFee.toDoubleOrNull() ?: livePricing.fullBundleFee,
                                    mileageRate = mileageRate.toDoubleOrNull() ?: livePricing.mileageRate,
                                    cargoVanFee = cargoVanFee.toDoubleOrNull() ?: livePricing.cargoVanFee,
                                    truck15FtFee = truck15Fee.toDoubleOrNull() ?: livePricing.truck15FtFee,
                                    truck20FtFee = truck20Fee.toDoubleOrNull() ?: livePricing.truck20FtFee,
                                    truck26FtFee = truck26Fee.toDoubleOrNull() ?: livePricing.truck26FtFee,
                                    suppliesBaseFee = suppliesBase.toDoubleOrNull() ?: livePricing.suppliesBaseFee,
                                    perBoxFee = perBoxFee.toDoubleOrNull() ?: livePricing.perBoxFee,
                                    stairsFlightFee = stairsFee.toDoubleOrNull() ?: livePricing.stairsFlightFee,
                                    heavyItemFee = heavyFee.toDoubleOrNull() ?: livePricing.heavyItemFee,
                                    highValueItemFee = highValueFee.toDoubleOrNull() ?: livePricing.highValueItemFee,
                                    depositPercent = depositPercent,
                                    fullValueRatePercent = fullValuePct.toDoubleOrNull() ?: livePricing.fullValueRatePercent,
                                    fullValueMinFee = fullValueMin.toDoubleOrNull() ?: livePricing.fullValueMinFee
                                )
                                viewModel.savePricingConfig(updated)
                            },
                            enabled = !isBusy,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = MaterialTheme.colorScheme.onSecondary
                            ),
                            modifier = Modifier
                                .weight(1.4f)
                                .height(50.dp)
                                .testTag("save_pricing_button")
                        ) {
                            if (isBusy) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = MaterialTheme.colorScheme.onSecondary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Save Pricing Rules", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(userDisplayName.ifBlank { "Back Up Operator" }, style = MaterialTheme.typography.titleSmall)
                        if (!userEmail.isNullOrBlank()) {
                            Text(
                                text = userEmail,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = onSignOut,
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("sign_out_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Sign Out")
                    }
                }
            }
        }
    }
}

@Composable
private fun RateFieldRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = { input ->
            if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d*$"))) {
                onValueChange(input)
            }
        },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}
