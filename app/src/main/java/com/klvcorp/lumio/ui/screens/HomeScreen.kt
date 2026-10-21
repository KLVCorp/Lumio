package com.klvcorp.lumio.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.klvcorp.lumio.LumioController
import com.klvcorp.lumio.R
import com.klvcorp.lumio.bluetooth.ElkBledomProtocol
import com.klvcorp.lumio.devices.SavedDevice
import com.klvcorp.lumio.ui.components.HueWheel
import com.klvcorp.lumio.ui.components.PresetDot
import com.klvcorp.lumio.effects.LightEffect
import com.klvcorp.lumio.automation.TimerMode
import com.klvcorp.lumio.automation.ScheduleAction
import kotlin.math.roundToInt

private enum class Page { HOME, DISCOVER, EFFECTS, PROGRAMS, SETTINGS }

@Composable
fun LumioApp(controller: LumioController) {
    var page by remember { mutableStateOf(Page.HOME) }
    val ctx = LocalContext.current
    val btPermissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (controller.ble.hasPermissions()) controller.ble.startScan()
    }
    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted && controller.ble.isReady) controller.startMicrophone()
    }
    LaunchedEffect(controller) { controller.reconnectIfEnabled() }
    fun scan() {
        if (controller.ble.hasPermissions()) controller.ble.startScan()
        else btPermissions.launch(controller.ble.permissions())
    }
    fun requestMic() {
        if (ctx.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) controller.startMicrophone()
        else micPermission.launch(Manifest.permission.RECORD_AUDIO)
    }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Lumio", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary)
                        Text("KLVCorp by Sckewe", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(remember(ctx) { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "1.0" }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                val tabs = listOf(
                    Triple(Page.HOME, "⌂", R.string.nav_home),
                    Triple(Page.DISCOVER, "+", R.string.nav_devices),
                    Triple(Page.EFFECTS, "◈", R.string.effects),
                    Triple(Page.PROGRAMS, "◷", R.string.automations),
                    Triple(Page.SETTINGS, "⚙", R.string.settings)
                )
                tabs.forEach { (tab, symbol, title) ->
                    NavigationBarItem(selected = page == tab, onClick = { page = tab },
                        icon = { Text(symbol, style = MaterialTheme.typography.titleLarge) },
                        label = { Text(stringResource(title), maxLines = 1) }, alwaysShowLabel = false)
                }
            }
        }
    ) { insets ->
        val modifier = Modifier.fillMaxSize().padding(insets)
        when (page) {
            Page.HOME -> HomeDashboard(controller, modifier, { page = Page.DISCOVER })
            Page.DISCOVER -> DevicesPage(controller, modifier, ::scan)
            Page.EFFECTS -> EffectsPage(controller, modifier, ::requestMic)
            Page.PROGRAMS -> ProgramsPage(controller, modifier)
            Page.SETTINGS -> SettingsPage(controller, modifier)
        }
    }
}

