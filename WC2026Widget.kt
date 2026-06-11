package com.mavis.wc2026.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.action.clickable
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import coil.ImageLoader
import coil.request.ImageRequest
import coil.size.Scale
import com.mavis.wc2026.ConfigActivity
import com.mavis.wc2026.R
import com.mavis.wc2026.data.Game
import com.mavis.wc2026.data.GroupStanding
import com.mavis.wc2026.data.StandingTeam
import com.mavis.wc2026.data.Team
import com.mavis.wc2026.data.WcPrefs
import com.mavis.wc2026.data.WcRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WC2026Widget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo        = WcRepository()
        val prefs       = WcPrefs(context)
        val chosenGroup = prefs.getGroup()   // "A".."L"

        val snap         = repo.loadAll()
        val teamsById    = snap.teams.associateBy { it.id }
        val featured     = snap.featured

        // ── Next kick-off: first upcoming game that is NOT the featured game ──
        val nextGame = repo.upcomingGames(snap.games, limit = 10)
            .firstOrNull { it.id != featured?.id }
            ?: repo.upcomingGames(snap.games, limit = 1).firstOrNull()

        // ── Group standing — case-insensitive match on name field ──
        val groupStanding: GroupStanding? = snap.groups.firstOrNull {
            (it.name ?: "").trim().equals(chosenGroup.trim(), ignoreCase = true)
        }

        // ── Next 3 group matches (upcoming, same group) ──
        val groupUpcoming: List<Game> = repo.upcomingGames(snap.games, limit = 100)
            .filter { g ->
                g.group?.trim()?.equals(chosenGroup.trim(), ignoreCase = true) == true
            }
            .take(3)

        // ── Pre-load flag bitmaps via Coil ──
        val flagsByTeam: Map<String, android.graphics.Bitmap?> = withContext(Dispatchers.IO) {
            val loader = ImageLoader(context)
            val map    = HashMap<String, android.graphics.Bitmap?>()
            teamsById.values.forEach { team ->
                val url = team.flag
                if (!url.isNullOrBlank()) {
                    val req = ImageRequest.Builder(context)
                        .data(url)
                        .size(128, 85)
                        .scale(Scale.FILL)
                        .allowHardware(false)
                        .build()
                    runCatching { loader.execute(req).drawable }
                        .getOrNull()
                        ?.let { d ->
                            val bmp = (d as? android.graphics.drawable.BitmapDrawable)?.bitmap
                            map[team.id] = bmp
                        }
                }
            }
            map
        }

        provideContent {
            GlanceTheme {
                WidgetBody(
                    featured      = featured,
                    home          = featured?.let { teamsById[it.homeTeamId] },
                    away          = featured?.let { teamsById[it.awayTeamId] },
                    homeFlag      = featured?.let { flagsByTeam[it.homeTeamId] },
                    awayFlag      = featured?.let { flagsByTeam[it.awayTeamId] },
                    featuredEpoch = repo.parseUtcEpoch(featured?.localDate),
                    nextGame      = nextGame,
                    nextHome      = nextGame?.let { teamsById[it.homeTeamId] },
                    nextAway      = nextGame?.let { teamsById[it.awayTeamId] },
                    nextEpoch     = repo.parseUtcEpoch(nextGame?.localDate),
                    groupStanding = groupStanding,
                    groupLabel    = chosenGroup,
                    groupUpcoming = groupUpcoming,
                    teamsById     = teamsById,
                    flagsByTeam   = flagsByTeam,
                    repo          = repo
                )
            }
        }
    }
}

class WC2026WidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WC2026Widget()
    override fun onUpdate(
        context: Context,
        appWidgetManager: android.appwidget.AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        WidgetUpdateWorker.schedule(context)
    }
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WidgetUpdateWorker.schedule(context)
    }
}

/* ─────────────────────── Design tokens ─────────────────────── */
private val Bg                  = ColorProvider(Color(0xFF0A1020))
private val CardLiveTop         = ColorProvider(Color(0xFFFF2D55))
private val CardLiveBottom      = ColorProvider(Color(0xFF7A1E5C))
private val CardBlueTop         = ColorProvider(Color(0xFF1E40AF))
private val CardBlueBottom      = ColorProvider(Color(0xFF0A1030))
private val CardStandingsTop    = ColorProvider(Color(0xFF1A2240))
private val CardStandingsBottom = ColorProvider(Color(0xFF0A1020))

private val TextPrimary = ColorProvider(Color(0xFFFFFFFF))
private val TextSoft    = ColorProvider(Color(0xFFE6EAF5))
private val TextMuted   = ColorProvider(Color(0xFFB7C0CC))
private val TextDim     = ColorProvider(Color(0xFF8A93A6))

