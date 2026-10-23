package se.alekoh.polarskal

import com.intellij.openapi.util.JDOMUtil
import org.jdom.Element
import se.alekoh.polarskal.colours.Palette

// The templates the bundled themes are built from (build.gradle.kts), read again so the live colours land where they did.
object Templates {
    // Every reference the theme makes, in its colours and its ui keys alike.
    val refs: Set<String> by lazy { Palette.TEMPLATE.findAll(read("/templates/theme.json")).map { it.value }.toSet() }

    val scheme: Element by lazy { JDOMUtil.load(read("/templates/scheme.icls")) }

    fun bundled(light: Boolean): Palette = Palette.parse(read("/palettes/${if (light) "light" else "dark"}.json"))!!

    private fun read(path: String) = Templates::class.java.getResource(path)!!.readText()
}
