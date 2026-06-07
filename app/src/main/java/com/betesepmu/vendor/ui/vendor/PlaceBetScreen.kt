package com.betesepmu.vendor.ui.vendor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.betesepmu.vendor.bet.PmuPricing
import com.betesepmu.vendor.model.BetSelection
import com.betesepmu.vendor.model.BetTypeOption
import com.betesepmu.vendor.model.Race
import com.betesepmu.vendor.ui.components.SectionCard
import com.betesepmu.vendor.vendor.VendorViewModel
import kotlinx.coroutines.delay

// Web-exact Tailwind colors used by this screen.
private val Gray50 = Color(0xFFF9FAFB)
private val Gray100 = Color(0xFFF3F4F6)
private val Gray200 = Color(0xFFE5E7EB)
private val Gray300 = Color(0xFFD1D5DB)
private val Gray400 = Color(0xFF9CA3AF)
private val Gray500 = Color(0xFF6B7280)
private val Gray700 = Color(0xFF374151)
private val Gray800 = Color(0xFF1F2937)
private val Blue200 = Color(0xFFBFDBFE)
private val Blue600 = Color(0xFF2563EB)
private val Blue700 = Color(0xFF1D4ED8)
private val Blue800 = Color(0xFF1E40AF)
private val Green100 = Color(0xFFDCFCE7)
private val Green700 = Color(0xFF15803D)
private val Red50 = Color(0xFFFEF2F2)
private val Red200 = Color(0xFFFECACA)
private val Red300 = Color(0xFFFCA5A5)
private val Red600 = Color(0xFFDC2626)
private val Red700 = Color(0xFFB91C1C)
private val Yellow400 = Color(0xFFFACC15)
private val Yellow500 = Color(0xFFEAB308)
private val Yellow900 = Color(0xFF713F12)
private val BeteseGreen = Color(0xFF008000)
private val BeteseYellow = Color(0xFFFFFF00)

/** The stroked horse glyph from RaceTimerButton.tsx, as a tintable ImageVector. */
private val HorseIcon: ImageVector by lazy {
    ImageVector.Builder("Horse", 24.dp, 24.dp, 24f, 24f).apply {
        path(stroke = SolidColor(Color.White), strokeLineWidth = 1.8f, strokeLineJoin = StrokeJoin.Round) {
            moveTo(4f, 14f); lineTo(8f, 9f); lineTo(13f, 8f); lineTo(16f, 10f); lineTo(20f, 10f)
            lineTo(19f, 13f); lineTo(16f, 13f); lineTo(14f, 16f); lineTo(10f, 16f); lineTo(8f, 19f)
            lineTo(5f, 19f); lineTo(6f, 16f); lineTo(4f, 14f); close()
        }
    }.build()
}

private const val CUTOFF_MS = 120_000L

@Composable
fun PlaceBetScreen(vm: VendorViewModel, onMessage: (String) -> Unit) {
    val races by vm.races.collectAsStateWithLifecycle()
    val slip by vm.slip.collectAsStateWithLifecycle()
    val slipTotal by vm.slipTotal.collectAsStateWithLifecycle()
    val placing by vm.placing.collectAsStateWithLifecycle()

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(1000) } }

    val activeRaces = remember(races, now) { races.filter { it.endDate.time > now } }

    var selectedRace by remember { mutableStateOf<Race?>(null) }
    var selectedBetType by remember { mutableStateOf<BetTypeOption?>(null) }
    var sequence by remember { mutableStateOf<List<String>>(emptyList()) }

    if (selectedRace == null && activeRaces.isNotEmpty()) selectedRace = activeRaces.first()
    if (selectedRace != null && activeRaces.none { it.id == selectedRace!!.id }) {
        selectedRace = activeRaces.firstOrNull(); selectedBetType = null; sequence = emptyList()
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        selectedRace?.let { ConsoleHeader(it, now) }

        SectionCard("1 · Select Race") {
            if (activeRaces.isEmpty()) {
                Text("No active races right now.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    activeRaces.forEach { race ->
                        RaceTimerButton(race, selectedRace?.id == race.id, now) {
                            selectedRace = race; selectedBetType = null; sequence = emptyList()
                        }
                    }
                }
            }
        }

        val race = selectedRace
        if (race != null) {
            SectionCard("2 · Bet Type & Horses", accent = Blue600) {
                BetTypeGrid(race, selectedBetType) { selectedBetType = it; sequence = emptyList() }

                selectedBetType?.let { bt ->
                    HorseSelector(race, bt, sequence) { sequence = it }

                    val numbers = sequence.filter { it != "X" }.mapNotNull { it.toIntOrNull() }
                    val xCount = sequence.count { it == "X" }
                    val unitCost = PmuPricing.costFor(bt, numbers.size, xCount)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Unit stake", color = Gray500)
                        Text(gmd(unitCost), fontWeight = FontWeight.Black, color = BeteseGreen)
                    }
                    Button(
                        onClick = {
                            val err = vm.addSelection(race, bt, sequence)
                            if (err != null) onMessage(err) else { sequence = emptyList(); selectedBetType = null }
                        },
                        enabled = sequence.isNotEmpty(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BeteseGreen),
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                    ) { Text("ADD TO SLIP", fontWeight = FontWeight.Black, letterSpacing = 0.5.sp) }
                }
            }
        }

        BetSlipPanel(slip, slipTotal, placing, vm)
    }
}

