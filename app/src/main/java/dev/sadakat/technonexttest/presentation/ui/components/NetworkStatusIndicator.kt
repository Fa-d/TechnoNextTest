package dev.sadakat.technonexttest.presentation.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun NetworkStatusIndicator(
    isOnline: Boolean, modifier: Modifier = Modifier
) {

    val errorContainerColor = MaterialTheme.colorScheme.errorContainer
    val onErrorContainerColor = MaterialTheme.colorScheme.onErrorContainer
    val bodySmallStyle = MaterialTheme.typography.bodySmall

    AnimatedVisibility(
        visible = !isOnline, enter = slideInVertically(
            animationSpec = tween(300), initialOffsetY = { -it }), exit = slideOutVertically(
            animationSpec = tween(300), targetOffsetY = { -it }), modifier = modifier
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp), colors = CardDefaults.cardColors(
                containerColor = errorContainerColor
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.WifiOff,
                    contentDescription = "No internet connection",
                    tint = onErrorContainerColor
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "No internet connection. Showing cached data.",
                    style = bodySmallStyle,
                    color = onErrorContainerColor
                )
            }
        }
    }
}