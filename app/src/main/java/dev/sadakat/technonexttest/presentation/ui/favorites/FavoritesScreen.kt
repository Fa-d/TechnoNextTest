package dev.sadakat.technonexttest.presentation.ui.favorites

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.sadakat.technonexttest.domain.model.Post
import dev.sadakat.technonexttest.presentation.ui.components.PostItem
import dev.sadakat.technonexttest.presentation.viewmodel.FavoritesViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    onPostClick: (Post) -> Unit,
    favoritesViewModel: FavoritesViewModel = hiltViewModel(),
) {
    val uiState by favoritesViewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by favoritesViewModel.searchQuery.collectAsStateWithLifecycle()

    val onSearchQueryChange =
        remember { { query: String -> favoritesViewModel.updateSearchQuery(query) } }
    val onClearSearch = remember { { favoritesViewModel.clearSearch() } }
    val onFavoriteClick = remember {
        { postId: Int ->
            favoritesViewModel.toggleFavorite(postId)
        }
    }

    val bodyLargeStyle = MaterialTheme.typography.bodyLarge
    val bodyMediumStyle = MaterialTheme.typography.bodyMedium
    val outlineColor = MaterialTheme.colorScheme.outline

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = { Text("Favorites") })

        if (uiState.favoritePosts.isNotEmpty()) {
            SearchBar(
                searchQuery = searchQuery,
                onSearchQueryChange = onSearchQueryChange,
                onClearSearch = onClearSearch,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                uiState.favoritePosts.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Filled.Favorite,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No favorite posts yet",
                                style = bodyLargeStyle,
                                color = outlineColor
                            )
                            Text(
                                text = "Tap the heart icon on posts to add them here",
                                style = bodyMediumStyle,
                                color = outlineColor
                            )
                        }
                    }
                }

                uiState.filteredPosts.isEmpty() && uiState.searchQuery.isNotBlank() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No favorites match your search", style = bodyLargeStyle
                            )
                            Text(
                                text = "Try a different search term",
                                style = bodyMediumStyle,
                                color = outlineColor
                            )
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(
                            items = uiState.filteredPosts, key = { post -> post.id }) { post ->
                            PostItem(
                                post = post,
                                onPostClick = onPostClick,
                                onFavoriteClick = onFavoriteClick
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val textFieldColors = TextFieldDefaults.colors(
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
    )

    TextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        placeholder = { Text("Search favorites...") },
        leadingIcon = {
            Icon(Icons.Filled.Search, contentDescription = "Search")
        },
        trailingIcon = {
            if (searchQuery.isNotBlank()) {
                IconButton(onClick = onClearSearch) {
                    Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                }
            }
        },
        colors = textFieldColors,
        modifier = modifier.fillMaxWidth()
    )
}