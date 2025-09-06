package dev.sadakat.technonexttest.presentation.ui.posts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import dev.sadakat.technonexttest.domain.model.Post
import dev.sadakat.technonexttest.presentation.ui.components.NetworkStatusIndicator
import dev.sadakat.technonexttest.presentation.ui.components.PostItem
import dev.sadakat.technonexttest.presentation.viewmodel.NetworkViewModel
import dev.sadakat.technonexttest.presentation.viewmodel.PostsViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostsScreen(
    onPostClick: (Post) -> Unit,
    postsViewModel: PostsViewModel = hiltViewModel(),
    networkViewModel: NetworkViewModel = hiltViewModel()
) {
    val lazyPagingItems = postsViewModel.pagingPosts.collectAsLazyPagingItems()
    val isOnline by networkViewModel.isOnline.collectAsStateWithLifecycle()
    val pullToRefreshState = rememberPullToRefreshState()
    val isRefreshing = lazyPagingItems.loadState.refresh is LoadState.Loading

    val onFavoriteClick = remember {
        { postId: Int ->
            postsViewModel.toggleFavorite(postId)
        }
    }

    val bodyLargeStyle = MaterialTheme.typography.bodyLarge
    val bodyMediumStyle = MaterialTheme.typography.bodyMedium

    LaunchedEffect(pullToRefreshState.isRefreshing) {
        if (pullToRefreshState.isRefreshing) {
            lazyPagingItems.refresh()
        }
    }

    LaunchedEffect(isRefreshing) {
        if (!isRefreshing) {
            pullToRefreshState.endRefresh()
        }
    }


    Column(modifier = Modifier.fillMaxSize()) {
        NetworkStatusIndicator(isOnline = isOnline)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(pullToRefreshState.nestedScrollConnection)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                items(
                    count = lazyPagingItems.itemCount,
                    key = { index -> lazyPagingItems[index]?.id ?: "loading_$index" }) { index ->
                    val post = lazyPagingItems[index]
                    post?.let { currentPost ->
                        PostItem(
                            post = currentPost,
                            onPostClick = onPostClick,
                            onFavoriteClick = onFavoriteClick
                        )
                    }
                }

                lazyPagingItems.loadState.let { loadState ->
                    when {
                        loadState.refresh is LoadState.Loading && lazyPagingItems.itemCount == 0 -> {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillParentMaxSize()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                        }

                        loadState.append is LoadState.Loading -> {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }

                        loadState.refresh is LoadState.Error && lazyPagingItems.itemCount == 0 -> {
                            val error = loadState.refresh as LoadState.Error
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillParentMaxSize()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "Failed to load posts", style = bodyLargeStyle
                                        )
                                        Text(
                                            text = error.error.localizedMessage ?: "Unknown error",
                                            style = bodyMediumStyle,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                        Button(
                                            onClick = { lazyPagingItems.retry() }) {
                                            Text("Retry")
                                        }
                                    }
                                }
                            }
                        }

                        loadState.append is LoadState.Error -> {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "Failed to load more",
                                            style = bodyMediumStyle,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                        TextButton(
                                            onClick = { lazyPagingItems.retry() }) {
                                            Text("Retry")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (pullToRefreshState.isRefreshing) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                )
            }
        }
    }
}

