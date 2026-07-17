package com.joekillion.bugholes.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "profiles")
data class ProfileEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val name: String, val createdAt: Long = System.currentTimeMillis())

@Entity(tableName = "players")
data class PlayerEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val name: String)

@Entity(tableName = "profile_players", primaryKeys = ["profileId", "playerId"])
data class ProfilePlayerEntity(val profileId: Long, val playerId: Long, val turnOrder: Int)

@Entity(tableName = "games")
data class GameEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val startedAt: Long,
    val endedAt: Long,
    val result: String,
    val totalStrokes: Int,
    val totalFouls: Int,
    val pocketsClosedAtEnd: Int,
    val scratchPlayerId: Long? = null
)

@Entity(tableName = "game_actions")
data class GameActionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val gameId: Long,
    val sequence: Int,
    val playerId: Long,
    val actionType: String,
    val strokes: Int,
    val pocketIndex: Int? = null,
    val occurredAt: Long
)

data class ProfileWithPlayers(
    @Embedded val profile: ProfileEntity,
    @Relation(parentColumn = "id", entityColumn = "id", associateBy = Junction(
        ProfilePlayerEntity::class, parentColumn = "profileId", entityColumn = "playerId"
    )) val players: List<PlayerEntity>
)

@Dao
interface BugHolesDao {
    @Transaction @Query("SELECT * FROM profiles ORDER BY createdAt DESC")
    fun profiles(): Flow<List<ProfileWithPlayers>>

    @Insert suspend fun insertProfile(profile: ProfileEntity): Long
    @Insert suspend fun insertPlayer(player: PlayerEntity): Long
    @Insert suspend fun insertProfilePlayer(link: ProfilePlayerEntity)
    @Insert suspend fun insertGame(game: GameEntity): Long
    @Insert suspend fun insertActions(actions: List<GameActionEntity>)

    @Query("SELECT * FROM games WHERE profileId=:profileId ORDER BY endedAt DESC")
    fun games(profileId: Long): Flow<List<GameEntity>>

    @Query("SELECT * FROM game_actions WHERE gameId=:gameId ORDER BY sequence")
    suspend fun actions(gameId: Long): List<GameActionEntity>
}

@Database(entities=[ProfileEntity::class,PlayerEntity::class,ProfilePlayerEntity::class,GameEntity::class,GameActionEntity::class], version=1, exportSchema=false)
abstract class AppDatabase: RoomDatabase() {
    abstract fun dao(): BugHolesDao
    companion object {
        fun create(context: Context): AppDatabase = Room.databaseBuilder(context, AppDatabase::class.java, "bug_holes.db").build()
    }
}