/** Green/red live countdown bar — "Console · Active Race". */
@Composable
private fun ConsoleHeader(race: Race, now: Long) {
    val remaining = (race.endDate.time - now).coerceAtLeast(0L)
    val closed = remaining <= CUTOFF_MS
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = if (closed) Red600 else BeteseGreen),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("CONSOLE — ACTIVE RACE", color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp, fontWeight = FontWeight.Black)
                Text(race.name.uppercase(), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(if (closed) "STATUS" else "REMAINING", color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp, fontWeight = FontWeight.Black)
                Text(
                    if (closed) "CLOSED" else timeMmSs(remaining),
                    color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace,
                )
            }
        }
    }
}

@Composable
private fun RaceTimerButton(race: Race, isSelected: Boolean, now: Long, onClick: () -> Unit) {
    val timeLeft = race.endDate.time - now
    val ended = timeLeft <= 0
    val closed = timeLeft <= CUTOFF_MS
    val closingSoon = !closed && timeLeft < CUTOFF_MS + 60_000L

    val bg: Color; val fg: Color; var ring: Color? = null
    when {
        ended -> { bg = Gray300; fg = Gray500 }
        closed -> { bg = Red50; fg = Red700; ring = Red200 }
        isSelected -> { bg = BeteseGreen; fg = Color.White; ring = BeteseYellow }
        closingSoon -> { bg = Yellow400; fg = Yellow900; ring = Color(0xFFCA8A04) }
        else -> { bg = Green100; fg = BeteseGreen }
    }

    Column(
        Modifier
            .width(128.dp).height(112.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .then(if (ring != null) Modifier.border(2.dp, ring, RoundedCornerShape(12.dp)) else Modifier)
            .clickable(enabled = !ended, onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(HorseIcon, null, tint = fg, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(2.dp))
        Text(race.name.uppercase(), color = fg, fontSize = 13.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, maxLines = 1)
        Spacer(Modifier.height(2.dp))
        if (ended) {
            Text("FINISHED", color = Gray500, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        } else {
            Text(timeMmSs(timeLeft.coerceAtLeast(0L)), color = fg, fontSize = 17.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            Text(if (closed) "CLOSED" else "Starts in", color = if (closed) Red600 else fg.copy(alpha = 0.7f), fontSize = 10.sp, fontWeight = if (closed) FontWeight.Black else FontWeight.Normal)
        }
    }
}

@Composable
private fun BetTypeGrid(race: Race, selected: BetTypeOption?, onSelect: (BetTypeOption) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        maxItemsInEachRow = 3,
    ) {
        BetTypeOption.entries.forEach { bt ->
            val enabled = bt !in race.disabledBetTypes
            val sel = selected == bt
            Box(
                Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (sel) Blue600 else Gray50)
                    .border(2.dp, if (sel) Blue800 else Gray200, RoundedCornerShape(8.dp))
                    .clickable(enabled = enabled) { onSelect(bt) }
                    .padding(4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    bt.label,
                    color = if (!enabled) Gray400 else if (sel) Color.White else Gray700,
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun HorseSelector(race: Race, betType: BetTypeOption, sequence: List<String>, onChange: (List<String>) -> Unit) {
    val pricing = PmuPricing.pricing(betType)
    val strictLimit = PmuPricing.strictLimit(betType)
    val hasX = sequence.contains("X")
    val maxSlots = if (hasX) strictLimit else 20
    val isFull = sequence.size >= maxSlots

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Gray50)
            .border(1.dp, Gray200, RoundedCornerShape(10.dp))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Header
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text("3. SELECT HORSES", color = BeteseGreen, fontSize = 13.sp, fontWeight = FontWeight.Black)
                Text("Bet Type: ${betType.label}", color = Blue700, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text("Select in order. Use 'X' for Field/Champ.", color = Gray500, fontSize = 10.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Req: ${pricing.minHorses}", color = Gray500, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                Text(
                    "Sel: ${sequence.size}/${if (hasX) strictLimit.toString() else "Max"}",
                    color = if (isFull) Red600 else Blue600, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace,
                )
            }
        }

        // Display area
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 40.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (isFull) Red50 else Color.White)
                .border(2.dp, if (isFull) Red200 else Blue200, RoundedCornerShape(6.dp))
                .padding(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.weight(1f)) {
                if (sequence.isEmpty()) {
                    Text("No selection…", color = Gray400, fontSize = 12.sp)
                } else {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        sequence.forEach { item -> SeqChip(item) }
                    }
                }
            }
            Box(
                Modifier.size(28.dp).clip(RoundedCornerShape(6.dp)).background(Gray200)
                    .clickable(enabled = sequence.isNotEmpty()) { onChange(sequence.dropLast(1)) },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.AutoMirrored.Filled.Backspace, "Undo", tint = Gray700, modifier = Modifier.size(16.dp)) }
            Spacer(Modifier.width(4.dp))
            Box(
                Modifier.size(28.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFFFEE2E2))
                    .clickable(enabled = sequence.isNotEmpty()) { onChange(emptyList()) },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Filled.DeleteOutline, "Clear", tint = Red600, modifier = Modifier.size(16.dp)) }
        }

        // 7-column number pad (X + 1..20)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            maxItemsInEachRow = 7,
        ) {
            val xCount = sequence.count { it == "X" }
            val xDisabled = sequence.size >= strictLimit || !PmuPricing.hasXOption(betType) || xCount >= PmuPricing.maxX(betType)
            PadCell(
                modifier = Modifier.weight(1f),
                text = "X",
                bg = if (xDisabled) Gray300 else Yellow500,
                fg = if (xDisabled) Gray500 else Color.White,
                enabled = !xDisabled,
            ) { onChange(sequence + "X") }

            (1..20).forEach { n ->
                val token = n.toString()
                val isSel = sequence.contains(token)
                val nonRunner = race.nonRunners.contains(n)
                val outOfRange = n > race.horseCount
                val disabled = nonRunner || outOfRange || (isFull && !isSel)
                val bg: Color; val fg: Color; var border: Color? = null
                when {
                    nonRunner -> { bg = Red50; fg = Red300 }
                    outOfRange -> { bg = Gray50; fg = Gray200 }
                    isSel -> { bg = BeteseGreen; fg = Color.White; border = Green700 }
                    isFull -> { bg = Gray100; fg = Gray400 }
                    else -> { bg = Color.White; fg = Gray800; border = Gray300 }
                }
                PadCell(
                    modifier = Modifier.weight(1f),
                    text = token,
                    bg = bg, fg = fg, border = border, enabled = !disabled, strikethrough = nonRunner,
                ) {
                    onChange(if (isSel) sequence.filter { it != token } else sequence + token)
                }
            }
        }
    }
}

