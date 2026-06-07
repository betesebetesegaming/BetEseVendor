package com.betesepmu.vendor.ui.vendor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.betesepmu.vendor.model.Ticket
import com.betesepmu.vendor.model.TicketStatus
import com.betesepmu.vendor.ui.components.SectionCard
import com.betesepmu.vendor.vendor.VendorViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

private val Orange200 = Color(0xFFFED7AA)
private val Orange500 = Color(0xFFF97316)
private val Orange600 = Color(0xFFEA580C)
private val SBlue600 = Color(0xFF2563EB)
private val SBlue700 = Color(0xFF1D4ED8)
private val SGreen600 = Color(0xFF16A34A)
private val SRed600 = Color(0xFFDC2626)
private val SRed700 = Color(0xFFB91C1C)
private val SGray50 = Color(0xFFF9FAFB)
private val SGray200 = Color(0xFFE5E7EB)
private val SGray500 = Color(0xFF6B7280)
private val SGray800 = Color(0xFF1F2937)
private val SPurple700 = Color(0xFF7E22CE)
private val SGreen100 = Color(0xFFDCFCE7)
private val SGreen800 = Color(0xFF166534)
private val SRed50 = Color(0xFFFEF2F2)
private val SRed100 = Color(0xFFFEE2E2)
private val SRed300 = Color(0xFFFCA5A5)
private val SAmber100 = Color(0xFFFEF3C7)
private val SAmber800 = Color(0xFF92400E)

@Composable
fun ScanPayScreen(vm: VendorViewModel, onMessage: (String) -> Unit) {
    val scope = rememberCoroutineScope()

    var query by remember { mutableStateOf("") }
    var scanMode by remember { mutableStateOf(true) }
    var searching by remember { mutableStateOf(false) }
    var actionBusy by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<Ticket?>(null) }
    var message by remember { mutableStateOf("") }
    var success by remember { mutableStateOf(false) }

    fun lookup(ref: String, fromScan: Boolean) {
        val normalized = ref.replace("\n", "").replace("\r", "").trim()
        if (normalized.isBlank()) { if (!fromScan) message = "Please enter Ticket Serial Number." ; return }
        searching = true
        scope.launch {
            val ticket = vm.lookupTicket(normalized)
            searching = false
            if (ticket != null) {
                result = ticket
                message = if (ticket.status == TicketStatus.PAID)
                    "PAID TICKET ALREADY — paid by ${ticket.paidByName ?: ticket.paidById ?: "unknown staff"}. Do not pay again."
                else ""
                success = false
            } else if (!fromScan) {
                result = null; message = "Ticket not found in backoffice database."; success = false
            }
        }
    }

    // Scan-mode: auto-lookup once the input looks like a serial / booking code.
    LaunchedEffect(query, scanMode) {
        if (!scanMode) return@LaunchedEffect
        val n = query.replace("\n", "").replace("\r", "").trim()
        val looksSerial = Regex("^\\d{7,}$").matches(n)
        val looksBooking = Regex("^B[A-Z0-9]{4,}$", RegexOption.IGNORE_CASE).matches(n)
        if (!looksSerial && !looksBooking) return@LaunchedEffect
        delay(180)
        lookup(n, fromScan = true)
    }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SectionCard("Scan & Payout", accent = Orange500, icon = Icons.Filled.Search) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Backoffice ticket rescue: serial number or booking code.", fontSize = 11.sp, color = SGray500, modifier = Modifier.weight(1f))
                Box(
                    Modifier.clip(RoundedCornerShape(8.dp))
                        .background(if (scanMode) SGreen100 else Color(0xFFF3F4F6))
                        .clickable { scanMode = !scanMode }
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                ) {
                    Text("SCAN MODE: ${if (scanMode) "ON" else "OFF"}", fontSize = 10.sp, fontWeight = FontWeight.Black, color = if (scanMode) SGreen800 else SGray500)
                }
            }

            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(SRed50).border(1.dp, SRed300, RoundedCornerShape(8.dp)).padding(8.dp)) {
                Text("CANCEL RULE: a ticket can only be canceled more than 2 minutes before race start.", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SRed700)
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it.replace("\n", "").replace("\r", "") },
                    placeholder = { Text("Scan / type serial no or booking code") },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { lookup(query, false) }),
                    modifier = Modifier.weight(1f),
                )
                Button(
                    onClick = { lookup(query, false) },
                    enabled = !searching && query.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Orange600),
                    modifier = Modifier.height(56.dp),
                ) {
                    if (searching) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                    else Text("CHECK", fontWeight = FontWeight.Black)
                }
            }

            if (message.isNotBlank()) {
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(if (success) SGreen100 else SRed50).padding(10.dp)) {
                    Text(message, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (success) SGreen800 else SRed700, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
            }

            result?.let { ticket -> FoundTicket(ticket, actionBusy, vm, onMessage,
                onPaid = { result = ticket.copy(status = TicketStatus.PAID); message = "Payout successful!"; success = true },
                onCanceled = { result = null; query = ""; message = "Cancel request sent."; success = false },
                setBusy = { actionBusy = it }) }
        }

        BookingCard(vm, onMessage)
        RecentTicketsCard(vm)
    }
}

