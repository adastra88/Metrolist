/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.metrolist.music.LocalDatabase
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.LocalSyncUtils
import com.metrolist.music.R
import com.metrolist.music.constants.ListThumbnailSize
import com.metrolist.music.constants.ThumbnailCornerRadius
import com.metrolist.music.db.entities.BlockSyncStatus
import com.metrolist.music.db.entities.BlockedSongEntity
import com.metrolist.music.ui.component.EmptyPlaceholder
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.utils.backToMain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockedSongsScreen(
    navController: NavController,
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val syncUtils = LocalSyncUtils.current
    val coroutineScope = rememberCoroutineScope()

    val blockedSongs by database.allBlockedSongs().collectAsStateWithLifecycle(initialValue = emptyList())
    var showConfirmClearDialog by remember { mutableStateOf(false) }

    if (showConfirmClearDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmClearDialog = false },
            title = { Text(stringResource(R.string.unblock_all)) },
            text = { Text(stringResource(R.string.unblock_all_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmClearDialog = false
                        coroutineScope.launch(Dispatchers.IO) {
                            database.query {
                                clearBlockedSongs()
                            }
                        }
                        Toast.makeText(context, R.string.all_songs_unblocked, Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text(stringResource(R.string.unblock_all))
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmClearDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (blockedSongs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(LocalPlayerAwareWindowInsets.current.asPaddingValues()),
                contentAlignment = Alignment.Center,
            ) {
                EmptyPlaceholder(
                    icon = R.drawable.block,
                    text = stringResource(R.string.no_blocked_songs),
                )
            }
        } else {
            LazyColumn(
                contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(
                    items = blockedSongs,
                    key = { it.id },
                ) { blockedSong ->
                    BlockedSongItem(
                        blockedSong = blockedSong,
                        onUnblock = {
                            coroutineScope.launch(Dispatchers.IO) {
                                database.query {
                                    deleteBlockedSong(blockedSong.id)
                                }
                            }
                            syncUtils.dislikeSong(blockedSong.id, dislike = false)
                            Toast.makeText(context, R.string.song_unblocked, Toast.LENGTH_SHORT).show()
                        },
                        onRetrySync = {
                            syncUtils.dislikeSong(blockedSong.id, dislike = true)
                        },
                    )
                }
            }
        }

        TopAppBar(
            title = { Text(stringResource(R.string.blocked_songs)) },
            navigationIcon = {
                IconButton(
                    onClick = navController::navigateUp,
                    onLongClick = navController::backToMain,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.arrow_back),
                        contentDescription = null,
                    )
                }
            },
            actions = {
                if (blockedSongs.isNotEmpty()) {
                    IconButton(
                        onClick = { showConfirmClearDialog = true },
                        onLongClick = {},
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.delete),
                            contentDescription = stringResource(R.string.unblock_all),
                        )
                    }
                }
            },
            modifier = Modifier.windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
        )
    }
}

@Composable
private fun BlockedSongItem(
    blockedSong: BlockedSongEntity,
    onUnblock: () -> Unit,
    onRetrySync: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        AsyncImage(
            model = blockedSong.thumbnailUrl,
            contentDescription = null,
            modifier = Modifier
                .size(ListThumbnailSize)
                .clip(RoundedCornerShape(ThumbnailCornerRadius)),
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = blockedSong.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            blockedSong.artist?.let { artist ->
                Text(
                    text = artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            val (statusText, statusColor) = when (blockedSong.remoteStatus) {
                BlockSyncStatus.SYNCED -> stringResource(R.string.block_sync_synced) to MaterialTheme.colorScheme.primary
                BlockSyncStatus.PENDING -> stringResource(R.string.block_sync_pending) to MaterialTheme.colorScheme.tertiary
                BlockSyncStatus.FAILED -> stringResource(R.string.block_sync_failed) to MaterialTheme.colorScheme.error
                BlockSyncStatus.LOCAL_ONLY -> stringResource(R.string.block_sync_local) to MaterialTheme.colorScheme.outline
            }

            Text(
                text = statusText,
                style = MaterialTheme.typography.labelSmall,
                color = statusColor,
                fontWeight = FontWeight.Medium,
            )
        }

        if (blockedSong.remoteStatus == BlockSyncStatus.FAILED) {
            IconButton(
                onClick = onRetrySync,
                onLongClick = {},
            ) {
                Icon(
                    painter = painterResource(R.drawable.sync),
                    contentDescription = stringResource(R.string.retry_dislike_sync),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }

        IconButton(
            onClick = onUnblock,
            onLongClick = {},
        ) {
            Icon(
                painter = painterResource(R.drawable.close),
                contentDescription = stringResource(R.string.unblock),
            )
        }
    }
}