private val AccentRose   = ColorProvider(Color(0xFFFF2D55))
private val AccentOrange = ColorProvider(Color(0xFFFF8A3D))
private val AccentCyan   = ColorProvider(Color(0xFF2EE6FF))
private val AccentGreen  = ColorProvider(Color(0xFF34D399))

private val BorderSubtle = ColorProvider(Color(0x14FFFFFF))

/* ─────────────────────── Main body ─────────────────────────── */
@Composable
private fun WidgetBody(
    featured      : Game?,
    home          : Team?,
    away          : Team?,
    homeFlag      : android.graphics.Bitmap?,
    awayFlag      : android.graphics.Bitmap?,
    featuredEpoch : Long?,
    nextGame      : Game?,
    nextHome      : Team?,
    nextAway      : Team?,
    nextEpoch     : Long?,
    groupStanding : GroupStanding?,
    groupLabel    : String,
    groupUpcoming : List<Game>,
    teamsById     : Map<String, Team>,
    flagsByTeam   : Map<String, android.graphics.Bitmap?>,
    repo          : WcRepository
) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(Bg)
            .padding(10.dp)
    ) {

        /* ══════════════════════════════════════════════════════
           1.  LIVE / FEATURED MATCH CARD
           ══════════════════════════════════════════════════════ */
        val isLive = featured?.timeElapsed.let { t ->
            !t.isNullOrBlank() &&
            t.lowercase() != "notstarted" && t.lowercase() != "ns" &&
            t.lowercase() != "finished"   && t.lowercase() != "ft"
        }

        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(ImageProvider(if (isLive) R.drawable.bg_hero_live else R.drawable.bg_hero_blue))
                .cornerRadius(20.dp)
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Column(modifier = GlanceModifier.fillMaxWidth()) {
                // ── Header ──
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = androidx.glance.LocalContext.current.getString(R.string.header_title),
                        style = TextStyle(color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    )
                    Spacer(GlanceModifier.defaultWeight())
                    if (isLive) LiveBadge()
                    else {
                        // Show local kick-off time for upcoming match
                        val timeStr = repo.formatLocalTime(featuredEpoch)
                        Text(
                            "⏱ $timeStr",
                            style = TextStyle(color = AccentOrange, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        )
                    }
                }
                Spacer(GlanceModifier.height(8.dp))

                if (featured != null) {
                    MatchRow(team = home, flag = homeFlag, score = featured.homeScore ?: if (isLive) "0" else "")
                    Spacer(GlanceModifier.height(2.dp))
                    MatchRow(team = away, flag = awayFlag, score = featured.awayScore ?: if (isLive) "0" else "")

                    Spacer(GlanceModifier.height(8.dp))
                    Box(modifier = GlanceModifier.fillMaxWidth().height(1.dp).background(BorderSubtle)) {}
                    Spacer(GlanceModifier.height(6.dp))

                    // ── Footer: elapsed / date ── (local time!)
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isLive) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = GlanceModifier.size(7.dp).background(AccentRose).cornerRadius(4.dp)) {}
                                Spacer(GlanceModifier.width(4.dp))
                                Text(
                                    "${featured.timeElapsed}'",
                                    style = TextStyle(color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                )
                            }
                        } else {
                            Text(
                                repo.formatLocalDate(featuredEpoch),
                                style = TextStyle(color = TextSoft, fontSize = 10.sp)
                            )
                        }
                        Spacer(GlanceModifier.defaultWeight())
                        featured.group?.let {
                            Text(
                                "Group $it",
                                style = TextStyle(color = TextDim, fontSize = 10.sp)
                            )
                        }
                    }
                } else {
                    Text(
                        androidx.glance.LocalContext.current.getString(R.string.no_match),
                        style = TextStyle(color = TextPrimary, fontSize = 12.sp)
                    )
                }
            }
        }

        Spacer(GlanceModifier.height(8.dp))

        /* ══════════════════════════════════════════════════════
           2.  NEXT KICK-OFF CARD  (different game from featured)
           ══════════════════════════════════════════════════════ */
        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(ImageProvider(R.drawable.bg_hero_blue))
                .cornerRadius(20.dp)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Column(modifier = GlanceModifier.fillMaxWidth()) {
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        androidx.glance.LocalContext.current.getString(R.string.next_kickoff),
                        style = TextStyle(color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    )
                    Spacer(GlanceModifier.defaultWeight())
                    // LOCAL time — properly converted from UTC
                    Text(
                        repo.formatLocalDateTime(nextEpoch),
                        style = TextStyle(color = TextSoft, fontSize = 10.sp)
                    )
                }
                Spacer(GlanceModifier.height(4.dp))
                if (nextGame != null) {
                    val hName = nextHome?.nameEn ?: nextGame.homeNameEn ?: "—"
                    val aName = nextAway?.nameEn ?: nextGame.awayNameEn ?: "—"
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Mini flags
                        flagsByTeam[nextGame.homeTeamId]?.let {
                            Image(
                                provider = ImageProvider(it),
                                contentDescription = null,
                                modifier = GlanceModifier.size(18.dp, 12.dp).cornerRadius(2.dp)
                            )
                            Spacer(GlanceModifier.width(4.dp))
                        }
                        Text(hName, style = TextStyle(color = TextSoft, fontSize = 12.sp))
                        Text("  vs  ", style = TextStyle(color = TextDim, fontSize = 11.sp))
                        flagsByTeam[nextGame.awayTeamId]?.let {
                            Image(
                                provider = ImageProvider(it),
                                contentDescription = null,
                                modifier = GlanceModifier.size(18.dp, 12.dp).cornerRadius(2.dp)
                            )
                            Spacer(GlanceModifier.width(4.dp))
                        }
                        Text(aName, style = TextStyle(color = TextSoft, fontSize = 12.sp))
                    }
                    nextGame.group?.let {
                        Spacer(GlanceModifier.height(2.dp))
                        Text("Group $it", style = TextStyle(color = TextDim, fontSize = 9.sp))
                    }
                } else {
                    Text("No upcoming matches", style = TextStyle(color = TextMuted, fontSize = 11.sp))
                }
            }
        }

        Spacer(GlanceModifier.height(8.dp))

        /* ══════════════════════════════════════════════════════
           3.  GROUP STANDINGS
           ══════════════════════════════════════════════════════ */
        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(ImageProvider(R.drawable.bg_standings))
                .cornerRadius(20.dp)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Column(modifier = GlanceModifier.fillMaxWidth()) {
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = GlanceModifier.width(3.dp).height(14.dp).background(AccentRose).cornerRadius(2.dp)
                    ) {}
                    Spacer(GlanceModifier.width(6.dp))
                    Text(
                        androidx.glance.LocalContext.current.getString(R.string.group_standings, groupLabel),
                        style = TextStyle(color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    )
                }
                Spacer(GlanceModifier.height(8.dp))
                StandingsHeader()
                Spacer(GlanceModifier.height(4.dp))

                val rows = groupStanding?.teams.orEmpty()
                if (rows.isNotEmpty()) {
                    rows.forEachIndexed { i, t ->
                        val team = teamsById[t.teamId ?: ""]
                        StandingRow(i + 1, team, flagsByTeam[t.teamId ?: ""], t)
                    }
                } else {
                    Text(
                        "Group $groupLabel — no data yet",
                        style = TextStyle(color = TextDim, fontSize = 10.sp)
                    )
                }

                // ── Upcoming group fixtures ──
                if (groupUpcoming.isNotEmpty()) {
                    Spacer(GlanceModifier.height(8.dp))
                    Box(modifier = GlanceModifier.fillMaxWidth().height(1.dp).background(BorderSubtle)) {}
                    Spacer(GlanceModifier.height(6.dp))
                    Text(
                        "Next in Group $groupLabel",
                        style = TextStyle(color = AccentCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    )
                    Spacer(GlanceModifier.height(4.dp))
                    groupUpcoming.forEach { g ->
                        val epoch = repo.parseUtcEpoch(g.localDate)
                        val hName = teamsById[g.homeTeamId]?.nameEn ?: g.homeNameEn ?: "—"
                        val aName = teamsById[g.awayTeamId]?.nameEn ?: g.awayNameEn ?: "—"
                        Row(
                            modifier = GlanceModifier.fillMaxWidth().padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                repo.formatLocalDateTime(epoch),
                                style = TextStyle(color = AccentOrange, fontSize = 9.sp),
                                modifier = GlanceModifier.width(72.dp)
                            )
                            Text(
                                "$hName · $aName",
                                style = TextStyle(color = TextSoft, fontSize = 10.sp),
                                modifier = GlanceModifier.defaultWeight()
                            )
                        }
                    }
                }
            }
        }

        Spacer(GlanceModifier.height(4.dp))

        // Footer — opens config
        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .clickable(actionStartActivity<ConfigActivity>())
                .padding(4.dp)
        ) {
            Text(
                androidx.glance.LocalContext.current.getString(R.string.open_settings),
                style = TextStyle(color = TextDim, fontSize = 10.sp)
            )
        }
    }
}

