/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.db.entities

import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

enum class BlockSyncStatus {
    PENDING,
    SYNCED,
    FAILED,
    LOCAL_ONLY,
}

@Immutable
@Entity(tableName = "blocked_song")
data class BlockedSongEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String? = null,
    val thumbnailUrl: String? = null,
    val duration: Int? = null,
    val blockedAt: LocalDateTime = LocalDateTime.now(),
    @ColumnInfo(defaultValue = "LOCAL_ONLY")
    val remoteStatus: BlockSyncStatus = BlockSyncStatus.LOCAL_ONLY,
)
