package dev.sadakat.technonexttest.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.sadakat.technonexttest.domain.model.Post
import dev.sadakat.technonexttest.domain.usecase.posts.GetFavoritePostsUseCase
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


data class FavoritesUiState(
    val favoritePosts: List<Post> = emptyList(),
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val filteredPosts: List<Post> = emptyList()
)


@OptIn(FlowPreview::class)
@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val getFavoritePostsUseCase: GetFavoritePostsUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    init {
        observeFavoritePosts()
        observeSearch()
    }

    private fun observeFavoritePosts() {
        getFavoritePostsUseCase()
            .onEach { posts ->
                _uiState.value = _uiState.value.copy(
                    favoritePosts = posts,
                    isLoading = false
                )
                filterPosts()
            }
            .launchIn(viewModelScope)
    }

    private fun observeSearch() {
        _searchQuery
            .debounce(300)
            .distinctUntilChanged()
            .onEach { query ->
                _uiState.value = _uiState.value.copy(searchQuery = query)
                filterPosts()
            }
            .launchIn(viewModelScope)
    }

    private fun filterPosts() {
        val query = _uiState.value.searchQuery
        val filtered = if (query.isBlank()) {
            _uiState.value.favoritePosts
        } else {
            _uiState.value.favoritePosts.filter { post ->
                post.title.contains(query, ignoreCase = true) ||
                        post.body.contains(query, ignoreCase = true)
            }
        }

        _uiState.value = _uiState.value.copy(filteredPosts = filtered)
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
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