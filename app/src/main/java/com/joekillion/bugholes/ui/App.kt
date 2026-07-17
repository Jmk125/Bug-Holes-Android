package com.joekillion.bugholes.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.verticalScroll
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RectangleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.joekillion.bugholes.data.ActiveGameState
import com.joekillion.bugholes.data.GameEntity
import com.joekillion.bugholes.data.GameResult
import com.joekillion.bugholes.data.PlayerEntity
import com.joekillion.bugholes.data.ProfileWithPlayers
import java.text.DateFormat
import java.util.Date

private val TableGreen = Color(0xFF173C27)
private val Yellow = Color(0xFFFFD429)
private val Dark = Color(0xFF0B1710)
private val DeepRed = Color(0xFF7A1515)

@Composable
fun BugHolesTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = androidx.compose.material3.darkColorScheme(
            primary = Yellow,
            background = Dark,
            surface = TableGreen,
            onPrimary = Color.White,
        ),
        content = content,
    )
}

@Composable
fun BugHolesApp(vm: MainViewModel) {
    val game by vm.game.collectAsState()
    val selected by vm.selected.collectAsState()
    val profiles by vm.profiles.collectAsState()
    val message by vm.message.collectAsState()
    val showHistory by vm.showHistory.collectAsState()
    val games by vm.games.collectAsState()

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        when {
            game != null -> GameScreen(game!!, vm)
            selected != null -> ProfileScreen(selected!!, vm)
            else -> ProfilesScreen(profiles, vm)
        }

        if (showHistory && selected != null) {
            HistoryScreen(selected!!, games, vm::closeHistory)
        }

        message?.let { msg ->
            AlertDialog(
                onDismissRequest = vm::clearMessage,
                confirmButton = { BugButton("OK", vm::clearMessage) },
                title = { Text("Bug Holes") },
                text = { Text(msg) },
            )
        }
    }
}

@Composable
private fun ProfilesScreen(profiles: List<ProfileWithPlayers>, vm: MainViewModel) {
    var creating by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("BUG HOLES", fontSize = 34.sp, fontWeight = FontWeight.Black, color = Yellow)
        Text("Team Profiles", style = MaterialTheme.typography.titleLarge)
        profiles.forEach { profile ->
            ElevatedCard(onClick = { vm.select(profile) }, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    Text(profile.profile.name, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text(profile.players.joinToString(" • ") { it.name })
                }
            }
        }
        BugButton("Create Team Profile", { creating = true }, Modifier.fillMaxWidth())
    }

    if (creating) {
        CreateProfileDialog({ creating = false }) { name, players ->
            vm.createProfile(name, players)
            creating = false
        }
    }
}

@Composable
private fun CreateProfileDialog(onDismiss: () -> Unit, onCreate: (String, List<String>) -> Unit) {
    var team by remember { mutableStateOf("") }
    var names by remember { mutableStateOf("Joe, Matt") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Team") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(team, { team = it }, label = { Text("Profile name") })
                OutlinedTextField(names, { names = it }, label = { Text("Players, comma separated") })
            }
        },
        confirmButton = {
            BugButton(
                text = "Create",
                onClick = { onCreate(team, names.split(',')) },
                enabled = team.isNotBlank() && names.split(',').any { it.isNotBlank() },
            )
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun ProfileScreen(profile: ProfileWithPlayers, vm: MainViewModel) {
    var chooseStarter by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text(profile.profile.name, fontSize = 30.sp, fontWeight = FontWeight.Black, color = Yellow)
        Text(profile.players.joinToString(" • ") { it.name })
        BugButton(
            "START NEW GAME",
            { chooseStarter = true },
            Modifier.fillMaxWidth().height(64.dp),
            fontSize = 20.sp,
        )
        BugButton("GAME HISTORY & STATS", vm::openHistory, Modifier.fillMaxWidth().height(56.dp))
    }

    if (chooseStarter) {
        StartGameDialog(profile.players.sortedBy { it.id }, { chooseStarter = false }) { starter ->
            vm.startGame(starter)
            chooseStarter = false
        }
    }
}

