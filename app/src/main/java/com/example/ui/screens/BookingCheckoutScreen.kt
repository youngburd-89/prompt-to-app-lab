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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.model.JobStatus
import com.example.model.MoveBooking
import com.example.model.ServicePackage
import com.example.model.TruckSize
import com.example.model.ValuationTier
import com.example.ui.AiUiState
import com.example.ui.BackUpViewModel
import com.example.ui.UiState
import com.example.ui.components.JobStatusBadge
import com.example.ui.components.QuoteBreakdownCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.formatUsd
import com.example.ui.theme.MonoMetricStyle

@Composable
fun BookingCheckoutScreen(
    viewModel: BackUpViewModel,
    defaultCustomerName: String,
    onOpenCrewDispatch: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val inputs by viewModel.estimatorInputs.collectAsStateWithLifecycle()
    val quote by viewModel.liveQuote.collectAsStateWithLifecycle()
    val aiState by viewModel.aiState.collectAsStateWithLifecycle()
    val bookingsState by viewModel.bookingsState.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusyAction.collectAsStateWithLifecycle()

    var customerName by remember(defaultCustomerName) { mutableStateOf(defaultCustomerName.ifBlank { "Jordan Taylor" }) }
    var pickupAddress by remember { mutableStateOf("") }
    var dropoffAddress by remember { mutableStateOf("") }
    var contractAccepted by remember { mutableStateOf(false) }
    var signerName by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }

    val aiCount = (aiState as? AiUiState.Ready)?.analysis?.items?.size ?: 0

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SectionHeader(
                    title = "Booking, Contract & ${quote.depositPercent}% Deposit",
                    subtitle = "Deposit funds just-in-time truck reservation & job supplies. Remaining ${100 - quote.depositPercent}% is collected upon final sign-off.",
                    badge = "${quote.depositPercent}/${100 - quote.depositPercent} SPLIT"
                )

                QuoteBreakdownCard(
                    quote = quote,
                    crewSize = inputs.crewSize,
                    hours = inputs.estimatedHours,
                    mileage = inputs.mileage
                )

                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Job Locations & Contact",
                            style = MaterialTheme.typography.titleMedium
                        )
                        OutlinedTextField(
                            value = customerName,
                            onValueChange = { customerName = it },
                            label = { Text("Customer Full Name") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("customer_name_input")
                        )
                        OutlinedTextField(
                            value = pickupAddress,
                            onValueChange = { pickupAddress = it },
                            label = { Text("Pickup / Origin Address") },
                            placeholder = { Text("e.g., 420 Market St, Apt 4B") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("pickup_address_input")
                        )
                        OutlinedTextField(
                            value = dropoffAddress,
                            onValueChange = { dropoffAddress = it },
                            label = { Text("Dropoff / Destination Address") },
                            placeholder = { Text("e.g., 1185 Pine Ridge Rd") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("dropoff_address_input")
                        )

                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Description,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = if (aiCount > 0) {
                                        "$aiCount AI pre-move condition items will attach automatically to this booking."
                                    } else {
                                        "Crew will complete an on-site pre-move condition walkthrough upon arrival."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        HorizontalDivider()

                        Text(
                            text = "Digital Contract & Liability Acceptance",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Selected Protection: ${inputs.valuationTier.title} (Declared Value ${formatUsd(inputs.declaredItemValue)}). I authorize Back Up to reserve a ${inputs.truckSize.label} and job supplies with my ${quote.depositPercent}% upfront deposit (${formatUsd(quote.depositAmount)}), with ${formatUsd(quote.finalBalanceAmount)} due upon job completion sign-off.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = contractAccepted,
                                onCheckedChange = { contractAccepted = it },
                                modifier = Modifier.testTag("contract_accept_checkbox")
                            )
                            Text(
                                text = "I accept the service agreement, valuation terms & pre-move condition policy.",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        OutlinedTextField(
                            value = signerName,
                            onValueChange = { signerName = it },
                            label = { Text("Digital Signature (Type Full Name)") },
                            placeholder = { Text(customerName) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("digital_signature_input")
                        )

                        if (validationError != null) {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = validationError!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        Button(
                            onClick = {
                                when {
                                    customerName.isBlank() -> validationError = "Enter customer name."
                                    pickupAddress.isBlank() -> validationError = "Enter pickup address."
                                    dropoffAddress.isBlank() -> validationError = "Enter dropoff address."
                                    !contractAccepted -> validationError = "Please accept the digital contract terms."
                                    signerName.isBlank() -> validationError = "Type your full name to sign the contract."
                                    else -> {
                                        validationError = null
                                        viewModel.confirmBookingWithDeposit(
                                            customerName = customerName,
                                            pickupAddress = pickupAddress,
                                            dropoffAddress = dropoffAddress,
                                            signerName = signerName,
                                            onBooked = { newId ->
                                                pickupAddress = ""
                                                dropoffAddress = ""
                                                contractAccepted = false
                                                signerName = ""
                                                onOpenCrewDispatch(newId)
                                            }
                                        )
                                    }
                                }
                            },
                            enabled = !isBusy,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = MaterialTheme.colorScheme.onSecondary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("pay_deposit_button")
                        ) {
                            if (isBusy) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onSecondary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Authorizing Deposit...")
                            } else {
                                Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Authorize ${quote.depositPercent}% Deposit • ${formatUsd(quote.depositAmount)}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp)
            ) {
                SectionHeader(
                    title = "Active Bookings & Final Balance",
                    subtitle = "Pay the remaining balance once your crew completes final sign-off."
                )
            }
        }

        when (val state = bookingsState) {
            UiState.Loading -> {
                item {
                    CircularProgressIndicator()
                }
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
                if (state.data.isEmpty()) {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 720.dp)
                        ) {
                            Text(
                                text = "No confirmed bookings yet. Complete the form above to reserve your truck & reinforcement crew.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(18.dp)
                            )
                        }
                    }
                } else {
                    items(state.data, key = { it.id }) { booking ->
                        BookingSummaryCard(
                            booking = booking,
                            isBusy = isBusy,
                            onPayFinalBalance = {
                                viewModel.collectFinalBalance(
                                    bookingId = booking.id,
                                    signOffName = booking.contractSignerName.ifBlank { booking.customerName }
                                )
                            },
                            onOpenDispatch = { onOpenCrewDispatch(booking.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BookingSummaryCard(
    booking: MoveBooking,
    isBusy: Boolean,
    onPayFinalBalance: () -> Unit,
    onOpenDispatch: () -> Unit
) {
    val status = JobStatus.fromWire(booking.status)
    val pkg = ServicePackage.fromWire(booking.servicePackage)
    val truck = TruckSize.fromWire(booking.truckSize)
    val tier = ValuationTier.fromWire(booking.valuationTier)

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 720.dp)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(pkg.title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${booking.pickupAddress} → ${booking.dropoffAddress}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                JobStatusBadge(status)
            }

            Text(
                text = "${truck.label} • ${booking.crewSize} Crew • ${tier.title}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Total Quote", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatUsd(booking.totalQuote), style = MonoMetricStyle)
                }
                Column {
                    Text("${booking.depositPercent}% Deposit", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "${formatUsd(booking.depositAmount)} PAID",
                        style = MonoMetricStyle,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Final Balance", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = if (booking.finalBalancePaid) "PAID" else formatUsd(booking.finalBalanceAmount),
                        style = MonoMetricStyle,
                        color = if (booking.finalBalancePaid) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenDispatch,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text("Crew View")
                }

                if (!booking.finalBalancePaid) {
                    Button(
                        onClick = onPayFinalBalance,
                        enabled = !isBusy,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary,
                            contentColor = MaterialTheme.colorScheme.onTertiary
                        ),
                        modifier = Modifier
                            .weight(1.4f)
                            .height(48.dp)
                            .testTag("pay_final_balance_${booking.id}")
                    ) {
                        Icon(Icons.Filled.CreditCard, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Pay ${formatUsd(booking.finalBalanceAmount)}")
                    }
                } else {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1.4f)
                            .height(48.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Paid in Full",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
}
