package dev.sadakat.technonexttest.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.sadakat.technonexttest.domain.model.Post
import dev.sadakat.technonexttest.domain.usecase.posts.SearchPostsUseCase
import dev.sadakat.technonexttest.domain.usecase.posts.ToggleFavoriteUseCase
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject


data class SearchUiState(
    val searchQuery: String = "",
    val searchResults: List<Post> = emptyList(),
    val isSearching: Boolean = false,
    val hasSearched: Boolean = false
)

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchPostsUseCase: SearchPostsUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    init {
        _searchQuery.debounce(300)
            .distinctUntilChanged().onEach { query ->
                _uiState.value = _uiState.value.copy(
                    searchQuery = query,
                    isSearching = query.isNotBlank(),
                    hasSearched = query.isNotBlank()
                )
                if (query.isNotBlank()) {
                    searchPosts(query)
                } else {
                    _uiState.value = _uiState.value.copy(
                        searchResults = emptyList(), isSearching = false, hasSearched = false
                    )
                }
            }.launchIn(viewModelScope)
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    private fun searchPosts(query: String) {
        searchPostsUseCase(query).onEach { posts ->
            _uiState.value = _uiState.value.copy(
                searchResults = posts, isSearching = false
            )
        }.launchIn(viewModelScope)
    }

    fun clearSearch() {
        _searchQuery.value = ""
    }

    fun toggleFavorite(postId: Int) {
        viewModelScope.launch {
            toggleFavoriteUseCase(postId)
        }
    }
}