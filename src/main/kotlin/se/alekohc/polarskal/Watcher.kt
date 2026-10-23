package se.alekohc.polarskal

import com.intellij.openapi.Disposable
import java.nio.file.ClosedWatchServiceException
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds.ENTRY_CREATE
import java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY

// The shell replaces the file by a rename, so the directory is watched, not the file.
class Watcher(private val file: Path, private val changed: () -> Unit) : Disposable {
    private val service = FileSystems.getDefault().newWatchService()

    init {
        Files.createDirectories(file.parent)
        file.parent.register(service, ENTRY_CREATE, ENTRY_MODIFY)
        Thread(::loop, "Polarskal palette watcher").apply { isDaemon = true }.start()
    }

    private fun loop() {
        try {
            while (true) {
                val key = service.take()
                // One write is a create and a modify or two; take them as one.
                Thread.sleep(150)
                val hit = key.pollEvents().any { (it.context() as? Path)?.fileName == file.fileName }
                key.reset()
                if (hit) {
                    changed()
                }
            }
        } catch (_: ClosedWatchServiceException) {
        } catch (_: InterruptedException) {
        }
    }

    override fun dispose() = service.close()
}
