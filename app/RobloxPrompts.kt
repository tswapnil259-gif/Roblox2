package com.robloxai.studio.domain

import com.robloxai.studio.data.model.RobloxScriptType

data class GameSystemTemplate(
    val title: String,
    val subtitle: String,
    val prompt: String
)

object RobloxPrompts {

    val gameBuilderTemplates: List<GameSystemTemplate> = listOf(
        GameSystemTemplate(
            title = "NPC Dialogue",
            subtitle = "ProximityPrompt + branching choices",
            prompt = "Build a complete Roblox NPC Dialogue system using ProximityPrompt, a ModuleScript dialogue tree in ReplicatedStorage, a LocalScript UI controller, and server-side validation for dialogue rewards."
        ),
        GameSystemTemplate(
            title = "Quest System",
            subtitle = "Track objectives & rewards",
            prompt = "Create a modular Quest System with quest definitions in ReplicatedStorage, server-authoritative progress tracking in ServerScriptService, and a client HUD tracker."
        ),
        GameSystemTemplate(
            title = "Combat & Weapons",
            subtitle = "Server-validated raycast/hitbox",
            prompt = "Generate a server-authoritative melee/ranged Weapon system with cooldowns, debounce checks, raycast/hit validation on the server, and responsive client effects."
        ),
        GameSystemTemplate(
            title = "Abilities & Powers",
            subtitle = "Cooldowns, stamina & VFX hooks",
            prompt = "Design a player Abilities/Powers framework with stamina/energy management, server-enforced cooldowns via RemoteEvent, and client VFX/animation hooks."
        ),
        GameSystemTemplate(
            title = "Shop & Economy",
            subtitle = "Currency, purchases & leaderstats",
            prompt = "Create a Shop and Currency system with leaderstats, server-side price/ownership verification via RemoteFunction, and a clean client Shop UI script."
        ),
        GameSystemTemplate(
            title = "DataStore Saving",
            subtitle = "Safe retry, session lock & AutoSave",
            prompt = "Write a production-safe DataStoreManager ModuleScript using DataStoreService with pcall retries, PlayerRemoving saving, BindToClose handling, and auto-save intervals."
        ),
        GameSystemTemplate(
            title = "Level & XP System",
            subtitle = "XP curve, level-up rewards",
            prompt = "Build a Level and XP progression system with an exponential XP formula, server-side XP awards, level-up notifications via RemoteEvent, and an XP bar LocalScript."
        )
    )

    fun unifiedAssistantSystem(projectName: String, projectContext: String): String = """
        You are Roblox AI Studio Assistant, an expert Roblox Studio & Luau game engineering assistant.
        Active Project: "$projectName"
        Project Notes: ${projectContext.ifBlank { "General Roblox experience" }}

        Rules:
        1. Write modern, strict, idiomatic Luau (--!strict where helpful, task.wait, task.spawn, game:GetService).
        2. Clearly state whether each script is a Script (ServerScriptService), LocalScript (StarterPlayerScripts/StarterGui), or ModuleScript (ReplicatedStorage).
        3. Enforce server-authoritative validation for RemoteEvents/RemoteFunctions (never trust client currency/damage claims).
        4. Never claim that this Android app can run Roblox Studio or edit a live .rbxl file directly; provide clear instructions for importing/pasting scripts into Roblox Studio.
        5. Wrap all Luau code in ```lua fenced code blocks so the user can send scripts directly to the editor.
    """.trimIndent()

    fun scriptGeneratorPrompt(
        scriptType: RobloxScriptType,
        specification: String,
        includeComments: Boolean
    ): String = """
        Generate a complete, production-ready Roblox Luau ${scriptType.label}.
        Target Explorer Location: ${scriptType.recommendedParent}
        Include detailed inline comments & Explorer setup instructions: $includeComments

        Feature Specification:
        $specification

        Output requirements:
        - Start with a brief Explorer hierarchy / RemoteEvent setup note if needed.
        - Provide the full script inside a single ```lua code block with `--!strict` at the top.
        - Avoid deprecated Roblox APIs (use `task.wait`, `task.spawn`, `game:GetService`, `Workspace`).
    """.trimIndent()

    fun errorFixerPrompt(studioError: String, brokenCode: String): String = """
        Diagnose and fix this Roblox Studio Output error.

        Roblox Studio Output Error:
        ```
        $studioError
        ```

        ${if (brokenCode.isNotBlank()) "Broken Luau Script:\n```lua\n$brokenCode\n```" else "No script body was provided; explain the root cause and provide a corrected reference implementation."}

        Output requirements:
        1. Root Cause: Explain clearly why Roblox Studio threw this error.
        2. Step-by-step Fix: Explain any Explorer/Instance or Script type changes required.
        3. Corrected Script: Provide the full fixed Luau script inside a single ```lua code block.
    """.trimIndent()

    fun model3DPrompt(
        assetName: String,
        category: String,
        style: String,
        polyBudget: String
    ): String = """
        Design a Roblox-ready 3D asset concept and import guide for:
        - Asset Name: $assetName
        - Category: $category
        - Art Style: $style
        - Triangle Budget: $polyBudget

        Provide:
        1. Visual & Structural Breakdown (dimensions in Roblox studs, silhouette, parts/materials, pivot point at base center).
        2. A concise, copy-ready prompt for AI 3D generators (Meshy, Tripo, Rodin) inside a ```prompt fenced code block.
        3. Roblox Studio Import Guide:
           - Exporting as .FBX or .OBJ (scale in meters/centimeters to match Roblox studs, single mesh or clean sub-meshes, UV/texture setup).
           - Importing via Asset Manager / MeshPart (`Import 3D`), setting CollisionFidelity (`Box` or `Hull` for performance), CastShadow, and SurfaceAppearance PBR maps.
    """.trimIndent()

    private val luaFenceRegex = Regex(
        """```(?:lua|luau)[ \t]*\r?\n([\s\S]*?)```""",
        setOf(RegexOption.IGNORE_CASE)
    )

    private val genericFenceRegex = Regex(
        """```(?!prompt\b)[a-zA-Z0-9_-]*[ \t]*\r?\n([\s\S]*?)```""",
        setOf(RegexOption.IGNORE_CASE)
    )

    private val promptFenceRegex = Regex(
        """```prompt[ \t]*\r?\n([\s\S]*?)```""",
        setOf(RegexOption.IGNORE_CASE)
    )

    fun extractFirstLuaBlock(markdown: String): String? {
        val luaMatch = luaFenceRegex.find(markdown)?.groupValues?.getOrNull(1)?.trim()
        if (!luaMatch.isNullOrBlank()) return luaMatch
        val genericMatch = genericFenceRegex.find(markdown)?.groupValues?.getOrNull(1)?.trim()
        return genericMatch?.takeIf { it.isNotBlank() }
    }

    fun extractPromptBlock(markdown: String): String? {
        val fenced = promptFenceRegex.find(markdown)?.groupValues?.getOrNull(1)?.trim()
        if (!fenced.isNullOrBlank()) return fenced

        val marker = Regex(
            """(?:AI\s*3D\s*(?:Generator\s*)?Prompt|Generator\s*Prompt)\s*:\s*(.+)""",
            setOf(RegexOption.IGNORE_CASE)
        ).find(markdown)?.groupValues?.getOrNull(1)?.trim()
        return marker?.takeIf { it.isNotBlank() }
    }
}
