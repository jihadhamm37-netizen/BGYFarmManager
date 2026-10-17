package com.bgy.farmmanager

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.bgy.farmmanager.data.FarmDatabase
import com.bgy.farmmanager.data.FarmEntity
import kotlinx.coroutines.launch

private val Purple = Color(0xFF7847F7)
private val Screen = Color(0xFF11141B)
private val Panel = Color(0xFF1C222D)
private val Muted = Color(0xFFADB3C0)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dao = FarmDatabase.get(this).farmDao()
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                primary = Purple, background = Screen, surface = Panel,
                onBackground = Color.White, onSurface = Color.White
            )) {
                val farms by dao.observeAll().collectAsStateWithLifecycle(initialValue = emptyList())
                FarmScreen(
                    farms = farms,
                    onSave = { name, group, gameId, linked, email, flags ->
                        lifecycleScope.launch {
                            dao.insert(FarmEntity(
                                farm_name = name.trim(), group_id = group.trim(),
                                game_id = gameId.trim(), is_scid_linked = linked,
                                scid_email = email.trim(), enable_sound_effects = flags[0],
                                enable_background_music = flags[1], skip_google_first_login = flags[2],
                                mark_google_disconnected = flags[3], diamond_notifications = flags[4],
                                prioritize_low_end_performance = flags[5]
                            ))
                        }
                    },
                    onDelete = { id -> lifecycleScope.launch { dao.deleteById(id) } },
                    onOpenGame = { openHayDay() }
                )
            }
        }
    }

    private fun openHayDay() {
        val launch = packageManager.getLaunchIntentForPackage("com.supercell.hayday")
        if (launch != null) startActivity(launch)
        else startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.supercell.hayday")))
    }
}