@Composable
private fun SeqChip(item: String) {
    val isX = item == "X"
    Box(
        Modifier.size(28.dp).clip(RoundedCornerShape(6.dp))
            .background(if (isX) Yellow400 else BeteseGreen)
            .border(1.dp, if (isX) Yellow500 else Green700, RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center,
    ) { Text(item, color = if (isX) Color.Black else Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp) }
}

@Composable
private fun PadCell(
    modifier: Modifier,
    text: String,
    bg: Color,
    fg: Color,
    border: Color? = null,
    enabled: Boolean,
    strikethrough: Boolean = false,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .height(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .then(if (border != null) Modifier.border(1.dp, border, RoundedCornerShape(8.dp)) else Modifier)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text, color = fg, fontWeight = FontWeight.Bold, fontSize = 14.sp,
            textDecoration = if (strikethrough) TextDecoration.LineThrough else TextDecoration.None,
        )
    }
}

/** The web BetSlipPanel: white card, selection rows with pattern chips, multiplier steppers, total, Place Bet. */
@Composable
private fun BetSlipPanel(slip: List<BetSelection>, total: Double, placing: Boolean, vm: VendorViewModel) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(Modifier.fillMaxWidth().height(4.dp).background(BeteseGreen))
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("BET SLIP", fontWeight = FontWeight.Black, fontSize = 18.sp, color = BeteseGreen)
                if (slip.isNotEmpty()) {
                    Text("CLEAR ALL", color = Red600, fontWeight = FontWeight.Black, fontSize = 12.sp,
                        modifier = Modifier.clickable { vm.clearSlip() })
                }
            }

            if (slip.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(vertical = 28.dp), contentAlignment = Alignment.Center) {
                    Text("YOUR BET SLIP IS EMPTY", color = Gray400, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            } else {
                slip.forEachIndexed { index, sel -> SlipCard(sel, vm, index) }
            }

            Spacer(Modifier.height(2.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(Gray200))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Text("TOTAL COST", color = Gray500, fontWeight = FontWeight.Black, fontSize = 12.sp)
                Text(gmd(total), color = BeteseGreen, fontWeight = FontWeight.Black, fontSize = 26.sp)
            }
            Button(
                onClick = { vm.placeBet() },
                enabled = slip.isNotEmpty() && !placing,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BeteseGreen, disabledContainerColor = Gray300),
                modifier = Modifier.fillMaxWidth().height(58.dp),
            ) { Text(if (placing) "PLACING…" else "PLACE BET", fontWeight = FontWeight.Black, fontSize = 18.sp, letterSpacing = 1.sp) }
        }
    }
}

