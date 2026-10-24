# Writing a Spinet extension

An extension adds a source of music to Spinet: something to search, play, and
maybe save from. Spinet itself only plays files on the device and on the user's
own servers, so anything else, from a radio station to a music library on your
own website, is an extension.

This guide takes you from nothing to an extension installed in the app. You
need to know some Kotlin. Everything else is explained here.

## What you need

- JDK 17 or newer.
- This repository, cloned. It holds the contract (`api/`) and a complete
  working example (`example-extension/`) you can copy.
- For Android: the Android SDK, or Android Studio.

## How it fits together

The app and your extension share one small file of Kotlin interfaces:
[`MusicSource.kt`](../api/src/commonMain/kotlin/dev/spinet/extension/MusicSource.kt).
Your extension implements `MusicSource`, and hands the app its sources through
a `SourceFactory`. The app knows nothing else about it.

On a PC an extension is a `.jar` file. On Android it is an `.apk` the user
installs like any other app. The same Kotlin code can go in both.

## 1. Pick an id

Every source has an `id` that never changes and is never reused, for example
`org.yourname.spinet.mystation`. The app stores the user's settings and choices
under it.

On Android the package name (the `applicationId`) must start with
`dev.spinet.extension.`, for example `dev.spinet.extension.mystation`. That
prefix is how the app finds extensions without looking at every app on the
phone.

## 2. Write the source

Two things are required: `search` and `resolve`. Everything else in the
contract has a default and can be left out.

```kotlin
class MyStation : MusicSource {
    override val id = "org.yourname.spinet.mystation"
    override val name = "My Station"
    override val versionCode = 1
    override val capabilities = setOf(Capability.SEARCH)

    override suspend fun search(query: String, page: Int): List<SourceTrack> {
        if (page > 1) return emptyList()
        // Ask your service and turn each result into a SourceTrack.
        return listOf(
            SourceTrack(id = "42", title = "A song", artist = "Someone", durationMs = 180_000)
        )
    }

    override suspend fun resolve(trackId: String): StreamLink? {
        // Called just before playback. Return a link the player can open.
        return StreamLink(url = "https://example.org/audio/$trackId.mp3", mimeType = "audio/mpeg")
    }
}

class MyFactory : SourceFactory {
    override fun create(): List<MusicSource> = listOf(MyStation())
}
```

Things to know:

- **`search`** is called with `page` starting at 1. Return an empty list when
  there are no more results.
- **`resolve`** is called one track at a time, right before it plays, because
  links from most services expire. Don't resolve a whole list in advance.
- **Ids are yours.** The app never looks inside a track id; it only hands it
  back to you.
- **Capabilities** say what your source can do: `SEARCH`, `BROWSE` (folders or
  playlists), `LOOKUP` (one track by id) and `DOWNLOAD`. The app hides
  anything you don't list.
- **Other versions.** With `SEARCH` listed, your source is also used by the
  "Other versions" window on every song, where people swap a track for a
  different upload, remix or live recording, or type their own words to search
  again. The app calls `searchVersions` for this. It runs your normal `search`
  unless you override it, so only override it when your service has a better
  way to find alternate versions, such as including videos as well as songs.
- **Saving songs.** Only add `DOWNLOAD`, and only set
  `StreamLink.downloadable = true`, when the service allows its music to be
  saved. That decision is yours as the author, because you know the terms the
  music is offered under.
- **Settings.** If your source needs an address or a key from the user, list
  them in `settings` as `SettingField`s. The app shows the fields, stores what
  the user types, and gives the values to `applySettings`. Mark passwords and
  keys with `secret = true`.

The example in [`example-extension/`](../example-extension) uses every part of
the contract and needs no network, so it is a good place to start from.

## 3. Package it for the PC

The extension is a jar with two extra entries.

`build.gradle.kts`:

```kotlin
plugins {
    kotlin("jvm")
}

dependencies {
    // compileOnly: the app already has the contract. A second copy inside
    // your jar would make the app unable to use your classes.
    compileOnly(project(":api"))
}

tasks.jar {
    archiveFileName.set("spinet-mystation.jar")
    manifest { attributes("Spinet-Extension-Api" to "1") }
}
```

And a file at `src/main/resources/META-INF/services/dev.spinet.extension.SourceFactory`
containing one line, the full name of your factory:

```
org.yourname.spinet.MyFactory
```

Build it with `./gradlew jar`. Libraries your extension uses (an HTTP client,
a JSON parser) go inside the jar; the app loads them separately from its own,
so versions never clash.

## 4. Package it for Android

The extension is an app with no screens. Make an Android application module
that depends on your source code, and give it this manifest:

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <!-- Only if your source uses the network. -->
    <uses-permission android:name="android.permission.INTERNET" />

    <application android:label="My Station" android:hasCode="true">
        <meta-data
            android:name="spinet.extension.factory"
            android:value="org.yourname.spinet.MyFactory" />
        <meta-data
            android:name="spinet.extension.api"
            android:value="1" />
    </application>
</manifest>
```

Remember the package name rule from step 1: `applicationId` must start with
`dev.spinet.extension.`. As on the PC, the contract is `compileOnly`.

## 5. Install it and try it

- **PC:** in Spinet, open Settings, turn on **Pluggable sources**, then under
  Sources press **Install from file** and pick your jar. You can also drag the
  jar onto the Spinet window.
- **Android:** install the APK, then open Spinet. The app lists it under
  Settings, Sources.

New extensions arrive switched off. Switch yours on, then search. If it can't
be loaded, it is still listed with the reason, for example a missing manifest
entry or a newer api than the app supports.

## 6. Share it

Send people the jar or APK, or publish a repository: a single `index.json` file
on any website that lists your extensions with download links and checksums.
Users add the repository's address in the app and install from there. The
format is in [REPO-FORMAT.md](REPO-FORMAT.md).

## Good to know

- **Licence.** The contract is MIT, so you can licence your extension however
  you like.
- **Versions.** Raise `versionCode` with every release. The app compares it
  numerically to offer updates from a repository.
- **The api version.** Extensions declare api `1`. A future contract only
  raises it for a change older extensions can't handle; adding an optional
  method with a default doesn't count, so extensions built today keep loading.
- **Respect the service.** Use services in line with their terms, and be clear
  in your extension's description about what it connects to.
