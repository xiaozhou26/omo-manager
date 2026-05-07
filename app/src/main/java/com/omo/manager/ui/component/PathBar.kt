package com.omo.manager.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun PathBar(
    path: String,
    isActive: Boolean,
    onNavigateUp: () -> Unit,
    onPathClick: () -> Unit,
    onSyncClick: () -> Unit,
    onBookmarkClick: () -> Unit
) {
    Surface(
        color = if (isActive) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateUp, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Filled.ArrowUpward,
                    contentDescription = "Up",
                    modifier = Modifier.size(18.dp)
                )
            }

            Text(
                text = path,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onPathClick)
                    .padding(horizontal = 4.dp),
                color = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant
            )

            IconButton(onClick = onSyncClick, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Filled.Sync,
                    contentDescription = "Sync",
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(onClick = onBookmarkClick, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Filled.BookmarkBorder,
                    contentDescription = "Bookmark",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
