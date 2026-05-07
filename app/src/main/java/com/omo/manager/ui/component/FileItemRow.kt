package com.omo.manager.ui.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.omo.manager.model.FileItem
import com.omo.manager.ui.theme.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileItemRow(
    file: FileItem,
    isSelected: Boolean,
    is_active: Boolean,
    onSelect: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onRunScript: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null
) {
    val bgColor = when {
        isSelected && is_active -> MaterialTheme.colorScheme.primaryContainer
        isSelected -> MaterialTheme.colorScheme.secondaryContainer
        is_active -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        else -> MaterialTheme.colorScheme.surface
    }

    Surface(
        color = bgColor,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    if (isSelected) onSelect() else onClick()
                },
                onLongClick = onLongClick
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when {
                    file.isDirectory -> Icons.Filled.Folder
                    file.isScriptFile -> Icons.Filled.Terminal
                    file.isTextFile -> Icons.Filled.Description
                    file.extension in listOf("png", "jpg", "jpeg", "gif", "webp", "svg") -> Icons.Filled.Image
                    file.extension in listOf("mp3", "wav", "flac", "ogg", "aac") -> Icons.Filled.AudioFile
                    file.extension in listOf("mp4", "mkv", "avi", "webm") -> Icons.Filled.VideoFile
                    file.extension in listOf("zip", "rar", "7z", "tar", "gz") -> Icons.Filled.FolderZip
                    else -> Icons.Filled.InsertDriveFile
                },
                contentDescription = null,
                tint = when {
                    file.isDirectory -> Blue600
                    file.isScriptFile -> Green500
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (file.isDirectory) Blue600
                    else MaterialTheme.colorScheme.onSurface
                )
                if (!file.isDirectory) {
                    Text(
                        text = "${file.displaySize}  ${file.permissions}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = file.permissions,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (file.isScriptFile && onRunScript != null) {
                IconButton(onClick = onRunScript, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Filled.PlayArrow,
                        contentDescription = "Run",
                        tint = Green500,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            if (file.isTextFile && onEdit != null) {
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = "Edit",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (isSelected) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
