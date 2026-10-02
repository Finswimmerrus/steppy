package ru.finswimmer.steppy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.finswimmer.steppy.model.*
import ru.finswimmer.steppy.ui.theme.*
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { SteppyTheme { SteppyApp() } }
    }
}

@Composable
private fun SteppyApp(vm: MainViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val permissionLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { vm.refreshPermission() }

    if (!state.onboarded) {
        OnboardingScreen(
            healthAvailable = state.healthAvailable,
            connected = state.healthConnected,
            onConnect = { permissionLauncher.launch(vm.healthPermissions()) },
            onContinue = vm::finishOnboarding,
        )
    } else {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = { SteppyNavigation(state.tab, vm::selectTab) },
        ) { padding ->
            AnimatedContent(state.tab, label = "main navigation") { tab ->
                when (tab) {
                    MainTab.TODAY -> TodayScreen(state, vm::sync, { vm.selectTab(MainTab.PROFILE) }, Modifier.padding(padding))
                    MainTab.MY_GAMES -> MyGamesScreen(state, Modifier.padding(padding))
                    MainTab.SEARCH -> SearchScreen(Modifier.padding(padding))
                    MainTab.CREATE -> CreateChallengeScreen(Modifier.padding(padding))
                    MainTab.PROFILE -> ProfileScreen(state, { permissionLauncher.launch(vm.healthPermissions()) }, Modifier.padding(padding))
                }
            }
        }
    }
}

@Composable
private fun OnboardingScreen(
    healthAvailable: Boolean,
    connected: Boolean,
    onConnect: () -> Unit,
    onContinue: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().background(SteppyYellow).statusBarsPadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(.35f))
        Mascot(modifier = Modifier.size(240.dp))
        Spacer(Modifier.height(28.dp))
        Text("Двигайся в своём ритме", fontSize = 36.sp, lineHeight = 39.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
        Spacer(Modifier.height(14.dp))
        Text(
            "Персональные цели, понятный прогресс и поддержка каждый день.",
            fontSize = 17.sp,
            lineHeight = 24.sp,
            textAlign = TextAlign.Center,
            color = SteppyInk.copy(alpha = .72f),
        )
        Spacer(Modifier.weight(.25f))
        Surface(color = Color.White.copy(alpha = .88f), shape = RoundedCornerShape(24.dp)) {
            Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Favorite, null, tint = SteppyGreen)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("Только шаги", fontWeight = FontWeight.Bold)
                    Text("Steppy запросит минимум данных и ничего не публикует без вашего согласия.", fontSize = 13.sp, lineHeight = 18.sp)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = if (connected) onContinue else onConnect,
            enabled = healthAvailable || connected,
            modifier = Modifier.fillMaxWidth().height(58.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SteppyInk),
        ) {
            Text(if (connected) "Продолжить" else if (healthAvailable) "Подключить Health Connect" else "Health Connect недоступен", fontWeight = FontWeight.Bold)
        }
        TextButton(onClick = onContinue) { Text("Посмотреть демо", color = SteppyInk) }
        Spacer(Modifier.navigationBarsPadding())
    }
}

