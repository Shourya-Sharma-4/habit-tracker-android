package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Habit
import com.example.data.model.HabitCompletion
import com.example.ui.theme.*
import com.example.ui.viewmodel.HabitViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
    viewModel: HabitViewModel,
    modifier: Modifier = Modifier
) {
    val habits by viewModel.habits.collectAsStateWithLifecycle()
    val completions by viewModel.completions.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val studentModeOnly by viewModel.studentModeOnly.collectAsStateWithLifecycle()
    val coachAdvice by viewModel.coachAdvice.collectAsStateWithLifecycle()
    val isCoachLoading by viewModel.isCoachLoading.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(0) } // 0: Dashboard, 1: Calendar & History, 2: Analytics & Achievements

    // Add / Edit Habit states
    var showAddDialog by remember { mutableStateOf(false) }
    var habitToEdit by remember { mutableStateOf<Habit?>(null) }
    
    // Log Miss structures
    var showMissDialog by remember { mutableStateOf<Habit?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = EmeraldAccent,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Habit Tracker",
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif,
                            letterSpacing = 0.5.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    // Quick student mode toggle
                    FilterChip(
                        selected = studentModeOnly,
                        onClick = { viewModel.toggleStudentMode(!studentModeOnly) },
                        label = { Text("🎓 Student Mode") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = IndigoAccent.copy(alpha = 0.2f),
                            selectedLabelColor = IndigoAccent
                        )
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = EmeraldAccent,
                contentColor = Color.Black,
                modifier = Modifier
                    .testTag("add_habit_fab")
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Habit")
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Dashboard") },
                    label = { Text("Dashboard") }
                )
                NavigationBarItem(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    icon = { Icon(Icons.Default.DateRange, contentDescription = "Calendar") },
                    label = { Text("Calendar") }
                )
                NavigationBarItem(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    icon = { Icon(Icons.Default.Star, contentDescription = "Analytics") },
                    label = { Text("Stats & Badges") }
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        // Constrain width on wider screens for adaptive fluid design
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 640.dp)
                    .align(Alignment.TopCenter)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Shared context: Display "Never Miss Twice" warning & "Streak Recovery" if applicable
                NeverMissTwiceWarning(viewModel = viewModel, habits = habits, completions = completions)
                StreakRecoveryAdvice(viewModel = viewModel)

                Spacer(modifier = Modifier.height(12.dp))

                when (activeTab) {
                    0 -> DashboardTab(
                        viewModel = viewModel,
                        habits = habits,
                        completions = completions,
                        selectedCategory = selectedCategory,
                        coachAdvice = coachAdvice,
                        isCoachLoading = isCoachLoading,
                        onEditHabit = { habitToEdit = it },
                        onLogMiss = { showMissDialog = it }
                    )
                    1 -> CalendarTab(
                        viewModel = viewModel,
                        habits = habits,
                        completions = completions,
                        selectedDate = selectedDate
                    )
                    2 -> AnalyticsTab(
                        viewModel = viewModel,
                        habits = habits,
                        completions = completions
                    )
                }
                
                Spacer(modifier = Modifier.height(80.dp)) // padding for FAB
            }
        }
    }

    // Modal forms and dialogs
    if (showAddDialog) {
        AddEditHabitDialog(
            habit = null,
            onDismiss = { showAddDialog = false },
            onSave = { name, cat, diff, isStudent ->
                viewModel.addHabit(name, cat, diff, isStudent)
                showAddDialog = false
            }
        )
    }

    habitToEdit?.let { habit ->
        AddEditHabitDialog(
            habit = habit,
            onDismiss = { habitToEdit = null },
            onSave = { name, cat, diff, isStudent ->
                viewModel.updateHabit(habit.copy(name = name, category = cat, difficulty = diff, isStudentMode = isStudent))
                habitToEdit = null
            }
        )
    }

    showMissDialog?.let { habit ->
        MissReasonDialog(
            habit = habit,
            dateString = selectedDate,
            onDismiss = { showMissDialog = null },
            onLogMiss = { reason, notes ->
                viewModel.logHabitMiss(habit.id, selectedDate, reason, notes)
                showMissDialog = null
            }
        )
    }
}

// ==========================================
// SHARED SYSTEMS: "Never Miss Twice" & "Recovery Engine"
// ==========================================

