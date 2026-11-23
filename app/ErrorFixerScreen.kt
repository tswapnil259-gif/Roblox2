package com.robloxai.studio.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.robloxai.studio.ui.theme.*

@Composable
fun ErrorFixerScreen(
    analysisResult: String,
    fixedCode: String,
    isBusy: Boolean,
    onAnalyzeError: (String, String) -> Unit,
    onSendFixedCodeToEditor: (String, String) -> Unit
) {
    var studioError by remember { mutableStateOf("") }
    var brokenCode by remember { mutableStateOf("") }
    val clipboard = LocalClipboardManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Roblox Studio Error Fixer", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(
            "Paste the Output window error and your Luau script to get root-cause analysis and a corrected script.",
            fontSize = 12.sp,
            color = TextMuted
        )

        OutlinedTextField(
            value = studioError,
            onValueChange = { studioError = it },
            label = { Text("Studio Output Error / Stack Trace") },
            placeholder = { Text("e.g., ServerScriptService.Combat:14: attempt to index nil with 'Humanoid'") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 5
        )

        OutlinedTextField(
            value = brokenCode,
            onValueChange = { brokenCode = it },
            label = { Text("Broken Luau Code (Optional)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 5,
            maxLines = 10,
            textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        )

        Button(
            onClick = { onAnalyzeError(studioError, brokenCode) },
            enabled = !isBusy && studioError.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(if (isBusy) "Analyzing..." else "Analyze & Fix Error")
        }

        if (analysisResult.isNotBlank()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateSurface),
                border = BorderStroke(1.dp, BorderSubtle)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Diagnosis & Fix", fontWeight = FontWeight.Bold, color = NeonMint)
                    Text(analysisResult, fontSize = 13.sp, lineHeight = 19.sp)
                }
            }
        }

        if (fixedCode.isNotBlank()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = ElevatedCard),
                border = BorderStroke(1.dp, RobloxBlue.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Corrected Luau Script", fontWeight = FontWeight.Bold, color = RobloxBlue)
                    Text(
                        fixedCode,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = TextPrimary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { clipboard.setText(AnnotatedString(fixedCode)) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Copy Fix")
                        }
                        Button(
                            onClick = { onSendFixedCodeToEditor("FixedScript", fixedCode) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Open in Editor")
                        }
                    }
                }
            }
        }
    }
}