@Composable
private fun TodayScreen(state: SteppyUiState, onSync: () -> Unit, onProfile: () -> Unit, modifier: Modifier = Modifier) {
    val goal = state.snapshot.activeGoal
    val progress = (state.steps.toFloat() / goal).coerceIn(0f, 1f)
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(todayLabel(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(.55f), letterSpacing = 1.sp)
                    Text("Привет, чемпион!", fontSize = 28.sp, fontWeight = FontWeight.Black)
                }
                Surface(
                    shape = CircleShape,
                    color = SteppyYellow,
                    modifier = Modifier.size(48.dp).clickable(onClick = onProfile),
                ) {
                    Box(contentAlignment = Alignment.Center) { Mascot(Modifier.size(52.dp)) }
                }
            }
        }
        item { CurrentWeekStrip(state.steps) }
        item {
            Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 2.dp) {
                Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        StatusPill("АКТИВНЫЙ ДЕНЬ", SteppyLime)
                        Spacer(Modifier.weight(1f))
                        Text("Неделя ${state.week} из ${state.totalWeeks}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(.6f))
                    }
                    Spacer(Modifier.height(22.dp))
                    StepRing(state.steps, goal, progress)
                    Spacer(Modifier.height(16.dp))
                    Text(if (progress >= 1f) "Цель выполнена — отличный ритм!" else "Осталось ${formatSteps((goal - state.steps).coerceAtLeast(0))} шагов", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("До конца дня · затем 12 часов на синхронизацию", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(.55f))
                }
            }
        }
        item { SyncCard(state, onSync) }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard("7", "дней подряд", Icons.Default.LocalFireDepartment, SteppyOrange, Modifier.weight(1f))
                MetricCard("5 / 7", "дней недели", Icons.Default.CheckCircle, SteppyGreen, Modifier.weight(1f))
            }
        }
        item {
            Surface(shape = RoundedCornerShape(24.dp), color = SteppyInk) {
                Row(Modifier.fillMaxWidth().heightIn(min = 130.dp).padding(start = 14.dp, end = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Mascot(Modifier.size(118.dp))
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("Ровный темп — сильный финиш", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text("Короткая прогулка вечером закроет большую часть остатка.", color = Color.White.copy(.7f), fontSize = 13.sp, lineHeight = 18.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun StepRing(steps: Long, goal: Long, progress: Float) {
    Box(Modifier.size(210.dp).semantics { contentDescription = "${formatSteps(steps)} из ${formatSteps(goal)} шагов" }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            drawArc(color = Color(0xFFEAE8E0), startAngle = -90f, sweepAngle = 360f, useCenter = false, style = Stroke(15.dp.toPx(), cap = StrokeCap.Round))
            drawArc(color = SteppyYellow, startAngle = -90f, sweepAngle = 360f * progress, useCenter = false, style = Stroke(15.dp.toPx(), cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.DirectionsWalk, null, tint = SteppyInk, modifier = Modifier.size(30.dp))
            Text(formatSteps(steps), fontSize = 38.sp, fontWeight = FontWeight.Black)
            Text("из ${formatSteps(goal)}", color = MaterialTheme.colorScheme.onSurface.copy(.55f))
        }
    }
}

@Composable
private fun SyncCard(state: SteppyUiState, onSync: () -> Unit) {
    Surface(shape = RoundedCornerShape(20.dp), color = if (state.healthConnected) Color(0xFFE9F7F0) else Color(0xFFFFF1D6)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (state.healthConnected) Icons.Default.CloudDone else Icons.Default.CloudOff, null, tint = if (state.healthConnected) SteppyGreen else Color(0xFF9A6500))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(if (state.healthConnected) "Данные синхронизированы" else "Демо-данные", fontWeight = FontWeight.Bold)
                Text(if (state.healthConnected) "Health Connect · ${state.lastSync}" else "Подключите Health Connect в профиле", fontSize = 12.sp)
            }
            IconButton(onClick = onSync, enabled = !state.isSyncing && state.healthConnected) {
                if (state.isSyncing) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp) else Icon(Icons.Default.Sync, "Синхронизировать")
            }
        }
    }
}

@Composable
private fun MetricCard(value: String, label: String, icon: ImageVector, tint: Color, modifier: Modifier) {
    Surface(modifier, shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(18.dp)) {
            Icon(icon, null, tint = tint)
            Spacer(Modifier.height(12.dp))
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.Black)
            Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(.6f))
        }
    }
}

