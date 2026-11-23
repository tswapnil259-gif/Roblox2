package com.robloxai.studio.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

object LuauSyntaxHighlighter {
    private val keywordColor = Color(0xFFFF7B72)
    private val builtinColor = Color(0xFF79C0FF)
    private val stringColor = Color(0xFFA5D6FF)
    private val commentColor = Color(0xFF8B949E)
    private val numberColor = Color(0xFFD2A8FF)
    private val typeColor = Color(0xFFFFA657)

    private val keywords = setOf(
        "and", "break", "do", "else", "elseif", "end", "false", "for", "function",
        "if", "in", "local", "nil", "not", "or", "repeat", "return", "then",
        "true", "until", "while", "continue", "export", "type"
    )

    private val builtins = setOf(
        "game", "workspace", "script", "task", "math", "string", "table",
        "Instance", "Vector3", "CFrame", "Color3", "UDim2", "Enum",
        "TweenInfo", "RaycastParams", "Players", "ReplicatedStorage",
        "ServerScriptService", "DataStoreService", "print", "warn", "error",
        "require", "pcall", "xpcall", "pairs", "ipairs", "tonumber", "tostring"
    )

    private val tokenRegex = Regex(
        """(--\[\[[\s\S]*?]]|--[^\n]*)|("(?:\\.|[^"\\])*"|'(?:\\.|[^'\\])*')|(\b0[xX][0-9A-Fa-f_]+\b|\b0[bB][01_]+\b|\b\d+(?:\.\d+)?(?:[eE][+-]?\d+)?\b)|(\b[A-Za-z_][A-Za-z0-9_]*\b)"""
    )

    fun highlight(code: String): AnnotatedString = buildAnnotatedString {
        append(code)
        for (match in tokenRegex.findAll(code)) {
            val start = match.range.first
            val end = match.range.last + 1
            when {
                match.groups[1] != null -> addStyle(SpanStyle(color = commentColor), start, end)
                match.groups[2] != null -> addStyle(SpanStyle(color = stringColor), start, end)
                match.groups[3] != null -> addStyle(SpanStyle(color = numberColor), start, end)
                match.groups[4] != null -> {
                    val word = match.value
                    when {
                        word in keywords -> addStyle(
                            SpanStyle(color = keywordColor, fontWeight = FontWeight.SemiBold),
                            start,
                            end
                        )
                        word in builtins -> addStyle(SpanStyle(color = builtinColor), start, end)
                        word.firstOrNull()?.isUpperCase() == true -> addStyle(SpanStyle(color = typeColor), start, end)
                    }
                }
            }
        }
    }

    val visualTransformation = VisualTransformation { text ->
        TransformedText(highlight(text.text), OffsetMapping.Identity)
    }
}
