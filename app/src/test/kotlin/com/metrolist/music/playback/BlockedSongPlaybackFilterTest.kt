/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.playback

import com.metrolist.innertube.models.Artist
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.filterBlocked
import com.metrolist.music.extensions.toMediaItem
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.playback.queues.Queue
import com.metrolist.music.playback.queues.filterBlocked
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = android.app.Application::class)
class BlockedSongPlaybackFilterTest {
    private fun item(id: String) =
        MediaMetadata(id = id, title = "Title $id", artists = emptyList(), duration = 180).toMediaItem()

    private val queueItems = listOf(item("song_1"), item("song_2"), item("song_3"), item("song_4"))

    @Test
    fun `Queue Status filterBlocked preserves active song if not blocked`() {
        val blockedIds = setOf("song_1", "song_3")
        val status = Queue.Status(
            title = "Test Queue",
            items = queueItems,
            mediaItemIndex = 1, // song_2
            position = 3000L,
        ).filterBlocked(blockedIds)

        assertEquals(listOf("song_2", "song_4"), status.items.map { it.mediaId })
        assertEquals("song_2", status.items[status.mediaItemIndex].mediaId)
        assertEquals(3000L, status.position)
    }

    @Test
    fun `Queue Status filterBlocked advances to next unblocked song when current song is blocked`() {
        val blockedIds = setOf("song_2")
        val status = Queue.Status(
            title = "Test Queue",
            items = queueItems,
            mediaItemIndex = 1, // song_2 is blocked
            position = 5000L,
        ).filterBlocked(blockedIds)

        assertEquals(listOf("song_1", "song_3", "song_4"), status.items.map { it.mediaId })
        // When song_2 was at index 1 and removed, the index stays at keptBefore count, pointing to song_3
        assertEquals("song_3", status.items[status.mediaItemIndex].mediaId)
        assertEquals(0L, status.position)
    }

    @Test
    fun `Queue Status filterBlocked handles all songs being blocked`() {
        val blockedIds = setOf("song_1", "song_2", "song_3", "song_4")
        val status = Queue.Status(
            title = "Test Queue",
            items = queueItems,
            mediaItemIndex = 0,
            position = 0L,
        ).filterBlocked(blockedIds)

        assertTrue(status.items.isEmpty())
        assertEquals(0, status.mediaItemIndex)
    }

    @Test
    fun `List MediaItem filterBlocked excludes blocked items`() {
        val blockedIds = setOf("song_2", "song_4")
        val filtered = queueItems.filterBlocked(blockedIds)

        assertEquals(listOf("song_1", "song_3"), filtered.map { it.mediaId })
    }

    @Test
    fun `List YTItem filterBlocked excludes blocked song items`() {
        val ytSongs = listOf(
            SongItem("s1", "Song 1", listOf(Artist("A1", null)), null, 100, null, null, null, "", false),
            SongItem("s2", "Song 2", listOf(Artist("A2", null)), null, 100, null, null, null, "", false),
            SongItem("s3", "Song 3", listOf(Artist("A3", null)), null, 100, null, null, null, "", false),
        )

        val filtered = ytSongs.filterBlocked(setOf("s2"))
        assertEquals(listOf("s1", "s3"), filtered.map { it.id })
    }
}
