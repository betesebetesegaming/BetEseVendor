package com.betesepmu.vendor.ui.vendor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.betesepmu.vendor.R
import com.betesepmu.vendor.ui.components.BrandLogo
import com.betesepmu.vendor.ui.theme.BrandGreen
import com.betesepmu.vendor.ui.theme.WebGreenTint
import com.betesepmu.vendor.ui.theme.WebYellowAccent
import com.betesepmu.vendor.ui.theme.WebYellowCard
import com.betesepmu.vendor.vendor.VendorViewModel

private enum class VendorView { DASHBOARD, PLACE_BET, SCAN_PAY, FINANCE, RESULTS, CHAT }

private val Yellow800 = Color(0xFF854D0E)

@Composable
fun VendorDashboardScreen(vm: VendorViewModel) {
    val user by vm.currentUser.collectAsStateWithLifecycle()
    val lastTicket by vm.lastTicket.collectAsStateWithLifecycle()
    val onMessage: (String) -> Unit = { vm.messages.tryEmit(it) }

    var view by remember { mutableStateOf(VendorView.DASHBOARD) }

    // "Ticket placed" confirmation after a successful bet.
    lastTicket?.let { ticket ->
        AlertDialog(
            onDismissRequest = { vm.dismissLastTicket() },
            title = { Text("Ticket Placed") },
            text = { Text("Ticket #${ticket.id}\nTotal ${gmd(ticket.totalCost)}\n\nThe ticket has been sent to the printer.") },
            confirmButton = { TextButton(onClick = { vm.dismissLastTicket() }) { Text("Done") } },
            dismissButton = { TextButton(onClick = { vm.reprint(ticket) }) { Text("Reprint") } },
        )
    }

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Brand header
        Row(verticalAlignment = Alignment.CenterVertically) {
            BrandLogo(size = 44.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("Hello, ${user?.name ?: "Vendor"}", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(user?.role ?: "Vendor", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        if (view != VendorView.DASHBOARD) ReturnToMenuButton(onClick = { view = VendorView.DASHBOARD })

        when (view) {
            VendorView.DASHBOARD -> DashboardHome(vm = vm, onNavigate = { view = it })
            VendorView.PLACE_BET -> PlaceBetScreen(vm, onMessage)
            VendorView.SCAN_PAY -> ScanPayScreen(vm, onMessage)
            VendorView.FINANCE -> FinanceScreen(vm, onMessage)
            VendorView.RESULTS -> ResultsScreen(vm)
            VendorView.CHAT -> ChatScreen(vm, onMessage)
        }
    }
}

@Composable
private fun DashboardHome(vm: VendorViewModel, onNavigate: (VendorView) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val recent by vm.recent.collectAsStateWithLifecycle()
    val lastTicket by vm.lastTicket.collectAsStateWithLifecycle()
    val summary = remember(recent) { vm.shiftSummary() }
    val reference = lastTicket ?: recent.firstOrNull()

    // ── Last reference (yellow) ──
    reference?.let { ticket ->
        AccentCard(accent = WebYellowAccent, container = WebYellowCard) {
            Column(Modifier.weight(1f)) {
                Text("LAST REFERENCE:", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Yellow800)
                Text("#${ticket.id}", fontSize = 24.sp, fontWeight = FontWeight.Black, color = cs.onSurface)
            }
            PillButton("REPRINT", container = WebYellowAccent, content = BrandGreen) { vm.reprint(ticket) }
        }
    }

    // ── Shift total sales (green) ──
    AccentCard(accent = BrandGreen, container = WebGreenTint) {
        Column(Modifier.weight(1f)) {
            Text("SHIFT TOTAL SALES:", fontSize = 11.sp, fontWeight = FontWeight.Black, color = BrandGreen)
            Text(gmd(summary.ticketSales), fontSize = 24.sp, fontWeight = FontWeight.Black, color = cs.onSurface)
            Text("${summary.ticketsSold} ticket(s) today", fontSize = 10.sp, color = cs.onSurfaceVariant)
        }
        PillButton("PRINT SALES", container = BrandGreen, content = Color.White) { vm.printSalesReport(endOfSale = false) }
    }

    // ── Menu grid: the signature photo tiles ──
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        maxItemsInEachRow = 2,
    ) {
        MenuPhotoButton("Place Bet", "New Ticket", R.drawable.menu_horse, Modifier.weight(1f)) { onNavigate(VendorView.PLACE_BET) }
        MenuPhotoButton("Scan / Pay", "Payout", R.drawable.menu_money, Modifier.weight(1f)) { onNavigate(VendorView.SCAN_PAY) }
        MenuPhotoButton("Finance", "Wallets", R.drawable.menu_wallet, Modifier.weight(1f)) { onNavigate(VendorView.FINANCE) }
        MenuPhotoButton("Rapport", "Print Results", R.drawable.menu_print, Modifier.weight(1f)) { onNavigate(VendorView.RESULTS) }
        MenuPhotoButton("Results", "View Only", R.drawable.menu_results, Modifier.weight(1f)) { onNavigate(VendorView.RESULTS) }
        MenuPhotoButton("Chat", "Support", R.drawable.menu_chat, Modifier.weight(1f)) { onNavigate(VendorView.CHAT) }
    }
}

/** A white card with a thick colored left bar + tinted fill (web `border-l-8 rounded-r-2xl`). */
@Composable
private fun AccentCard(accent: Color, container: Color, content: @Composable RowScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = container),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.height(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(8.dp).fillMaxHeight().background(accent))
            Row(
                Modifier.weight(1f).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                content = content,
            )
        }
    }
}

@Composable
private fun PillButton(text: String, container: Color, content: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = content),
    ) {
        Icon(Icons.Filled.Print, null, Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, fontWeight = FontWeight.Black, fontSize = 13.sp)
    }
}
