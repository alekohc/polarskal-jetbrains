package se.alekohc.polarskal

import org.jdom.Element
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import se.alekohc.polarskal.colours.Palette
import java.awt.Color

class TemplatesTest {
    private val dark = Templates.bundled(false)

    private val vars = setOf("{{ name }}", "{{ dark }}", "{{ parentTheme }}", "{{ scheme }}")

    @Test
    fun everyThemeReferenceIsAColour() {
        for (ref in Templates.refs - vars) {
            assertNotNull(ref, dark.resolve(ref))
            assertNotNull(ref, Templates.bundled(true).resolve(ref))
        }
    }

    @Test
    fun rampsAndAlpha() {
        assertEquals(dark.resolve("{{ accent }}"), dark.resolve("{{ accent~80 }}"))
        assertEquals(dark.resolve("{{ surfaceSunken }}"), dark.resolve("{{ neutral~10 }}"))
        assertEquals(dark.resolve("{{ textPrimary }}"), dark.resolve("{{ neutral~160 }}"))
        assertEquals("#7fe3c066", Palette.hex(dark.resolve("{{ accent/40 }}")!!, true))
    }

    @Test
    fun everySchemeColourResolves() {
        fun walk(element: Element) {
            element.getAttributeValue("value")?.takeIf { "{{" in it }?.let { assertNotNull(it, dark.resolve(it)) }
            element.children.forEach(::walk)
        }
        walk(Templates.scheme.getChild("colors"))
        walk(Templates.scheme.getChild("attributes"))
    }

    @Test
    fun tintMixesOverThePanel() {
        assertEquals(dark.resolve("{{ accent }}"), dark.resolve("{{ accent@100 }}"))
        assertEquals(dark.resolve("{{ surfacePanel }}"), dark.resolve("{{ accent@0 }}"))
        assertEquals(Color(0x2b, 0x36, 0x4b), dark.resolve("{{ focus@20 }}"))
    }

    @Test
    fun tone() {
        assertFalse(dark.isLight)
        assertTrue(Templates.bundled(true).isLight)
    }

    @Test
    fun olderShellKeepsTheOldCodeColours() {
        val older = Palette.parse(javaClass.getResource("/palettes/dark.json")!!.readText().replace(Regex(""""code\w+": "#\w+",?\s*"""), ""))!!
        assertEquals(older.resolve("{{ focus }}"), older.resolve("{{ codeAnnotation }}"))
    }

    @Test
    fun brokenFileIsNoPalette() {
        assertEquals(null, Palette.parse("{\"accent\": \"#7fe3c0\""))
        assertEquals(null, Palette.parse("{\"accent\": \"#7fe3c0\"}"))
    }
}
