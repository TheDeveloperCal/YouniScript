package com.youniscript.app.ui

import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleEventObserver
import java.io.File
import java.util.UUID

@Composable
fun AudioRecordingDialog(onDismiss: () -> Unit, onSave: (ByteArray) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalContext.current as? LifecycleOwner
    val activeRecorder = remember { mutableStateOf<MediaRecorder?>(null) }
    val activeFile = remember { mutableStateOf<File?>(null) }
    var isRecording by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var finished by remember { mutableStateOf(false) }
    val latestOnSave by rememberUpdatedState(onSave)

    fun finish(save: Boolean) {
        val recorder = activeRecorder.value
        val file = activeFile.value
        if (recorder == null || file == null) return
        try {
            if (isRecording) recorder.stop()
            recorder.reset()
            recorder.release()
            activeRecorder.value = null
            activeFile.value = null
            isRecording = false
            if (save && file.exists() && file.length() in 1..(25L * 1024 * 1024)) {
                latestOnSave(file.readBytes())
                finished = true
            } else if (save) {
                errorMessage = "The recording is empty or longer than the 25 MB attachment limit."
            }
        } catch (_: RuntimeException) {
            errorMessage = "The recording could not be finalized. Try another recording."
        } finally {
            runCatching { recorder.release() }
            activeRecorder.value = null
            activeFile.value = null
            isRecording = false
            file.delete()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val owner = lifecycleOwner
        if (owner == null) onDispose { finish(save = false) }
        else {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_STOP) finish(save = true)
            }
            owner.lifecycle.addObserver(observer)
            onDispose {
                owner.lifecycle.removeObserver(observer)
                finish(save = false)
            }
        }
    }

    AlertDialog(
        onDismissRequest = { finish(save = false); onDismiss() },
        title = { Text("Audio note") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(when {
                    isRecording -> "Recording on this device…"
                    finished -> "Recording saved to this page."
                    else -> "Record a private audio note. YouniScript does not transcribe or upload it."
                })
                errorMessage?.let { Text(it) }
            }
        },
        confirmButton = {
            when {
                isRecording -> Button(onClick = { finish(save = true); onDismiss() }) { Text("Stop and save") }
                finished -> TextButton(onClick = onDismiss) { Text("Done") }
                else -> Button(onClick = {
                    val output = File(context.cacheDir, "recording-${UUID.randomUUID()}.m4a")
                    val recorder = runCatching {
                        newMediaRecorder(context).apply {
                            setAudioSource(MediaRecorder.AudioSource.MIC)
                            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                            setAudioEncodingBitRate(64_000)
                            setAudioSamplingRate(44_100)
                            setOutputFile(output.absolutePath)
                            prepare()
                            start()
                        }
                    }.getOrElse {
                        output.delete()
                        errorMessage = "Microphone access is unavailable. Check Android's permission setting and try again."
                        null
                    }
                    if (recorder != null) {
                        activeRecorder.value = recorder
                        activeFile.value = output
                        isRecording = true
                        errorMessage = null
                    }
                }) { Text("Start recording") }
            }
        },
        dismissButton = {
            if (!finished) TextButton(onClick = { finish(save = false); onDismiss() }) { Text("Cancel") }
        },
    )
}

@Suppress("DEPRECATION")
private fun newMediaRecorder(context: android.content.Context): MediaRecorder =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else MediaRecorder()

@Composable
fun AudioAttachmentControls(uri: String) {
    val player = remember(uri) { mutableStateOf<MediaPlayer?>(null) }
    var playing by remember(uri) { mutableStateOf(false) }
    DisposableEffect(uri) {
        onDispose { runCatching { player.value?.release() }; player.value = null }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        TextButton(onClick = {
            if (playing) {
                player.value?.pause()
                playing = false
            } else if (player.value != null) {
                player.value?.start()
                playing = true
            } else {
                runCatching {
                    val path = android.net.Uri.parse(uri).path ?: error("Invalid audio path")
                    MediaPlayer().apply {
                        setDataSource(path)
                        setOnCompletionListener { completed -> completed.pause(); completed.seekTo(0); playing = false }
                        prepare()
                        start()
                    }.also { player.value = it; playing = true }
                }
            }
        }) { Text(if (playing) "Pause" else "Play") }
        if (player.value != null) TextButton(onClick = {
            runCatching { player.value?.stop(); player.value?.prepare() }
            playing = false
        }) { Text("Stop") }
    }
}

@Composable
fun RenameAttachmentDialog(name: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var value by remember(name) { mutableStateOf(name.substringBeforeLast('.', name)) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename audio note") },
        text = { OutlinedTextField(value, { value = it }, label = { Text("Name") }, singleLine = true) },
        confirmButton = { TextButton(onClick = { if (value.isNotBlank()) onSave(value.trim()) }, enabled = value.isNotBlank()) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