@Composable
fun NeverMissTwiceWarning(
    viewModel: HabitViewModel,
    habits: List<Habit>,
    completions: List<HabitCompletion>
) {
    val yesterday = viewModel.getYesterdayDateString()
    val today = viewModel.getTodayDateString()

    // Find in database if any habit was completed on yesterday
    // A habit was missed yesterday if there is a miss log or NO completion log of type complete
    val completedYesterdayIds = completions.filter { it.dateString == yesterday && !it.isMissed }.map { it.habitId }.toSet()
    val missedYesterdayHabits = habits.filter { h -> h.id !in completedYesterdayIds }

    // If today is also missed or not completed yet, show Warning
    val completedTodayIds = completions.filter { it.dateString == today && !it.isMissed }.map { it.habitId }.toSet()
    val criticalMisses = missedYesterdayHabits.filter { it.id !in completedTodayIds }

    if (criticalMisses.isNotEmpty()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .border(2.dp, CoralAccent.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = CoralAccent.copy(alpha = 0.08f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(CoralAccent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Critical Warning",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "NEVER MISS TWICE!",
                        fontWeight = FontWeight.Bold,
                        color = CoralAccent,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "You missed yesterday. Complete today's target to protect your growth! Critical items: ${criticalMisses.joinToString { it.name }}",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun StreakRecoveryAdvice(viewModel: HabitViewModel) {
    val brokenHabit = viewModel.getStreakRecoveryHabit() ?: return

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = IndigoAccent.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(IndigoAccent, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Streak Recovery",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "RECOVERY PROTOCOL ACTIVATED",
                    fontWeight = FontWeight.Bold,
                    color = IndigoAccent,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Your streak of ${brokenHabit.longestStreak} days on '${brokenHabit.name}' broke. Resetting is normal. Action breeds motivation—spend just 3 minutes working on it right now!",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

// ==========================================
// TAB 0: DASHBOARD TAB
// ==========================================

@Composable
fun DashboardTab(
    viewModel: HabitViewModel,
    habits: List<Habit>,
    completions: List<HabitCompletion>,
    selectedCategory: String?,
    coachAdvice: String,
    isCoachLoading: Boolean,
    onEditHabit: (Habit) -> Unit,
    onLogMiss: (Habit) -> Unit
) {
    // 1. Dynamic Statistics Overview Row
    DashboardStatsRow(viewModel = viewModel)

    Spacer(modifier = Modifier.height(16.dp))

    // 2. AI Coach Socrates Card
    SocratesCoachCard(
        advice = coachAdvice,
        isLoading = isCoachLoading,
        onRefresh = { viewModel.refreshCoachAdvice() }
    )

    Spacer(modifier = Modifier.height(20.dp))

    // Student Productivity Fast Preset chips
    StudentPresetChips(viewModel = viewModel)

    Spacer(modifier = Modifier.height(16.dp))

    // 3. Category Filter list
    Text(
        text = "Categories",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 8.dp)
    )
    val categories = listOf("Coding", "Study", "Fitness", "Health", "Reading", "Personal Development")
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        item {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { viewModel.selectCategory(null) },
                label = { Text("All") }
            )
        }
        items(categories) { cat ->
            val color = getCategoryColor(cat)
            FilterChip(
                selected = selectedCategory == cat,
                onClick = { viewModel.selectCategory(cat) },
                label = { Text(cat) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = color.copy(alpha = 0.2f),
                    selectedLabelColor = color
                )
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // 4. Habits List for TODAY
    val filteredHabits = habits.filter { habit ->
        (selectedCategory == null || habit.category == selectedCategory) &&
        (!viewModel.studentModeOnly.value || habit.isStudentMode)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Today's Agenda (${filteredHabits.size})",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
    }

    if (filteredHabits.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 40.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "No habits defined here yet.",
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Press the (+) FAB button to create your first habit!",
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            filteredHabits.forEach { habit ->
                val todayDate = viewModel.getTodayDateString()
                val currentRecord = completions.find { it.habitId == habit.id && it.dateString == todayDate }
                val isCompleted = currentRecord != null && !currentRecord.isMissed
                val isFlaggedMissed = currentRecord != null && currentRecord.isMissed

                HabitItemRow(
                    habit = habit,
                    isCompleted = isCompleted,
                    isFlaggedMissed = isFlaggedMissed,
                    missReason = currentRecord?.missReason,
                    onToggleComplete = { viewModel.toggleHabitCompletion(habit.id, todayDate) },
                    onFlagMiss = { onLogMiss(habit) },
                    onEdit = { onEditHabit(habit) },
                    onDelete = { viewModel.deleteHabit(habit) }
                )
            }
        }
    }
}

@Composable
fun DashboardStatsRow(viewModel: HabitViewModel) {
    val totalCount = viewModel.habits.value.size
    val completedCount = viewModel.getCompletionsTodayCount()
    val consistencyScore = viewModel.getConsistencyScore()
    val currentStreak = viewModel.getCurrentStreak()

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Simple Ring gauge for Consistency Score
        Card(
            modifier = Modifier
                .weight(1.3f)
                .height(130.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Custom Canvas Ring
                Box(
                    modifier = Modifier.size(72.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(64.dp)) {
                        drawArc(
                            color = Color.LightGray.copy(alpha = 0.2f),
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = EmeraldAccent,
                            startAngle = -90f,
                            sweepAngle = (consistencyScore.toFloat() / 100f) * 360f,
                            useCenter = false,
                            style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$consistencyScore%",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = EmeraldAccent
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Consistency",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Text(
                        text = when {
                            consistencyScore >= 80 -> "Elite Builder"
                            consistencyScore >= 50 -> "Growing"
                            else -> "Rebooting"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Stats grid
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(61.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Today's", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    Text("$completedCount / $totalCount Done", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = EmeraldAccent)
                }
            }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(61.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Top Streak", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Favorite, contentDescription = null, tint = CoralAccent, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("$currentStreak Days", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun SocratesCoachCard(
    advice: String,
    isLoading: Boolean,
    onRefresh: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(EmeraldAccent, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Face,
                            contentDescription = "Coach Socrates",
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Socrates AI Coach",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                IconButton(
                    onClick = { onRefresh() },
                    enabled = !isLoading,
                    modifier = Modifier.size(24.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Coach",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            AnimatedContent(
                targetState = advice,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                }
            ) { text ->
                Text(
                    text = if (text.isEmpty()) "Click refresh to have Socrates analyze your current consistency and streaks." else text,
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun StudentPresetChips(viewModel: HabitViewModel) {
    Text(
        text = "🎯 Student Fast-Presets",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 6.dp)
    )
    val presets = listOf(
        Pair("LeetCode Practice", "Coding"),
        Pair("DSA Focus Study", "Study"),
        Pair("Read 15 Pages", "Reading"),
        Pair("Compose Project Build", "Coding"),
        Pair("Python Scripting", "Coding"),
        Pair("30 Min Workout", "Fitness"),
        Pair("8hr Sleep Target", "Health")
    )
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(presets) { preset ->
            SuggestionChip(
                onClick = {
                    viewModel.addHabit(
                        name = preset.first,
                        category = preset.second,
                        difficulty = "Medium",
                        isStudentMode = true
                    )
                },
                label = { Text(preset.first) },
                icon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(12.dp)) }
            )
        }
    }
}

@Composable
fun HabitItemRow(
    habit: Habit,
    isCompleted: Boolean,
    isFlaggedMissed: Boolean,
    missReason: String?,
    onToggleComplete: () -> Unit,
    onFlagMiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val categoryColor = getCategoryColor(habit.category)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("habit_item_${habit.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isCompleted -> EmeraldAccent.copy(alpha = 0.08f)
                isFlaggedMissed -> CoralAccent.copy(alpha = 0.05f)
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category color visual sidebar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(44.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(categoryColor)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Main Details
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = habit.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isCompleted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (habit.isStudentMode) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(IndigoAccent.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text("🎓 Student", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = IndigoAccent)
                        }
                    }
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        text = habit.category,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = categoryColor
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "•  ${habit.difficulty}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    if (habit.streakCount > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Favorite, contentDescription = null, tint = CoralAccent, modifier = Modifier.size(10.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("${habit.streakCount}d streak", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CoralAccent)
                        }
                    }
                }

                if (isFlaggedMissed) {
                    Text(
                        text = "Missed: $missReason",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CoralAccent,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // Quick Operations Buttons
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                // X button to Flag Missed
                IconButton(
                    onClick = { onFlagMiss() },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (isFlaggedMissed) CoralAccent else Color.Transparent,
                        contentColor = if (isFlaggedMissed) Color.White else CoralAccent.copy(alpha = 0.6f)
                    )
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Log missed", modifier = Modifier.size(18.dp))
                }

                // Check button to Complete
                IconButton(
                    onClick = { onToggleComplete() },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (isCompleted) EmeraldAccent else Color.Transparent,
                        contentColor = if (isCompleted) Color.Black else EmeraldAccent.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.testTag("complete_checkbox_${habit.id}")
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = "Toggle Complete", modifier = Modifier.size(18.dp))
                }

                // Edit/Delete drop context
                Box {
                    IconButton(onClick = { expanded = true }) {
                        Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Menu")
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit Habit") },
                            onClick = {
                                expanded = false
                                onEdit()
                            },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", color = CoralAccent) },
                            onClick = {
                                expanded = false
                                onDelete()
                            },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = CoralAccent) }
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// TAB 1: CALENDAR TAB
// ==========================================

@Composable
fun CalendarTab(
    viewModel: HabitViewModel,
    habits: List<Habit>,
    completions: List<HabitCompletion>,
    selectedDate: String
) {
    val weekDays = viewModel.getWeekDates()

    Text(
        text = "Weekly History Tracker",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 12.dp)
    )

    // Calendar Week Grid Selection Row
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        weekDays.forEach { dateInfo ->
            val isSelected = dateInfo.dateString == selectedDate
            val dayCompletions = completions.filter { it.dateString == dateInfo.dateString }
            val hasCompleted = dayCompletions.any { !it.isMissed }
            val hasMissed = dayCompletions.any { it.isMissed }

            Card(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 2.dp)
                    .clickable { viewModel.selectDate(dateInfo.dateString) },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) EmeraldAccent else MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = dateInfo.dayOfWeek,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = dateInfo.dayNumber,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    // Performance indicators: small progress dot
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        if (hasCompleted) {
                            Box(modifier = Modifier.size(5.dp).background(if (isSelected) Color.Black else EmeraldAccent, CircleShape))
                        }
                        if (hasMissed) {
                            Box(modifier = Modifier.size(5.dp).background(if (isSelected) Color.White else CoralAccent, CircleShape))
                        }
                        if (!hasCompleted && !hasMissed) {
                            Box(modifier = Modifier.size(5.dp).background(Color.Transparent, CircleShape))
                        }
                    }
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // List performance logged for the selected calendar date
    Text(
        text = "Activity Log for $selectedDate",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 8.dp)
    )

    if (habits.isEmpty()) {
        Text("No habits to check.", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            habits.forEach { habit ->
                val record = completions.find { it.habitId == habit.id && it.dateString == selectedDate }
                val completed = record != null && !record.isMissed
                val missed = record != null && record.isMissed

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = habit.name,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        when {
                            completed -> {
                                Box(
                                    modifier = Modifier
                                        .background(EmeraldAccent.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("✓ Completed", fontWeight = FontWeight.Bold, color = EmeraldAccent, fontSize = 11.sp)
                                }
                            }
                            missed -> {
                                Box(
                                    modifier = Modifier
                                        .background(CoralAccent.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("✗ Miss: ${record.missReason}", fontWeight = FontWeight.Bold, color = CoralAccent, fontSize = 11.sp)
                                }
                            }
                            else -> {
                                Box(
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("Unlogged", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// TAB 2: ANALYTICS & ACHIEVEMENTS TAB
// ==========================================

@Composable
fun AnalyticsTab(
    viewModel: HabitViewModel,
    habits: List<Habit>,
    completions: List<HabitCompletion>
) {
    Text(
        text = "Analytics Reports",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 12.dp)
    )

    // 1. Completion Chart (Horizontal dynamic Bar chart drawn inside custom Canvas)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Habit Completion Distribution", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text("Number of successes scored in history", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            
            Spacer(modifier = Modifier.height(16.dp))

            if (habits.isEmpty()) {
                Text("No data to plot.", modifier = Modifier.align(Alignment.CenterHorizontally))
            } else {
                habits.forEach { habit ->
                    val successCount = completions.count { it.habitId == habit.id && !it.isMissed }
                    val progressRatio = if (completions.isEmpty()) 0f else (successCount.toFloat() / maxOf(1f, completions.count { !it.isMissed }.toFloat()))
                    val categoryColor = getCategoryColor(habit.category)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = habit.name,
                            modifier = Modifier.width(90.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))

                        // Custom canvas bar
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(16.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.background)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(if (progressRatio > 0) progressRatio else 0.05f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(categoryColor)
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Text(
                            text = "$successCount",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = categoryColor,
                            modifier = Modifier.width(20.dp)
                        )
                    }
                }
            }
        }
    }

    // 2. Missed Reason breakdown
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Friction Analysis (Why Missed?)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text("Identifying pain points in building consistency", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))

            Spacer(modifier = Modifier.height(16.dp))

            val missLogs = completions.filter { it.isMissed }
            if (missLogs.isEmpty()) {
                Text("Excellent! No logged misses registered in your log history yet.", color = EmeraldAccent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            } else {
                val reasons = listOf("Procrastination", "Busy Schedule", "Lack of Motivation", "Forgot", "Sick", "Other")
                reasons.forEach { r ->
                    val count = missLogs.count { it.missReason == r }
                    val totalMisses = missLogs.size
                    val percentRatio = if (totalMisses > 0) (count.toFloat() / totalMisses.toFloat()) else 0f

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(r, fontSize = 11.sp, modifier = Modifier.width(110.dp), fontWeight = FontWeight.SemiBold)
                        
                        LinearProgressIndicator(
                            progress = { percentRatio },
                            modifier = Modifier
                                .weight(1f)
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp)),
                            color = CoralAccent,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("${(percentRatio * 100).toInt()}% ($count)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // 3. Achievements Badge Grid
    Text(
        text = "Unlocked Achievements",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 12.dp)
    )

    val achievementList = viewModel.getAchievements()
    achievementList.forEach { badge ->
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (badge.isUnlocked) EmeraldAccent.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
            )
        ) {
            Row(
                modifier = Modifier
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            if (badge.isUnlocked) EmeraldAccent.copy(alpha = 0.2f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (badge.isUnlocked) badge.icon else "🔒",
                        fontSize = 20.sp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = badge.title,
                        fontWeight = FontWeight.Bold,
                        color = if (badge.isUnlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Text(
                        text = badge.description,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                if (badge.isUnlocked) {
                    Box(
                        modifier = Modifier
                            .background(EmeraldAccent, RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("ACTIVE", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black)
                    }
                }
            }
        }
    }
}

// ==========================================
// FORM DIALOGS: ADD / EDIT & LOG MISS REASON
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditHabitDialog(
    habit: Habit?,
    onDismiss: () -> Unit,
    onSave: (name: String, category: String, difficulty: String, isStudentMode: Boolean) -> Unit
) {
    var name by remember { mutableStateOf(habit?.name ?: "") }
    var category by remember { mutableStateOf(habit?.category ?: "Coding") }
    var difficulty by remember { mutableStateOf(habit?.difficulty ?: "Medium") }
    var isStudent by remember { mutableStateOf(habit?.isStudentMode ?: false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (habit == null) "Create Habit" else "Edit Habit",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("dialog_title")
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Habit Target Name") },
                    placeholder = { Text("e.g. Solve 3 LeetCode, Gym, Read Docs") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("habit_name_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                // Category dropdown setup
                Text("Select Category", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                val categories = listOf("Coding", "Study", "Fitness", "Health", "Reading", "Personal Development")
                
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = getCategoryColor(cat).copy(alpha = 0.2f),
                                selectedLabelColor = getCategoryColor(cat)
                            )
                        )
                    }
                }

                // Difficulty selector
                Text("Target Difficulty", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Easy", "Medium", "Hard").forEach { diff ->
                        FilterChip(
                            selected = difficulty == diff,
                            onClick = { difficulty = diff },
                            label = { Text(diff) }
                        )
                    }
                }

                // Student mode toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Student Focus Mode", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Pre-loads student goals in dashboard", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                    Switch(checked = isStudent, onCheckedChange = { isStudent = it })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                onSave(name, category, difficulty, isStudent)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent, contentColor = Color.Black),
                        modifier = Modifier.testTag("save_habit_button")
                    ) {
                        Text("Save Habit")
                    }
                }
            }
        }
    }
}

@Composable
fun MissReasonDialog(
    habit: Habit,
    dateString: String,
    onDismiss: () -> Unit,
    onLogMiss: (reason: String, notes: String?) -> Unit
) {
    var selectedReason by remember { mutableStateOf("Procrastination") }
    var notes by remember { mutableStateOf("") }
    val reasons = listOf("Procrastination", "Busy Schedule", "Lack of Motivation", "Forgot", "Sick", "Other")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Why did you miss '${habit.name}' today?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = CoralAccent
                )

                Text(
                    text = "Analyzing your specific friction points helps of AI Coach build better actionable solutions.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                // Render Reason buttons list
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    reasons.forEach { r ->
                        val isSel = selectedReason == r
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedReason = r },
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSel) CoralAccent.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                                contentColor = if (isSel) CoralAccent else MaterialTheme.colorScheme.onSurface
                            ),
                            border = BorderStroke(1.dp, if (isSel) CoralAccent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isSel) Icons.Default.Check else Icons.Outlined.Info,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(r, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Optional Notes (How to avoid next?)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 2
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onLogMiss(selectedReason, notes.ifBlank { null }) },
                        colors = ButtonDefaults.buttonColors(containerColor = CoralAccent, contentColor = Color.White)
                    ) {
                        Text("Log Miss")
                    }
                }
            }
        }
    }
}

// Helper to resolve stylized colors and icons for categories
fun getCategoryColor(category: String): Color {
    return when (category) {
        "Coding" -> IndigoAccent
        "Study" -> OrangeAccent
        "Fitness" -> SkyAccent
        "Health" -> EmeraldAccent
        "Reading" -> YellowAccent
        "Personal Development" -> VioletAccent
        else -> IndigoAccent
    }
}