@Composable
private fun FarmScreen(
    farms: List<FarmEntity>,
    onSave: (String, String, String, Boolean, String, List<Boolean>) -> Unit,
    onDelete: (Long) -> Unit,
    onOpenGame: () -> Unit
) {
    var farmName by remember { mutableStateOf("") }
    var groupName by remember { mutableStateOf("main") }
    var gameId by remember { mutableStateOf("") }
    var linked by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    val labels = listOf(
        "Enable sound effects", "Enable background music",
        "Skip Google first login (saved preference only)",
        "Mark Google disconnected (saved preference only)",
        "Diamond notifications", "Prioritize low-end performance"
    )
    val defaults = listOf(false, false, false, true, true, false)
    val flags = remember { mutableStateListOf(false, false, false, true, true, false) }
    var error by remember { mutableStateOf("") }
    var search by remember { mutableStateOf("") }
    var selectedGroup by remember { mutableStateOf("All") }
    var importPrefix by remember { mutableStateOf("") }
    var showImport by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }
    val clipboard = LocalClipboardManager.current

    Column(
        modifier = Modifier.fillMaxSize().background(Screen).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("BGY PROFILES", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("●", color = Color(0xFF4BD38A))
            Spacer(Modifier.width(8.dp))
            Text("LOCAL FARM MANAGER", color = Muted, fontSize = 12.sp)
        }
        OutlinedTextField(value = search, onValueChange = { search = it }, label = { Text("Search Farm...") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        val groups = listOf("All") + farms.map { it.group_id }.distinct().sorted()
        Text("FARM GROUPS  •  ${farms.size} FARMS", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            groups.take(4).forEach { group ->
                FilterChip(selected = selectedGroup == group, onClick = { selectedGroup = group },
                    label = { Text(if (group == "All") "All (${farms.size})" else "${group} (${farms.count { it.group_id == group }})") })
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = { showImport = true }, modifier = Modifier.weight(1f)) { Text("IMPORT") }
            Button(onClick = { clipboard.setText(AnnotatedString(farms.joinToString("\n") { "${it.group_id},${it.farm_name},${it.game_id}" })); error = "Farm labels copied to clipboard." }, modifier = Modifier.weight(1f)) { Text("EXPORT LIST") }
        }
        Text("New Farm", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(value = farmName, onValueChange = { farmName = it },
            label = { Text("Farm name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(value = groupName, onValueChange = { groupName = it },
            label = { Text("Group name (e.g. main, lvl 50)") },
            modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(value = gameId, onValueChange = { gameId = it },
            label = { Text("Game ID (optional label)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = linked, onCheckedChange = { linked = it })
            Text("Linked to Supercell ID")
        }
        if (linked) {
            OutlinedTextField(value = email, onValueChange = { email = it },
                label = { Text("Supercell ID email (optional)") }, modifier = Modifier.fillMaxWidth(),
                singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
        }
        Text("Settings", color = Purple, fontWeight = FontWeight.Bold)
        labels.forEachIndexed { index, label ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = flags[index], onCheckedChange = { flags[index] = it })
                Text(label, color = Color.White, fontSize = 13.sp)
            }
        }
        if (error.isNotBlank()) Text(error, color = Color(0xFFFF7777))
        Button(onClick = {
            if (farmName.isBlank() || groupName.isBlank()) {
                error = "Enter a farm name and group name."
            } else {
                onSave(farmName, groupName, gameId, linked, email, flags.toList())
                farmName = ""; gameId = ""; email = ""; linked = false
                flags.indices.forEach { flags[it] = defaults[it] }
                error = "Farm saved locally."
            }
        }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
            Text("SAVE FARM")
        }
        Button(onClick = onOpenGame, modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF393F4D)),
            shape = RoundedCornerShape(14.dp)) { Text("OPEN HAY DAY") }
        Text("Saved farms (${farms.size})", color = Color.White, fontWeight = FontWeight.Bold)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(farms.filter { (it.farm_name.contains(search, ignoreCase = true) || it.group_id.contains(search, ignoreCase = true) || it.game_id.contains(search, ignoreCase = true)) && (selectedGroup == "All" || it.group_id == selectedGroup) }, key = { it.id }) { farm ->
                Card(modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Panel)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(farm.farm_name, fontWeight = FontWeight.Bold)
                            Text("${farm.group_id} • ${if (farm.is_scid_linked) "SCID linked" else "Not linked"}",
                                color = Muted, fontSize = 12.sp)
                        }
                        TextButton(onClick = { onDelete(farm.id) }) {
                            Text("DELETE", color = Color(0xFFFF8080))
                        }
                    }
                }
            }
        }
    }
    if (showImport) {
        AlertDialog(
            onDismissRequest = { showImport = false },
            title = { Text("Import farm labels") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("One entry per line: ID,Pass,Token or ID|Pass|Token. Only the ID is imported; passwords and tokens are ignored. You can add a prefix to each imported farm name.")
                    OutlinedTextField(value = importPrefix, onValueChange = { importPrefix = it }, label = { Text("Farm name prefix (optional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = importText, onValueChange = { importText = it },
                        modifier = Modifier.fillMaxWidth().height(150.dp), placeholder = { Text("farm123,Pass-ignored,Token-ignored\nfarm456|Pass-ignored|Token-ignored\nMy farm") })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val entries = importText.lines().map { it.trim() }.filter { it.isNotEmpty() }
                    val labels = entries.map { line ->
                        val fields = when {
                            line.contains("|") -> line.split("|", limit = 3)
                            line.contains(",") -> line.split(",", limit = 3)
                            else -> listOf(line)
                        }
                        fields.first().trim()
                    }.filter { it.isNotEmpty() }.distinct()
                    labels.forEach { id ->
                        val label = if (id.length > 28) id.take(28) else id
                        onSave("${importPrefix.trim()}$label", groupName.ifBlank { "main" }, id, false, "", defaults)
                    }
                    error = "Imported ${labels.size} local labels. Passwords/tokens were ignored."
                    importText = ""; showImport = false
                }) { Text("IMPORT") }
            },
            dismissButton = { TextButton(onClick = { showImport = false }) { Text("CANCEL") } }
        )
    }
}
