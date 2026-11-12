package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.auth.AuthRepository
import com.example.auth.AuthState
import com.example.auth.AuthUser
import com.example.auth.MockAuthRepository
import com.example.data.EnergyDataSource
import com.example.data.LiveDataSource
import com.example.data.MockDataSource
import com.example.persistence.datastore.DataSourceMode
import com.example.services.BudgetMonitor
import com.example.services.CommandResult
import com.example.services.DeviceControlService
import com.example.services.LimitMonitor
import com.example.types.Appliance
import com.example.types.DeviceCommandStatus
import com.example.types.DeviceControlState
import com.example.types.HistoryPeriod
import com.example.types.HouseholdStats
import com.example.types.HourlyUsage
import com.example.types.LimitAction
import com.example.types.VoltoAlert
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class TabItem(val title: String) {
    HOME("Home"),
    APPLIANCES("Appliances"),
    LIMITS("Limits"),
    BUDGET("Budget"),
    REPORTS("Reports"),
    ALERTS("Alerts"),
    SETTINGS("Settings"),
    ASK_VOLTO("Ask AI"),
    SIMULATOR("Simulator")
}

enum class ApplianceFilter(val label: String) {
    ALL("All"),
    RUNNING("Running"),
    OFF("Off"),
    CRITICAL("Critical")
}

