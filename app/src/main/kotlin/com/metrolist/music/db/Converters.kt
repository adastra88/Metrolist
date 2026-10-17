/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.db

import androidx.room.TypeConverter
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): LocalDateTime? =
        if (value != null) {
            LocalDateTime.ofInstant(Instant.ofEpochMilli(value), ZoneOffset.UTC)
        } else {
            null
        }

    @TypeConverter
    fun dateToTimestamp(date: LocalDateTime?): Long? =
        date?.atZone(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()

    @TypeConverter
    fun fromBlockSyncStatus(value: String?): com.metrolist.music.db.entities.BlockSyncStatus? =
        value?.let { runCatching { com.metrolist.music.db.entities.BlockSyncStatus.valueOf(it) }.getOrDefault(com.metrolist.music.db.entities.BlockSyncStatus.LOCAL_ONLY) }

    @TypeConverter
    fun blockSyncStatusToString(status: com.metrolist.music.db.entities.BlockSyncStatus?): String? =
        status?.name
}
