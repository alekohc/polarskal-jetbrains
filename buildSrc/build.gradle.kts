plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}

// The plugin's own colour code, so the bundled themes are rendered exactly as the live ones.
sourceSets.main {
    kotlin.srcDir("../src/main/kotlin/se/alekohc/polarskal/colours")
}
