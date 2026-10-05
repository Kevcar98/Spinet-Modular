# Turning Spinet into a host

What has to change in the existing apps, in the order it is worth doing. Each
step leaves a working app.

## 1. Define the boundary (done — `api/`)

`MusicSource` and friends. No dependencies, no platform, nothing that can
change without breaking every extension ever built against it.

## 2. Make the current sources implement it (done — `extension-host` branch)

Before removing anything, reshape what exists. `NetworkRepository` already does
search / resolve / browse against several backends; the work is expressing each
one as a `MusicSource` rather than methods on one class with a flag for which
backend is meant.

Keep them in-tree for this step. The app behaves exactly as it does now, and the
diff is a refactor rather than a feature change — easy to verify, easy to undo.

## 3. Load extensions alongside the built-ins (done)

Add the loaders (`loader/`), a Sources screen listing both, and a toggle per
source. The app now runs on a mixture: compiled-in sources and installed ones,
reached through the same interface. Still shipping everything it does today.

This is the point where the design is proven. Everything after it is removal.

## 4. Lift each source out

One per release, highest risk first:

| Source | Lifts to | Why it goes |
|--------|----------|-------------|
| The in-app extractor | its own extension — **done** | the extractor, the GPL obligation it brings, and the terms question all leave the app together |
| A hard-coded list of public instances | **removed** | a list of third-party servers is a curation decision the app should not be making |

Your own server **stays in the app**. It is not a third-party source, it is the
user's own machine, and the whole sync story depends on it being there.

On-device local music stays too, for the same reason: a folder on the device is
not a source anyone is being handed access to.

## 5. What is left

A player that:

- plays files on the device
- plays files on a server the user runs
- loads sources the user installed
- ships no scrapers, no instance lists, no repository list

With the extractor gone, the GPL-3.0 obligation it imposed goes with it, and
the core becomes licensable however you like — the extension that carries the
extractor stays GPL-3.0 on its own. Worth a solicitor's five minutes before you
change the LICENSE file; it is not a question to answer from a README.

## Cost, honestly

Step 2 is a week of evenings on a codebase this size. Step 3 is a few days.
Step 4 is a day per source plus a build pipeline for extensions. None of it is
hard; all of it is work, and the app is less capable out of the box at the end.

## What it actually changes

**It does help.** A player that ships no extractors is a general-purpose media
client. The code that touches a particular service is written, hosted and
installed by other people, and the user chooses to add it. That is the Mihon /
Aniyomi / Tachiyomi shape, and it is a materially different position from
shipping the scraper yourself.

**It is not a shield.** Tachiyomi was the project that defined this approach and
it shut down in January 2024 after legal pressure; its extension repositories
were taken down with it. Distance is not immunity — especially if you also point
people at where to find the extensions, which is why the app ships no default
repository and this one should not either.

**None of this is legal advice.** It is a description of an architecture and
what happened to the project that is best known for it.
