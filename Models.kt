package com.mavis.wc2026.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Worldcup26.ir team — bilingual with FIFA code, ISO2 and a flag URL.
 */
@JsonClass(generateAdapter = false)
data class Team(
    val id       : String,
    @Json(name = "name_en")   val nameEn   : String?,
    @Json(name = "name_fa")   val nameFa   : String?,
    @Json(name = "fifa_code") val fifaCode : String?,
    val iso2     : String?,
    val groups   : String?,
    val flag     : String?
)

/** Worldcup26.ir match */
@JsonClass(generateAdapter = false)
data class Game(
    val id            : String,
    @Json(name = "home_team_id")      val homeTeamId : String,
    @Json(name = "away_team_id")      val awayTeamId : String,
    @Json(name = "home_team_name_en") val homeNameEn : String?,
    @Json(name = "away_team_name_en") val awayNameEn : String?,
    @Json(name = "home_team_name_fa") val homeNameFa : String?,
    @Json(name = "away_team_name_fa") val awayNameFa : String?,
    @Json(name = "home_score")        val homeScore  : String?,
    @Json(name = "away_score")        val awayScore  : String?,
    @Json(name = "time_elapsed")      val timeElapsed: String?,
    val finished   : String?,
    val group      : String?,
    val matchday   : String?,
    @Json(name = "local_date") val localDate : String?,   // UTC "MM/dd/yyyy HH:mm"
    val type       : String?,
    @Json(name = "home_team_label") val homeLabel : String? = null,
    @Json(name = "away_team_label") val awayLabel : String? = null
)

/** Worldcup26.ir group with standings */
@JsonClass(generateAdapter = false)
data class GroupStanding(
    val name  : String?,
    val teams : List<StandingTeam>?
) {
    val group: String? get() = name
}

@JsonClass(generateAdapter = false)
data class StandingTeam(
    @Json(name = "team_id") val teamId : String?,
    val mp  : String?,
    val w   : String?,
    val d   : String?,
    val l   : String?,
    val pts : String?,
    val gf  : String?,
    val ga  : String?,
    val gd  : String?
)
