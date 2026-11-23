package com.robloxai.studio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.robloxai.studio.data.model.AiProvider
import com.robloxai.studio.data.model.ConnectionStatus
import com.robloxai.studio.data.model.ModelOption
import com.robloxai.studio.data.model.ProviderStatusInfo
import com.robloxai.studio.ui.theme.*

@Composable
fun ProviderModelBar(
    selectedProvider: AiProvider,
    selectedModel: String,
    availableModels: Map<AiProvider, List<ModelOption>>,
    statuses: Map<AiProvider, ProviderStatusInfo>,
    onSelectProvider: (AiProvider) -> Unit,
    onSelectModel: (String) -> Unit,
    onRefreshOpenRouterFree: () -> Unit,
    onOpenSettings: () -> Unit
) {
    var providerExpanded by remember { mutableStateOf(false) }
    var modelExpanded by remember { mutableStateOf(false) }

    val models = availableModels[selectedProvider] ?: selectedProvider.defaultModels
    val currentModelLabel = models.firstOrNull { it.id == selectedModel }?.label ?: selectedModel
    val statusInfo = statuses[selectedProvider] ?: ProviderStatusInfo()
    val dotColor = when (statusInfo.status) {
        ConnectionStatus.CONNECTED -> NeonMint
        ConnectionStatus.CHECKING, ConnectionStatus.RATE_LIMITED -> AmberWarn
        ConnectionStatus.AUTH_FAILED, ConnectionStatus.ERROR -> CrimsonError
        ConnectionStatus.UNCONFIGURED -> TextMuted
    }

    Surface(color = SlateSurface) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Provider dropdown
            Box {
                Surface(
                    color = ElevatedCard,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.clickable { providerExpanded = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                        )
                        Text(
                            selectedProvider.displayName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = "Select Provider",
                            tint = TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                DropdownMenu(
                    expanded = providerExpanded,
                    onDismissRequest = { providerExpanded = false }
                ) {
                    AiProvider.entries.forEach { provider ->
                        DropdownMenuItem(
                            text = { Text(provider.displayName) },
                            onClick = {
                                providerExpanded = false
                                onSelectProvider(provider)
                            }
                        )
                    }
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Configure API Keys...") },
                        onClick = {
                            providerExpanded = false
                            onOpenSettings()
                        }
                    )
                }
            }

            // Model dropdown
            Box(modifier = Modifier.weight(1f)) {
                Surface(
                    color = ElevatedCard,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { modelExpanded = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = currentModelLabel,
                            fontSize = 12.sp,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = "Select Model",
                            tint = TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                DropdownMenu(
                    expanded = modelExpanded,
                    onDismissRequest = { modelExpanded = false }
                ) {
                    models.forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    option.label,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            onClick = {
                                modelExpanded = false
                                onSelectModel(option.id)
                            }
                        )
                    }
                }
            }

            if (selectedProvider == AiProvider.OPENROUTER) {
                IconButton(
                    onClick = onRefreshOpenRouterFree,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Refresh OpenRouter Free Models",
                        tint = RobloxBlue,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
