package com.klvcorp.lumio.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.klvcorp.lumio.LumioController
import com.klvcorp.lumio.R
import com.klvcorp.lumio.automation.ScheduleAction
import com.klvcorp.lumio.automation.TimerMode
import kotlin.math.roundToInt

@Composable
fun ProgramsPage(ctrl: LumioController, modifier: Modifier) {
    var timerMode by remember { mutableStateOf(TimerMode.FADE_OUT) }
    var duration by remember { mutableIntStateOf(30) }
    var scheduleEnabled by remember { mutableStateOf(ctrl.automation.scheduleEnabled) }
    var hour by remember { mutableIntStateOf(ctrl.automation.scheduleHour) }
    var minute by remember { mutableIntStateOf(ctrl.automation.scheduleMinute) }
    var action by remember { mutableStateOf(ctrl.automation.scheduleAction) }
    var weekdays by remember { mutableStateOf(ctrl.automation.scheduleWeekdays) }
    var saved by remember { mutableStateOf(false) }
    val modes = listOf(TimerMode.OFF to R.string.auto_off, TimerMode.FADE_OUT to R.string.fade_out,
        TimerMode.WAKE_UP to R.string.wake_up)
    val dayIds = listOf(R.string.mon, R.string.tue, R.string.wed, R.string.thu, R.string.fri, R.string.sat, R.string.sun)
    LazyColumn(modifier.padding(horizontal = 18.dp), contentPadding = PaddingValues(vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { SectionTitle(stringResource(R.string.automations)) }
        item { Panel {
            Text(stringResource(R.string.timer), fontWeight = FontWeight.SemiBold)
            modes.forEach { (value, name) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = timerMode == value, onClick = { timerMode = value })
                    Text(stringResource(name), modifier = Modifier.weight(1f))
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.duration))
                Text(stringResource(R.string.duration_value, duration))
            }
            Slider(value = duration.toFloat(), onValueChange = { duration = (it / 5).roundToInt() * 5 },
                valueRange = 5f..180f)
            if (ctrl.automation.active) {
                Text(stringResource(R.string.time_left, (ctrl.automation.remainingSeconds + 59) / 60))
                OutlinedButton(onClick = { ctrl.cancelSavedTimer() }) { Text(stringResource(R.string.cancel_timer)) }
            } else {
                Button(enabled = ctrl.ble.isReady, onClick = { ctrl.startTimer(timerMode, duration) }) {
                    Text(stringResource(R.string.start))
                }
            }
            if (!ctrl.ble.isReady) TinyHint(stringResource(R.string.v04_connect_timer))
            TinyHint(if (ctrl.backgroundEnabled) stringResource(R.string.v04_timer_background_on) else stringResource(R.string.timer_foreground))
        } }
        item { Panel {
            Text(stringResource(R.string.schedule), fontWeight = FontWeight.SemiBold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.schedule_enabled), modifier = Modifier.weight(1f))
                Switch(checked = scheduleEnabled, onCheckedChange = { scheduleEnabled = it; saved = false })
            }
            Text(stringResource(R.string.schedule_action))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = action == ScheduleAction.TURN_ON, onClick = { action = ScheduleAction.TURN_ON; saved = false },
                    label = { Text(stringResource(R.string.turn_on)) })
                FilterChip(selected = action == ScheduleAction.TURN_OFF, onClick = { action = ScheduleAction.TURN_OFF; saved = false },
                    label = { Text(stringResource(R.string.turn_off)) })
            }
            Text(stringResource(R.string.schedule_time, hour, minute), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.hours))
            Slider(value = hour.toFloat(), onValueChange = { hour = it.roundToInt(); saved = false }, valueRange = 0f..23f, steps = 22)
            Text(stringResource(R.string.minutes))
            Slider(value = minute.toFloat(), onValueChange = { minute = it.roundToInt(); saved = false }, valueRange = 0f..59f)
            Text(stringResource(R.string.weekdays))
            dayIds.forEachIndexed { index, label ->
                val day = index + 1
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = weekdays.contains(day), onCheckedChange = {
                        weekdays = if (it) weekdays + day else weekdays - day
                        saved = false
                    })
                    Text(stringResource(label))
                }
            }
            Button(enabled = !scheduleEnabled || weekdays.isNotEmpty(), onClick = {
                ctrl.setSchedule(scheduleEnabled, hour, minute, action, weekdays)
                saved = true
            }) { Text(stringResource(R.string.save_schedule)) }
            if (saved) TinyHint(stringResource(R.string.saved))
            TinyHint(if (ctrl.backgroundEnabled) stringResource(R.string.v04_schedule_background_on) else stringResource(R.string.schedule_foreground))
        } }
    }
}