@Composable
private fun SlipCard(sel: BetSelection, vm: VendorViewModel, index: Int) {
    val pattern = sel.pattern.ifEmpty { List(sel.xCount) { "X" } + sel.numbers.map { it.toString() } }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Gray50)
            .border(2.dp, Gray100, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text(sel.betType.label, color = BeteseGreen, fontWeight = FontWeight.Black, fontSize = 13.sp)
                Text(sel.raceName.uppercase(), color = Gray500, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text("SELECTED COMBINATION:", color = Gray400, fontSize = 9.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(2.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    pattern.forEach { SeqChip(it) }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(gmd(sel.cost), fontWeight = FontWeight.Black, fontSize = 16.sp)
                Text("REMOVE", color = Red600, fontWeight = FontWeight.Black, fontSize = 10.sp,
                    modifier = Modifier.clickable { vm.removeSelection(index) })
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Gray200))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier.clip(RoundedCornerShape(8.dp)).border(1.dp, Gray200, RoundedCornerShape(8.dp)).padding(2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StepBox("−", enabled = sel.multiplier > 1) { vm.setMultiplier(index, sel.multiplier - 1) }
                Text("x${sel.multiplier}", Modifier.width(44.dp), textAlign = TextAlign.Center, fontWeight = FontWeight.Black, fontSize = 14.sp)
                StepBox("+", enabled = true) { vm.setMultiplier(index, sel.multiplier + 1) }
            }
            Text(gmd(sel.lineTotal), color = BeteseGreen, fontWeight = FontWeight.Black, fontSize = 18.sp)
        }
    }
}

@Composable
private fun StepBox(symbol: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(32.dp).clip(RoundedCornerShape(6.dp)).background(Gray100)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(symbol, fontWeight = FontWeight.Black, fontSize = 18.sp, color = if (enabled) Gray800 else Gray400) }
}

private fun timeMmSs(ms: Long): String {
    val totalSec = ms / 1000
    val m = (totalSec / 60).toString().padStart(2, '0')
    val s = (totalSec % 60).toString().padStart(2, '0')
    return "$m:$s"
}