/* ─────────────────── Match row ─────────────────── */
@Composable
private fun MatchRow(
    team  : Team?,
    flag  : android.graphics.Bitmap?,
    score : String
) {
    Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (flag != null) {
            Image(
                provider = ImageProvider(flag),
                contentDescription = null,
                modifier = GlanceModifier.size(26.dp, 17.dp).cornerRadius(3.dp)
            )
        } else {
            Box(
                modifier = GlanceModifier.size(26.dp, 17.dp)
                    .background(ImageProvider(R.drawable.bg_flag_chip)).cornerRadius(3.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    team?.fifaCode?.take(3) ?: "—",
                    style = TextStyle(color = TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                )
            }
        }
        Spacer(GlanceModifier.width(10.dp))
        Text(
            team?.nameEn ?: "—",
            style = TextStyle(color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium),
            modifier = GlanceModifier.defaultWeight()
        )
        if (!team?.fifaCode.isNullOrBlank()) {
            Text(
                team?.fifaCode ?: "",
                style = TextStyle(color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold),
                modifier = GlanceModifier.padding(end = 6.dp)
            )
        }
        if (score.isNotBlank()) {
            Text(
                score,
                style = TextStyle(color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            )
        }
    }
}

/* ─────────────────── LIVE badge ─────────────────── */
@Composable
private fun LiveBadge() {
    Row(
        modifier = GlanceModifier
            .background(ImageProvider(R.drawable.bg_live_pill))
            .cornerRadius(8.dp)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = GlanceModifier.size(6.dp).background(ColorProvider(Color(0xFFFFFFFF))).cornerRadius(3.dp)) {}
        Spacer(GlanceModifier.width(4.dp))
        Text(
            androidx.glance.LocalContext.current.getString(R.string.live),
            style = TextStyle(color = TextPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        )
    }
}

/* ─────────────────── Standings header ─────────────────── */
@Composable
private fun StandingsHeader() {
    Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("#",    style = TextStyle(color = TextDim, fontSize = 9.sp, fontWeight = FontWeight.Bold), modifier = GlanceModifier.width(16.dp))
        Spacer(GlanceModifier.width(2.dp))
        Text("TEAM", style = TextStyle(color = TextDim, fontSize = 9.sp, fontWeight = FontWeight.Bold), modifier = GlanceModifier.defaultWeight())
        Text("P",    style = TextStyle(color = TextDim, fontSize = 9.sp, fontWeight = FontWeight.Bold), modifier = GlanceModifier.width(18.dp))
        Text("GD",   style = TextStyle(color = TextDim, fontSize = 9.sp, fontWeight = FontWeight.Bold), modifier = GlanceModifier.width(22.dp))
        Text("PTS",  style = TextStyle(color = TextDim, fontSize = 9.sp, fontWeight = FontWeight.Bold), modifier = GlanceModifier.width(32.dp))
    }
}

