package com.robloxai.studio

import com.robloxai.studio.domain.RobloxPrompts
import com.robloxai.studio.ui.components.LuauSyntaxHighlighter
import org.junit.Assert.*
import org.junit.Test

class LuauUtilsTest {

    @Test
    fun `extracts lua code fence from markdown`() {
        val md = """
            Place this inside ServerScriptService:

            ```lua
            --!strict
            local Players = game:GetService("Players")
            print("Ready")
            ```
        """.trimIndent()

        val extracted = RobloxPrompts.extractFirstLuaBlock(md)
        assertNotNull(extracted)
        assertTrue(extracted!!.contains("game:GetService(\"Players\")"))
        assertFalse(extracted.contains("```"))
    }

    @Test
    fun `extracts luau code fence and ignores prompt fence`() {
        val md = """
            ```prompt
            Low-poly medieval tavern with timber beams
            ```

            ```luau
            local module = {}
            return module
            ```
        """.trimIndent()

        val lua = RobloxPrompts.extractFirstLuaBlock(md)
        val prompt = RobloxPrompts.extractPromptBlock(md)

        assertEquals("local module = {}\nreturn module", lua)
        assertEquals("Low-poly medieval tavern with timber beams", prompt)
    }

    @Test
    fun `highlights keywords strings comments and numbers without altering text`() {
        val sample = """
            -- Initialize coins
            local coins = 0xFF
            local label = "Gold"
        """.trimIndent()

        val annotated = LuauSyntaxHighlighter.highlight(sample)
        assertEquals(sample, annotated.text)
        assertTrue(annotated.spanStyles.isNotEmpty())
    }
}
