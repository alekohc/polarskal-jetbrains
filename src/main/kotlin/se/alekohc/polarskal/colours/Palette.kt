package se.alekohc.polarskal.colours

import java.awt.Color

// The shell's tokens ({"accent": "#7fe3c0", ...}) and the `{{ ... }}` references the templates make to them. Plain Kotlin:
// buildSrc compiles this file too, to render the bundled themes.
class Palette(private val colours: Map<String, Color>) {
    val isLight: Boolean
        get() {
            val c = colours.getValue("surfacePanel")
            return (maxOf(c.red, c.green, c.blue) + minOf(c.red, c.green, c.blue)) / 510.0 >= 0.5
        }

    // {{ accent }} that colour, {{ accent@20 }} it 20% over the panel, {{ accent/40 }} it at 40% alpha, {{ accent~60 }}
    // step 60 of a 10–160 ramp that passes through it at 80, {{ neutral~60 }} a step of the grey ramp.
    fun resolve(ref: String): Color? {
        val m = REF.matchEntire(ref.trim()) ?: return null
        val (name, op, arg) = m.destructured
        val n = arg.toIntOrNull()
        if (op == "~" && n != null) {
            val dark = colours.getValue(if (isLight) "textPrimary" else "surfaceSunken")
            val light = colours.getValue(if (isLight) "surfaceRaised" else "textPrimary")
            if (name == "neutral") {
                return mix(light, dark, (n - 10) / 150.0)
            }
            val c = colour(name) ?: return null
            return if (n <= 80) mix(c, dark, 1 - (80 - n) / 70.0 * 0.85) else mix(c, light, 1 - (n - 80) / 80.0 * 0.85)
        }
        val c = colour(name) ?: return null
        return when {
            op.isEmpty() -> c
            op == "@" && n != null -> mix(c, colours.getValue("surfacePanel"), n / 100.0)
            op == "/" && n != null -> Color(c.red, c.green, c.blue, Math.round(n * 2.55).toInt())
            else -> null
        }
    }

    // A shell from before the code colours does not write them; those keep the colour they had then.
    private fun colour(name: String): Color? = colours[name] ?: OLDER[name]?.let(colours::get)

    // Every reference in `text` as #rrggbb (#rrggbbaa with alpha), without the # for an .icls; `vars` are plain text.
    fun render(text: String, hash: Boolean, vars: Map<String, String> = emptyMap()): String = TEMPLATE.replace(text) { m ->
        vars[m.value.removePrefix("{{").removeSuffix("}}").trim()]
            ?: resolve(m.value)?.let { hex(it, hash) }
            ?: throw IllegalArgumentException("unknown reference ${m.value}")
    }

    companion object {
        private val REF = Regex("""\{\{\s*([A-Za-z]+)\s*(?:([@/~])\s*(\d+))?\s*}}""")
        private val ENTRY = Regex(""""(\w+)"\s*:\s*"(#[0-9a-fA-F]{6})"""")
        private val REQUIRED = listOf("surfaceSunken", "surfacePanel", "surfaceRaised", "textPrimary")
        private val OLDER = mapOf("codeKeyword" to "accentAlt", "codeString" to "warning", "codeNumber" to "focus",
                                  "codeFunction" to "accent", "codeType" to "ice", "codeField" to "textPrimary",
                                  "codeParameter" to "textPrimary", "codeAnnotation" to "focus", "codeEscape" to "focus")

        val TEMPLATE = Regex("""\{\{[^}]*}}""")

        // The file is flat and written by the shell, so a pattern reads it; null when a token the ramps need is missing.
        fun parse(text: String): Palette? {
            val colours = ENTRY.findAll(text).associate { it.groupValues[1] to Color.decode(it.groupValues[2]) }
            return if (REQUIRED.all(colours::containsKey)) Palette(colours) else null
        }

        fun hex(c: Color, hash: Boolean) =
            (if (hash) "#" else "") + "%06x".format(c.rgb and 0xffffff) + (if (c.alpha < 255) "%02x".format(c.alpha) else "")

        private fun mix(a: Color, b: Color, t: Double): Color {
            fun m(x: Int, y: Int) = Math.round(x * t + y * (1 - t)).toInt()
            return Color(m(a.red, b.red), m(a.green, b.green), m(a.blue, b.blue))
        }
    }
}