@Composable
private fun CurrentWeekStrip(todaySteps: Long) {
    val today = LocalDate.now()
    val monday = today.minusDays((today.dayOfWeek.value - 1).toLong())
    val dates = (0L..6L).map { monday.plusDays(it) }
    Column {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Текущая неделя", fontSize = 20.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.weight(1f))
            Text("${weekOfMonth(today)} неделя", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(.55f))
        }
        Spacer(Modifier.height(10.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(dates) { date ->
                val isToday = date == today
                val isPast = date < today
                val background = when { isToday -> SteppyYellow; isPast -> Color(0xFFE7F5EC); else -> MaterialTheme.colorScheme.surface }
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = background,
                    border = if (isToday) androidx.compose.foundation.BorderStroke(2.dp, SteppyInk) else null,
                ) {
                    Column(
                        Modifier.width(52.dp).padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(date.format(DateTimeFormatter.ofPattern("EE", Locale.forLanguageTag("ru-RU"))).uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(date.dayOfMonth.toString(), fontSize = 20.sp, fontWeight = FontWeight.Black)
                        Icon(
                            when { isToday -> Icons.Default.DirectionsWalk; isPast -> Icons.Default.CheckCircle; else -> Icons.Default.Circle },
                            null,
                            tint = when { isPast -> SteppyGreen; isToday -> SteppyInk; else -> MaterialTheme.colorScheme.outline },
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MyGamesScreen(state: SteppyUiState, modifier: Modifier = Modifier) {
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text("Мои игры", fontSize = 30.sp, fontWeight = FontWeight.Black)
            Text("Активные и завершённые челленджи", color = MaterialTheme.colorScheme.onSurface.copy(.6f))
        }
        item {
            GameCard(
                title = state.challengeTitle,
                subtitle = "Неделя ${state.week} из ${state.totalWeeks} · 128 участников",
                progress = .43f,
                badge = "В ИГРЕ",
                color = SteppyYellow,
            )
        }
        item {
            Text("Завершённые", fontSize = 20.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 8.dp))
        }
        item {
            GameCard("Весенний старт", "Завершён · 21 день подряд", 1f, "ФИНИШ", Color(0xFFDDE8FF))
        }
        item {
            GameCard("Командный темп", "Завершён · +750 баллов", 1f, "ФИНИШ", SteppyLime)
        }
    }
}

@Composable
private fun GameCard(title: String, subtitle: String, progress: Float, badge: String, color: Color) {
    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp) {
        Column(Modifier.fillMaxWidth().padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(54.dp).clip(RoundedCornerShape(16.dp)).background(color), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.DirectionsWalk, null, tint = SteppyInk)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    StatusPill(badge, color)
                    Spacer(Modifier.height(5.dp))
                    Text(title, fontSize = 19.sp, fontWeight = FontWeight.Black)
                    Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(.55f))
                }
                Icon(Icons.Default.ChevronRight, null)
            }
            Spacer(Modifier.height(14.dp))
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape), color = SteppyInk, trackColor = MaterialTheme.colorScheme.surfaceVariant)
        }
    }
}

