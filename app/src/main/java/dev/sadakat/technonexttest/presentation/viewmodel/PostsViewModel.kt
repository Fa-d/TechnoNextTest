package dev.sadakat.technonexttest.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.sadakat.technonexttest.domain.model.Post
import dev.sadakat.technonexttest.domain.usecase.posts.GetAllPostsUseCase
import dev.sadakat.technonexttest.domain.usecase.posts.GetPaginatedPostsUseCase
import dev.sadakat.technonexttest.domain.usecase.posts.GetPostsUseCase
import dev.sadakat.technonexttest.domain.usecase.posts.LoadMorePostsUseCase
import dev.sadakat.technonexttest.domain.usecase.posts.RefreshPostsUseCase
import dev.sadakat.technonexttest.domain.usecase.posts.ToggleFavoriteUseCase
import dev.sadakat.technonexttest.util.NetworkResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject


data class PostsUiState(
    val posts: List<Post> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val isLoadingMore: Boolean = false,
    val currentPage: Int = 1,
    val hasMorePages: Boolean = true
)


@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PostsViewModel @Inject constructor(
    private val getPostsUseCase: GetPostsUseCase,
    private val getAllPostsUseCase: GetAllPostsUseCase,
    private val getPaginatedPostsUseCase: GetPaginatedPostsUseCase,
    private val refreshPostsUseCase: RefreshPostsUseCase,
    private val loadMorePostsUseCase: LoadMorePostsUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(PostsUiState())
    val uiState: StateFlow<PostsUiState> = _uiState.asStateFlow()

    val pagingPosts: Flow<PagingData<Post>> = getPaginatedPostsUseCase().cachedIn(viewModelScope)

    init {
        loadPosts()
        observePosts()
        refreshPostsInBackground()
    }

    private fun refreshPostsInBackground() {
        viewModelScope.launch {
            refreshPosts()
        }
    }

    private fun observePosts() {
        getAllPostsUseCase().onEach { posts ->
            _uiState.value = _uiState.value.copy(
                posts = posts, isLoading = false, isRefreshing = false
            )
        }.launchIn(viewModelScope)
    }

    fun loadPosts() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            when (val result = getPostsUseCase(page = 1)) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false, errorMessage = null, currentPage = 1
                    )
                }

                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false, errorMessage = result.message
                    )
                }

                is NetworkResult.Loading -> {}
            }
        }
    }

    fun refreshPosts() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true, errorMessage = null)

            when (val result = refreshPostsUseCase()) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isRefreshing = false,
                        errorMessage = null,
                        currentPage = 1,
                        hasMorePages = true
                    )
                }

                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isRefreshing = false, errorMessage = result.message
                    )
                }

                is NetworkResult.Loading -> {
                    // Already handled by isRefreshing state
                }
            }
        }
    }

    fun loadMorePosts() {
        if (_uiState.value.isLoadingMore || !_uiState.value.hasMorePages) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingMore = true)
            val nextPage = _uiState.value.currentPage + 1

            when (val result = loadMorePostsUseCase(nextPage)) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoadingMore = false,
                        currentPage = nextPage,
                        hasMorePages = result.data.isNotEmpty()
                    )
                }

                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoadingMore = false, errorMessage = result.message
                    )
                }

                is NetworkResult.Loading -> {
                    // Already handled by isLoadingMore state
                }
            }
        }
    }

    fun toggleFavorite(postId: Int) {
        viewModelScope.launch {
//            toggleFavoriteUseCase(postId)
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}