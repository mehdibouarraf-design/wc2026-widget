package com.mavis.wc2026.data

import retrofit2.http.GET

/**
 * Free, no-key public API for the 2026 FIFA World Cup.
 * Docs: https://github.com/rezarahiminia/worldcup2026
 */
interface WcApi {
    @GET("get/games")
    suspend fun getGames(): List<Game>

    @GET("get/groups")
    suspend fun getGroups(): List<GroupStanding>

    @GET("get/teams")
    suspend fun getTeams(): List<Team>
}
