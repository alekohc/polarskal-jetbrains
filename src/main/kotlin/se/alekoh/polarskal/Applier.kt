package se.alekoh.polarskal

import com.intellij.ide.ui.LafManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.colors.ColorKey
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.markup.TextAttributes
import org.jdom.Element
import se.alekoh.polarskal.colours.Palette
import java.awt.Color
import java.awt.Window
import javax.swing.SwingUtilities
import javax.swing.UIManager
import javax.swing.plaf.ColorUIResource

// On the EDT. Only while a Polarskal theme is the current one: any other theme the user picked is left alone.
object Applier {
    const val DARK = "polarskal"
    const val LIGHT = "polarskal-light"

    // The colours the UI shows now; null right after a theme loads, when it shows the bundled ones.
    private var shown: Palette? = null

    fun themeLoaded() {
        shown = null
    }

    fun apply(palette: Palette) {
        val laf = LafManager.getInstance()
        val current = laf.currentUIThemeLookAndFeel?.id ?: return
        if (current != DARK && current != LIGHT) {
            return
        }
        val wanted = if (palette.isLight) LIGHT else DARK
        if (current != wanted) {
            // The LafManagerListener brings the colours once the other theme is in.
            laf.installedThemes.firstOrNull { it.id == wanted }?.let {
                laf.setCurrentLookAndFeel(it, false)
                laf.updateUI()
            }
            return
        }
        applyUi(palette)
        applyEditor(palette)
    }

    // Every UI colour that is one of the theme's references, also the ones Islands derives from its named colours, is
    // found by its value under the colours shown now and replaced by that reference under the new ones.
    private fun applyUi(palette: Palette) {
        val from = shown ?: Templates.bundled(palette.isLight)
        val refs = HashMap<Int, String>()
        for (ref in Templates.refs) {
            from.resolve(ref)?.let { refs.putIfAbsent(it.rgb, ref) }
        }
        val defaults = UIManager.getLookAndFeelDefaults()
        for (key in defaults.keys.toList()) {
            val old = defaults[key] as? Color ?: continue
            val ref = refs[old.rgb] ?: continue
            palette.resolve(ref)?.let { defaults[key] = ColorUIResource(it) }
        }
        shown = palette
        Window.getWindows().forEach(SwingUtilities::updateComponentTreeUI)
    }

    // In memory only: the scheme on disk, and any copy the user made of it, stay as they are.
    private fun applyEditor(palette: Palette) {
        val scheme = EditorColorsManager.getInstance().globalScheme
        if (!scheme.name.contains("Polarskal")) {
            return
        }
        val rendered = Templates.scheme.clone().also { render(it, palette) }
        for (option in rendered.getChild("colors").getChildren("option")) {
            val value = option.getAttributeValue("value") ?: continue
            scheme.setColor(ColorKey.find(option.getAttributeValue("name")), Color.decode("#$value"))
        }
        for (option in rendered.getChild("attributes").getChildren("option")) {
            val value = option.getChild("value") ?: continue
            scheme.setAttributes(TextAttributesKey.find(option.getAttributeValue("name")), TextAttributes(value))
        }
        ApplicationManager.getApplication().messageBus.syncPublisher(EditorColorsManager.TOPIC).globalSchemeChange(scheme)
        EditorFactory.getInstance().refreshAllEditors()
    }

    private fun render(element: Element, palette: Palette) {
        element.getAttributeValue("value")?.let { ref ->
            palette.resolve(ref)?.let { element.setAttribute("value", Palette.hex(it, false)) }
        }
        element.children.forEach { render(it, palette) }
    }
}
