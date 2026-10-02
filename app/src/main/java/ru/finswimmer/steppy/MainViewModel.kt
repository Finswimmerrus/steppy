package ru.finswimmer.steppy

import android.app.Application
import androidx.health.connect.client.HealthConnectClient
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.finswimmer.steppy.data.HealthConnectRepository
import ru.finswimmer.steppy.model.MainTab
import ru.finswimmer.steppy.model.SteppyUiState
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val health = HealthConnectRepository(application)
    private val prefs = application.getSharedPreferences("steppy", 0)
    private val _state = MutableStateFlow(
        SteppyUiState(
            onboarded = prefs.getBoolean("onboarded", false),
            healthAvailable = health.availability() == HealthConnectClient.SDK_AVAILABLE,
        )
    )
    val state: StateFlow<SteppyUiState> = _state.asStateFlow()

    init { refreshPermission() }

    fun finishOnboarding() {
        prefs.edit().putBoolean("onboarded", true).apply()
        _state.update { it.copy(onboarded = true) }
    }

    fun selectTab(tab: MainTab) = _state.update { it.copy(tab = tab) }

    fun refreshPermission() = viewModelScope.launch {
        val granted = runCatching { health.hasPermissions() }.getOrDefault(false)
        _state.update { it.copy(healthConnected = granted) }
        if (granted) sync()
    }

    fun sync() = viewModelScope.launch {
        _state.update { it.copy(isSyncing = true) }
        val connected = runCatching { health.hasPermissions() }.getOrDefault(false)
        val value = if (connected) runCatching { health.readTodaySteps() }.getOrDefault(0) else 0
        delay(450)
        _state.update {
            it.copy(
                steps = if (connected) value else it.steps,
                healthConnected = connected,
                isSyncing = false,
                lastSync = "сегодня, " + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")),
            )
        }
    }

    fun healthPermissions() = health.permissions
}
