package com.klvcorp.lumio.ui.screens

import android.app.Activity
import android.Manifest
import android.os.Build
import android.content.pm.PackageManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.klvcorp.lumio.LumioController
import com.klvcorp.lumio.LumioRuntime
import com.klvcorp.lumio.R
import com.klvcorp.lumio.devices.BackupManager

@Composable
fun SettingsPage(ctrl: LumioController, modifier: Modifier) {
    val context = LocalContext.current
    val activity = context as? Activity
    val manager = remember(context) { BackupManager(context.applicationContext) }
    val imported = stringResource(R.string.v04_import_ok)
    val failed = stringResource(R.string.v04_file_error)
    val exported = stringResource(R.string.v04_export_ok)
    val notificationDenied = stringResource(R.string.v04_notification_denied)
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) ctrl.setBackgroundEnabled(true)
        else Toast.makeText(context, notificationDenied, Toast.LENGTH_LONG).show()
    }

    val saveFile = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            val result = runCatching {
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
                    writer.write(manager.exportJson())
                } ?: error("Failed to open file")
            }
            Toast.makeText(context, if (result.isSuccess) exported else failed, Toast.LENGTH_LONG).show()
        }
    }
    val openFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val result = runCatching {
                val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: error("Failed to read file")
                // Do not leave a background Bluetooth session running while replacing configuration.
                ctrl.setBackgroundEnabled(false)
                manager.importJson(text)
                // An imported backup must never silently re-enable a stopped foreground service.
                context.getSharedPreferences("lumio_preferences", Context.MODE_PRIVATE)
                    .edit().putBoolean("keep_running", false).apply()
                LumioRuntime.reset()
            }
            Toast.makeText(context, if (result.isSuccess) imported else failed, Toast.LENGTH_LONG).show()
            if (result.isSuccess) activity?.recreate()
        }
    }

    LazyColumn(modifier.padding(horizontal = 18.dp), contentPadding = PaddingValues(vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { SectionTitle(stringResource(R.string.settings)) }
        item { Panel {
            Text(stringResource(R.string.language), fontWeight = FontWeight.SemiBold)
            listOf("system" to R.string.system_default, "ru" to R.string.russian, "en" to R.string.english).forEach { (code, id) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = ctrl.language == code, onClick = {
                        if (ctrl.language != code) { ctrl.setLanguage(code); activity?.recreate() }
                    })
                    Text(stringResource(id))
                }
            }
        } }
        item { Panel {
            Text(stringResource(R.string.theme), fontWeight = FontWeight.SemiBold)
            listOf("dark" to R.string.dark, "light" to R.string.light, "system" to R.string.system_default).forEach { (value, id) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = ctrl.theme == value, onClick = { ctrl.setTheme(value) })
                    Text(stringResource(id))
                }
            }
        } }
        item { Panel {
            Text(stringResource(R.string.bluetooth_settings), fontWeight = FontWeight.SemiBold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.auto_connect), modifier = Modifier.weight(1f))
                Switch(checked = ctrl.devices.autoConnect, onCheckedChange = { ctrl.devices.setAutoConnect(it) })
            }
            TinyHint(stringResource(R.string.auto_connect_note))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.v04_background_mode), modifier = Modifier.weight(1f))
                Switch(checked = ctrl.backgroundEnabled, enabled = ctrl.ble.hasPermissions(), onCheckedChange = { enable ->
                    if (enable && Build.VERSION.SDK_INT >= 33 &&
                        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else ctrl.setBackgroundEnabled(enable)
                })
            }
            TinyHint(stringResource(R.string.v04_background_hint))
            if (ctrl.ble.serviceDetails.isNotBlank()) {
                Text(stringResource(R.string.gatt_details), fontWeight = FontWeight.Medium)
                TinyHint(ctrl.ble.serviceDetails)
            }
            if (ctrl.ble.lastError.isNotBlank()) TinyHint(ctrl.ble.lastError)
            val message = stringResource(R.string.v04_device_report)
            OutlinedButton(onClick = {
                val diagnostics = "Lumio ${context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0"}\n${message}\n" +
                    "Status: ${ctrl.ble.status}\nReady: ${ctrl.ble.isReady}\n" +
                    "Connected: ${ctrl.ble.connectedAddress ?: "-"}\nGATT: ${ctrl.ble.serviceDetails}\n" +
                    "Last error: ${ctrl.ble.lastError}\nCommand: ${ctrl.ble.commandStatus}"
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Lumio BLE diagnostics", diagnostics))
            }) { Text(stringResource(R.string.v04_copy_diagnostics)) }
        } }
        item { Panel {
            Text(stringResource(R.string.v04_backup_heading), fontWeight = FontWeight.SemiBold)
            TinyHint(stringResource(R.string.v04_backup_hint))
            Button(onClick = { saveFile.launch("Lumio-backup.json") }) { Text(stringResource(R.string.v04_export)) }
            OutlinedButton(onClick = { openFile.launch(arrayOf("application/json", "text/plain")) }) {
                Text(stringResource(R.string.v04_import))
            }
        } }
        item { Panel {
            Text(stringResource(R.string.about), fontWeight = FontWeight.SemiBold)
            Text("Lumio · 0.4.0-alpha")
            Text("KLVCorp by Sckewe")
            Text(stringResource(R.string.license_name))
            TinyHint(stringResource(R.string.license_notice))
            TinyHint(stringResource(R.string.offline_note))
        } }
    }
}