@Composable
internal fun Panel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable
internal fun SectionTitle(label: String) { Text(label, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }

@Composable
internal fun TinyHint(text: String) { Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }

@Composable
private fun ColorChip(color: Int, selected: Boolean, diameter: Int, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(diameter.dp),
        shape = CircleShape,
        color = Color(color),
        border = BorderStroke(if (selected) 2.dp else 1.dp,
            if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
    ) {}
}

@Composable
private fun HomeDashboard(ctrl: LumioController, modifier: Modifier, onAdd: () -> Unit) {
    var renaming by remember { mutableStateOf<SavedDevice?>(null) }
    var renameValue by remember { mutableStateOf("") }
    var confirmForget by remember { mutableStateOf<SavedDevice?>(null) }
    var sceneDialog by remember { mutableStateOf(false) }
    var sceneName by remember { mutableStateOf("") }
    var removeFavorite by remember { mutableStateOf<Int?>(null) }
    LazyColumn(modifier.padding(horizontal = 18.dp), contentPadding = PaddingValues(vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween) {
                SectionTitle(stringResource(R.string.my_devices))
                TextButton(onClick = onAdd) { Text("+ " + stringResource(R.string.add_device)) }
            }
        }
        if (ctrl.devices.saved.isEmpty()) {
            item { Panel {
                Text(stringResource(R.string.no_devices))
                Button(onClick = onAdd) { Text(stringResource(R.string.search_devices)) }
            } }
        }
        items(ctrl.devices.saved, key = { it.address }) { item ->
            val active = ctrl.ble.isReady && ctrl.ble.connectedAddress == item.address
            Panel {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text(item.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        TinyHint(item.name + " · " + item.address)
                    }
                    Text(if (active) "●" else "○", color = if (active) Color(0xFF66DFB5) else MaterialTheme.colorScheme.outline)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (active) OutlinedButton(onClick = { ctrl.ble.disconnect() }) { Text(stringResource(R.string.disconnect)) }
                    else Button(onClick = { ctrl.ble.connect(item.address) }, enabled = ctrl.ble.hasPermissions()) {
                        Text(stringResource(R.string.connect))
                    }
                    TextButton(onClick = { renaming = item; renameValue = item.nickname }) { Text(stringResource(R.string.rename)) }
                    TextButton(onClick = { confirmForget = item }) { Text(stringResource(R.string.forget)) }
                }
            }
        }
        if (ctrl.ble.isReady) {
            item { Panel {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    SectionTitle(stringResource(R.string.light_control))
                    Switch(checked = ctrl.powered, onCheckedChange = { ctrl.power(it) })
                }
                TinyHint(stringResource(R.string.last_requested_notice))
                if (ctrl.powered) {
                    HueWheel(ctrl.color, ctrl::chooseColor, Modifier.fillMaxWidth().height(240.dp))
                    val hex = String.format("#%06X", 0xFFFFFF and ctrl.color)
                    Text(hex, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    var manualHex by remember { mutableStateOf("") }
                    var colorInputError by remember { mutableStateOf(false) }
                    OutlinedTextField(
                        value = manualHex,
                        onValueChange = { manualHex = it.take(7); colorInputError = false },
                        label = { Text(stringResource(R.string.v04_hex_color)) },
                        placeholder = { Text("#FF0000") },
                        singleLine = true,
                        isError = colorInputError,
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            TextButton(onClick = {
                                val raw = manualHex.trim().removePrefix("#")
                                val parsed = if (raw.length == 6 && raw.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' })
                                    raw.toLongOrNull(16) else null
                                if (parsed != null) {
                                    ctrl.chooseColor((0xFF000000L or parsed).toInt())
                                    colorInputError = false
                                } else colorInputError = true
                            }) { Text(stringResource(R.string.v04_apply_color)) }
                        }
                    )
                    val presets = listOf(0xFFFF0000.toInt(), 0xFFFF8800.toInt(), 0xFFFFFF00.toInt(),
                        0xFF00FF00.toInt(), 0xFF0000FF.toInt(), 0xFFAA00FF.toInt(), 0xFFFFFFFF.toInt())
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        presets.forEach { preset ->
                            ColorChip(preset, ctrl.color == preset, 33) { ctrl.chooseColor(preset) }
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.favorite_colors), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        TextButton(
                            onClick = { ctrl.favorites.add(ctrl.color) },
                            enabled = !ctrl.favorites.colors.contains(ctrl.color) && ctrl.favorites.colors.size < 12
                        ) { Text(stringResource(R.string.favorite_add)) }
                    }
                    if (ctrl.favorites.colors.isEmpty()) TinyHint(stringResource(R.string.favorite_empty))
                    else Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ctrl.favorites.colors.forEach { favorite ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                ColorChip(favorite, ctrl.color == favorite, 34) { ctrl.chooseColor(favorite) }
                                // Dedicated delete control so tapping the color always applies it.
                                TextButton(
                                    onClick = { removeFavorite = favorite },
                                    modifier = Modifier.height(30.dp),
                                    contentPadding = PaddingValues(horizontal = 5.dp, vertical = 0.dp)
                                ) { Text("×", style = MaterialTheme.typography.titleMedium) }
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(R.string.brightness)); Text("${ctrl.brightness}%")
                    }
                    Slider(value = ctrl.brightness.toFloat(), onValueChange = { ctrl.setBrightness(it.roundToInt()) }, valueRange = 1f..100f)
                }
            } }
            item { Panel {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.scenes), fontWeight = FontWeight.SemiBold)
                    TextButton(onClick = { sceneName = ""; sceneDialog = true }) { Text("+ " + stringResource(R.string.save_scene)) }
                }
                if (ctrl.scenes.scenes.isEmpty()) TinyHint(stringResource(R.string.no_scenes))
                ctrl.scenes.scenes.forEach { scene ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(24.dp).background(Color(scene.color), RoundedCornerShape(12.dp)))
                        Spacer(Modifier.width(10.dp))
                        TextButton(onClick = {
                            ctrl.chooseColor(scene.color); ctrl.setBrightness(scene.brightness)
                        }, modifier = Modifier.weight(1f)) { Text("${scene.title} · ${scene.brightness}%") }
                        TextButton(onClick = { ctrl.scenes.remove(scene.title) }) { Text("×") }
                    }
                }
            } }
        } else {
            item { TinyHint(stringResource(R.string.connect_to_control) + " " + statusText(ctrl)) }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
    removeFavorite?.let { favorite ->
        AlertDialog(
            onDismissRequest = { removeFavorite = null },
            title = { Text(stringResource(R.string.favorite_remove_title)) },
            text = { Text(String.format("#%06X", favorite and 0xFFFFFF)) },
            confirmButton = { TextButton(onClick = { ctrl.favorites.remove(favorite); removeFavorite = null }) { Text(stringResource(R.string.favorite_remove)) } },
            dismissButton = { TextButton(onClick = { removeFavorite = null }) { Text(stringResource(R.string.cancel)) } }
        )
    }
    if (renaming != null) AlertDialog(onDismissRequest = { renaming = null }, title = { Text(stringResource(R.string.rename)) },
        text = { OutlinedTextField(renameValue, { renameValue = it }, singleLine = true, label = { Text(stringResource(R.string.device_name)) }) },
        confirmButton = { TextButton(onClick = { renaming?.let { ctrl.devices.rename(it.address, renameValue) }; renaming = null }) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(onClick = { renaming = null }) { Text(stringResource(R.string.cancel)) } })
    if (confirmForget != null) AlertDialog(onDismissRequest = { confirmForget = null },
        title = { Text(stringResource(R.string.forget_device)) },
        text = { Text(confirmForget!!.displayName) },
        confirmButton = { TextButton(onClick = {
            confirmForget?.let { if (ctrl.ble.connectedAddress == it.address) ctrl.ble.disconnect(); ctrl.devices.remove(it.address) }
            confirmForget = null
        }) { Text(stringResource(R.string.forget)) } },
        dismissButton = { TextButton(onClick = { confirmForget = null }) { Text(stringResource(R.string.cancel)) } })
    if (sceneDialog) AlertDialog(onDismissRequest = { sceneDialog = false }, title = { Text(stringResource(R.string.save_scene)) },
        text = { OutlinedTextField(sceneName, { sceneName = it }, singleLine = true, label = { Text(stringResource(R.string.scene_name)) }) },
        confirmButton = { TextButton(enabled = sceneName.isNotBlank(), onClick = {
            ctrl.scenes.add(sceneName, ctrl.color, ctrl.brightness); sceneDialog = false
        }) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(onClick = { sceneDialog = false }) { Text(stringResource(R.string.cancel)) } })
}