@Composable
private fun StartGameDialog(
    players: List<PlayerEntity>,
    onDismiss: () -> Unit,
    onStart: (Int) -> Unit,
) {
    var starter by remember { mutableIntStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Who goes first?") },
        text = {
            Column {
                Text("Choose the starting player for this game.")
                players.forEachIndexed { index, player ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { starter = index },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = starter == index, onClick = { starter = index })
                        Text(player.name)
                    }
                }
            }
        },
        confirmButton = { BugButton("Start Game", { onStart(starter) }) },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun GameScreen(game: ActiveGameState, vm: MainViewModel) {
    var confirmScratch by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("CURRENT SHOOTER", fontSize = 13.sp)
        Text(game.currentPlayer.name.uppercase(), fontSize = 36.sp, fontWeight = FontWeight.Black, color = Yellow)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Stat("Shots", game.totalStrokes.toString())
            Stat("Fouls", game.totalFouls.toString())
            Stat("Closed", "${game.closedPockets.count { it }}/6")
        }
        PoolTable(game.closedPockets, vm::pocket)
        game.players.forEach { player ->
            Text("${player.name}: ${game.playerStrokes(player.id)} shots • ${game.playerFouls(player.id)} fouls")
        }
        BugButton("Shot +1", vm::miss, Modifier.fillMaxWidth().height(58.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BugButton("Foul +2", vm::foul, Modifier.weight(1f).height(56.dp))
            BugButton("SCRATCH", { confirmScratch = true }, Modifier.weight(1f).height(56.dp), deepRed = true)
        }
        BugButton("Undo Last Turn", vm::undo, Modifier.fillMaxWidth(), enabled = game.actions.isNotEmpty())
        BugButton(
            if (game.allClosed) "COMPLETE GAME" else "${6 - game.closedPockets.count { it }} HOLES REMAIN",
            vm::complete,
            Modifier.fillMaxWidth().height(58.dp),
            enabled = game.allClosed,
        )
    }

    if (confirmScratch) {
        AlertDialog(
            onDismissRequest = { confirmScratch = false },
            title = { Text("Record scratch?") },
            text = { Text("A scratch immediately ends this game as a loss.") },
            confirmButton = {
                BugButton("Record Loss", { confirmScratch = false; vm.scratch() }, deepRed = true)
            },
            dismissButton = { TextButton(onClick = { confirmScratch = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun HistoryScreen(profile: ProfileWithPlayers, games: List<GameEntity>, onClose: () -> Unit) {
    val wins = games.count { it.result == GameResult.WIN.name }
    val losses = games.size - wins

    Surface(Modifier.fillMaxSize(), color = Dark) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("GAME HISTORY", fontSize = 30.sp, fontWeight = FontWeight.Black, color = Yellow)
            Text(profile.profile.name, style = MaterialTheme.typography.titleMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Stat("Games", games.size.toString())
                Stat("Wins", wins.toString())
                Stat("Losses", losses.toString())
            }
            if (games.isEmpty()) {
                Text("No completed games yet. Finish a game to see its stats here.")
            }
            games.forEach { game ->
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            if (game.result == GameResult.WIN.name) "WIN" else "LOSS",
                            fontWeight = FontWeight.Black,
                            color = if (game.result == GameResult.WIN.name) Yellow else DeepRed,
                        )
                        Text(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(game.endedAt)))
                        Text("${game.totalStrokes} shots • ${game.totalFouls} fouls • ${game.pocketsClosedAtEnd}/6 holes closed")
                    }
                }
            }
            BugButton("Back to Profile", onClose, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun BugButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    deepRed: Boolean = false,
    fontSize: TextUnit = 16.sp,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val accent = if (deepRed) DeepRed else Yellow
    val filled = pressed && enabled

    Button(
        onClick = onClick,
        enabled = enabled,
        interactionSource = source,
        shape = RectangleShape,
        border = BorderStroke(2.dp, accent),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (filled) accent else Dark,
            contentColor = if (filled && !deepRed) Dark else Color.White,
            disabledContainerColor = Dark,
            disabledContentColor = Color.Gray,
        ),
        modifier = modifier,
    ) {
        Text(text, fontSize = fontSize, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column {
        Text(value, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text(label, fontSize = 12.sp)
    }
}

@Composable
private fun PoolTable(closed: List<Boolean>, onPocket: (Int) -> Unit) {
    val pocketPositions = listOf(
        Alignment.TopStart,
        Alignment.TopCenter,
        Alignment.TopEnd,
        Alignment.BottomStart,
        Alignment.BottomCenter,
        Alignment.BottomEnd,
    )

    Box(
        modifier = Modifier.fillMaxWidth()
            .aspectRatio(1.8f)
            .background(Color(0xFF175A3A), RectangleShape)
            .border(8.dp, Color(0xFF4A2C16), RectangleShape),
    ) {
        pocketPositions.forEachIndexed { index, alignment ->
            Box(
                modifier = Modifier.align(alignment)
                    .padding(if (index % 3 == 1) 2.dp else 4.dp)
                    .size(48.dp)
                    .background(if (closed[index]) Yellow else Color.Black, CircleShape)
                    .clickable { onPocket(index) },
                contentAlignment = Alignment.Center,
            ) {
                Text(if (closed[index]) "✓" else "", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
        Text("Tap the pocket made", Modifier.align(Alignment.Center), fontWeight = FontWeight.Bold)
    }
}
