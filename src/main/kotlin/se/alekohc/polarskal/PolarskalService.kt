package se.alekohc.polarskal

import com.intellij.ide.ui.LafManagerListener
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.util.Disposer
import se.alekohc.polarskal.colours.Palette
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicBoolean

@Service(Service.Level.APP)
class PolarskalService : Disposable {
    private val started = AtomicBoolean()
    private var warned = false

    fun start() {
        if (!started.compareAndSet(false, true)) {
            return
        }
        val file = paletteFile()
        Disposer.register(this, Watcher(file) { reload(file) })
        // A theme change resets the UI defaults, and picking Polarskal should bring the live colours at once.
        ApplicationManager.getApplication().messageBus.connect(this)
            .subscribe(LafManagerListener.TOPIC, LafManagerListener {
                Applier.themeLoaded()
                reload(file)
            })
        reload(file)
    }

    // Without the file the bundled colours stay.
    private fun reload(file: Path) {
        if (!Files.isRegularFile(file)) {
            return
        }
        val palette = Palette.parse(runCatching { Files.readString(file) }.getOrDefault(""))
        if (palette == null) {
            if (!warned) {
                warned = true
                LOG.warn("Polarskal: cannot read the colours in $file")
            }
            return
        }
        ApplicationManager.getApplication().invokeLater { Applier.apply(palette) }
    }

    override fun dispose() = Unit

    companion object {
        private val LOG = logger<PolarskalService>()

        // Where the shell's [appearance.apps] jetbrains switch writes the colours.
        fun paletteFile(): Path {
            val data = System.getenv("XDG_DATA_HOME")?.takeIf { it.isNotEmpty() } ?: (System.getProperty("user.home") + "/.local/share")
            return Path.of(data, "polarskal", "jetbrains", "palette.json")
        }
    }
}
