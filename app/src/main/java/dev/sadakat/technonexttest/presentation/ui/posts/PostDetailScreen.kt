package dev.sadakat.technonexttest.presentation.ui.posts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.sadakat.technonexttest.domain.model.Post


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    post: Post,
    onBackClick: () -> Unit,
    onFavoriteClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
    val outlineColor = MaterialTheme.colorScheme.outline
    val headlineSmallStyle = MaterialTheme.typography.headlineSmall
    val bodyLargeStyle = MaterialTheme.typography.bodyLarge
    val labelMediumStyle = MaterialTheme.typography.labelMedium
    val labelSmallStyle = MaterialTheme.typography.labelSmall

    val favoriteIcon = remember(post.isFavorite) {
        if (post.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder
    }
    val favoriteContentDescription = remember(post.isFavorite) {
        if (post.isFavorite) "Remove from favorites" else "Add to favorites"
    }
    val favoriteTint = remember(post.isFavorite) {
        if (post.isFavorite) primaryColor else onSurfaceVariantColor
    }

    val scrollState = rememberScrollState()
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        TopAppBar(title = { Text("Post Details") }, navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        }, actions = {
            IconButton(onClick = { onFavoriteClick(post.id) }) {
                Icon(
                    imageVector = favoriteIcon,
                    contentDescription = favoriteContentDescription,
                    tint = favoriteTint
                )
            }
        })

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            Text(
                text = post.title,
                style = headlineSmallStyle,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text(
                text = post.body,
                style = bodyLargeStyle,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "By User ${post.userId}", style = labelMediumStyle, color = outlineColor
                )

                Text(
                    text = "Post ID: ${post.id}", style = labelSmallStyle, color = outlineColor
                )
            }
        }
    }
}