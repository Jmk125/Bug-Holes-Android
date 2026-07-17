package com.joekillion.bugholes.ui

import androidx.lifecycle.*
import com.joekillion.bugholes.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(private val repo: GameRepository): ViewModel() {
    val profiles = repo.profiles.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val _selected = MutableStateFlow<ProfileWithPlayers?>(null)
    val selected = _selected.asStateFlow()
    private val _game = MutableStateFlow<ActiveGameState?>(null)
    val game = _game.asStateFlow()
    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()
    private val _showHistory = MutableStateFlow(false)
    val showHistory = _showHistory.asStateFlow()
    val games = selected.filterNotNull().flatMapLatest { repo.games(it.profile.id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createProfile(name: String, players: List<String>) = viewModelScope.launch { repo.createProfile(name, players) }
    fun select(profile: ProfileWithPlayers) { _selected.value = profile }
    fun startGame(startingPlayerIndex: Int = 0) {
        val p = _selected.value ?: return
        val players = p.players.sortedBy { it.id }
        _game.value = ActiveGameState(players = players, startingPlayerIndex = startingPlayerIndex.coerceIn(0, players.lastIndex.coerceAtLeast(0)))
    }
    fun miss() { _game.value = _game.value?.add(ActionType.MISS) }
    fun foul() { _game.value = _game.value?.add(ActionType.FOUL) }
    fun pocket(index: Int) {
        val g = _game.value ?: return
        _game.value = g.add(if (g.closedPockets[index]) ActionType.REOPEN_POCKET else ActionType.CLOSE_POCKET, index)
    }
    fun undo() { _game.value = _game.value?.undo() }
    fun scratch() = finish(GameResult.LOSS)
    fun complete() {
        val g = _game.value ?: return
        if (!g.allClosed) { _message.value = "All six bug holes must be closed before completing the game."; return }
        finish(GameResult.WIN)
    }
    private fun finish(result: GameResult) {
        val p = _selected.value ?: return
        val g = _game.value ?: return
        val finalState = if (result == GameResult.LOSS) g.add(ActionType.SCRATCH) else g
        viewModelScope.launch { repo.saveGame(p.profile.id, finalState, result); _game.value = null; _message.value = if(result==GameResult.WIN) "Victory saved" else "Loss saved" }
    }
    fun clearMessage() { _message.value = null }
    fun openHistory() { _showHistory.value = true }
    fun closeHistory() { _showHistory.value = false }
}

class MainViewModelFactory(private val repo: GameRepository): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T = MainViewModel(repo) as T
}
