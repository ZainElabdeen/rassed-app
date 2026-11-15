package app.injaz.starter.ui.settings

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.injaz.starter.data.AppGraph
import app.injaz.starter.domain.BackupCodec
import app.injaz.starter.domain.BackupError
import app.injaz.starter.domain.BackupFile
import app.injaz.starter.domain.BackupResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** What the person is told after an action; the screen turns each into a string resource. */
sealed interface SettingsMessage {
    data object Saved : SettingsMessage
    data object WriteFailed : SettingsMessage
    data object ReadFailed : SettingsMessage
    data object TooLarge : SettingsMessage
    data class Rejected(val error: BackupError) : SettingsMessage
    data object ImportDone : SettingsMessage
    data object ImportFailed : SettingsMessage
}

data class SettingsUiState(
    val busy: Boolean = false,
    val message: SettingsMessage? = null,
    /** A backup that passed validation and waits for the person's confirmation. Nothing is written until then. */
    val pendingImport: BackupFile? = null,
)

class SettingsViewModel : ViewModel() {
    private val repo = AppGraph.repository
    private val settings = AppGraph.settings

    val language: StateFlow<String> =
        settings.language.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "ar")

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    fun setLanguage(language: String) {
        viewModelScope.launch { settings.setLanguage(language) }
    }

    fun dismissMessage() {
        _state.update { it.copy(message = null) }
    }

    fun suggestedFileName(): String = "raseed-backup-${SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())}.json"

    /** Writes the full backup to the file the person picked. */
    fun saveTo(resolver: ContentResolver, uri: Uri) {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true, message = null) }
        viewModelScope.launch {
            val ok = try {
                val json = repo.exportJson()
                withContext(Dispatchers.IO) {
                    val out = resolver.openOutputStream(uri, "wt") ?: throw IOException("no stream")
                    out.use { it.write(json.toByteArray(Charsets.UTF_8)) }
                }
                true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                false
            }
            _state.update { it.copy(busy = false, message = if (ok) SettingsMessage.Saved else SettingsMessage.WriteFailed) }
        }
    }

    /** Writes the backup into the cache's shared folder and hands the file back, on the main thread, to be shared. */
    fun prepareShare(cacheDir: File, onReady: (File) -> Unit) {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true, message = null) }
        viewModelScope.launch {
            val file = try {
                val json = repo.exportJson()
                withContext(Dispatchers.IO) {
                    val dir = File(cacheDir, "shared")
                    dir.mkdirs()
                    val target = File(dir, suggestedFileName())
                    target.writeText(json, Charsets.UTF_8)
                    target
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
            _state.update { it.copy(busy = false, message = if (file == null) SettingsMessage.WriteFailed else null) }
            if (file != null) onReady(file)
        }
    }

    /** Reads and checks a backup file. It only fills [SettingsUiState.pendingImport]; the data on the phone is not touched. */
    fun readBackup(resolver: ContentResolver, uri: Uri) {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true, message = null, pendingImport = null) }
        viewModelScope.launch {
            val outcome: Any = try {
                withContext(Dispatchers.IO) {
                    val text = readLimited(resolver, uri)
                    if (text == null) SettingsMessage.TooLarge else BackupCodec.parseAndValidate(text)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                SettingsMessage.ReadFailed
            }
            _state.update {
                when (outcome) {
                    is BackupResult.Valid -> it.copy(busy = false, pendingImport = outcome.file)
                    is BackupResult.Invalid -> it.copy(busy = false, message = SettingsMessage.Rejected(outcome.error))
                    is SettingsMessage -> it.copy(busy = false, message = outcome)
                    else -> it.copy(busy = false, message = SettingsMessage.ReadFailed)
                }
            }
        }
    }

    fun cancelImport() {
        _state.update { it.copy(pendingImport = null) }
    }

    /** Replaces all data with the confirmed backup, inside the repository's single transaction. */
    fun confirmImport() {
        val file = _state.value.pendingImport ?: return
        _state.update { it.copy(busy = true, pendingImport = null, message = null) }
        viewModelScope.launch {
            val ok = try {
                repo.importBackup(file.data)
                true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                false
            }
            _state.update { it.copy(busy = false, message = if (ok) SettingsMessage.ImportDone else SettingsMessage.ImportFailed) }
        }
    }

    /** The file's text, or null when it is over [MAX_BYTES] (stops reading as soon as the limit is passed). */
    private fun readLimited(resolver: ContentResolver, uri: Uri): String? {
        val input = resolver.openInputStream(uri) ?: throw IOException("no stream")
        input.use { stream ->
            val out = ByteArrayOutputStream()
            val buffer = ByteArray(16 * 1024)
            var total = 0L
            while (true) {
                val n = stream.read(buffer)
                if (n < 0) break
                total += n
                if (total > MAX_BYTES) return null
                out.write(buffer, 0, n)
            }
            val text = String(out.toByteArray(), Charsets.UTF_8)
            return text.removePrefix("\uFEFF")
        }
    }

    private companion object {
        const val MAX_BYTES = 20L * 1024 * 1024
    }
}