@Composable
private fun SearchScreen(modifier: Modifier = Modifier) {
    var query by rememberSaveable { mutableStateOf("") }
    val games = listOf(
        Triple("Город в движении", "4 недели · старт 12 октября", SteppyYellow),
        Triple("Первые 8 000", "3 недели · для ровного ритма", SteppyLime),
        Triple("Выходные вместе", "2 недели · командная игра", Color(0xFFDDE8FF)),
    ).filter { it.first.contains(query, ignoreCase = true) }
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Text("Найти игру", fontSize = 30.sp, fontWeight = FontWeight.Black) }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Название или код") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                shape = RoundedCornerShape(18.dp),
                singleLine = true,
            )
        }
        item { Text("Открытые челленджи", fontSize = 20.sp, fontWeight = FontWeight.Black) }
        items(games) { game -> GameCard(game.first, game.second, 0f, "МОЖНО ВСТУПИТЬ", game.third) }
        if (games.isEmpty()) item {
            Column(Modifier.fillMaxWidth().padding(36.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.SearchOff, null, modifier = Modifier.size(44.dp))
                Spacer(Modifier.height(12.dp))
                Text("Ничего не найдено", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CreateChallengeScreen(modifier: Modifier = Modifier) {
    var title by rememberSaveable { mutableStateOf("") }
    var weeks by rememberSaveable { mutableIntStateOf(4) }
    var created by rememberSaveable { mutableStateOf(false) }
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("Создать игру", fontSize = 30.sp, fontWeight = FontWeight.Black)
            Text("Создать челлендж может любой пользователь", color = MaterialTheme.colorScheme.onSurface.copy(.6f))
        }
        item {
            Surface(shape = RoundedCornerShape(24.dp), color = SteppyYellow) {
                Row(Modifier.fillMaxWidth().height(130.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Mascot(Modifier.size(120.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Собери свою команду", fontSize = 20.sp, fontWeight = FontWeight.Black)
                        Text("Название, срок и расписание — всё можно настроить.", fontSize = 13.sp)
                    }
                }
            }
        }
        item {
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Название челленджа") }, placeholder = { Text("Например, Команда в движении") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), singleLine = true)
        }
        item {
            Text("Продолжительность", fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(2, 4, 6).forEach { value ->
                    FilterChip(selected = weeks == value, onClick = { weeks = value }, label = { Text("$value нед.") })
                }
            }
        }
        item {
            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                Column(Modifier.padding(16.dp)) {
                    Text("Правила до публикации", fontWeight = FontWeight.Bold)
                    Text("Участники увидят типы дней, персональные цели и 12-часовое окно синхронизации до вступления.", fontSize = 13.sp, lineHeight = 18.sp)
                }
            }
        }
        item {
            Button(
                onClick = { created = true },
                enabled = title.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SteppyInk),
            ) { Text("Продолжить к расписанию", fontWeight = FontWeight.Bold) }
        }
        if (created) item {
            Surface(shape = RoundedCornerShape(18.dp), color = Color(0xFFE7F5EC)) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, null, tint = SteppyGreen)
                    Spacer(Modifier.width(12.dp))
                    Text("Черновик «$title» на $weeks недели создан", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun CalendarScreen(state: SteppyUiState, modifier: Modifier = Modifier) {
    val days = remember(state.steps) {
        listOf(
            ChallengeDay("ПН", 28, DayType.ACTIVE, 8_000, 8_421, DayStatus.COMPLETED),
            ChallengeDay("ВТ", 29, DayType.POWER, 10_000, 10_184, DayStatus.COMPLETED),
            ChallengeDay("СР", 30, DayType.FREE, null, 5_930, DayStatus.COMPLETED),
            ChallengeDay("ЧТ", 1, DayType.ACTIVE, 8_000, 8_806, DayStatus.COMPLETED),
            ChallengeDay("ПТ", 2, DayType.ACTIVE, 8_000, state.steps, DayStatus.TODAY),
            ChallengeDay("СБ", 3, DayType.POWER, 10_000, 0, DayStatus.UPCOMING),
            ChallengeDay("ВС", 4, DayType.FREE, null, 0, DayStatus.UPCOMING),
        )
    }
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("Календарь", fontSize = 30.sp, fontWeight = FontWeight.Black)
            Text(state.challengeTitle, color = MaterialTheme.colorScheme.onSurface.copy(.6f))
        }
        item {
            Surface(shape = RoundedCornerShape(24.dp), color = SteppyYellow) {
                Column(Modifier.fillMaxWidth().padding(20.dp)) {
                    Text("НЕДЕЛЯ ${state.week}", fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Text("4 из 7 дней завершено", fontSize = 22.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(progress = { 4f / 7f }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape), color = SteppyInk, trackColor = Color.White.copy(.55f))
                }
            }
        }
        items(days) { DayRow(it) }
        item {
            Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                Row(Modifier.padding(16.dp)) {
                    Icon(Icons.Default.Info, null)
                    Spacer(Modifier.width(12.dp))
                    Text("Цели взяты из вашего неизменяемого snapshot (${state.snapshot.algorithmVersion}). Официальный результат всегда подтверждает сервер.", fontSize = 13.sp, lineHeight = 18.sp)
                }
            }
        }
    }
}

@Composable
private fun DayRow(day: ChallengeDay) {
    val tint = when (day.type) { DayType.ACTIVE -> SteppyLime; DayType.POWER -> SteppyYellow; DayType.FREE -> Color(0xFFDDE8FF) }
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface, border = if (day.status == DayStatus.TODAY) androidx.compose.foundation.BorderStroke(2.dp, SteppyInk) else null) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(44.dp)) {
                Text(day.weekday, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(.5f))
                Text(day.dayNumber.toString(), fontSize = 22.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                StatusPill(day.type.label.uppercase(), tint)
                Spacer(Modifier.height(5.dp))
                Text(if (day.target == null) "Без обязательной цели" else "${formatSteps(day.steps)} / ${formatSteps(day.target)} шагов", fontSize = 13.sp)
            }
            when (day.status) {
                DayStatus.COMPLETED -> Icon(Icons.Default.CheckCircle, "Выполнено", tint = SteppyGreen)
                DayStatus.TODAY -> Text("Сегодня", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                else -> Icon(Icons.Default.LockClock, "Впереди", tint = MaterialTheme.colorScheme.onSurface.copy(.3f))
            }
        }
    }
}

