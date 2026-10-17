# Spinet Modular

The extension system for [Spinet](https://spinet.dev): the interface every
extension implements, the format for sharing extensions, and an example
extension. The code that loads extensions lives in the apps themselves.

Spinet plays files on your device and on a server you run. Anything else, such
as an online music service, is added as an extension that the user installs and
switches on.

```
api/               the interface extensions implement
example-extension/ a complete example extension, with no network access
docs/              how to write an extension, and the repository format
```

## Writing an extension

Start with [WRITING-EXTENSIONS.md](docs/WRITING-EXTENSIONS.md), a step-by-step
guide from an empty project to an extension installed on a PC or a phone.

## The interface

One file: [`MusicSource.kt`](api/src/commonMain/kotlin/dev/spinet/extension/MusicSource.kt).
Two methods are required and everything else is optional.

```kotlin
interface MusicSource {
    val id: String
    val name: String
    val versionCode: Int
    val capabilities: Set<Capability>

    suspend fun search(query: String, page: Int = 1): List<SourceTrack>
    suspend fun resolve(trackId: String): StreamLink?
    suspend fun browse(parentId: String? = null): List<SourceItem> = emptyList()
    suspend fun track(trackId: String): SourceTrack? = null
    suspend fun open(url: String): SourceTrack? = null
    suspend fun resolveForDownload(trackId: String): StreamLink? = resolve(trackId)
    suspend fun searchVersions(query: String): List<SourceTrack> = search(query, 1)
    suspend fun openPlaylist(url: String): SourcePlaylist? = null
    val canRecognize: Boolean get() = false
    suspend fun recognize(pcm: ShortArray, sampleRate: Int): SourceTrack? = null
    val settings: List<SettingField> get() = emptyList()
    fun applySettings(values: Map<String, String>) {}
}
```

Everything after `resolve` has a default, so an extension only implements what
it does. A playlist importer, for example, can leave search and resolve alone.

The interface has no dependencies, and new methods are only ever added with a
default, so extensions built today keep working with later versions of the app.

## How extensions are installed

**Android:** an extension is an APK the user installs like any other app. The
app lists the extensions installed on the phone; it never downloads code itself.

**PC:** an extension is a jar in the `extensions` folder inside the app's data
folder (`%USERPROFILE%\.spinet`, or `data\` beside a portable install). A jar
names its factory in `META-INF/services/dev.spinet.extension.SourceFactory` and
its api version in the manifest as `Spinet-Extension-Api: 1`.

A newly installed extension appears under Settings > Sources switched off.

An extension built for a newer api than the app supports is refused, and is
still listed with the reason so the user can see why it isn't working.

## Repositories

An extension repository is a single `index.json` file on any website. See
[REPO-FORMAT.md](docs/REPO-FORMAT.md). The app downloads the index, checks a
SHA-256 for every file it installs, and only installs what the user picks.

The app comes with no repositories. Users add the ones they want.

## Trying it

```bash
./gradlew :example-extension:jar
```

Copy `example-extension/build/libs/spinet-example-extension.jar` into the PC
app's `extensions` folder, switch **Example** on under Sources, and search for
"tone". It plays a generated sine wave.

## Licence

The interface and the example are MIT, so an extension can use any licence its
author likes. The Spinet apps themselves are GPL-3.0.
