package se.alekohc.polarskal

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

// Per project, but the service starts once; also runs after an install without a restart.
class Startup : ProjectActivity {
    override suspend fun execute(project: Project) = service<PolarskalService>().start()
}
