package app.injaz.starter.data

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

/** Preferences: only the language, Arabic unless the person chose English. */
class SettingsStore(private val context: Context) {
    private val languageKey = stringPreferencesKey("language")

    val language: Flow<String> = context.settingsDataStore.data.map { it[languageKey] ?: "ar" }

    suspend fun setLanguage(language: String) {
        context.settingsDataStore.edit { it[languageKey] = language }
    }
}

/** The app's singletons, built once in [app.injaz.starter.RaseedApp]. ViewModels read the repository from here. */
object AppGraph {
    lateinit var repository: Repository
        private set
    lateinit var settings: SettingsStore
        private set

    fun init(app: Application) {
        repository = Repository(AppDatabase.create(app))
        settings = SettingsStore(app)
    }
}
