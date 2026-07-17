package com.joekillion.bugholes.data

import kotlinx.coroutines.flow.Flow

class GameRepository(private val dao: BugHolesDao) {
    val profiles: Flow<List<ProfileWithPlayers>> = dao.profiles()
    fun games(profileId: Long) = dao.games(profileId)

    suspend fun createProfile(name: String, playerNames: List<String>): Long {
        val profileId = dao.insertProfile(ProfileEntity(name = name.trim()))
        playerNames.map(String::trim).filter(String::isNotBlank).forEachIndexed { index, playerName ->
            val playerId = dao.insertPlayer(PlayerEntity(name = playerName))
            dao.insertProfilePlayer(ProfilePlayerEntity(profileId, playerId, index))
        }
        return profileId
    }

    suspend fun saveGame(profileId: Long, state: ActiveGameState, result: GameResult) {
        val gameId = dao.insertGame(GameEntity(
            profileId = profileId,
            startedAt = state.startedAt,
            endedAt = System.currentTimeMillis(),
            result = result.name,
            totalStrokes = state.totalStrokes,
            totalFouls = state.totalFouls,
            pocketsClosedAtEnd = state.closedPockets.count { it },
            scratchPlayerId = if (result == GameResult.LOSS) state.actions.lastOrNull()?.playerId else null
        ))
        dao.insertActions(state.actions.mapIndexed { i, a ->
            GameActionEntity(gameId=gameId, sequence=i, playerId=a.playerId, actionType=a.type.name,
                strokes=a.strokes, pocketIndex=a.pocketIndex, occurredAt=a.occurredAt)
        })
    }
}

enum class ActionType { MISS, FOUL, CLOSE_POCKET, REOPEN_POCKET, SCRATCH }
enum class GameResult { WIN, LOSS }
data class GameAction(val playerId: Long, val type: ActionType, val strokes: Int, val pocketIndex: Int?=null, val occurredAt: Long=System.currentTimeMillis())

data class ActiveGameState(
    val players: List<PlayerEntity>,
    val startedAt: Long = System.currentTimeMillis(),
    val startingPlayerIndex: Int = 0,
    val actions: List<GameAction> = emptyList()
) {
    val currentPlayerIndex: Int get() = if (players.isEmpty()) 0 else (startingPlayerIndex + actions.size) % players.size
    val currentPlayer: PlayerEntity get() = players[currentPlayerIndex]
    val totalStrokes: Int get() = actions.sumOf { it.strokes }
    val totalFouls: Int get() = actions.count { it.type == ActionType.FOUL }
    fun playerStrokes(id: Long) = actions.filter { it.playerId == id }.sumOf { it.strokes }
    fun playerFouls(id: Long) = actions.count { it.playerId == id && it.type == ActionType.FOUL }
    val closedPockets: List<Boolean> get() {
        val states = MutableList(6) { false }
        actions.forEach { a -> a.pocketIndex?.let { states[it] = a.type == ActionType.CLOSE_POCKET } }
        return states
    }
    val allClosed get() = closedPockets.all { it }
    fun add(type: ActionType, pocket: Int?=null): ActiveGameState {
        val strokes = if (type == ActionType.FOUL) 2 else 1
        return copy(actions = actions + GameAction(currentPlayer.id, type, strokes, pocket))
    }
    fun undo() = if (actions.isEmpty()) this else copy(actions = actions.dropLast(1))
}