@Composable
private fun FoundTicket(
    ticket: Ticket,
    busy: Boolean,
    vm: VendorViewModel,
    onMessage: (String) -> Unit,
    onPaid: () -> Unit,
    onCanceled: () -> Unit,
    setBusy: (Boolean) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val statusLabel = when (ticket.status) {
        TicketStatus.ACTIVE -> "Awaiting Result"
        TicketStatus.WINNING -> "Awaiting Cashier Payout"
        TicketStatus.BOOKED -> "Booking Pending Payment"
        TicketStatus.PAID -> "Paid"
        TicketStatus.LOST -> "Lost"
        TicketStatus.CANCELED -> "Canceled"
        else -> ticket.status
    }
    val statusColor = when (ticket.status) {
        TicketStatus.WINNING -> SBlue600
        TicketStatus.LOST -> SRed600
        else -> SGray800
    }
    val payable = ticket.status == TicketStatus.WINNING && ticket.customerId.isNullOrBlank()
    val cancelable = ticket.status == TicketStatus.ACTIVE || ticket.status == TicketStatus.BOOKED

    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(SGray50).border(2.dp, Orange200, RoundedCornerShape(16.dp)).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        DetailRow("Serial No", ticket.id, mono = true)
        DetailRow("Booking Code", ticket.bookingCode ?: "---", mono = true)
        DetailRow("Vendor", ticket.vendorName ?: ticket.vendorId ?: "---")
        DetailRow("Status", statusLabel.uppercase(), valueColor = statusColor)
        DetailRow("Cost", gmd(ticket.totalCost))
        if (ticket.status == TicketStatus.PAID) DetailRow("Paid By", ticket.paidByName ?: ticket.paidById ?: "—", valueColor = SPurple700)

        // Bet combinations
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color.White).border(1.dp, SGray200, RoundedCornerShape(8.dp)).padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("BET COMBINATIONS", fontSize = 10.sp, fontWeight = FontWeight.Black, color = SGray500)
            Column(Modifier.heightIn(max = 140.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                ticket.selections.forEach { s ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(buildString { append(s.betType.label); if (s.multiplier > 1) append("  ×${s.multiplier}") }, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        val pat = s.pattern.ifEmpty { List(s.xCount) { "X" } + s.numbers.map { it.toString() } }
                        Text(pat.joinToString("-"), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        if (ticket.status == TicketStatus.WINNING) {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SBlue600).padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("WINNING AMOUNT", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(String.format(Locale.US, "%.2f", ticket.winnings ?: 0.0), color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Black)
                Text("GMD", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
        if (ticket.status == TicketStatus.PAID) {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SRed100).border(2.dp, SRed300, RoundedCornerShape(12.dp)).padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("PAID TICKET ALREADY", color = SRed700, fontSize = 14.sp, fontWeight = FontWeight.Black)
                Text("Do not pay again", color = SRed700, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Actions
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { vm.reprint(ticket) },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SBlue600),
                modifier = Modifier.weight(1f).height(52.dp),
            ) { Icon(Icons.Filled.Print, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("VIEW / PRINT", fontWeight = FontWeight.Black) }

            if (payable) {
                Button(
                    onClick = {
                        setBusy(true)
                        scope.launch {
                            vm.payout(ticket).onSuccess { onPaid() }.onFailure { onMessage(it.message ?: "Payout failed") }
                            setBusy(false)
                        }
                    },
                    enabled = !busy,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SGreen600),
                    modifier = Modifier.weight(1f).height(52.dp),
                ) { Text("CONFIRM PAYOUT", fontWeight = FontWeight.Black) }
            }
        }
        if (cancelable) {
            Button(
                onClick = {
                    setBusy(true)
                    scope.launch {
                        vm.cancel(ticket).onSuccess { onCanceled() }.onFailure { onMessage(it.message ?: "Cancel failed") }
                        setBusy(false)
                    }
                },
                enabled = !busy,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SRed600),
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text("CANCEL TICKET", fontWeight = FontWeight.Black) }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String, valueColor: Color = SGray800, mono: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label.uppercase(), color = SGray500, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Text(value, color = valueColor, fontWeight = FontWeight.Black, fontSize = 13.sp, fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default)
    }
}

@Composable
private fun BookingCard(vm: VendorViewModel, onMessage: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    var code by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    SectionCard("Pay a Booking", accent = Color(0xFFEAB308)) {
        Text("Enter a booking code to pay for and print a booked ticket.", fontSize = 12.sp, color = SGray500)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = code, onValueChange = { code = it }, label = { Text("Booking code") },
                singleLine = true, enabled = !busy, modifier = Modifier.weight(1f),
            )
            Button(
                onClick = {
                    if (code.isBlank()) return@Button
                    busy = true
                    scope.launch {
                        vm.payForBooking(code.trim()).onSuccess { code = "" }.onFailure { onMessage(it.message ?: "Booking failed") }
                        busy = false
                    }
                },
                enabled = !busy && code.isNotBlank(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SGreen600),
                modifier = Modifier.height(56.dp),
            ) { Text(if (busy) "…" else "PAY", fontWeight = FontWeight.Black) }
        }
    }
}

@Composable
private fun RecentTicketsCard(vm: VendorViewModel) {
    val recent by vm.recent.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.refreshRecent() }
    SectionCard("Recent Tickets") {
        if (recent.isEmpty()) {
            Text("No recent tickets.", color = SGray500)
        } else {
            recent.take(10).forEach { ticket ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("#${ticket.id}", fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                        Text(
                            "${SimpleDateFormat("dd MMM HH:mm", Locale.US).format(ticket.timestamp)} · ${gmd(ticket.totalCost)} · ${ticket.status}",
                            fontSize = 11.sp, color = SGray500,
                        )
                    }
                    TextButton(onClick = { vm.reprint(ticket) }) { Text("Reprint", fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}
