/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.db

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.metrolist.music.db.entities.BlockSyncStatus
import com.metrolist.music.db.entities.BlockedSongEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDateTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class)
class BlockedSongPersistenceTest {
    private lateinit var internalDb: InternalDatabase
    private lateinit var database: MusicDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        internalDb = Room.inMemoryDatabaseBuilder(context, InternalDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        database = MusicDatabase(internalDb)
    }

    @After
    fun tearDown() {
        internalDb.close()
    }

    @Test
    fun insertAndRetrieveBlockedSong() = runBlocking {
        val blockedSong = BlockedSongEntity(
            id = "song_123",
            title = "Blocked Track",
            artist = "Artist Name",
            thumbnailUrl = "https://example.com/thumb.jpg",
            duration = 210,
            blockedAt = LocalDateTime.now(),
            remoteStatus = BlockSyncStatus.PENDING,
        )

        database.insert(blockedSong)

        val retrieved = database.blockedSong("song_123").first()
        assertNotNull(retrieved)
        assertEquals("song_123", retrieved?.id)
        assertEquals("Blocked Track", retrieved?.title)
        assertEquals("Artist Name", retrieved?.artist)
        assertEquals(BlockSyncStatus.PENDING, retrieved?.remoteStatus)
    }

    @Test
    fun isBlockedReturnsCorrectBoolean() = runBlocking {
        assertFalse(database.isBlocked("song_456").first())

        database.insert(
            BlockedSongEntity(
                id = "song_456",
                title = "Test Song",
                artist = "Test Artist",
                thumbnailUrl = null,
                duration = 180,
                blockedAt = LocalDateTime.now(),
                remoteStatus = BlockSyncStatus.LOCAL_ONLY,
            )
        )

        assertTrue(database.isBlocked("song_456").first())
    }

    @Test
    fun deleteBlockedSongRemovesIt() = runBlocking {
        val blockedSong = BlockedSongEntity(
            id = "song_789",
            title = "To Be Unblocked",
            artist = "Artist",
            thumbnailUrl = null,
            duration = 150,
            blockedAt = LocalDateTime.now(),
            remoteStatus = BlockSyncStatus.SYNCED,
        )

        database.insert(blockedSong)
        assertTrue(database.isBlocked("song_789").first())

        database.deleteBlockedSong("song_789")
        assertFalse(database.isBlocked("song_789").first())
        assertNull(database.blockedSong("song_789").first())
    }

    @Test
    fun updateRemoteStatusWorksCorrectly() = runBlocking {
        val blockedSong = BlockedSongEntity(
            id = "sync_song",
            title = "Sync Test",
            artist = "Sync Artist",
            thumbnailUrl = null,
            duration = 200,
            blockedAt = LocalDateTime.now(),
            remoteStatus = BlockSyncStatus.PENDING,
        )

        database.insert(blockedSong)
        assertEquals(BlockSyncStatus.PENDING, database.blockedSong("sync_song").first()?.remoteStatus)

        database.updateBlockedSongRemoteStatus("sync_song", BlockSyncStatus.SYNCED)
        assertEquals(BlockSyncStatus.SYNCED, database.blockedSong("sync_song").first()?.remoteStatus)

        database.updateBlockedSongRemoteStatus("sync_song", BlockSyncStatus.FAILED)
        assertEquals(BlockSyncStatus.FAILED, database.blockedSong("sync_song").first()?.remoteStatus)
    }

    @Test
    fun clearBlockedSongsRemovesAllEntries() = runBlocking {
        database.insert(
            BlockedSongEntity("s1", "Song 1", "Artist", null, 100, LocalDateTime.now(), BlockSyncStatus.SYNCED)
        )
        database.insert(
            BlockedSongEntity("s2", "Song 2", "Artist", null, 100, LocalDateTime.now(), BlockSyncStatus.SYNCED)
        )

        assertEquals(2, database.allBlockedSongs().first().size)
        assertEquals(2, database.allBlockedSongIdsDirect().size)

        database.clearBlockedSongs()

        assertTrue(database.allBlockedSongs().first().isEmpty())
        assertTrue(database.allBlockedSongIdsDirect().isEmpty())
    }
}
