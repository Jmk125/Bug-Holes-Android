package com.joekillion.bugholes.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.joekillion.bugholes.data.*

private val BugGreen = Color(0xFF173C27)
private val Acid = Color(0xFFB7F34A)

@Composable fun BugHolesTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = darkColorScheme(primary=Acid, background=Color(0xFF0B1710), surface=BugGreen), content=content)
}

@Composable fun BugHolesApp(vm: MainViewModel) {
    val game by vm.game.collectAsState()
    val selected by vm.selected.collectAsState()
    val profiles by vm.profiles.collectAsState()
    val message by vm.message.collectAsState()
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        when {
            game != null -> GameScreen(game!!, vm)
            selected != null -> ProfileScreen(selected!!, vm)
            else -> ProfilesScreen(profiles, vm)
        }
        message?.let { msg ->
            AlertDialog(onDismissRequest=vm::clearMessage, confirmButton={TextButton(onClick=vm::clearMessage){Text("OK")}}, title={Text("Bug Holes")}, text={Text(msg)})
        }
    }
}

@Composable private fun ProfilesScreen(profiles: List<ProfileWithPlayers>, vm: MainViewModel) {
    var creating by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement=Arrangement.spacedBy(16.dp)) {
        Text("BUG HOLES", fontSize=34.sp, fontWeight=FontWeight.Black, color=Acid)
        Text("Team Profiles", style=MaterialTheme.typography.titleLarge)
        profiles.forEach { p -> ElevatedCard(onClick={vm.select(p)}, modifier=Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp)) { Text(p.profile.name, fontWeight=FontWeight.Bold, fontSize=20.sp); Text(p.players.joinToString(" • "){it.name}) }
        }}
        Button(onClick={creating=true}, modifier=Modifier.fillMaxWidth()){Text("Create Team Profile")}
    }
    if (creating) CreateProfileDialog(onDismiss={creating=false}) { n, ps -> vm.createProfile(n,ps); creating=false }
}

@Composable private fun CreateProfileDialog(onDismiss:()->Unit, onCreate:(String,List<String>)->Unit) {
    var team by remember { mutableStateOf("") }; var names by remember { mutableStateOf("Joe, Matt") }
    AlertDialog(onDismissRequest=onDismiss, title={Text("Create Team")}, text={Column(verticalArrangement=Arrangement.spacedBy(10.dp)){
        OutlinedTextField(team,{team=it},label={Text("Profile name")}); OutlinedTextField(names,{names=it},label={Text("Players, comma separated")})
    }}, confirmButton={Button(onClick={onCreate(team, names.split(','))}, enabled=team.isNotBlank() && names.split(',').count{it.isNotBlank()}>=1){Text("Create")}}, dismissButton={TextButton(onClick=onDismiss){Text("Cancel")}})
}

@Composable private fun ProfileScreen(profile: ProfileWithPlayers, vm: MainViewModel) {
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement=Arrangement.spacedBy(18.dp)) {
        Text(profile.profile.name, fontSize=30.sp, fontWeight=FontWeight.Black, color=Acid)
        Text(profile.players.joinToString(" • "){it.name})
        Button(onClick=vm::startGame, modifier=Modifier.fillMaxWidth().height(64.dp)){Text("START NEW GAME", fontSize=20.sp)}
        Text("Version 0.1 saves completed wins and scratch losses locally. Detailed history and statistics are the next screen to add.")
    }
}

@Composable private fun GameScreen(g: ActiveGameState, vm: MainViewModel) {
    var confirmScratch by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text("CURRENT SHOOTER", fontSize=13.sp)
        Text(g.currentPlayer.name.uppercase(), fontSize=36.sp, fontWeight=FontWeight.Black, color=Acid)
        Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween) {
            Stat("Strokes", g.totalStrokes.toString()); Stat("Fouls", g.totalFouls.toString()); Stat("Closed", "${g.closedPockets.count{it}}/6")
        }
        PoolTable(g.closedPockets, vm::pocket)
        g.players.forEach { p -> Text("${p.name}: ${g.playerStrokes(p.id)} strokes • ${g.playerFouls(p.id)} fouls") }
        Button(onClick=vm::miss, modifier=Modifier.fillMaxWidth().height(58.dp)){Text("MISS / SHOT  +1")}
        Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            Button(onClick=vm::foul, modifier=Modifier.weight(1f).height(56.dp)){Text("FOUL  +2")}
            Button(onClick={confirmScratch=true}, modifier=Modifier.weight(1f).height(56.dp), colors=ButtonDefaults.buttonColors(containerColor=MaterialTheme.colorScheme.error)){Text("SCRATCH")}
        }
        OutlinedButton(onClick=vm::undo, enabled=g.actions.isNotEmpty(), modifier=Modifier.fillMaxWidth()){Text("Undo Last Turn")}
        Button(onClick=vm::complete, enabled=g.allClosed, modifier=Modifier.fillMaxWidth().height(58.dp)) {
            Text(if(g.allClosed) "COMPLETE GAME" else "${6-g.closedPockets.count{it}} HOLES REMAIN")
        }
    }
    if(confirmScratch) AlertDialog(onDismissRequest={confirmScratch=false}, title={Text("Record scratch?")}, text={Text("A scratch immediately ends this game as a loss.")}, confirmButton={Button(onClick={confirmScratch=false;vm.scratch()}, colors=ButtonDefaults.buttonColors(containerColor=MaterialTheme.colorScheme.error)){Text("Record Loss")}}, dismissButton={TextButton(onClick={confirmScratch=false}){Text("Cancel")}})
}

@Composable private fun Stat(label:String,value:String){ Column { Text(value,fontSize=26.sp,fontWeight=FontWeight.Bold);Text(label,fontSize=12.sp) } }

@Composable private fun PoolTable(closed: List<Boolean>, onPocket:(Int)->Unit) {
    Box(Modifier.fillMaxWidth().aspectRatio(1.8f).background(Color(0xFF175A3A), RoundedCornerShape(18.dp)).border(8.dp,Color(0xFF4A2C16),RoundedCornerShape(18.dp))) {
        val positions=listOf(Alignment.TopStart,Alignment.TopCenter,Alignment.TopEnd,Alignment.BottomStart,Alignment.BottomCenter,Alignment.BottomEnd)
        positions.forEachIndexed { i,a -> Box(Modifier.align(a).padding(if(i%3==1) 2.dp else 4.dp).size(48.dp).background(if(closed[i]) Acid else Color.Black,CircleShape).clickable{onPocket(i)}, contentAlignment=Alignment.Center){Text(if(closed[i]) "✓" else "",color=Color.Black,fontWeight=FontWeight.Bold)} }
        Text("Tap the pocket made",Modifier.align(Alignment.Center),fontWeight=FontWeight.Bold)
    }
}
