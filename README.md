# Polarskal Theme for JetBrains IDEs

The Polarskal shell's colours as a dark and a light theme with matching editor schemes. When the shell's
`[appearance.apps] jetbrains` switch is on, open IDEs follow its colours live, and flip between the dark and the light
theme with its tone.

## How it works

- `src/main/resources/templates/theme.json` and `scheme.icls` hold `{{ token }}` placeholders for the shell's tokens
  (`{{ token@NN }}` is that token at NN% over `surfacePanel`).
- At build time, `generateThemes` renders them with `palettes/dark.json` and `light.json` into the two bundled themes.
- At run time, the plugin watches `$XDG_DATA_HOME/polarskal/jetbrains/palette.json`. It renders the same templates
  with those colours onto the UI defaults and the editor scheme, but only while a Polarskal theme is selected.

## Build

Needs JDK 25, as the 2026.x platform does.

```sh
./gradlew test buildPlugin
./gradlew runIde      # a sandbox IDE with the plugin
```

Install `build/distributions/polarskal-jetbrains-<version>.zip` with Settings → Plugins → ⚙ → Install Plugin from Disk,
then pick Polarskal under Settings → Appearance → Theme.

## Release

Push a tag: `v0.2.0` builds and tests the plugin as version 0.2.0 and attaches the zip to a GitHub release of that tag
(`.github/workflows/release.yml`). The version in `gradle.properties` is only for local builds.
