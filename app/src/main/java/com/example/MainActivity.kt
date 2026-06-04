package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.layout.ContentScale
import coil.compose.rememberAsyncImagePainter
import com.example.api.FoodRecognitionResult
import com.example.data.local.AppDatabase
import com.example.data.model.DailyGoal
import com.example.data.model.FoodEntry
import com.example.data.repository.FoodRepository
import com.example.ui.theme.*
import com.example.ui.viewmodel.CalorieViewModel
import com.example.ui.viewmodel.DailyTotals
import com.example.ui.viewmodel.ChartDay
import com.example.ui.viewmodel.ScanningState
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

enum class AppTab {
    Diary, Scanner, Analytics
}

class CalorieViewModelFactory(private val repository: FoodRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CalorieViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CalorieViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getDatabase(applicationContext)
        val repository = FoodRepository(database.foodDao())
        val factory = CalorieViewModelFactory(repository)
        val viewModel = ViewModelProvider(this, factory)[CalorieViewModel::class.java]

        setContent {
            MyApplicationTheme {
                MainAppScreen(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(viewModel: CalorieViewModel) {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf(AppTab.Diary) }

    // State bindings
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val dailyGoal by viewModel.selectedDateGoal.collectAsStateWithLifecycle()
    val totals by viewModel.selectedDateTotals.collectAsStateWithLifecycle()
    val streakCount by viewModel.streakCount.collectAsStateWithLifecycle()
    val scanningState by viewModel.scanningState.collectAsStateWithLifecycle()
    val weeklyHistory by viewModel.weeklyHistory.collectAsStateWithLifecycle()
    val foodEntries by viewModel.selectedDateFoodEntries.collectAsStateWithLifecycle()

    // Modals visibility
    var showManualAddDialog by remember { mutableStateOf(false) }
    var showGoalSettingsByDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DeepOnyxBg,
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = DeepOnyxBg,
                    titleContentColor = TextPrimary
                ),
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // User Avatar matching JD in the design HTML
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(EmeraldPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "JD",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "GOOD MORNING",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Alex Rivera",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = TextPrimary
                            )
                        }
                    }
                },
                actions = {
                    // Elevated Streak micro widget badge matching Design HTML
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(LightStreakBg)
                            .border(1.dp, StreakBorderColor, RoundedCornerShape(20.dp))
                            .clickable {
                                Toast
                                    .makeText(
                                        context,
                                        "You have logged $streakCount days consecutively!",
                                        Toast.LENGTH_SHORT
                                    )
                                    .show()
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "🔥",
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$streakCount Day Streak",
                            color = EmeraldPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = ContentSurface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = currentTab == AppTab.Diary,
                    onClick = { currentTab = AppTab.Diary },
                    label = { Text("Home", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home Tab") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = EmeraldPrimary,
                        selectedTextColor = EmeraldPrimary,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary,
                        indicatorColor = ActivePillBg
                    )
                )
                NavigationBarItem(
                    selected = currentTab == AppTab.Scanner,
                    onClick = { currentTab = AppTab.Scanner },
                    label = { Text("Diary", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                    icon = { Icon(Icons.Default.Book, contentDescription = "Diary Tab") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = EmeraldPrimary,
                        selectedTextColor = EmeraldPrimary,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary,
                        indicatorColor = ActivePillBg
                    )
                )
                NavigationBarItem(
                    selected = currentTab == AppTab.Analytics,
                    onClick = { currentTab = AppTab.Analytics },
                    label = { Text("Stats", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                    icon = { Icon(Icons.Default.Leaderboard, contentDescription = "Stats Tab") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = EmeraldPrimary,
                        selectedTextColor = EmeraldPrimary,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary,
                        indicatorColor = ActivePillBg
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(DeepOnyxBg)
        ) {
            when (currentTab) {
                AppTab.Diary -> {
                    DiaryView(
                        selectedDate = selectedDate,
                        totals = totals,
                        goal = dailyGoal,
                        entries = foodEntries,
                        viewModel = viewModel,
                        onAddManualClick = { showManualAddDialog = true },
                        onGoToScannerClick = { currentTab = AppTab.Scanner }
                    )
                }
                AppTab.Scanner -> {
                    ScannerView(
                        state = scanningState,
                        viewModel = viewModel,
                        onLoggedMeal = {
                            currentTab = AppTab.Diary
                            Toast.makeText(context, "Logged scanning meal to diary!", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
                AppTab.Analytics -> {
                    AnalyticsView(
                        weeklyHistory = weeklyHistory,
                        totals = totals,
                        goal = dailyGoal,
                        streak = streakCount,
                        onAdjustGoalsClick = { showGoalSettingsByDialog = true }
                    )
                }
            }

            // High priority warning caution notice at base for prototype integrity
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp, start = 16.dp, end = 16.dp)
            ) {
                // Background caution anchor text is muted
            }
        }
    }

    // Modal dialogs
    if (showManualAddDialog) {
        ManualAddMealDialog(
            onDismiss = { showManualAddDialog = false },
            onSave = { name, kcal, prot, carbs, fat, meal ->
                viewModel.addManualFood(name, kcal, prot, carbs, fat, meal)
                showManualAddDialog = false
                Toast.makeText(context, "Logged custom meal successfully!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showGoalSettingsByDialog) {
        GoalSettingsDialog(
            activeGoal = dailyGoal,
            onDismiss = { showGoalSettingsByDialog = false },
            onSave = { kcal, prot, carbs, fat ->
                viewModel.adjustDailyCustomGoals(kcal, prot, carbs, fat)
                showGoalSettingsByDialog = false
                Toast.makeText(context, "Updated daily custom goals!", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

// ==================== TABS VIEWS IMPLEMENTATIONS ====================

@Composable
fun DiaryView(
    selectedDate: String,
    totals: DailyTotals,
    goal: DailyGoal,
    entries: List<FoodEntry>,
    viewModel: CalorieViewModel,
    onAddManualClick: () -> Unit,
    onGoToScannerClick: () -> Unit
) {
    val remainingKcal = (goal.calorieGoal - totals.calories).coerceAtLeast(0.0)
    val capPercent = if (goal.calorieGoal > 0) (totals.calories / goal.calorieGoal).toFloat() else 0f

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
    ) {
        // Date switcher bar
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(ContentSurface)
                    .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                    .padding(vertical = 6.dp, horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = { viewModel.selectOffsetDays(-1) }) {
                    Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Previous Day", tint = TextPrimary)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = formatHumanFriendlyDate(selectedDate),
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 15.sp
                    )
                }
                IconButton(onClick = { viewModel.selectOffsetDays(1) }) {
                    Icon(Icons.Default.ArrowForwardIos, contentDescription = "Next Day", tint = TextPrimary)
                }
            }
        }

        // Calorie Progress overview dashboard card with 1:1 circle progress ring layout
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ContentSurface),
                shape = RoundedCornerShape(32.dp),
                border = BorderStroke(1.dp, CardBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "DAILY CALORIES",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Text(
                                    text = String.format("%,d", totals.calories.toInt()),
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = String.format("/ %,d kcal", goal.calorieGoal.toInt()),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextSecondary
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(EmeraldPrimary)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${remainingKcal.toInt()} kcal remaining",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary
                                )
                            }
                        }

                        // Circular Progress Ring Graphic matching Design HTML
                        Box(
                            modifier = Modifier.size(72.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val sweepAngle = (capPercent * 360f).coerceIn(0f, 360f)
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val strokeW = 6.dp.toPx()
                                // Background circle ring
                                drawCircle(
                                    color = Color(0xFFF3F3F3),
                                    radius = size.minDimension / 2 - strokeW / 2,
                                    style = Stroke(width = strokeW)
                                )
                                // Active color progress arc
                                drawArc(
                                    color = EmeraldPrimary,
                                    startAngle = -90f,
                                    sweepAngle = sweepAngle,
                                    useCenter = false,
                                    style = Stroke(width = strokeW, cap = StrokeCap.Round)
                                )
                            }
                            Text(
                                text = "${(capPercent * 100).toInt()}%",
                                color = EmeraldPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Horizontal sub-progress macros indicators styled in matching container panels
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MacroBarIndicator(
                            label = "Protein",
                            current = totals.protein,
                            target = goal.proteinGoal,
                            color = MacroProtein
                        )
                        MacroBarIndicator(
                            label = "Carbs",
                            current = totals.carbs,
                            target = goal.carbsGoal,
                            color = MacroCarbs
                        )
                        MacroBarIndicator(
                            label = "Fat",
                            current = totals.fat,
                            target = goal.fatGoal,
                            color = MacroFat
                        )
                    }
                }
            }
        }

        // Lumina Vision AI Custom Scanner Button matching the mockup's high-contrast charcoal banner
        item {
            Card(
                onClick = onGoToScannerClick,
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = ScannerDarkBg),
                shape = RoundedCornerShape(32.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp, horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(ScannerCircleAccent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = "Camera Icon",
                            tint = ScannerDarkBg,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Lumina Vision AI",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Snap a photo to scan macros instantly",
                            color = ScannerCircleAccent,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Secondary custom manual logging shortcut
        item {
            OutlinedButton(
                onClick = onAddManualClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, CardBorder),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = ContentSurface,
                    contentColor = TextSecondary
                ),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Manual logging icon",
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Manual Log Custom Meal",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }

        // Meal categories sectioning
        val meals = listOf("Breakfast", "Lunch", "Dinner", "Snack")
        meals.forEach { mealType ->
            val mealEntries = entries.filter { it.mealType == mealType }
            val mealCalories = mealEntries.sumOf { it.calories }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when (mealType) {
                            "Breakfast" -> "🍳 Breakfast"
                            "Lunch" -> "🥪 Lunch"
                            "Dinner" -> "🥩 Dinner"
                            else -> "🍎 Snacks"
                        },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "${mealCalories.toInt()} kcal",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary
                    )
                }
            }

            if (mealEntries.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                CardBorder,
                                RoundedCornerShape(12.dp)
                            )
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No logs logged in $mealType. Tap to add.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(mealEntries, key = { it.id }) { entry ->
                    FoodEntryRowItem(
                        entry = entry,
                        onDeleteClick = { viewModel.deleteFoodEntry(entry) }
                    )
                }
            }
        }
    }
}

fun formatTimestampToHour(timestamp: Long): String {
    return try {
        val date = java.util.Date(timestamp)
        val formatter = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
        formatter.format(date).uppercase()
    } catch (e: Exception) {
        "12:00 PM"
    }
}

@Composable
fun FoodEntryRowItem(entry: FoodEntry, onDeleteClick: () -> Unit) {
    val helperHour = formatTimestampToHour(entry.timestamp)
    val emojiChar = when (entry.mealType) {
        "Breakfast" -> "🍳"
        "Lunch" -> "🥗"
        "Dinner" -> "🥩"
        else -> "🍎"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { },
        colors = CardDefaults.cardColors(containerColor = ContentSurface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Emoji Container matching Design HTML
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF3E7FF)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = emojiChar, fontSize = 20.sp)
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = entry.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${entry.mealType.uppercase()} • $helperHour",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        MacroMicroPill(text = "P: ${entry.protein.toInt()}g", color = MacroProtein)
                        MacroMicroPill(text = "C: ${entry.carbs.toInt()}g", color = MacroCarbs)
                        MacroMicroPill(text = "F: ${entry.fat.toInt()}g", color = MacroFat)
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "${entry.calories.toInt()} kcal",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                val isAi = entry.imageUri != null && entry.imageUri.isNotEmpty()
                Text(
                    text = if (isAi) "AI ANALYZED" else "MANUAL",
                    color = if (isAi) Color(0xFF059669) else TextMuted,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 9.sp,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete Item",
                        tint = TextMuted.copy(alpha = 0.8f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// Tab 2: Scanner (Aura AI Nutritionist Assistant)
@Composable
fun ScannerView(
    state: ScanningState,
    viewModel: CalorieViewModel,
    onLoggedMeal: () -> Unit
) {
    val context = LocalContext.current

    // Set up standard non-permitted pickers
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                viewModel.startFoodAnalysis(bitmap, null)
            } catch (e: Exception) {
                Toast.makeText(context, "Error opening library image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            viewModel.startFoodAnalysis(bitmap, null)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        when (state) {
            is ScanningState.Idle -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Header text
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = "AURA NUTRITION SCANNER",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary,
                            letterSpacing = 1.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Photograph Meal to Track",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Analyze nutrition details instantaneously with white-label AI Engine.",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }

                    // Huge interactive scan card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(ContentSurface)
                            .border(2.dp, CardBorder, RoundedCornerShape(16.dp))
                            .clickable { cameraLauncher.launch() },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Camera placeholder",
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(54.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                "Snap Photo of Your Food",
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Instant visual scan triggers the engine",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Standard Capture Trigger Buttons Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Button(
                            onClick = { cameraLauncher.launch() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = "Camera")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Camera Snap", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { galleryLauncher.launch("image/*") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ContentSurface),
                            border = BorderStroke(1.dp, CardBorder)
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = "Gallery")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Gallery Pic", fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                    }

                    // Custom text descriptors logging backup
                    Text(
                        text = "Or enter a text description directly:",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Start)
                    )

                    var simpleTextQuery by remember { mutableStateOf("") }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextField(
                            value = simpleTextQuery,
                            onValueChange = { simpleTextQuery = it },
                            placeholder = { Text("e.g. 2 fried eggs and toast...", color = TextMuted) },
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp)),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = ContentSurface,
                                unfocusedContainerColor = ContentSurface,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        IconButton(
                            onClick = {
                                if (simpleTextQuery.isNotBlank()) {
                                    viewModel.startFoodAnalysis(null, simpleTextQuery)
                                    simpleTextQuery = ""
                                }
                            },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(EmeraldPrimary)
                                .size(48.dp)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Analyze text description", tint = Color.White)
                        }
                    }

                    // Beautiful preset mockup cards section for bulletproof sandboxed emulators
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "PRESENTATION MOCK CORES (PRESET FASTRACKS)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted,
                            letterSpacing = 1.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            PresetFoodCard(
                                title = "Avocado Toast",
                                emoji = "🥑🍞",
                                calories = "360 kcal",
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    // Simulates an immediate smart load
                                    viewModel.startFoodAnalysis(null, "Avocado bread with eggs")
                                }
                            )

                            PresetFoodCard(
                                title = "Beef Burger",
                                emoji = "🍔🍟",
                                calories = "620 kcal",
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    viewModel.startFoodAnalysis(null, "Cheeseburger bun with beef")
                                }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            PresetFoodCard(
                                title = "Caesar Salad",
                                emoji = "🥗🍗",
                                calories = "420 kcal",
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    viewModel.startFoodAnalysis(null, "Chicken Caesar Salad romaine")
                                }
                            )

                            PresetFoodCard(
                                title = "Salmon Bowl",
                                emoji = "🍣🍚",
                                calories = "540 kcal",
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    viewModel.startFoodAnalysis(null, "Teriyaki salmon rice bowl")
                                }
                            )
                        }
                    }
                }
            }

            is ScanningState.Analyzing -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = EmeraldPrimary,
                        strokeWidth = 5.dp,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "AURA PRO CORE RUNNING",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Analyzing Food Visual Matrix...",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Estimating portion sizes, ingredients, and matching macronutrients. This takes just a moment.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(32.dp))
                    // Motivational nutrition advice rotating inline
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(ContentSurface)
                            .padding(14.dp)
                    ) {
                        Text(
                            text = "💡 Advice: Fiber intake slows sugar absorption, helping protect your bloodstream energy curves uniformly throughout the workday.",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            is ScanningState.Success -> {
                // Let's draw the interactive draft confirmation screen!
                ScannerResultDraftScreen(
                    result = state.result,
                    image = state.image,
                    onSave = { draftName, draftCal, draftProt, draftCarb, draftFat, draftMeal ->
                        viewModel.logScannedFood(
                            name = draftName,
                            calories = draftCal,
                            protein = draftProt,
                            carbs = draftCarb,
                            fat = draftFat,
                            mealType = draftMeal
                        )
                        onLoggedMeal()
                    },
                    onCancel = { viewModel.resetScanner() }
                )
            }

            is ScanningState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Analysis error",
                        tint = MacroFat,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = "Analysis Disrupted",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = state.message,
                        fontSize = 13.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = { viewModel.resetScanner() },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Text("Reset & Choose Again", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun PresetFoodCard(
    title: String,
    emoji: String,
    calories: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = ContentSurface.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(emoji, fontSize = 28.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(title, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(calories, color = EmeraldPrimary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
    }
}

// Scanning validation draft interface (The editable scanned values confirmation screen)
@Composable
fun ScannerResultDraftScreen(
    result: FoodRecognitionResult,
    image: Bitmap?,
    onSave: (String, Double, Double, Double, Double, String) -> Unit,
    onCancel: () -> Unit
) {
    // Scaffold mutable values
    var mealName by remember { mutableStateOf(result.foodName) }
    var itemCalories by remember { mutableStateOf(result.calories.toString()) }
    var itemProtein by remember { mutableStateOf(result.protein.toString()) }
    var itemCarbs by remember { mutableStateOf(result.carbs.toString()) }
    var itemFat by remember { mutableStateOf(result.fat.toString()) }
    var selectedMealType by remember { mutableStateOf("Lunch") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            Text(
                "AI CONFIRMATION DECK",
                fontSize = 11.sp,
                color = EmeraldPrimary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text("Adjust Calculated Portions", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
            Text("The white-labeled Aura Pro Engine identified these core values, adjust if needed before logging.", fontSize = 12.sp, color = TextSecondary)
        }

        // Preview space
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                    .background(ContentSurface),
                contentAlignment = Alignment.Center
            ) {
                if (image != null) {
                    androidx.compose.foundation.Image(
                        painter = rememberAsyncImagePainter(image),
                        contentDescription = "Food Snapshot preview",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Beautiful vector glowing gradient layout
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.radialGradient(
                                    listOf(ContentSurface, DeepOnyxBg)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Photo, "Graphic preview placeholder", tint = EmeraldPrimary, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("AURA VISION PRESET PORTION", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                        }
                    }
                }
            }
        }

        // Custom brief summary descriptors
        if (result.description.isNotBlank()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ContentSurface),
                    border = BorderStroke(0.5.dp, CardBorder)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "IDENTIFIED ELEMENTS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = result.description,
                            color = TextSecondary,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }

        // Editable variables
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ContentSurface),
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = mealName,
                        onValueChange = { mealName = it },
                        label = { Text("Food Name", color = TextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = CardBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = itemCalories,
                            onValueChange = { itemCalories = it },
                            label = { Text("kcal", color = TextSecondary) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = EmeraldPrimary,
                                unfocusedBorderColor = CardBorder
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = itemProtein,
                            onValueChange = { itemProtein = it },
                            label = { Text("Protein (g)", color = TextSecondary) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = EmeraldPrimary,
                                unfocusedBorderColor = CardBorder
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = itemCarbs,
                            onValueChange = { itemCarbs = it },
                            label = { Text("Carbs (g)", color = TextSecondary) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = EmeraldPrimary,
                                unfocusedBorderColor = CardBorder
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = itemFat,
                            onValueChange = { itemFat = it },
                            label = { Text("Fat (g)", color = TextSecondary) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = EmeraldPrimary,
                                unfocusedBorderColor = CardBorder
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Segmented categorization buttons row
                    Column {
                        Text("MEAL SESSION CATEGORY", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val categories = listOf("Breakfast", "Lunch", "Dinner", "Snack")
                            categories.forEach { type ->
                                val selected = selectedMealType == type
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selected) EmeraldPrimary else CardBorder)
                                        .clickable { selectedMealType = type }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = type,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = if (selected) Color.White else TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Action controls
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        val kcalVal = itemCalories.toDoubleOrNull() ?: 0.0
                        val protVal = itemProtein.toDoubleOrNull() ?: 0.0
                        val carbVal = itemCarbs.toDoubleOrNull() ?: 0.0
                        val fatVal = itemFat.toDoubleOrNull() ?: 0.0
                        onSave(mealName, kcalVal, protVal, carbVal, fatVal, selectedMealType)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1.5f)
                ) {
                    Text("Register Calculated Meal", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                Button(
                    onClick = onCancel,
                    colors = ButtonDefaults.buttonColors(containerColor = ContentSurface),
                    border = BorderStroke(1.dp, CardBorder),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Recancel", fontWeight = FontWeight.Bold, color = TextSecondary, fontSize = 14.sp)
                }
            }
        }
    }
}

// Tab 3: Analytics dashboard
@Composable
fun AnalyticsView(
    weeklyHistory: List<ChartDay>,
    totals: DailyTotals,
    goal: DailyGoal,
    streak: Int,
    onAdjustGoalsClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
    ) {
        item {
            Text(
                "ANALYTICS & STATS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = EmeraldPrimary,
                letterSpacing = 1.5.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text("Track Your Journey", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
            Text("Pristine breakdown of logged progress history.", fontSize = 12.sp, color = TextSecondary)
        }

        // 1. Streak dashboard card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ContentSurface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(StreakActiveColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🔥", fontSize = 28.sp)
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (streak > 0) "$streak Day Dynamic Streak!" else "Start Your Log Streak",
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary,
                            fontSize = 17.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (streak > 0) "Keep logging meals daily to protect and advance your streak!" else "Log something today to trigger your consecutive days streak counter.",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        // 2. Concentric Macro Progress Rings Graph
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ContentSurface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        "MACRONUTRIENT BALANCE RINGS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Drawing custom nested concentric rings
                        Box(
                            modifier = Modifier
                                .size(140.dp)
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val pPercent = if (goal.proteinGoal > 0) (totals.protein / goal.proteinGoal).toFloat() else 0f
                            val cPercent = if (goal.carbsGoal > 0) (totals.carbs / goal.carbsGoal).toFloat() else 0f
                            val fPercent = if (goal.fatGoal > 0) (totals.fat / goal.fatGoal).toFloat() else 0f

                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val strokeWidth = 10.dp.toPx()

                                // 1. Protein Ring (Outer)
                                drawCircle(
                                    color = MacroProtein.copy(alpha = 0.1f),
                                    radius = 60.dp.toPx(),
                                    style = Stroke(width = strokeWidth)
                                )
                                drawArc(
                                    color = MacroProtein,
                                    startAngle = -90f,
                                    sweepAngle = (pPercent * 360f).coerceIn(0f, 360f),
                                    useCenter = false,
                                    size = Size(120.dp.toPx(), 120.dp.toPx()),
                                    topLeft = Offset(center.x - 60.dp.toPx(), center.y - 60.dp.toPx()),
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                )

                                // 2. Carbs Ring (Middle)
                                drawCircle(
                                    color = MacroCarbs.copy(alpha = 0.1f),
                                    radius = 44.dp.toPx(),
                                    style = Stroke(width = strokeWidth)
                                )
                                drawArc(
                                    color = MacroCarbs,
                                    startAngle = -90f,
                                    sweepAngle = (cPercent * 360f).coerceIn(0f, 360f),
                                    useCenter = false,
                                    size = Size(88.dp.toPx(), 88.dp.toPx()),
                                    topLeft = Offset(center.x - 44.dp.toPx(), center.y - 44.dp.toPx()),
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                )

                                // 3. Fat Ring (Inner)
                                drawCircle(
                                    color = MacroFat.copy(alpha = 0.1f),
                                    radius = 28.dp.toPx(),
                                    style = Stroke(width = strokeWidth)
                                )
                                drawArc(
                                    color = MacroFat,
                                    startAngle = -90f,
                                    sweepAngle = (fPercent * 360f).coerceIn(0f, 360f),
                                    useCenter = false,
                                    size = Size(56.dp.toPx(), 56.dp.toPx()),
                                    topLeft = Offset(center.x - 28.dp.toPx(), center.y - 28.dp.toPx()),
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                )
                            }

                            // Dynamic remaining calorie metrics in core interior
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                val remaining = (goal.calorieGoal - totals.calories).coerceAtLeast(0.0)
                                Text(
                                    text = "${remaining.toInt()}",
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TextPrimary,
                                    fontSize = 15.sp,
                                    lineHeight = 16.sp
                                )
                                Text(
                                    text = "kcal left",
                                    color = TextMuted,
                                    fontSize = 9.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(20.dp))

                        // Custom dynamic visual indices indicators
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            MacroLegendPill(
                                label = "Protein",
                                value = "${totals.protein.toInt()}/${goal.proteinGoal.toInt()}g",
                                pct = if (goal.proteinGoal > 0) ((totals.protein / goal.proteinGoal) * 100).toInt() else 0,
                                color = MacroProtein
                            )
                            MacroLegendPill(
                                label = "Carbs",
                                value = "${totals.carbs.toInt()}/${goal.carbsGoal.toInt()}g",
                                pct = if (goal.carbsGoal > 0) ((totals.carbs / goal.carbsGoal) * 100).toInt() else 0,
                                color = MacroCarbs
                            )
                            MacroLegendPill(
                                label = "Fats",
                                value = "${totals.fat.toInt()}/${goal.fatGoal.toInt()}g",
                                pct = if (goal.fatGoal > 0) ((totals.fat / goal.fatGoal) * 100).toInt() else 0,
                                color = MacroFat
                            )
                        }
                    }
                }
            }
        }

        // 3. 7-Day History Chart
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ContentSurface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        "CALORIE LOG TRENDS (LAST 7 DAYS)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    if (weeklyHistory.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No history logs found.", color = TextMuted)
                        }
                    } else {
                        // Drawing dynamic bar graph
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            val maxCalLimit = (weeklyHistory.maxOf { it.calories }).coerceAtLeast(100.0)

                            weeklyHistory.forEach { day ->
                                val proportion = (day.calories / maxCalLimit).toFloat().coerceIn(0.04f, 1f)

                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    // Animated indicator tooltip
                                    if (day.calories > 0) {
                                        Text(
                                            "${day.calories.toInt()}",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = EmeraldPrimary
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                    }

                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight(proportion)
                                            .width(16.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (day.calories > day.target) MacroFat
                                                else if (day.calories > 0) EmeraldPrimary
                                                else CardBorder
                                            )
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = day.dayLabel,
                                        fontSize = 10.sp,
                                        color = TextSecondary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Goal Settings triggers
        item {
            Button(
                onClick = onAdjustGoalsClick,
                colors = ButtonDefaults.buttonColors(containerColor = ContentSurface),
                border = BorderStroke(1.dp, CardBorder),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                Icon(Icons.Default.EditAttributes, contentDescription = "Adjust target targets Icon", tint = EmeraldPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Customize Target Goals", fontWeight = FontWeight.Bold, color = TextPrimary)
            }
        }
    }
}

@Composable
fun MacroLegendPill(label: String, value: String, pct: Int, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(value, fontSize = 11.sp, color = TextSecondary)
            Spacer(modifier = Modifier.width(6.dp))
            Text("($pct%)", fontSize = 11.sp, color = color, fontWeight = FontWeight.ExtraBold)
        }
    }
}


// ==================== HELPER SMALL UI COMPOSABLES ====================

@Composable
fun MacroBarIndicator(label: String, current: Double, target: Double, color: Color) {
    val progress = if (target > 0) (current / target).toFloat().coerceIn(0f, 1f) else 0f

    Column(
        modifier = Modifier
            .width(100.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFF7F2FA))  // Cozy soft container matching HTML
            .padding(vertical = 10.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label.uppercase(),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .background(Color(0xFFE2E8F0))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "${current.toInt()}g",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = "/${target.toInt()}",
                fontSize = 10.sp,
                fontWeight = FontWeight.Normal,
                color = TextMuted
            )
        }
    }
}

@Composable
fun MacroMicroPill(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text = text, color = color, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
    }
}

// Format selected date YYYY-MM-DD into readable Mon, Jun 4 format
fun formatHumanFriendlyDate(dateStr: String): String {
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = parser.parse(dateStr) ?: return dateStr
        val formatter = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())
        formatter.format(date)
    } catch (e: Exception) {
        dateStr
    }
}

// ==================== EDIT DIALOGS COMPOSABLES ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualAddMealDialog(
    onDismiss: () -> Unit,
    onSave: (String, Double, Double, Double, Double, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var calories by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }
    var mealType by remember { mutableStateOf("Lunch") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = ContentSurface),
            border = BorderStroke(1.dp, CardBorder)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Log Custom Meal",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Meal description (e.g. Rice with tofu)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = CardBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = calories,
                    onValueChange = { calories = it },
                    label = { Text("Calories (kcal)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = CardBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = protein,
                        onValueChange = { protein = it },
                        label = { Text("Protein (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = CardBorder
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = carbs,
                        onValueChange = { carbs = it },
                        label = { Text("Carbs (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = CardBorder
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = fat,
                    onValueChange = { fat = it },
                    label = { Text("Fat (g)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = CardBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Meal Category row picker
                Column {
                    Text("Select Meal Duration Category", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val durationMeals = listOf("Breakfast", "Lunch", "Dinner", "Snack")
                        durationMeals.forEach { type ->
                            val active = mealType == type
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (active) EmeraldPrimary else CardBorder)
                                    .clickable { mealType = type }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = type,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (active) Color.White else TextSecondary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = TextSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val calD = calories.toDoubleOrNull() ?: 0.0
                            val protD = protein.toDoubleOrNull() ?: 0.0
                            val carbD = carbs.toDoubleOrNull() ?: 0.0
                            val fatD = fat.toDoubleOrNull() ?: 0.0
                            onSave(name, calD, protD, carbD, fatD, mealType)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Text("Record Log", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalSettingsDialog(
    activeGoal: DailyGoal,
    onDismiss: () -> Unit,
    onSave: (Double, Double, Double, Double) -> Unit
) {
    var calGoal by remember { mutableStateOf(activeGoal.calorieGoal.toString()) }
    var proteinGoal by remember { mutableStateOf(activeGoal.proteinGoal.toString()) }
    var carbsGoal by remember { mutableStateOf(activeGoal.carbsGoal.toString()) }
    var fatGoal by remember { mutableStateOf(activeGoal.fatGoal.toString()) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = ContentSurface),
            border = BorderStroke(1.dp, CardBorder)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Adjust Customize Target Goals",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary
                )

                Text(
                    text = "These numbers modify your targets indices across all progress rings and charts dynamically.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                OutlinedTextField(
                    value = calGoal,
                    onValueChange = { calGoal = it },
                    label = { Text("Calorie Target (kcal)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = CardBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = proteinGoal,
                        onValueChange = { proteinGoal = it },
                        label = { Text("Protein Target") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = CardBorder
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = carbsGoal,
                        onValueChange = { carbsGoal = it },
                        label = { Text("Carbs Target") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = CardBorder
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = fatGoal,
                    onValueChange = { fatGoal = it },
                    label = { Text("Fat Target (g)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = CardBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Dismiss", color = TextSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val calD = calGoal.toDoubleOrNull() ?: 2000.0
                            val protD = proteinGoal.toDoubleOrNull() ?: 130.0
                            val carbD = carbsGoal.toDoubleOrNull() ?: 220.0
                            val fatD = fatGoal.toDoubleOrNull() ?: 65.0
                            onSave(calD, protD, carbD, fatD)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Text("Apply", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

