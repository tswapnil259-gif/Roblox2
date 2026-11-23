package com.robloxai.studio.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.robloxai.studio.data.local.Concept3DEntity
import com.robloxai.studio.ui.theme.*

@Composable
fun Model3DScreen(
    concepts: List<Concept3DEntity>,
    latestOutput: String,
    isBusy: Boolean,
    onGenerate3D: (String, String, String, String) -> Unit,
    onDeleteConcept: (Concept3DEntity) -> Unit
) {
    var assetName by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Prop / Weapon") }
    var style by remember { mutableStateOf("Stylized Low-Poly") }
    var polyBudget by remember { mutableStateOf("2,500 - 5,000 tris") }
    val clipboard = LocalClipboardManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("3D Asset & Mesh Planner", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(
            "Generate Roblox-ready 3D asset specs, Studio import settings, and prompts for Meshy/Tripo/Rodin.",
            fontSize = 12.sp,
            color = TextMuted
        )

        OutlinedTextField(
            value = assetName,
            onValueChange = { assetName = it },
            label = { Text("Asset Name") },
            placeholder = { Text("e.g., Crystal Greatsword, Sci-Fi Crate") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = category,
                onValueChange = { category = it },
                label = { Text("Category") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = style,
                onValueChange = { style = it },
                label = { Text("Art Style") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }

        OutlinedTextField(
            value = polyBudget,
            onValueChange = { polyBudget = it },
            label = { Text("Triangle / Poly Budget") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = { onGenerate3D(assetName, category, style, polyBudget) },
            enabled = !isBusy && assetName.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.ViewInAr, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(if (isBusy) "Generating 3D Concept..." else "Generate 3D Asset Plan")
        }

        if (latestOutput.isNotBlank()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateSurface),
                border = BorderStroke(1.dp, RobloxBlue.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("Latest 3D Concept", fontWeight = FontWeight.Bold, color = RobloxBlue)
                    Text(latestOutput, fontSize = 13.sp, lineHeight = 19.sp)
                }
            }
        }

        if (concepts.isNotEmpty()) {
            Text("Saved 3D Concepts (${concepts.size})", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            concepts.forEach { concept ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = ElevatedCard),
                    border = BorderStroke(1.dp, BorderSubtle)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(concept.title, fontWeight = FontWeight.Bold)
                                Text(concept.category, fontSize = 12.sp, color = RobloxBlue)
                            }
                            IconButton(onClick = { clipboard.setText(AnnotatedString(concept.generatorPrompt)) }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy Prompt", tint = NeonMint)
                            }
                            IconButton(onClick = { onDeleteConcept(concept) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Concept", tint = TextMuted)
                            }
                        }
                        Text(
                            "Prompt: ${concept.generatorPrompt}",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    }
                }
            }
        }
    }
}
