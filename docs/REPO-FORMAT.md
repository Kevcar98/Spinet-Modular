# Extension repositories

A repository is one JSON file on a static host. Nothing runs on the server; the
app fetches the index, shows what is in it, and hands any install to the
platform. Anyone can publish one, and the app ships with none.

## index.json

```json
{
  "schema": 1,
  "name": "Example extensions",
  "website": "https://example.org/spinet",
  "extensions": [
    {
      "id": "org.bgdynamix.spinet.extension.example",
      "name": "Example",
      "versionCode": 3,
      "versionName": "1.2.0",
      "api": 1,
      "author": "someone",
      "license": "Apache-2.0",
      "description": "One line, shown in the list.",
      "iconUrl": "https://example.org/spinet/example.png",
      "capabilities": ["SEARCH", "BROWSE"],
      "artifacts": {
        "android": {
          "url": "https://example.org/spinet/example-1.2.0.apk",
          "size": 184320,
          "sha256": "9f2c…"
        },
        "desktop": {
          "url": "https://example.org/spinet/example-1.2.0.jar",
          "size": 96256,
          "sha256": "4ab1…"
        }
      }
    }
  ]
}
```

### Fields

| Field | Required | Notes |
|-------|----------|-------|
| `schema` | yes | This document is `1`. The app refuses what it cannot read. |
| `extensions[].id` | yes | Must match the extension's own `id`, and must never be reused for something else. |
| `versionCode` | yes | Integer, compared numerically. An update is a higher number. |
| `api` | yes | The contract it was built against. Hidden by apps with a lower one. |
| `capabilities` | no | Shown before install, so what it can do is visible before it is on the device. |
| `artifacts.*.sha256` | yes | Checked after download, before the file is handed on. A mismatch is discarded. |
| `license` | no | Shown as given. The app makes no judgement about it. |

## What the app does with it

1. The user adds a repository URL. The app stores the URL and nothing else.
2. It fetches `index.json` over HTTPS and lists what it finds.
3. On install it downloads the artifact, verifies the SHA-256, and hands the
   file to the platform: Android's package installer, or the extensions folder
   on the desktop.
4. On every launch it loads what is installed. Nothing is fetched, enabled or
   updated on its own.

## Rules the app enforces

- **HTTPS only.** An index served over plain HTTP is refused.
- **The checksum must match.** Not a warning — the file is deleted.
- **Same origin for artifacts.** An index may not point an artifact at an
  unrelated host, so adding a repository is a decision about one domain rather
  than an open-ended one.
- **No silent updates.** A newer version is shown; installing it is a choice.
- **No bundled repositories.** A fresh install has an empty list. There is no
  default, no suggestion, and no list to curate.

That last rule is the one that matters, and it is a design decision rather than
a technical necessity. A shipped default repository would make the app the
distributor of whatever is in it.
