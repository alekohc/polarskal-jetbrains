import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import se.alekohc.polarskal.colours.Palette

plugins {
    id("org.jetbrains.kotlin.jvm") version "2.4.21"
    id("org.jetbrains.intellij.platform") version "2.19.0"
}

group = "se.alekohc.polarskal"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdea("2026.2")
        testFramework(TestFrameworkType.Platform)
    }
    testImplementation("junit:junit:4.13.2")
}

kotlin {
    jvmToolchain(25)
}

// The Islands colour names the themes override are those of 2026.1 and later; 2025.3 named them differently.
intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "261"
            untilBuild = provider { null }
        }
    }
}

// The bundled themes: the templates rendered with the default palettes, by the same code that renders the live ones.
val generateThemes = tasks.register("generateThemes") {
    val templates = file("src/main/resources/templates")
    val palettes = file("src/main/resources/palettes")
    val out = layout.buildDirectory.dir("generated/themes")
    inputs.dir(templates)
    inputs.dir(palettes)
    outputs.dir(out)
    doLast {
        val variants = listOf(
            mapOf("scheme" to "polarskal", "name" to "Polarskal", "dark" to "true", "parentTheme" to "Islands Dark",
                  "parentScheme" to "Darcula", "palette" to "dark"),
            mapOf("scheme" to "polarskal-light", "name" to "Polarskal Light", "dark" to "false", "parentTheme" to "Islands Light",
                  "parentScheme" to "Default", "palette" to "light"),
        )
        val dir = out.get().dir("themes").asFile.apply { mkdirs() }
        for (v in variants) {
            val palette = Palette.parse(File(palettes, "${v["palette"]}.json").readText())!!
            val theme = File(templates, "theme.json").readText().replace("\"{{ dark }}\"", v.getValue("dark"))
            File(dir, "${v["scheme"]}.theme.json").writeText(palette.render(theme, true, v))
            File(dir, "${v["scheme"]}.xml").writeText(palette.render(File(templates, "scheme.icls").readText(), false, v))
        }
    }
}

sourceSets.main {
    resources.srcDir(generateThemes)
}

// A fresh sandbox starts on Polarskal instead of Islands Dark; a theme picked in the sandbox afterwards is kept. Without the
// registry entry the first start counts as a switch to the new UI, which resets the theme to the default.
tasks.prepareSandbox {
    doLast {
        val registry = sandboxConfigDirectory.get().file("early-access-registry.txt").asFile
        if (!registry.exists()) {
            registry.parentFile.mkdirs()
            registry.writeText("ide.experimental.ui\ntrue\n")
        }
        val laf = sandboxConfigDirectory.get().file("options/laf.xml").asFile
        if (!laf.exists()) {
            laf.parentFile.mkdirs()
            laf.writeText("<application>\n  <component name=\"LafManager\" autodetect=\"false\">\n    <laf themeId=\"polarskal\" />\n  </component>\n</application>\n")
        }
    }
}
