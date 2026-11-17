package com.pocketradar.smartcoach.sample

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pocketradar.smartcoach.DeviceType
import com.pocketradar.smartcoach.MeasurementData
import com.pocketradar.smartcoach.RadarMeasurementState
import com.pocketradar.smartcoach.RadarMeasurementUnit
import com.pocketradar.smartcoach.RadarSensitivity
import com.pocketradar.smartcoach.RadarSpeedRange
import com.pocketradar.smartcoach.SmartCoach
import com.pocketradar.smartcoach.SmartCoachRadar
import com.pocketradar.smartcoach.SmartCoachSessionState
import com.pocketradar.smartcoach.sample.HomeViewModel.Setting
import com.pocketradar.smartcoach.sample.HomeViewModel.SettingStatus

@Composable
fun HomeScreen(viewModel: HomeViewModel = viewModel()) {
    val state by viewModel.sessionState.collectAsStateWithLifecycle()
    val speeds by viewModel.speeds.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val settingStatus by viewModel.settingStatus.collectAsStateWithLifecycle()

    val context = LocalContext.current
    var permitted by remember { mutableStateOf(BluetoothPermissions.granted(context)) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { permitted = BluetoothPermissions.granted(context) }

    Scaffold { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { StatusHeader(state) }

            if (!permitted) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.permissions_needed))
                        Button(onClick = {
                            permissionLauncher.launch((BluetoothPermissions.required + BluetoothPermissions.optional).toTypedArray())
                        }) { Text(stringResource(R.string.permissions_grant)) }
                    }
                }
                return@LazyColumn
            }

            item { SessionActions(state, viewModel) }

            when (val s = state) {
                is SmartCoachSessionState.scanning -> {
                    if (s.radars.isEmpty()) item { Text(stringResource(R.string.no_radars_found)) }
                    items(s.radars, key = { it.id }) { radar ->
                        ScannedRadarRow(radar, onConnect = { viewModel.connect(radar) })
                    }
                }

                is SmartCoachSessionState.connected -> {
                    item { RadarInfo(s.radar) }
                    item { HorizontalDivider() }
                    item { RadarSettings(s.radar, settingStatus, viewModel) }
                    item { HorizontalDivider() }
                    item {
                        Button(onClick = viewModel::startMeasuring) { Text(stringResource(R.string.action_start_measuring)) }
                    }
                }

                is SmartCoachSessionState.measuring -> {
                    item { RadarInfo(s.radar) }
                    item { HorizontalDivider() }
                    item { RadarSettings(s.radar, settingStatus, viewModel) }
                    item { HorizontalDivider() }
                    item {
                        OutlinedButton(onClick = viewModel::stopMeasuring) { Text(stringResource(R.string.action_stop_measuring)) }
                    }
                    if (speeds.isEmpty()) {
                        item { Text(stringResource(R.string.readings_waiting), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                    items(speeds) { ReadingRow(it) }
                }

                else -> Unit
            }

            item {
                Text(
                    stringResource(R.string.sdk_version, SmartCoach.getVersion()),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    error?.let { e ->
        AlertDialog(
            onDismissRequest = viewModel::dismissError,
            confirmButton = { TextButton(onClick = viewModel::dismissError) { Text(stringResource(R.string.ok)) } },
            title = { Text(stringResource(R.string.error_title)) },
            text = { Text(errorText(e)) },
        )
    }
}

@Composable
private fun StatusHeader(state: SmartCoachSessionState) {
    val text = when (state) {
        is SmartCoachSessionState.disconnected -> stringResource(
            if (state.error != null) R.string.status_ended_with_error else R.string.status_disconnected,
        )
        is SmartCoachSessionState.scanning -> stringResource(R.string.status_scanning)
        is SmartCoachSessionState.connecting -> stringResource(R.string.status_connecting, state.radar.name)
        is SmartCoachSessionState.connected -> stringResource(R.string.status_connected, state.radar.name)
        is SmartCoachSessionState.measuring -> stringResource(R.string.status_measuring, state.radar.name)
        is SmartCoachSessionState.reconnecting -> stringResource(R.string.status_reconnecting, state.radar.name)
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text, style = MaterialTheme.typography.headlineSmall)
        // Errors that end the session arrive on the state, e.g. a radar model the API key doesn't cover.
        (state as? SmartCoachSessionState.disconnected)?.error?.let {
            Text(errorText(it), color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun SessionActions(state: SmartCoachSessionState, viewModel: HomeViewModel) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        when (state) {
            is SmartCoachSessionState.disconnected -> {
                Button(onClick = { viewModel.startScanning(connectToLastPairedDevice = false) }) {
                    Text(stringResource(R.string.action_scan))
                }
                Button(onClick = { viewModel.startScanning(connectToLastPairedDevice = true) }) {
                    Text(stringResource(R.string.action_scan_auto_connect))
                }
            }
            is SmartCoachSessionState.scanning -> OutlinedButton(onClick = viewModel::stopScanning) {
                Text(stringResource(R.string.action_stop_scanning))
            }
            is SmartCoachSessionState.connecting,
            is SmartCoachSessionState.connected,
            is SmartCoachSessionState.measuring,
            is SmartCoachSessionState.reconnecting,
            -> OutlinedButton(onClick = viewModel::disconnect) { Text(stringResource(R.string.action_disconnect)) }
        }
    }
}

@Composable
private fun ScannedRadarRow(radar: SmartCoachRadar, onConnect: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(1f)) {
            Text(radar.name, style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.radar_id, radar.id) + "  " + stringResource(R.string.radar_rssi, radar.rssi),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        SignalStrength(SignalLevel.fromRssi(radar.rssi))
        Box(Modifier.width(12.dp))
        OutlinedButton(onClick = onConnect) { Text(stringResource(R.string.action_connect)) }
    }
}

@Composable
private fun RadarInfo(radar: SmartCoachRadar) {
    val unknown = stringResource(R.string.unknown)
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(radar.name, style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.radar_id, radar.id))
        Text(stringResource(R.string.radar_rssi, radar.rssi))
        Text(stringResource(R.string.radar_type, deviceTypeLabel(radar.deviceType)))
        Text(stringResource(R.string.radar_mac, radar.macAddress ?: unknown))
        Text(stringResource(R.string.radar_unit, radar.measurementUnit.name))
        Text(stringResource(R.string.radar_battery, radar.batteryLevel.name))
        // Reported by the radar: flips to measuring when its own trigger is used.
        Text(
            stringResource(R.string.radar_measurement_state, radar.measurementState.name),
            fontWeight = if (radar.measurementState == RadarMeasurementState.measuring) FontWeight.Bold else FontWeight.Normal,
        )
        Text(stringResource(R.string.radar_power, radar.powerSource.name))
        Text(stringResource(R.string.radar_model, radar.modelNumber ?: unknown))
    }
}

@Composable
private fun deviceTypeLabel(type: DeviceType): String = when (type) {
    DeviceType.smartCoach1 -> stringResource(R.string.device_type_sc1)
    DeviceType.smartCoach2 -> stringResource(R.string.device_type_sc2)
    is DeviceType.unknown -> type.identifier ?: stringResource(R.string.unknown)
}

/** Each control waits for the radar's acknowledgement; [status] shows the latest one. */
@Composable
private fun RadarSettings(radar: SmartCoachRadar, status: SettingStatus?, viewModel: HomeViewModel) {
    var low by rememberSaveable { mutableIntStateOf(RadarSpeedRange.minimumMPH) }
    var high by rememberSaveable { mutableIntStateOf(RadarSpeedRange.maximumMPH) }
    var sensitivity by rememberSaveable { mutableIntStateOf(RadarSensitivity.default.level) }
    val sc2 = radar.deviceType == DeviceType.smartCoach2

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.titleMedium)

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.settings_units), modifier = Modifier.weight(1f))
            OutlinedButton(
                onClick = { viewModel.setUnit(RadarMeasurementUnit.mph) },
                enabled = radar.measurementUnit != RadarMeasurementUnit.mph,
            ) { Text(RadarMeasurementUnit.mph.name) }
            OutlinedButton(
                onClick = { viewModel.setUnit(RadarMeasurementUnit.kph) },
                enabled = radar.measurementUnit != RadarMeasurementUnit.kph,
            ) { Text(RadarMeasurementUnit.kph.name) }
        }

        Stepper(stringResource(R.string.settings_low, low), low, RadarSpeedRange.minimumMPH..high, step = 5) { low = it }
        Stepper(stringResource(R.string.settings_high, high), high, low..RadarSpeedRange.maximumMPH, step = 5) { high = it }
        OutlinedButton(onClick = { viewModel.setSpeedRange(low, high) }) {
            Text(stringResource(R.string.settings_apply_range))
        }

        Stepper(
            stringResource(R.string.settings_sensitivity, sensitivity, RadarSensitivity.maximumLevel),
            sensitivity,
            RadarSensitivity.minimumLevel..RadarSensitivity.maximumLevel,
            enabled = !sc2,
        ) { sensitivity = it }
        OutlinedButton(onClick = { viewModel.setSensitivity(sensitivity) }, enabled = !sc2) {
            Text(stringResource(R.string.settings_apply_sensitivity))
        }
        if (sc2) {
            Text(
                stringResource(R.string.settings_sensitivity_sc2),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        status?.let {
            Text(settingStatusText(it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun settingStatusText(status: SettingStatus): String {
    val label = when (val setting = status.setting) {
        is Setting.Units -> stringResource(R.string.setting_units, setting.unit.name)
        is Setting.SpeedRange -> stringResource(R.string.setting_range, setting.range.toString())
        is Setting.Sensitivity -> stringResource(R.string.setting_sensitivity, setting.sensitivity.toString())
    }
    return when (status.result) {
        SettingStatus.Result.Waiting -> stringResource(R.string.setting_waiting, label)
        SettingStatus.Result.Acknowledged -> stringResource(R.string.setting_acknowledged, label)
        SettingStatus.Result.Failed -> stringResource(R.string.setting_failed, label)
    }
}

@Composable
private fun Stepper(
    label: String,
    value: Int,
    range: IntRange,
    step: Int = 1,
    enabled: Boolean = true,
    onValueChange: (Int) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, modifier = Modifier.weight(1f))
        OutlinedButton(
            onClick = { onValueChange((value - step).coerceIn(range)) },
            enabled = enabled && value > range.first,
        ) { Text("−") }
        OutlinedButton(
            onClick = { onValueChange((value + step).coerceIn(range)) },
            enabled = enabled && value < range.last,
        ) { Text("+") }
    }
}

/** SC1 readings show "unknown · generic"; direction, type and tilt come from SC2 firmware. */
@Composable
private fun ReadingRow(reading: MeasurementData) {
    val detail = buildList {
        add(reading.direction.name)
        add(reading.velocityType.name)
        reading.tilt?.let { add(stringResource(R.string.reading_tilt, it.direction.name, it.degrees)) }
    }.joinToString(" · ")
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(
            "%.1f %s".format(reading.measurement.value, reading.measurement.unit.name),
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Same RSSI buckets as the iOS sample. */
private enum class SignalLevel(val bars: Int) {
    None(0), Weak(1), Low(2), Medium(3), Strong(4);

    companion object {
        fun fromRssi(rssi: Int) = when {
            rssi < -90 -> None
            rssi < -70 -> Weak
            rssi < -60 -> Low
            rssi < -50 -> Medium
            else -> Strong
        }
    }
}

@Composable
private fun SignalStrength(level: SignalLevel) {
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.size(24.dp)) {
        for (bar in 1..4) {
            val color = if (bar <= level.bars) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
            Box(Modifier.width(4.dp).height((6 * bar).dp).background(color))
        }
    }
}
