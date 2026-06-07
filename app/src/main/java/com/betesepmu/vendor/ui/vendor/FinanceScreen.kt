package com.betesepmu.vendor.ui.vendor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.betesepmu.vendor.model.PaymentMethod
import com.betesepmu.vendor.model.VendorUser
import com.betesepmu.vendor.ui.components.DropdownSetting
import com.betesepmu.vendor.ui.components.InfoRow
import com.betesepmu.vendor.ui.components.SectionCard
import com.betesepmu.vendor.vendor.VendorViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val FGreen = Color(0xFF008000)
private val FOrange = Color(0xFFF97316)
private val FBlue = Color(0xFF1D4ED8)
private val FRed = Color(0xFFDC2626)
private val FGray400 = Color(0xFF9CA3AF)
private val FGray200 = Color(0xFFE5E7EB)

@Composable
fun FinanceScreen(vm: VendorViewModel, onMessage: (String) -> Unit) {
    val user by vm.currentUser.collectAsStateWithLifecycle()
    val recent by vm.recent.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.refreshRecent() }
    val summary = remember(recent) { vm.shiftSummary() }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        VendorSalesSummary(user?.name ?: "Vendor", summary, onPrint = { vm.printSalesReport(endOfSale = true) })
        DepositPanel(vm, onMessage)
        WithdrawalPanel(vm, onMessage)
    }
}

@Composable
private fun VendorSalesSummary(vendorName: String, summary: com.betesepmu.vendor.vendor.ShiftSummary, onPrint: () -> Unit) {
    val date = SimpleDateFormat("EEE, dd MMM yyyy", Locale.US).format(Date())
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        // Green header
        Row(Modifier.fillMaxWidth().background(FGreen).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("VENDOR SALES SUMMARY", color = Color.White.copy(alpha = 0.75f), fontSize = 10.sp, fontWeight = FontWeight.Black)
                Text(vendorName, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
            }
            Text(date, color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        // Stat grid
        FlowRow(maxItemsInEachRow = 2, modifier = Modifier.fillMaxWidth()) {
            StatCell(Modifier.weight(1f), "Ticket Sales", gmd(summary.ticketSales), FGreen, "${summary.ticketsSold} ticket(s)")
            StatCell(Modifier.weight(1f), "Paid Out", gmd(summary.paidOut), FOrange, "${summary.payoutCount} paid")
            StatCell(Modifier.weight(1f), "Total Sales", gmd(summary.ticketSales), FGreen, "tickets")
            StatCell(Modifier.weight(1f), "Net Balance", gmd(summary.net), if (summary.net >= 0) FBlue else FRed, "sales − payouts")
        }
        Box(Modifier.padding(12.dp)) {
            Button(
                onClick = onPrint,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FGreen),
                modifier = Modifier.fillMaxWidth().height(50.dp),
            ) { Icon(Icons.Filled.Print, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("PRINT END OF SALE", fontWeight = FontWeight.Black) }
        }
    }
}

@Composable
private fun StatCell(modifier: Modifier, label: String, value: String, valueColor: Color, sub: String) {
    Column(
        modifier.border(0.5.dp, FGray200).padding(vertical = 16.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label.uppercase(), color = FGray400, fontSize = 10.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
        Spacer(Modifier.height(2.dp))
        Text(value, color = valueColor, fontSize = 20.sp, fontWeight = FontWeight.Black)
        Text(sub, color = FGray400, fontSize = 9.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun DepositPanel(vm: VendorViewModel, onMessage: (String) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()

    var query by remember { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }
    var customer by remember { mutableStateOf<VendorUser?>(null) }
    var notFound by remember { mutableStateOf(false) }
    var amount by remember { mutableStateOf("") }
    var method by remember { mutableStateOf(PaymentMethod.Cash) }
    var busy by remember { mutableStateOf(false) }

    SectionCard("Customer Deposit") {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Customer phone or name") },
                singleLine = true,
                enabled = !searching,
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = {
                    if (query.isBlank()) return@Button
                    searching = true; notFound = false; customer = null
                    scope.launch {
                        val found = vm.findCustomer(query.trim())
                        searching = false; customer = found; notFound = found == null
                    }
                },
                enabled = !searching && query.isNotBlank(),
                modifier = Modifier.height(56.dp),
            ) { if (searching) CircularProgressIndicator(Modifier.height(20.dp), strokeWidth = 2.dp, color = cs.onPrimary) else Text("Find") }
        }
        if (notFound) Text("No customer found.", color = cs.error, fontSize = 13.sp)

        customer?.let { c ->
            InfoRow("Customer", c.name)
            c.phone?.let { InfoRow("Phone", it) }
            InfoRow("Wallet", gmd(c.walletBalance))
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it.filter { ch -> ch.isDigit() || ch == '.' } },
                label = { Text("Amount (GMD)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            DropdownSetting(
                label = "Method",
                options = PaymentMethod.entries,
                selected = method,
                labelOf = { it.label },
                onSelect = { method = it },
            )
            Button(
                onClick = {
                    val amt = amount.toDoubleOrNull()
                    if (amt == null || amt <= 0) { onMessage("Enter a valid amount."); return@Button }
                    busy = true
                    scope.launch {
                        vm.deposit(c, amt, method)
                            .onSuccess {
                                onMessage("Deposited ${gmd(amt)} for ${c.name}")
                                amount = ""; customer = c.copy(walletBalance = c.walletBalance + amt)
                            }
                            .onFailure { onMessage(it.message ?: "Deposit failed") }
                        busy = false
                    }
                },
                enabled = !busy && amount.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (busy) "PROCESSING…" else "DEPOSIT & PRINT RECEIPT", fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun WithdrawalPanel(vm: VendorViewModel, onMessage: (String) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    var code by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    SectionCard("Process Withdrawal") {
        Text("Enter the customer's withdrawal code to pay out.", fontSize = 12.sp, color = cs.onSurfaceVariant)
        OutlinedTextField(
            value = code,
            onValueChange = { code = it },
            label = { Text("Withdrawal code") },
            singleLine = true,
            enabled = !busy,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = {
                if (code.isBlank()) return@Button
                busy = true
                scope.launch {
                    vm.processWithdrawal(code.trim())
                        .onSuccess { onMessage("Paid out ${gmd(it.amount)} (${it.code})"); code = "" }
                        .onFailure { onMessage(it.message ?: "Withdrawal failed") }
                    busy = false
                }
            },
            enabled = !busy && code.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) { Text(if (busy) "PROCESSING…" else "PAY OUT & PRINT RECEIPT", fontWeight = FontWeight.Bold) }
    }
}
