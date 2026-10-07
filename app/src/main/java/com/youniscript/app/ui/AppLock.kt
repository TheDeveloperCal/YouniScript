package com.youniscript.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.youniscript.app.security.AppLockCrypto
import com.youniscript.app.ui.theme.YouniColors

@Composable
fun AppLockScreen(
    onUnlock: (CharArray) -> Boolean,
    onBiometric: () -> Unit,
) {
    var pin by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    Surface(Modifier.fillMaxSize(), color = YouniColors.library) {
        Column(
            Modifier.fillMaxSize().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("YouniScript", color = YouniColors.ink, fontFamily = androidx.compose.ui.text.font.FontFamily.Serif, fontSize = 32.sp)
            Text("Your library is private.", Modifier.padding(top = 8.dp, bottom = 28.dp), color = YouniColors.mutedInk)
            OutlinedTextField(
                value = pin,
                onValueChange = { value -> if (value.length <= 12 && value.all { it in '0'..'9' }) pin = value },
                modifier = Modifier.widthIn(max = 360.dp).fillMaxWidth(),
                label = { Text("PIN") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            )
            message?.let { Text(it, Modifier.padding(top = 8.dp), color = YouniColors.error) }
            TextButton(
                onClick = {
                    val value = pin.toCharArray()
                    pin = ""
                    message = if (onUnlock(value)) null else "That PIN didn't match, or a brief security wait is active."
                    value.fill('\u0000')
                },
                enabled = pin.length >= 4,
                modifier = Modifier.padding(top = 12.dp),
            ) { Text("Unlock", color = YouniColors.sage, style = MaterialTheme.typography.titleMedium) }
            TextButton(onClick = onBiometric, modifier = Modifier.padding(top = 4.dp)) {
                Text("Use device authentication", color = YouniColors.mutedInk)
            }
        }
    }
}

@Composable
fun AppLockSettings(
    pinConfigured: Boolean,
    autoLockTimeout: Long,
    onSetPin: (currentPin: CharArray?, newPin: CharArray) -> Boolean,
    onDisable: (currentPin: CharArray) -> Boolean,
    onTimeoutChanged: (Long) -> Unit,
    onLockNow: () -> Unit,
) {
    var showPinDialog by remember { mutableStateOf(false) }
    var showDisableDialog by remember { mutableStateOf(false) }
    var timeoutMenu by remember { mutableStateOf(false) }

    Text(
        if (pinConfigured) "A PIN protects this library when you leave the app. The PIN verifier is checked with a non-exportable Android Keystore key."
        else "Set a PIN to lock YouniScript when you leave it. Your PIN itself is never saved.",
        color = YouniColors.mutedInk,
        style = MaterialTheme.typography.bodyLarge,
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = { showPinDialog = true }) {
            Text(if (pinConfigured) "Change PIN" else "Set PIN", color = YouniColors.ink)
        }
        if (pinConfigured) TextButton(onClick = { showDisableDialog = true }) {
            Text("Turn off lock", color = YouniColors.mutedInk)
        }
    }
    if (pinConfigured) {
        TextButton(onClick = onLockNow) { Text("Lock now", color = YouniColors.ink) }
        TextButton(onClick = { timeoutMenu = true }) {
            val value = when (autoLockTimeout) {
                AppLockCrypto.ONE_MINUTE -> "1 minute"
                AppLockCrypto.FIVE_MINUTES -> "5 minutes"
                AppLockCrypto.FIFTEEN_MINUTES -> "15 minutes"
                else -> "Immediately"
            }
            Text("Lock after leaving · $value", color = YouniColors.ink)
        }
        DropdownMenu(expanded = timeoutMenu, onDismissRequest = { timeoutMenu = false }) {
            listOf(
                AppLockCrypto.IMMEDIATE to "Immediately",
                AppLockCrypto.ONE_MINUTE to "1 minute",
                AppLockCrypto.FIVE_MINUTES to "5 minutes",
                AppLockCrypto.FIFTEEN_MINUTES to "15 minutes",
            ).forEach { (timeout, label) ->
                DropdownMenuItem(text = { Text(label) }, onClick = { onTimeoutChanged(timeout); timeoutMenu = false })
            }
        }
    }

    if (showPinDialog) PinSetupDialog(
        requireCurrentPin = pinConfigured,
        onDismiss = { showPinDialog = false },
        onSave = { current, next ->
            val saved = onSetPin(current, next)
            current?.fill('\u0000')
            next.fill('\u0000')
            if (saved) showPinDialog = false
            saved
        },
    )
    if (showDisableDialog) CurrentPinDialog(
        title = "Turn off app lock",
        confirmLabel = "Turn off",
        onDismiss = { showDisableDialog = false },
        onConfirm = { current ->
            val removed = onDisable(current)
            current.fill('\u0000')
            if (removed) showDisableDialog = false
            removed
        },
    )
}

@Composable
private fun PinSetupDialog(
    requireCurrentPin: Boolean,
    onDismiss: () -> Unit,
    onSave: (CharArray?, CharArray) -> Boolean,
) {
    var current by remember { mutableStateOf("") }
    var next by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    fun dismissAndClear() {
        current = ""
        next = ""
        confirm = ""
        error = null
        onDismiss()
    }
    val valid = AppLockCrypto.validPin(next.toCharArray()) && next == confirm && (!requireCurrentPin || AppLockCrypto.validPin(current.toCharArray()))
    AlertDialog(
        onDismissRequest = ::dismissAndClear,
        title = { Text(if (requireCurrentPin) "Change your PIN" else "Set an app PIN") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (requireCurrentPin) PinField("Current PIN", current) { current = it }
                PinField("New PIN · 4 to 12 digits", next) { next = it }
                PinField("Confirm new PIN", confirm) { confirm = it }
                error?.let { Text(it, color = YouniColors.error) }
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = {
                error = if (onSave(if (requireCurrentPin) current.toCharArray() else null, next.toCharArray())) null else "The current PIN could not be verified."
                if (error == null) dismissAndClear()
            }) { Text("Save PIN", color = YouniColors.sage) }
        },
        dismissButton = { TextButton(onClick = ::dismissAndClear) { Text("Cancel") } },
    )
}

@Composable
private fun CurrentPinDialog(
    title: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (CharArray) -> Boolean,
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    fun dismissAndClear() {
        pin = ""
        error = false
        onDismiss()
    }
    AlertDialog(
        onDismissRequest = ::dismissAndClear,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PinField("Current PIN", pin) { pin = it }
                if (error) Text("That PIN didn't match.", color = YouniColors.error)
            }
        },
        confirmButton = {
            TextButton(enabled = AppLockCrypto.validPin(pin.toCharArray()), onClick = {
                error = !onConfirm(pin.toCharArray())
                if (!error) dismissAndClear()
            }) {
                Text(confirmLabel, color = YouniColors.sage)
            }
        },
        dismissButton = { TextButton(onClick = ::dismissAndClear) { Text("Cancel") } },
    )
}

@Composable
private fun PinField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { text -> if (text.length <= 12 && text.all { it in '0'..'9' }) onValueChange(text) },
        label = { Text(label) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        modifier = Modifier.fillMaxWidth(),
    )
}
