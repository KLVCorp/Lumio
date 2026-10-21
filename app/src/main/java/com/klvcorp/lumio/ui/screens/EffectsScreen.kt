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
import com.klvcorp.lumio.effects.LightEffect
import kotlin.math.roundToInt

@Composable
fun EffectsPage(ctrl: LumioController, modifier: Modifier, requestMicrophone: () -> Unit) {
    val names = mapOf(
        LightEffect.STATIC to R.string.effect_static,
        LightEffect.RAINBOW to R.string.effect_rainbow,
        LightEffect.BREATHING to R.string.effect_breath,
        LightEffect.PULSE to R.string.effect_pulse,
        LightEffect.STROBE to R.string.effect_strobe,
        LightEffect.CYCLE to R.string.effect_cycle
    )
    LazyColumn(modifier.padding(horizontal = 18.dp), contentPadding = PaddingValues(vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { SectionTitle(stringResource(R.string.effects)) }
        if (!ctrl.ble.isReady) item { Panel { TinyHint(stringResource(R.string.connect_to_control)) } }
        item { Panel {
            Text(stringResource(R.string.effect_modes), fontWeight = FontWeight.SemiBold)
            names.forEach { (mode, label) ->
                val selected = ctrl.effects.mode == mode && !ctrl.music.active
                OutlinedButton(
                    onClick = { ctrl.chooseEffect(mode) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = ctrl.ble.isReady,
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(label))
                        if (selected) Text("✓")
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.effect_speed))
                Text("${ctrl.effects.speed}%")
            }
            Slider(value = ctrl.effects.speed.toFloat(), onValueChange = { ctrl.effects.setSpeed(it.roundToInt()) }, valueRange = 1f..100f)
            OutlinedButton(onClick = { ctrl.nativeRainbow() }, enabled = ctrl.ble.isReady) {
                Text(stringResource(R.string.v04_native_rainbow))
            }
            TinyHint(stringResource(R.string.v04_native_hint))
            TinyHint(stringResource(R.string.effects_foreground))
        } }
        item { Panel {
            Text(stringResource(R.string.music_mode), fontWeight = FontWeight.SemiBold)
            TinyHint(stringResource(R.string.mic_explanation))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (ctrl.music.active) stringResource(R.string.microphone_active) else stringResource(R.string.microphone_off),
                    modifier = Modifier.weight(1f))
                Switch(checked = ctrl.music.active, enabled = ctrl.ble.isReady, onCheckedChange = { on ->
                    if (on) requestMicrophone() else ctrl.stopMicrophone()
                })
            }
            if (ctrl.music.active) LinearProgressIndicator(progress = { ctrl.music.level }, modifier = Modifier.fillMaxWidth())
            if (ctrl.music.error.isNotBlank()) TinyHint(ctrl.music.error)
        } }
        item { Panel {
            Text(stringResource(R.string.warning_flash), color = MaterialTheme.colorScheme.error)
            TinyHint(stringResource(R.string.warning_flash_detail))
        } }
    }
}
