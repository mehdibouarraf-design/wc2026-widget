package com.mavis.wc2026.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Single source of truth — fetches live data from worldcup26.ir, picks the
 * "current" or "next" match, and exposes group standings for a chosen group.
 *
 * FIX: API local_date is UTC ("06/11/2026 13:00").
 *      We parse it as UTC and convert to device local time for display.
 *
 * "Current match" rules:
 *  1. any match where time_elapsed is a running minute ("67", "90+2" …) -> live
 *  2. else the soonest match whose UTC timestamp is in the future
 *  3. else the most recently finished match
 */
class WcRepository(
    private val api: WcApi = ApiClient.api
) {

    suspend fun loadAll(): Snapshot {
        val games  = runCatching { api.getGames()  }.getOrDefault(emptyList())
        val groups = runCatching { api.getGroups() }.getOrDefault(emptyList())
        val teams  = runCatching { api.getTeams()  }.getOrDefault(emptyList())

        val featured = pickFeatured(games)
        return Snapshot(
            games    = games,
            groups   = groups,
            teams    = teams,
            featured = featured
        )
    }

    fun pickFeatured(games: List<Game>): Game? {
        if (games.isEmpty()) return null

        // 1) live
        val live = games.firstOrNull { g ->
            val t = g.timeElapsed?.lowercase().orEmpty()
            t.isNotBlank() && t != "notstarted" && t != "finished" && t != "ft" && t != "ns"
        }
        if (live != null) return live

        // 2) next upcoming (by UTC epoch)
        val now = System.currentTimeMillis()
        val upcoming = games
            .mapNotNull { g -> parseUtcEpoch(g.localDate)?.let { g to it } }
            .filter  { it.second >= now }
            .minByOrNull { it.second }
            ?.first
        if (upcoming != null) return upcoming

        // 3) most recently finished
        return games
            .mapNotNull { g -> parseUtcEpoch(g.localDate)?.let { g to it } }
            .maxByOrNull { it.second }
            ?.first
    }

    /** Returns the next N upcoming games (after 'now'), sorted by kick-off. */
    fun upcomingGames(games: List<Game>, limit: Int = 5): List<Game> {
        val now = System.currentTimeMillis()
        return games
            .mapNotNull { g -> parseUtcEpoch(g.localDate)?.let { g to it } }
            .filter  { it.second >= now }
            .sortedBy { it.second }
            .take(limit)
            .map { it.first }
    }

    /**
     * API date format: "MM/dd/yyyy HH:mm" in UTC.
     * Returns epoch millis (UTC) or null on parse error.
     */
    fun parseUtcEpoch(raw: String?): Long? {
        if (raw.isNullOrBlank()) return null
        val patterns = listOf("MM/dd/yyyy HH:mm", "MM/dd/yyyy")
        for (p in patterns) {
            try {
                val sdf = SimpleDateFormat(p, Locale.US)
                sdf.timeZone = TimeZone.getTimeZone("UTC")
                return sdf.parse(raw)?.time
            } catch (_: Throwable) { /* try next */ }
        }
        return null
    }

    /**
     * Converts a UTC epoch to a display string in the device's local timezone.
     * Returns e.g. "Jun 11 · 15:00" or "—" on error.
     */
    fun formatLocalDateTime(epoch: Long?): String {
        if (epoch == null) return "—"
        val sdf = SimpleDateFormat("MMM dd · HH:mm", Locale.US)
        sdf.timeZone = TimeZone.getDefault()          // device local time
        return sdf.format(Date(epoch))
    }

    /** Returns only the time portion "HH:mm" in local timezone. */
    fun formatLocalTime(epoch: Long?): String {
        if (epoch == null) return "—"
        val sdf = SimpleDateFormat("HH:mm", Locale.US)
        sdf.timeZone = TimeZone.getDefault()
        return sdf.format(Date(epoch))
    }

    /** Returns only the date portion "MMM dd" in local timezone. */
    fun formatLocalDate(epoch: Long?): String {
        if (epoch == null) return "—"
        val sdf = SimpleDateFormat("MMM dd", Locale.US)
        sdf.timeZone = TimeZone.getDefault()
        return sdf.format(Date(epoch))
    }

    data class Snapshot(
        val games    : List<Game>,
        val groups   : List<GroupStanding>,
        val teams    : List<Team>,
        val featured : Game?
    )
}