@Composable
private fun DevicesPage(ctrl: LumioController, modifier: Modifier, scan: () -> Unit) {
    var showAll by remember { mutableStateOf(false) }
    val ctx = LocalContext.current
    val found = ctrl.ble.devices.filter { showAll || ElkBledomProtocol.compatible(it.name) }
    LazyColumn(modifier.padding(horizontal = 18.dp), contentPadding = PaddingValues(vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { SectionTitle(stringResource(R.string.find_devices)) }
        item { Panel {
            Text(statusText(ctrl))
            TinyHint(stringResource(R.string.total_found, ctrl.ble.devices.size))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = scan, enabled = !ctrl.ble.isScanning) { Text(stringResource(R.string.search_devices)) }
                OutlinedButton(onClick = { ctrl.ble.stopScan() }, enabled = ctrl.ble.isScanning) { Text(stringResource(R.string.stop)) }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.show_all), modifier = Modifier.weight(1f))
                Switch(checked = showAll, onCheckedChange = { showAll = it })
            }
            TinyHint(stringResource(R.string.bluetooth_note))
            if (!ctrl.ble.hasPermissions()) {
                Text(stringResource(R.string.permission_help))
                TextButton(onClick = {
                    ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}")))
                }) { Text(stringResource(R.string.open_settings)) }
            }
        } }
        if (ctrl.ble.connectedAddress != null) item { Panel {
            Text(stringResource(R.string.connected_to, ctrl.ble.connectedAddress.orEmpty()), fontWeight = FontWeight.SemiBold)
            Text(if (ctrl.ble.isReady) stringResource(R.string.ready) else statusText(ctrl))
            TinyHint(ctrl.ble.serviceDetails)
            if (ctrl.ble.commandStatus.isNotBlank()) TinyHint(ctrl.ble.commandStatus)
            OutlinedButton(onClick = { ctrl.ble.disconnect() }) { Text(stringResource(R.string.disconnect)) }
        } }
        items(found, key = { it.address }) { device ->
            val compatible = ElkBledomProtocol.compatible(device.name)
            Panel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(device.name.ifBlank { stringResource(R.string.unknown_ble) }, fontWeight = FontWeight.Bold)
                        TinyHint(device.address + " · RSSI ${device.rssi}")
                    }
                    if (compatible) Text("●", color = Color(0xFF66DFB5))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = { ctrl.ble.connect(device.address) }, enabled = ctrl.ble.connectedAddress != device.address) {
                        Text(stringResource(R.string.connect))
                    }
                    if (!compatible) TinyHint(stringResource(R.string.unknown_protocol))
                }
            }
        }
        if (found.isEmpty() && !ctrl.ble.isScanning) item { TinyHint(stringResource(R.string.no_results)) }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
private fun statusText(ctrl: LumioController): String = when (ctrl.ble.status) {
    "idle" -> stringResource(R.string.ble_idle)
    "scanning" -> stringResource(R.string.ble_scanning)
    "connecting" -> stringResource(R.string.ble_connecting)
    "discovering" -> stringResource(R.string.ble_discovering)
    "connected" -> stringResource(R.string.connected)
    "disconnected" -> stringResource(R.string.disconnected)
    "permission_required" -> stringResource(R.string.permissions_missing)
    "bluetooth_off" -> stringResource(R.string.bluetooth_disabled)
    "unsupported_characteristic" -> stringResource(R.string.unknown_protocol)
    else -> ctrl.ble.status
}
