package com.example.suppli

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

// --- 🎨 VIBRANT COLOR PALETTE ---
val AppBackground = Color(0xFFF5F7FB)
val PrimaryNeon = Color(0xFF6C5CE7)
val SecondaryAqua = Color(0xFF00CEC9)
val AccentPink = Color(0xFFE84393)
val CardWhite = Color(0xFFFFFFFF)

enum class DayPartition { MORNING, EVENING }

data class Supplement(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val emoji: String,
    val doseValue: String,
    val doseUnit: String,
    val targetDoses: Int = 1,
    val takenDoses: Int = 0,
    val timeOfDay: DayPartition = DayPartition.MORNING
) {
    val isComplete: Boolean get() = takenDoses >= targetDoses
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = PrimaryNeon,
                    secondary = SecondaryAqua,
                    tertiary = AccentPink,
                    background = AppBackground,
                    surface = CardWhite
                )
            ) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    SupplementTrackerApp()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplementTrackerApp() {
    var supplements by remember { mutableStateOf(listOf<Supplement>()) }
    var showAddSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    var supplementToEdit by remember { mutableStateOf<Supplement?>(null) }
    var supplementToDelete by remember { mutableStateOf<Supplement?>(null) }

    var lastOpenedDate by remember { mutableStateOf(getCurrentDateString()) }

    LaunchedEffect(Unit) {
        val today = getCurrentDateString()
        if (today != lastOpenedDate) {
            supplements = supplements.map { it.copy(takenDoses = 0) }
            lastOpenedDate = today
        }
    }

    fun updateTakenDoses(list: List<Supplement>, clicked: Supplement): List<Supplement> {
        return list.map {
            if (it.id == clicked.id && it.takenDoses < it.targetDoses) {
                it.copy(takenDoses = it.takenDoses + 1)
            } else it
        }
    }

    val areAllPillsTaken = supplements.isNotEmpty() && supplements.all { it.isComplete }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {

            HeaderBanner(allPillsTakenToday = areAllPillsTaken)

            WeeklyOverviewTracker(allPillsTakenToday = areAllPillsTaken)

            if (supplements.isEmpty()) {
                EmptyStateView()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // --- ☀️ MORNING STACK ---
                    item { CategoryHeaderChip("Morning Stack ☀️", Color(0xFFFD9644)) }
                    val morningSupps = supplements.filter { it.timeOfDay == DayPartition.MORNING }
                    if (morningSupps.isEmpty()) {
                        item { DropZonePlaceholder("Drag morning supplements here") }
                    } else {
                        items(morningSupps, key = { it.id }) { supplement ->
                            DraggableSupplementCard(
                                supplement = supplement,
                                onIncrement = { supplements = updateTakenDoses(supplements, supplement) },
                                onLongClick = { supplementToEdit = it },
                                onDraggedTo = { target ->
                                    supplements = supplements.map {
                                        if (it.id == supplement.id) it.copy(timeOfDay = target) else it
                                    }
                                }
                            )
                        }
                    }

                    // --- 🌙 EVENING STACK ---
                    item { CategoryHeaderChip("Evening Stack 🌙", Color(0xFF45AAF2)) }
                    val eveningSupps = supplements.filter { it.timeOfDay == DayPartition.EVENING }
                    if (eveningSupps.isEmpty()) {
                        item { DropZonePlaceholder("Drag evening supplements here") }
                    } else {
                        items(eveningSupps, key = { it.id }) { supplement ->
                            DraggableSupplementCard(
                                supplement = supplement,
                                onIncrement = { supplements = updateTakenDoses(supplements, supplement) },
                                onLongClick = { supplementToEdit = it },
                                onDraggedTo = { target ->
                                    supplements = supplements.map {
                                        if (it.id == supplement.id) it.copy(timeOfDay = target) else it
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // --- ➕ FLOATING HUB ---
        FloatingControlHub(
            onClick = { showAddSheet = true },
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        if (showAddSheet) {
            ModalBottomSheet(
                onDismissRequest = { showAddSheet = false },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 16.dp,
                shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp)
            ) {
                AddSupplementSheet(onAdd = { newSup ->
                    supplements = supplements + newSup
                    showAddSheet = false
                })
            }
        }

        supplementToEdit?.let { editSup ->
            EditSupplementDialog(
                supplement = editSup,
                onDismiss = { supplementToEdit = null },
                onSave = { updatedSup ->
                    supplements = supplements.map { if (it.id == updatedSup.id) updatedSup else it }
                    supplementToEdit = null
                },
                onDeleteTrigger = {
                    supplementToDelete = editSup
                    supplementToEdit = null
                }
            )
        }

        supplementToDelete?.let { deleteSup ->
            AlertDialog(
                onDismissRequest = { supplementToDelete = null },
                title = { Text("Delete Supplement? 🗑️", fontWeight = FontWeight.Bold) },
                text = { Text("Are you sure you want to delete ${deleteSup.name} from your stack?") },
                confirmButton = {
                    Button(
                        onClick = {
                            supplements = supplements.filter { it.id != deleteSup.id }
                            supplementToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text("Delete") }
                },
                dismissButton = {
                    TextButton(onClick = { supplementToDelete = null }) { Text("Cancel") }
                }
            )
        }
    }
}

@Composable
fun HeaderBanner(allPillsTakenToday: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 36.dp, start = 24.dp, end = 24.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Daily Stack", fontSize = 34.sp, fontWeight = FontWeight.Black, color = Color.Black)
            Text(if (allPillsTakenToday) "Everything taken! 🥳" else "Tap to track your doses today", fontSize = 14.sp, color = Color.Gray)
        }
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(PrimaryNeon),
            contentAlignment = Alignment.Center
        ) {
            Text("💊", fontSize = 28.sp)
        }
    }
}

@Composable
fun CategoryHeaderChip(title: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(modifier = Modifier.width(10.dp))
        Text(title, fontWeight = FontWeight.ExtraBold, color = color, fontSize = 14.sp)
    }
}

@Composable
fun DraggableSupplementCard(
    supplement: Supplement,
    onIncrement: () -> Unit,
    onLongClick: (Supplement) -> Unit,
    onDraggedTo: (DayPartition) -> Unit
) {
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(if (isDragging) 1.08f else 1f, animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
    val shadowElev by animateFloatAsState(if (isDragging) 32f else 2f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { isDragging = true },
                    onDragEnd = {
                        isDragging = false
                        if (offsetY < -150f && supplement.timeOfDay == DayPartition.EVENING) onDraggedTo(DayPartition.MORNING)
                        if (offsetY > 150f && supplement.timeOfDay == DayPartition.MORNING) onDraggedTo(DayPartition.EVENING)
                        offsetX = 0f
                        offsetY = 0f
                    },
                    onDragCancel = {
                        isDragging = false
                        offsetX = 0f
                        offsetY = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y
                    }
                )
            }
            .scale(scale)
            .shadow(shadowElev.dp, RoundedCornerShape(28.dp))
    ) {
        GlassSupplementItem(supplement, onIncrement, onLongClick = { onLongClick(supplement) })
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GlassSupplementItem(supplement: Supplement, onIncrement: () -> Unit, onLongClick: () -> Unit) {
    val progressFraction = supplement.takenDoses.toFloat() / supplement.targetDoses.toFloat()

    var squishScale by remember { mutableStateOf(1f) }
    val animatedSquish by animateFloatAsState(
        targetValue = squishScale,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        finishedListener = { squishScale = 1f }
    )

    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = if (supplement.isComplete) Color(0xFFF0FFF4) else Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .scale(animatedSquish)
            .combinedClickable(onClick = { squishScale = 0.94f; onIncrement() }, onLongClick = onLongClick)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(64.dp)) {
                    drawCircle(
                        color = Color.LightGray.copy(0.2f),
                        radius = size.minDimension / 2,
                        style = Stroke(width = 4.dp.toPx())
                    )
                    drawArc(
                        brush = Brush.sweepGradient(listOf(SecondaryAqua, PrimaryNeon)),
                        startAngle = -90f,
                        sweepAngle = progressFraction * 360f,
                        useCenter = false,
                        style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(PrimaryNeon.copy(0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(supplement.emoji, fontSize = 26.sp)
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = supplement.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    textDecoration = if (supplement.isComplete) TextDecoration.LineThrough else null
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (supplement.doseUnit == "pills") "Progress: ${supplement.takenDoses}/${supplement.targetDoses} taken" else "${supplement.doseValue} ${supplement.doseUnit}",
                    fontSize = 13.sp, color = Color.Gray
                )
            }

            AnimatedVisibility(supplement.isComplete, enter = scaleIn() + fadeIn()) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = SecondaryAqua, modifier = Modifier.size(32.dp))
            }
        }
    }
}

@Composable
fun WeeklyOverviewTracker(allPillsTakenToday: Boolean) {
    val days = listOf("M", "T", "W", "T", "F", "S", "S")
    val todayIdx = Calendar.getInstance().get(Calendar.DAY_OF_WEEK).let { if (it == Calendar.SUNDAY) 6 else it - 2 }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            days.forEachIndexed { idx, day ->
                val isToday = idx == todayIdx
                val circleColor = when {
                    isToday && allPillsTakenToday -> SecondaryAqua
                    isToday -> PrimaryNeon
                    else -> Color.Transparent
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(circleColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = day,
                            fontWeight = FontWeight.Bold,
                            color = if (isToday) Color.White else Color.Black
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FloatingControlHub(onClick: () -> Unit, modifier: Modifier) {
    Box(
        modifier = modifier
            .padding(bottom = 32.dp)
            .shadow(16.dp, RoundedCornerShape(32.dp))
            .clip(RoundedCornerShape(32.dp))
            .background(Color.Black)
            .clickable { onClick() }
            .padding(horizontal = 26.dp, vertical = 18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Add, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(10.dp))
            Text("Add Supplement", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSupplementSheet(onAdd: (Supplement) -> Unit) {
    var name by remember { mutableStateOf("") }
    var value by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("mg") }
    var target by remember { mutableStateOf("1") }
    var selectedEmoji by remember { mutableStateOf("💊") }
    var selectedTiming by remember { mutableStateOf(DayPartition.MORNING) }

    val units = listOf("mg", "pills", "IU", "g", "mcg")
    val emojis = listOf("💊", "🌿", "☀️", "🐟", "⚡", "🦠", "💪", "🧠", "🌙", "💧", "🧪", "🧡")

    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(onTap = { focusManager.clearFocus() })
            }
            .padding(horizontal = 24.dp)
            .padding(bottom = 40.dp)
            .navigationBarsPadding()
    ) {
        Text("Add Supplement 🔬", fontSize = 24.sp, fontWeight = FontWeight.Black)
        Spacer(modifier = Modifier.height(18.dp))

        LazyVerticalGrid(columns = GridCells.Fixed(6), verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.heightIn(max = 120.dp)) {
            items(emojis) { emoji ->
                val isSelected = selectedEmoji == emoji
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isSelected) PrimaryNeon else AppBackground)
                        .clickable {
                            focusManager.clearFocus()
                            selectedEmoji = emoji
                        },
                    contentAlignment = Alignment.Center
                ) { Text(emoji, fontSize = 24.sp) }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Supplement Name") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
        )
        Spacer(modifier = Modifier.height(14.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text("Dose Value") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                singleLine = true
            )

            var dropExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = dropExpanded,
                onExpandedChange = {
                    focusManager.clearFocus()
                    dropExpanded = !dropExpanded
                },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = unit,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Unit") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropExpanded) },
                    modifier = Modifier.menuAnchor(),
                    shape = RoundedCornerShape(16.dp)
                )
                ExposedDropdownMenu(expanded = dropExpanded, onDismissRequest = { dropExpanded = false }) {
                    units.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                unit = option
                                dropExpanded = false
                            }
                        )
                    }
                }
            }
        }

        if (unit == "pills") {
            Spacer(modifier = Modifier.height(14.dp))
            OutlinedTextField(
                value = target,
                onValueChange = { target = it },
                label = { Text("Pill Count Target") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                singleLine = true,
                keyboardActions = KeyboardActions(
                    onDone = { focusManager.clearFocus() }
                )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            FilterChip(
                selected = selectedTiming == DayPartition.MORNING,
                onClick = {
                    focusManager.clearFocus()
                    selectedTiming = DayPartition.MORNING
                },
                label = { Text("Morning Stack") }
            )
            FilterChip(
                selected = selectedTiming == DayPartition.EVENING,
                onClick = {
                    focusManager.clearFocus()
                    selectedTiming = DayPartition.EVENING
                },
                label = { Text("Evening Stack") }
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = {
                if (name.isNotBlank() && value.isNotBlank()) {
                    focusManager.clearFocus()
                    onAdd(Supplement(name = name, emoji = selectedEmoji, doseValue = value, doseUnit = unit, targetDoses = if (unit == "pills") target.toIntOrNull() ?: 1 else 1, timeOfDay = selectedTiming))
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Text("Add to Stack", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditSupplementDialog(supplement: Supplement, onDismiss: () -> Unit, onSave: (Supplement) -> Unit, onDeleteTrigger: () -> Unit) {
    var name by remember { mutableStateOf(supplement.name) }
    var value by remember { mutableStateOf(supplement.doseValue) }
    var unit by remember { mutableStateOf(supplement.doseUnit) }
    var target by remember { mutableStateOf(supplement.targetDoses.toString()) }
    var partition by remember { mutableStateOf(supplement.timeOfDay) }

    val units = listOf("mg", "pills", "IU", "g", "mcg")

    val focusManager = LocalFocusManager.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Supplement ✏️", fontWeight = FontWeight.Black) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.pointerInput(Unit) {
                    detectTapGestures(onTap = { focusManager.clearFocus() })
                }
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Update Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = value,
                        onValueChange = { value = it },
                        label = { Text("Value") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        singleLine = true
                    )

                    var dropExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = dropExpanded,
                        onExpandedChange = {
                            focusManager.clearFocus()
                            dropExpanded = !dropExpanded
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = unit,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Unit") },
                            modifier = Modifier.menuAnchor(),
                            shape = RoundedCornerShape(16.dp)
                        )
                        ExposedDropdownMenu(expanded = dropExpanded, onDismissRequest = { dropExpanded = false }) {
                            units.forEach { u ->
                                DropdownMenuItem(
                                    text = { Text(u) },
                                    onClick = {
                                        unit = u
                                        dropExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                if (unit == "pills") {
                    OutlinedTextField(
                        value = target,
                        onValueChange = { target = it },
                        label = { Text("Update Target Count") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                        singleLine = true,
                        keyboardActions = KeyboardActions(
                            onDone = { focusManager.clearFocus() }
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                focusManager.clearFocus()
                onSave(supplement.copy(name = name, doseValue = value, doseUnit = unit, targetDoses = if (unit == "pills") target.toIntOrNull() ?: 1 else 1, timeOfDay = partition))
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = {
                focusManager.clearFocus()
                onDeleteTrigger()
            }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
        }
    )
}

@Composable
fun EmptyStateView() {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(110.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(AccentPink.copy(0.12f)),
            contentAlignment = Alignment.Center
        ) { Text("🏜️", fontSize = 54.sp) }
        Spacer(modifier = Modifier.height(24.dp))
        Text("Stack is empty!", fontWeight = FontWeight.Black, fontSize = 22.sp)
        Text("Add below to track your supplements.", color = Color.Gray, fontSize = 14.sp)
    }
}

@Composable
fun DropZonePlaceholder(txt: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(Color.White.copy(0.5f)),
        contentAlignment = Alignment.Center
    ) { Text(txt, color = Color.Gray, fontSize = 14.sp) }
}

fun getCurrentDateString(): String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())