# Spinet — modular

The plug, not the thing plugged in.

This is the extension architecture for [Spinet](https://spinet.dev): a contract
small enough to live forever, a repository format, and a worked example. The
loaders that find installed extensions live in the apps themselves, beside the
code that uses them. The app that uses it ships **no sources of its own** —
people install the ones they want, from repositories they choose.

```
api/               the whole contract. no dependencies, no platform
example-extension/ a complete source, with no network in it; builds a jar
docs/              repository format, and what has to change in the apps
```

## Why

A player that contains a scraper is a player that does what the scraper does. A
player that contains a plug is a player — the code for a particular service is
written, hosted and installed by other people.

That is the shape Mihon, Aniyomi and Tachiyomi before them settled on. It is a
genuinely different position from shipping the extractor yourself, and
[MIGRATION.md](docs/MIGRATION.md) is honest about where it stops: Tachiyomi
defined this approach and still shut down in 2024. Distance is not immunity.
None of this is legal advice.

## The contract

One file: [`MusicSource.kt`](api/src/commonMain/kotlin/org/bgdynamix/spinet/extension/MusicSource.kt).
Two methods are required; everything else is optional.

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

Everything after `resolve` has a body. A source implements only what it does —
a playlist importer can leave search, resolve and the rest alone.

It has no dependencies on purpose. Every extension ever built compiles against
this file, so anything added to it has to be carried forever.

## How loading works

**Android** — extensions are APKs the user installs like any other app. The
player never downloads code; it lists what is installed, and the package
installer does the rest.

**Desktop** — extensions are jars in a folder. The equivalent of "the user
installed it" on a machine with no package manager is "the user put the file
there": `extensions/` inside the app's data folder (`%USERPROFILE%\.spinet`, or
`data\` beside a portable install). A jar names its factory in
`META-INF/services/org.bgdynamix.spinet.extension.SourceFactory` and its api in
the manifest as `Spinet-Extension-Api: 1`.

Either way, installed is not enabled: a new source appears in Settings →
Sources switched off, and the app has to be restarted to see it.

Both loaders refuse an extension built against a newer api than the app
understands, and keep what they rejected so a missing source can be explained
rather than just absent.

## Repositories

A static `index.json` on any host — see [REPO-FORMAT.md](docs/REPO-FORMAT.md).
The app fetches it, verifies a SHA-256 on every download, and installs nothing
by itself.

**No repository ships with the app.** A fresh install has an empty list, no
default and no suggestions. Shipping one would make the app the distributor of
whatever was in it, which is the entire thing this design avoids.

## Trying it

```bash
./gradlew :example-extension:jar
```

Copy `example-extension/build/libs/spinet-example-extension.jar` into the
desktop app's `extensions` folder, restart, switch **Example** on under
Sources, and search for "tone". It plays a generated sine wave.

## Status

The contract is wired into both apps and the existing sources go through it
(MIGRATION steps 1–3). Lifting each one out is step 4.

## Licence

The contract and the loaders are MIT, so an extension can be written under any
licence its author likes. The player that hosts them keeps its own.