@Composable
private fun CommunityScreen(modifier: Modifier = Modifier) {
    val events = listOf(
        FeedEvent("АК", "Анна закрыла активный день", "Поддержать · 12 мин", 0xFFFFD928),
        FeedEvent("МИ", "Михаил держит серию 14 дней", "🔥 18 · 1 ч", 0xFFC9F23A),
        FeedEvent("ЕК", "Команда прошла 1 000 000 шагов", "Общий milestone · 3 ч", 0xFFDDE8FF),
    )
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text("Вместе легче", fontSize = 30.sp, fontWeight = FontWeight.Black)
            Text("Новости челленджа без точных личных данных", color = MaterialTheme.colorScheme.onSurface.copy(.6f))
        }
        item {
            Surface(shape = RoundedCornerShape(24.dp), color = SteppyInk) {
                Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Groups, null, tint = SteppyYellow, modifier = Modifier.size(42.dp))
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("128 участников в игре", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 19.sp)
                        Text("Сегодня цель закрыли 72 человека", color = Color.White.copy(.65f), fontSize = 13.sp)
                    }
                }
            }
        }
        items(events) { event ->
            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(48.dp).clip(CircleShape).background(Color(event.accent)), contentAlignment = Alignment.Center) { Text(event.initials, fontWeight = FontWeight.Black) }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(event.title, fontWeight = FontWeight.Bold)
                        Text(event.detail, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(.55f))
                    }
                    IconButton(onClick = {}) { Icon(Icons.Default.FavoriteBorder, "Поддержать") }
                }
            }
        }
    }
}

@Composable
private fun ProfileScreen(state: SteppyUiState, onConnect: () -> Unit, modifier: Modifier = Modifier) {
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Text("Профиль", fontSize = 30.sp, fontWeight = FontWeight.Black) }
        item {
            Surface(shape = RoundedCornerShape(24.dp), color = SteppyYellow) {
                Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(64.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) { Text("S", fontSize = 30.sp, fontWeight = FontWeight.Black) }
                    Spacer(Modifier.width(16.dp))
                    Column { Text("Степан", fontSize = 22.sp, fontWeight = FontWeight.Black); Text("В игре с сентября 2026") }
                }
            }
        }
        item { SettingsRow(Icons.Default.Favorite, "Health Connect", if (state.healthConnected) "Подключено" else "Требуется подключение", onConnect) }
        item { SettingsRow(Icons.Default.Notifications, "Уведомления", "Цели, риск и синхронизация") {} }
        item { SettingsRow(Icons.Default.Security, "Приватность", "Вы контролируете видимость") {} }
        item { SettingsRow(Icons.Default.Help, "Помощь и апелляция", "Синхронизация и спорные данные") {} }
        item {
            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                Column(Modifier.padding(18.dp)) {
                    Text("Тестовый режим", fontWeight = FontWeight.Bold)
                    Text("Денежные операции и окончательное выбывание отключены до утверждения юридических и календарных правил.", fontSize = 13.sp, lineHeight = 18.sp)
                }
            }
        }
    }
}

@Composable
private fun SettingsRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Surface(Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.Bold); Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(.55f)) }
            Icon(Icons.Default.ChevronRight, null)
        }
    }
}

@Composable
private fun SteppyNavigation(selected: MainTab, onSelect: (MainTab) -> Unit) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
        NavigationItem(MainTab.TODAY, selected, "Сегодня", Icons.Default.Home, onSelect)
        NavigationItem(MainTab.MY_GAMES, selected, "Мои игры", Icons.Default.SportsScore, onSelect)
        NavigationItem(MainTab.SEARCH, selected, "Поиск", Icons.Default.Search, onSelect)
        NavigationItem(MainTab.CREATE, selected, "Создать", Icons.Default.AddCircle, onSelect)
    }
}

@Composable
private fun RowScope.NavigationItem(tab: MainTab, selected: MainTab, label: String, icon: ImageVector, onSelect: (MainTab) -> Unit) {
    NavigationBarItem(selected = tab == selected, onClick = { onSelect(tab) }, icon = { Icon(icon, label) }, label = { Text(label, fontSize = 11.sp) }, colors = NavigationBarItemDefaults.colors(indicatorColor = SteppyYellow))
}

@Composable
private fun StatusPill(text: String, color: Color) {
    Box(Modifier.clip(CircleShape).background(color).padding(horizontal = 10.dp, vertical = 5.dp)) { Text(text, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = .5.sp, color = SteppyInk) }
}

@Composable
private fun Mascot(modifier: Modifier = Modifier) {
    androidx.compose.foundation.Image(
        painter = painterResource(R.drawable.steppy_mascot),
        contentDescription = "Маскот Steppy",
        contentScale = ContentScale.Fit,
        modifier = modifier,
    )
}

private fun formatSteps(value: Long): String = NumberFormat.getIntegerInstance(Locale("ru", "RU")).format(value)

private fun todayLabel(): String = LocalDate.now()
    .format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale("ru", "RU")))
    .uppercase(Locale("ru", "RU"))

private fun weekOfMonth(date: LocalDate): Int = ((date.dayOfMonth - 1) / 7) + 1
