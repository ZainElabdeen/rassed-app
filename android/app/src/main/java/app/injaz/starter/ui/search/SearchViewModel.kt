package app.injaz.starter.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.injaz.starter.data.AppData
import app.injaz.starter.data.AppGraph
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed interface SearchUiState {
    data class Ready(val data: AppData) : SearchUiState
    data object Failed : SearchUiState
}

/** Serves the search screen the whole data snapshot; it stays current while the screen is open. null = loading. */
@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModel : ViewModel() {
    private val retryTick = MutableStateFlow(0)

    val state: StateFlow<SearchUiState?> = retryTick
        .flatMapLatest {
            AppGraph.repository.data
                .map<AppData, SearchUiState> { SearchUiState.Ready(it) }
                .catch { emit(SearchUiState.Failed) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun retry() {
        retryTick.value = retryTick.value + 1
    }
}