class VoltoViewModel(
    private var mockDataSource: MockDataSource = MockDataSource(),
    private var liveDataSource: LiveDataSource = LiveDataSource(),
    private val authRepo: AuthRepository = MockAuthRepository(),
    private val deviceControlService: DeviceControlService = DeviceControlService()
) : ViewModel() {

    private val _currentSourceMode = MutableStateFlow(DataSourceMode.MOCK)
    val currentSourceMode: StateFlow<DataSourceMode> = _currentSourceMode.asStateFlow()

    private var activeDataSource: EnergyDataSource = mockDataSource

    // Authentication State
    val authState: StateFlow<AuthState> = authRepo.authState
    private val _authErrorMessage = MutableStateFlow<String?>(null)
    val authErrorMessage: StateFlow<String?> = _authErrorMessage.asStateFlow()
    private val _isAuthLoading = MutableStateFlow(false)
    val isAuthLoading: StateFlow<Boolean> = _isAuthLoading.asStateFlow()

    private val _activeTab = MutableStateFlow(TabItem.HOME)
    val activeTab: StateFlow<TabItem> = _activeTab.asStateFlow()

    private val _selectedApplianceId = MutableStateFlow<String?>(null)
    val selectedApplianceId: StateFlow<String?> = _selectedApplianceId.asStateFlow()

    private val _detailsHistoryPeriod = MutableStateFlow(HistoryPeriod.DAY)
    val detailsHistoryPeriod: StateFlow<HistoryPeriod> = _detailsHistoryPeriod.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filterCategory = MutableStateFlow(ApplianceFilter.ALL)
    val filterCategory: StateFlow<ApplianceFilter> = _filterCategory.asStateFlow()

    private val _deviceControlStates = MutableStateFlow<Map<String, DeviceControlState>>(emptyMap())
    val deviceControlStates: StateFlow<Map<String, DeviceControlState>> = _deviceControlStates.asStateFlow()

    private val _criticalConfirmationAppliance = MutableStateFlow<Appliance?>(null)
    val criticalConfirmationAppliance: StateFlow<Appliance?> = _criticalConfirmationAppliance.asStateFlow()

    private val _showAddApplianceDialog = MutableStateFlow(false)
    val showAddApplianceDialog: StateFlow<Boolean> = _showAddApplianceDialog.asStateFlow()

    private val _limitConfigAppliance = MutableStateFlow<Appliance?>(null)
    val limitConfigAppliance: StateFlow<Appliance?> = _limitConfigAppliance.asStateFlow()

    private val _isDarkMode = MutableStateFlow(true)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _currentLanguage = MutableStateFlow("en")
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

    private val _householdName = MutableStateFlow("Sharma Family")
    val householdName: StateFlow<String> = _householdName.asStateFlow()

    private val _homeAddress = MutableStateFlow("Flat 402, Green Glen Layout, Bellandur, Bengaluru")
    val homeAddress: StateFlow<String> = _homeAddress.asStateFlow()

    private val _discomProvider = MutableStateFlow("BESCOM (Bengaluru)")
    val discomProvider: StateFlow<String> = _discomProvider.asStateFlow()

    private val _monthlyBudget = MutableStateFlow(3500.0)
    val monthlyBudget: StateFlow<Double> = _monthlyBudget.asStateFlow()

    private val _tariffRate = MutableStateFlow(6.50)
    val tariffRate: StateFlow<Double> = _tariffRate.asStateFlow()

    // Dynamic Streams hooked to activeDataSource
    val appliances: StateFlow<List<Appliance>> = activeDataSource.appliancesStream
    val householdStats: StateFlow<HouseholdStats> = activeDataSource.householdStatsStream
    val hourlyHistory: StateFlow<List<HourlyUsage>> = activeDataSource.hourlyHistoryStream
    val alerts: StateFlow<List<VoltoAlert>> = activeDataSource.alertsStream

    val unreadAlertsCount: StateFlow<Int> = alerts.map { list ->
        list.count { !it.isRead }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 2)

    init {
        startAutomatedLimitMonitoring()
    }

    private fun startAutomatedLimitMonitoring() {
        viewModelScope.launch {
            appliances.collect { appList ->
                appList.forEach { app ->
                    val result = LimitMonitor.evaluateApplianceLimit(app)
                    if (result.shouldAutoCut && app.isOn && !app.isCritical) {
                        activeDataSource.setApplianceState(app.id, false)
                    }
                }
            }
        }
    }

    fun selectTab(tab: TabItem) {
        _activeTab.value = tab
        if (tab != TabItem.APPLIANCES) {
            _selectedApplianceId.value = null
        }
    }

    fun openApplianceDetails(applianceId: String) {
        _selectedApplianceId.value = applianceId
    }

    fun closeApplianceDetails() {
        _selectedApplianceId.value = null
    }

    fun setDetailsHistoryPeriod(period: HistoryPeriod) {
        _detailsHistoryPeriod.value = period
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilterCategory(filter: ApplianceFilter) {
        _filterCategory.value = filter
    }

    fun openAddApplianceDialog() {
        _showAddApplianceDialog.value = true
    }

    fun closeAddApplianceDialog() {
        _showAddApplianceDialog.value = false
    }

    fun openLimitConfigDialog(appliance: Appliance) {
        _limitConfigAppliance.value = appliance
    }

    fun closeLimitConfigDialog() {
        _limitConfigAppliance.value = null
    }

    fun toggleDarkMode() {
        _isDarkMode.value = !_isDarkMode.value
    }

    fun setLanguage(lang: String) {
        _currentLanguage.value = lang
    }

    fun switchDataSource(mode: DataSourceMode) {
        _currentSourceMode.value = mode
        activeDataSource = if (mode == DataSourceMode.LIVE) liveDataSource else mockDataSource
    }

    fun simulateHardwareDisconnect(deviceId: String) {
        liveDataSource.simulateDeviceDrop(deviceId)
    }

    fun simulateHardwareReconnect(deviceId: String) {
        liveDataSource.simulateDeviceReconnect(deviceId)
    }

    fun updateHouseholdSettings(name: String, budget: Double, rate: Double) {
        _householdName.value = name
        _monthlyBudget.value = budget
        _tariffRate.value = rate
    }

    fun updateProfile(name: String, address: String, provider: String) {
        _householdName.value = name
        _homeAddress.value = address
        _discomProvider.value = provider
    }

    fun requestToggleAppliance(appliance: Appliance) {
        if (appliance.isCritical && appliance.isOn) {
            _criticalConfirmationAppliance.value = appliance
            return
        }
        executeApplianceToggle(appliance, !appliance.isOn)
    }

    fun confirmCriticalTurnOff() {
        val app = _criticalConfirmationAppliance.value ?: return
        _criticalConfirmationAppliance.value = null
        executeApplianceToggle(app, false)
    }

    fun dismissCriticalConfirmation() {
        _criticalConfirmationAppliance.value = null
    }

    private fun executeApplianceToggle(appliance: Appliance, targetState: Boolean) {
        val deviceId = appliance.deviceId
        _deviceControlStates.value = _deviceControlStates.value + (
            deviceId to DeviceControlState(
                deviceId = deviceId,
                status = DeviceCommandStatus.SENDING,
                targetState = targetState,
                message = "Connecting to ${appliance.name}..."
            )
        )

        viewModelScope.launch {
            val result = deviceControlService.sendCommand(deviceId, targetState, appliance.isOnline)
            when (result) {
                is CommandResult.Success -> {
                    activeDataSource.setApplianceState(appliance.id, targetState)
                    _deviceControlStates.value = _deviceControlStates.value + (
                        deviceId to DeviceControlState(
                            deviceId = deviceId,
                            status = DeviceCommandStatus.CONFIRMED,
                            targetState = targetState,
                            message = if (targetState) "Turned ON" else "Turned OFF"
                        )
                    )
                    delay(1200L)
                    _deviceControlStates.value = _deviceControlStates.value - deviceId
                }
                is CommandResult.Failure -> {
                    _deviceControlStates.value = _deviceControlStates.value + (
                        deviceId to DeviceControlState(
                            deviceId = deviceId,
                            status = DeviceCommandStatus.FAILED,
                            targetState = targetState,
                            message = result.reason
                        )
                    )
                    delay(3000L)
                    _deviceControlStates.value = _deviceControlStates.value - deviceId
                }
                is CommandResult.Timeout -> {
                    _deviceControlStates.value = _deviceControlStates.value + (
                        deviceId to DeviceControlState(
                            deviceId = deviceId,
                            status = DeviceCommandStatus.FAILED,
                            targetState = targetState,
                            message = "Timed out after 5s. Check Wi-Fi."
                        )
                    )
                    delay(3500L)
                    _deviceControlStates.value = _deviceControlStates.value - deviceId
                }
                is CommandResult.DeviceOffline -> {
                    _deviceControlStates.value = _deviceControlStates.value + (
                        deviceId to DeviceControlState(
                            deviceId = deviceId,
                            status = DeviceCommandStatus.OFFLINE,
                            targetState = targetState,
                            message = "Device is offline."
                        )
                    )
                    delay(2500L)
                    _deviceControlStates.value = _deviceControlStates.value - deviceId
                }
            }
        }
    }

    fun saveApplianceLimit(applianceId: String, limitKwh: Double, action: LimitAction) {
        activeDataSource.setApplianceLimit(applianceId, limitKwh, action)
        _limitConfigAppliance.value = null
    }

    fun addNewAppliance(
        name: String,
        type: String,
        deviceId: String,
        isCritical: Boolean,
        dailyLimitKwh: Double,
        limitAction: LimitAction,
        baseWatts: Int
    ) {
        activeDataSource.addAppliance(
            name = name,
            type = type,
            deviceId = deviceId,
            isCritical = isCritical,
            dailyLimitKwh = dailyLimitKwh,
            limitAction = limitAction,
            baseWatts = baseWatts
        )
        _showAddApplianceDialog.value = false
    }

    fun markAlertAsRead(alertId: String) {
        activeDataSource.markAlertAsRead(alertId)
    }

    fun markAllAlertsAsRead() {
        activeDataSource.markAllAlertsAsRead()
    }

    fun refreshDashboard() {
        activeDataSource.refresh()
    }

    fun getApplianceHourlyData(applianceId: String): List<HourlyUsage> = activeDataSource.getApplianceHourlyHistory(applianceId)
    fun getApplianceDailyData(applianceId: String): List<Double> = activeDataSource.getApplianceDailyHistory(applianceId)
    fun getApplianceWeeklyData(applianceId: String): List<Double> = activeDataSource.getApplianceWeeklyHistory(applianceId)

    // Auth actions
    fun login(email: String, pass: String) {
        _isAuthLoading.value = true
        _authErrorMessage.value = null
        viewModelScope.launch {
            val result = authRepo.signInWithEmail(email, pass)
            _isAuthLoading.value = false
            if (result.isFailure) {
                _authErrorMessage.value = result.exceptionOrNull()?.message ?: "Sign in failed"
            }
        }
    }

    fun register(name: String, email: String, pass: String) {
        _isAuthLoading.value = true
        _authErrorMessage.value = null
        viewModelScope.launch {
            val result = authRepo.registerWithEmail(name, email, pass)
            _isAuthLoading.value = false
            if (result.isFailure) {
                _authErrorMessage.value = result.exceptionOrNull()?.message ?: "Registration failed"
            }
        }
    }

    fun googleSignIn() {
        _isAuthLoading.value = true
        _authErrorMessage.value = null
        viewModelScope.launch {
            val result = authRepo.signInWithGoogle()
            _isAuthLoading.value = false
            if (result.isFailure) {
                _authErrorMessage.value = result.exceptionOrNull()?.message ?: "Google sign in failed"
            }
        }
    }

    fun forgotPassword(email: String) {
        viewModelScope.launch {
            authRepo.sendPasswordReset(email)
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepo.signOut()
        }
    }

    fun deleteAccountAndData(onCompleted: () -> Unit = {}) {
        viewModelScope.launch {
            authRepo.signOut()
            onCompleted()
        }
    }
}
