package com.pocketradar.smartcoach.sample

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketradar.smartcoach.MeasurementData
import com.pocketradar.smartcoach.RadarMeasurementUnit
import com.pocketradar.smartcoach.RadarSensitivity
import com.pocketradar.smartcoach.RadarSpeedRange
import com.pocketradar.smartcoach.SmartCoach
import com.pocketradar.smartcoach.SmartCoachError
import com.pocketradar.smartcoach.SmartCoachRadar
import com.pocketradar.smartcoach.SmartCoachSessionState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Drives the sample's one screen. Every SDK call is a suspend function that throws
 * [SmartCoachError]; the session state is the single source of truth for what to show.
 */
class HomeViewModel : ViewModel() {

    val sessionState: StateFlow<SmartCoachSessionState> = SmartCoach.sessionStateStream()

    private val _speeds = MutableStateFlow<List<MeasurementData>>(emptyList())

    /** Latest readings first. */
    val speeds: StateFlow<List<MeasurementData>> = _speeds.asStateFlow()

    private val _error = MutableStateFlow<SmartCoachError?>(null)
    val error: StateFlow<SmartCoachError?> = _error.asStateFlow()

    private val _settingStatus = MutableStateFlow<SettingStatus?>(null)
    val settingStatus: StateFlow<SettingStatus?> = _settingStatus.asStateFlow()

    private var speedsJob: Job? = null

    init {
        viewModelScope.launch {
            sessionState.collect { state ->
                when (state) {
                    // The radar may have started on its own (its trigger button); the session
                    // followed it, so subscribe to the readings if we haven't already.
                    is SmartCoachSessionState.measuring -> if (speedsJob?.isActive != true) collectReadings(start = false)
                    else -> cancelReadings()
                }
            }
        }
    }

    fun startScanning(connectToLastPairedDevice: Boolean) = launchCall {
        SmartCoach.startScanning(connectToLastPairedDevice = connectToLastPairedDevice)
    }

    fun stopScanning() = launchCall { SmartCoach.stopScanning() }

    /** Returns once the connection starts; [SmartCoachSessionState.connected] means it's ready. */
    fun connect(radar: SmartCoachRadar) = launchCall { SmartCoach.connect(radar) }

    fun disconnect() = launchCall { SmartCoach.disconnect() }

    fun startMeasuring() {
        if (speedsJob?.isActive != true) collectReadings(start = true)
    }

    fun stopMeasuring() {
        cancelReadings()
        launchCall { SmartCoach.stopMeasuring() }
    }

    fun setUnit(unit: RadarMeasurementUnit) =
        runSetting(Setting.Units(unit)) { SmartCoach.setMeasurementUnit(unit) }

    /** [low] and [high] come from steppers bounded to the valid range, so the range exists. */
    fun setSpeedRange(low: Int, high: Int) {
        val range = RadarSpeedRange(low, high) ?: return
        runSetting(Setting.SpeedRange(range)) { SmartCoach.setSpeedRange(range) }
    }

    fun setSensitivity(level: Int) {
        val sensitivity = RadarSensitivity(level) ?: return
        runSetting(Setting.Sensitivity(sensitivity)) { SmartCoach.setSensitivity(sensitivity) }
    }

    fun dismissError() {
        _error.value = null
    }

    private fun collectReadings(start: Boolean) {
        _speeds.value = emptyList()
        speedsJob = viewModelScope.launch {
            try {
                // startMeasuring turns the radar on when connected and only subscribes when it
                // is already measuring. The flow completes when measuring ends.
                SmartCoach.startMeasuring().collect { reading ->
                    _speeds.update { (listOf(reading) + it).take(MAX_READINGS) }
                }
            } catch (e: SmartCoachError) {
                if (start) _error.value = e
            }
        }
    }

    private fun cancelReadings() {
        speedsJob?.cancel()
        speedsJob = null
        _speeds.value = emptyList()
    }

    /** Each setter suspends until the radar acknowledges, or throws (e.g. `commandFailed`). */
    private fun runSetting(setting: Setting, apply: suspend () -> Unit) {
        _settingStatus.value = SettingStatus(setting, SettingStatus.Result.Waiting)
        viewModelScope.launch {
            val result = try {
                apply()
                SettingStatus.Result.Acknowledged
            } catch (e: SmartCoachError) {
                _error.value = e
                SettingStatus.Result.Failed
            }
            _settingStatus.value = SettingStatus(setting, result)
        }
    }

    private fun launchCall(call: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                call()
            } catch (e: SmartCoachError) {
                _error.value = e
            }
        }
    }

    sealed interface Setting {
        data class Units(val unit: RadarMeasurementUnit) : Setting
        data class SpeedRange(val range: RadarSpeedRange) : Setting
        data class Sensitivity(val sensitivity: RadarSensitivity) : Setting
    }

    data class SettingStatus(val setting: Setting, val result: Result) {
        enum class Result { Waiting, Acknowledged, Failed }
    }

    private companion object {
        const val MAX_READINGS = 10
    }
}