/* ─────────────────── Standing row ─────────────────── */
@Composable
private fun StandingRow(
    rank : Int,
    team : Team?,
    flag : android.graphics.Bitmap?,
    t    : StandingTeam
) {
    Row(
        modifier = GlanceModifier.fillMaxWidth().padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = GlanceModifier.width(16.dp), contentAlignment = Alignment.CenterStart) {
            Text(
                "$rank",
                style = TextStyle(
                    color = when (rank) { 1 -> AccentCyan; 2 -> AccentGreen; else -> TextDim },
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
        Spacer(GlanceModifier.width(2.dp))
        if (flag != null) {
            Image(
                provider = ImageProvider(flag),
                contentDescription = null,
                modifier = GlanceModifier.size(18.dp, 12.dp).cornerRadius(2.dp)
            )
        } else {
            Box(modifier = GlanceModifier.size(18.dp, 12.dp)
                .background(ImageProvider(R.drawable.bg_flag_chip)).cornerRadius(2.dp)) {}
        }
        Spacer(GlanceModifier.width(6.dp))
        Text(
            team?.nameEn ?: "—",
            style = TextStyle(color = TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Medium),
            modifier = GlanceModifier.defaultWeight()
        )
        Text("${t.mp ?: "0"}", style = TextStyle(color = TextMuted, fontSize = 10.sp), modifier = GlanceModifier.width(18.dp))
        Text("${t.gd ?: "0"}", style = TextStyle(color = TextMuted, fontSize = 10.sp), modifier = GlanceModifier.width(22.dp))
        Box(
            modifier = GlanceModifier.width(32.dp).height(18.dp)
                .background(if (rank == 1) ImageProvider(R.drawable.bg_pts_pill) else ImageProvider(R.drawable.bg_pts_dim))
                .cornerRadius(6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "${t.pts ?: "0"}",
                style = TextStyle(color = TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            )
        }
    }
}

/* ─────────────────── sp helper ─────────────────── */
private val Int.sp get() = androidx.compose.ui.unit.TextUnit(
    this.toFloat(), androidx.compose.ui.unit.TextUnitType.Sp
)
