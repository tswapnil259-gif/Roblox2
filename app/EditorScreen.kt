package com.robloxai.studio.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.robloxai.studio.data.local.ScriptEntity
import com.robloxai.studio.data.model.RobloxScriptType
import com.robloxai.studio.ui.components.LuauSyntaxHighlighter
import com.robloxai.studio.ui.theme.*

@Composable
fun EditorScreen(
    scripts: List<ScriptEntity>,
    activeScriptId: Long?,
    title: String,
    scriptType: RobloxScriptType,
    code: String,
    notes: String,
    isBusy: Boolean,
    onFieldsChange: (String, RobloxScriptType, String) -> Unit,
    onGenerateScript: (String, RobloxScriptType, String, Boolean) -> Unit,
    onNewScript: () -> Unit,
    onSaveScript: () -> Unit,
    onLoadScript: (ScriptEntity) -> Unit,
    onDeleteScript: (ScriptEntity) -> Unit,
    onBuildShareIntent: (String, RobloxScriptType, String) -> Intent
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var showGeneratorSheet by remember { mutableStateOf(false) }
    var showPlacementNotes by remember { mutableStateOf(false) }

    val exportDocLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(code.toByteArray())
                }
                Toast.makeText(context, "Exported Luau script", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            item {
                AssistChip(
                    onClick = onNewScript,
                    label = { Text("+ New", fontSize = 12.sp) }
                )
            }
            items(scripts, key = { it.id }) { sc ->
                InputChip(
                    selected = sc.id == activeScriptId,
                    onClick = { onLoadScript(sc) },
                    label = { Text(sc.title, fontSize = 12.sp) },
                    trailingIcon = {
                        IconButton(onClick = { onDeleteScript(sc) }, modifier = Modifier.size(16.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Delete", modifier = Modifier.size(14.dp))
                        }
                    }
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = title,
                onValueChange = { onFieldsChange(it, scriptType, code) },
                label = { Text("Script Name") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Button(onClick = { showGeneratorSheet = true }, enabled = !isBusy) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("AI Generate")
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            RobloxScriptType.entries.forEach { t ->
                FilterChip(
                    selected = scriptType == t,
                    onClick = { onFieldsChange(title, t, code) },
                    label = { Text(t.shortLabel(), fontSize = 11.sp) }
                )
            }
        }

        Text(
            text = "Studio Explorer Target: ${scriptType.recommendedParent} (${scriptType.extension})",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = NeonMint
        )

        if (isBusy) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        // Luau Syntax-Highlighted Code Editor
        Surface(
            color = ObsidianBg,
            border = BorderStroke(1.dp, BorderSubtle),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            val verticalScroll = rememberScrollState()
            val horizontalScroll = rememberScrollState()
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(verticalScroll)
                    .horizontalScroll(horizontalScroll)
                    .padding(12.dp)
            ) {
                BasicTextField(
                    value = code,
                    onValueChange = { onFieldsChange(title, scriptType, it) },
                    textStyle = TextStyle(
                        color = TextPrimary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    ),
                    cursorBrush = SolidColor(RobloxBlue),
                    visualTransformation = LuauSyntaxHighlighter.visualTransformation,
                    modifier = Modifier.defaultMinSize(
                        minWidth = maxWidth,
                        minHeight = maxHeight
                    )
                )
            }
        }

        // Action Bar: Save, Copy, Export .lua, Share, Placement Guide
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(onClick = onSaveScript, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Save", fontSize = 12.sp)
            }
            OutlinedButton(onClick = { clipboard.setText(AnnotatedString(code)) }) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
            }
            OutlinedButton(
                onClick = {
                    val safeName = title.replace(Regex("[^A-Za-z0-9_-]"), "_").ifBlank { "Script" }
                    exportDocLauncher.launch("$safeName${scriptType.extension}")
                }
            ) {
                Icon(Icons.Default.Download, contentDescription = "Export .lua", modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(".lua", fontSize = 12.sp)
            }
            OutlinedButton(
                onClick = {
                    val intent = onBuildShareIntent(title, scriptType, code)
                    context.startActivity(Intent.createChooser(intent, "Share Luau Script"))
                }
            ) {
                Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(16.dp))
            }
            if (notes.isNotBlank()) {
                OutlinedButton(onClick = { showPlacementNotes = true }) {
                    Icon(Icons.Default.Info, contentDescription = "Setup Guide", modifier = Modifier.size(16.dp))
                }
            }
        }
    }

    if (showGeneratorSheet) {
        var genTitle by remember { mutableStateOf(title.ifBlank { "CombatSystem" }) }
        var genType by remember { mutableStateOf(scriptType) }
        var spec by remember { mutableStateOf("") }
        var comments by remember { mutableStateOf(true) }

        AlertDialog(
            onDismissRequest = { showGeneratorSheet = false },
            title = { Text("AI Luau Script Generator") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = genTitle,
                        onValueChange = { genTitle = it },
                        label = { Text("Script Name") }
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        RobloxScriptType.entries.forEach { t ->
                            FilterChip(
                                selected = genType == t,
                                onClick = { genType = t },
                                label = { Text(t.shortLabel(), fontSize = 10.sp) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = spec,
                        onValueChange = { spec = it },
                        label = { Text("Describe the system to generate") },
                        placeholder = { Text("e.g., Double-jump + stamina bar with server validation") },
                        minLines = 3
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = comments, onCheckedChange = { comments = it })
                        Text("Include inline comments & Explorer setup", fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = spec.isNotBlank(),
                    onClick = {
                        showGeneratorSheet = false
                        onGenerateScript(genTitle, genType, spec, comments)
                    }
                ) { Text("Generate") }
            },
            dismissButton = {
                TextButton(onClick = { showGeneratorSheet = false }) { Text("Cancel") }
            }
        )
    }

    if (showPlacementNotes) {
        AlertDialog(
            onDismissRequest = { showPlacementNotes = false },
            title = { Text("Roblox Studio Placement & Setup") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(notes, fontSize = 13.sp)
                }
            },
            confirmButton = {
                Button(onClick = { showPlacementNotes = false }) { Text("Close") }
            }
        )
    }
}

private fun RobloxScriptType.shortLabel(): String = when (this) {
    RobloxScriptType.SCRIPT -> "Server Script"
    RobloxScriptType.LOCAL_SCRIPT -> "LocalScript"
    RobloxScriptType.MODULE_SCRIPT -> "ModuleScript"
}
