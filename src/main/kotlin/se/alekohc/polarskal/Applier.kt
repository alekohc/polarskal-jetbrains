package se.alekohc.polarskal

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.ide.ui.LafManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.colors.ColorKey
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.colors.impl.AbstractColorsScheme
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.options.colors.ColorSettingsPage
import com.intellij.openapi.project.ProjectManager
import org.jdom.Element
import se.alekohc.polarskal.colours.Palette
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

    // Language plugins ship their own colours for Darcula, which the scheme inherits and which beat the language
    // defaults: every key the template does not set goes back to its fallback, so any language follows the palette.
    fun inheritLanguageKeys() {
        val scheme = EditorColorsManager.getInstance().globalScheme
        if (scheme.name.contains("Polarskal")) {
            inherit(scheme)
            refresh(scheme)
        }
    }

    private fun inherit(scheme: EditorColorsScheme) {
        val ours = Templates.scheme.getChild("attributes").getChildren("option").map { it.getAttributeValue("name") }.toSet()
        for (page in ColorSettingsPage.EP_NAME.extensionList) {
            for (descriptor in page.attributeDescriptors) {
                val key = descriptor.key
                if (key.fallbackAttributeKey != null && key.externalName !in ours) {
                    scheme.setAttributes(key, AbstractColorsScheme.INHERITED_ATTRS_MARKER)
                }
            }
        }
    }

    // In memory only: the scheme on disk, and any copy the user made of it, stay as they are.
    private fun applyEditor(palette: Palette) {
        val scheme = EditorColorsManager.getInstance().globalScheme
        if (!scheme.name.contains("Polarskal")) {
            return
        }
        inherit(scheme)
        val rendered = Templates.scheme.clone().also { render(it, palette) }
        for (option in rendered.getChild("colors").getChildren("option")) {
            val value = option.getAttributeValue("value") ?: continue
            scheme.setColor(ColorKey.find(option.getAttributeValue("name")), Color.decode("#$value"))
        }
        for (option in rendered.getChild("attributes").getChildren("option")) {
            val value = option.getChild("value") ?: continue
            scheme.setAttributes(TextAttributesKey.find(option.getAttributeValue("name")), TextAttributes(value))
        }
        refresh(scheme)
    }

    private fun refresh(scheme: EditorColorsScheme) {
        ApplicationManager.getApplication().messageBus.syncPublisher(EditorColorsManager.TOPIC).globalSchemeChange(scheme)
        EditorFactory.getInstance().refreshAllEditors()
        // Semantic colours (annotations, calls, types) are held by the last analysis; a new one takes the new scheme.
        ProjectManager.getInstance().openProjects.forEach { DaemonCodeAnalyzer.getInstance(it).restart() }
    }

    private fun render(element: Element, palette: Palette) {
        element.getAttributeValue("value")?.let { ref ->
            palette.resolve(ref)?.let { element.setAttribute("value", Palette.hex(it, false)) }
        }
        element.children.forEach { render(it, palette) }
    }
}
