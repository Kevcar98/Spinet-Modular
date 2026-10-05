package org.bgdynamix.spinet.extension

/**
 * Everything an extension has to implement, and the only thing the app knows
 * about a source.
 *
 * This file is the whole contract. It is deliberately small and deliberately
 * free of dependencies — no Ktor, no serialization, no Android, no Compose —
 * because every extension in existence compiles against it, and anything added
 * here has to be supported forever or it breaks them all.
 *
 * The app ships none of these. It knows how to load them, call them, and show
 * what they return.
 */
interface MusicSource {

    /** Stable, unique, and never reused: "org.example.myservice". */
    val id: String

    /** Shown in the sources list. */
    val name: String

    /** Bumped on every release of the extension. Compared numerically. */
    val versionCode: Int

    /**
     * Which [ApiVersion] this was built against.
     *
     * The app refuses to load an extension whose api is newer than its own —
     * the alternative is calling methods that do not exist, which fails at the
     * worst possible moment rather than at load time.
     */
    val apiVersion: Int get() = ApiVersion.CURRENT

    /** What this source can do. The UI hides what is not offered. */
    val capabilities: Set<Capability>

    /**
     * Free-text search.
     *
     * [page] starts at 1. Returning fewer results than asked for, or an empty
     * list, both mean "no more" — there is no total count, because most sources
     * cannot give an honest one.
     */
    suspend fun search(query: String, page: Int = 1): List<SourceTrack>

    /**
     * Turn a track into something playable.
     *
     * Called immediately before playback, never in bulk: links from most
     * sources expire, and resolving a whole list up front produces a list of
     * links that are dead by the time anyone presses play.
     */
    suspend fun resolve(trackId: String): StreamLink?

    /**
     * Browse a source that is organised into folders, albums or playlists.
     *
     * [parentId] null means the top level. Only called when the source declares
     * [Capability.BROWSE].
     */
    suspend fun browse(parentId: String? = null): List<SourceItem> = emptyList()

    /**
     * One track by id, when the app has an id but no metadata — a saved
     * playlist entry, a resumed session. Only with [Capability.LOOKUP].
     */
    suspend fun track(trackId: String): SourceTrack? = null

    /**
     * A link the user pasted, if it points at something this source has.
     *
     * Null means "not mine", which is the answer for almost every link, so the
     * app can offer one pasted url to each source in turn. Added after api 1
     * shipped, with a body, so extensions built before it still load.
     */
    suspend fun open(url: String): SourceTrack? = null

    /**
     * Like [resolve], for saving to disk rather than playing.
     *
     * The best stream to play and the best file to keep are often different —
     * a high-bitrate Opus stream plays well and makes an awkward file. Only
     * called with [Capability.DOWNLOAD], and the result is only saved when its
     * [StreamLink.downloadable] says so. Defaults to [resolve].
     */
    suspend fun resolveForDownload(trackId: String): StreamLink? = resolve(trackId)

    // ---- Added after api 1 shipped. Every one has a body, so an extension
    // ---- built before it still loads, and an app built before it never calls it.

    /**
     * Broader results for the same query, for picking a different version of a
     * song — a live take, a remix. Defaults to [search].
     */
    suspend fun searchVersions(query: String): List<SourceTrack> = search(query, 1)

    /**
     * A pasted link to a playlist, if this source knows it.
     *
     * Null means "not mine". When [SourcePlaylist.playable] is false the tracks
     * are only names — the app looks each one up in the user's other sources,
     * the way it imports a CSV file.
     */
    suspend fun openPlaylist(url: String): SourcePlaylist? = null

    /**
     * Whether [recognize] does anything. The app offers its listen button only
     * when a switched-on source says yes.
     */
    val canRecognize: Boolean get() = false

    /**
     * Name the song in a short recording: 16-bit mono PCM at [sampleRate].
     * Null when nothing matched. The app records; the source decides how to
     * identify — the app knows nothing about that.
     */
    suspend fun recognize(pcm: ShortArray, sampleRate: Int): SourceTrack? = null

    /**
     * Settings the user fills in for this source — an address, a key. The app
     * shows them under the source, stores the values, and hands them back
     * through [applySettings].
     */
    val settings: List<SettingField> get() = emptyList()

    /**
     * The user's values for [settings], keyed by [SettingField.key]. Called
     * once after loading and again whenever the user saves. A key the user has
     * never filled in is absent.
     */
    fun applySettings(values: Map<String, String>) {}
}

/** A playlist a source opened from a link. */
data class SourcePlaylist(
    val name: String,
    val tracks: List<SourceTrack>,
    /**
     * Whether the tracks play through this same source. False when the source
     * only knows the names — then the app matches them against the user's
     * other sources.
     */
    val playable: Boolean = true
)

/** One setting a source asks the user for. */
data class SettingField(
    /** Stable: the stored value is found by it. */
    val key: String,
    val label: String,
    /** Shown in the empty field as an example. */
    val hint: String = "",
    /** Typed into a password field and never shown again once saved. */
    val secret: Boolean = false
)

/** What a source supports; anything absent is hidden rather than failing. */
enum class Capability {
    SEARCH,
    BROWSE,
    LOOKUP,

    /**
     * The app may save the audio to disk.
     *
     * A source that only permits streaming leaves this out, and the save
     * actions do not appear for its tracks. The decision belongs to whoever
     * wrote the extension, because they are the ones who know the terms the
     * source is served under.
     */
    DOWNLOAD
}

/** A track as a source describes it. Ids are opaque to the app. */
data class SourceTrack(
    val id: String,
    val title: String,
    val artist: String = "",
    val album: String = "",
    val durationMs: Long = 0,
    val artworkUrl: String = "",
    /** Free-text, shown as-is: "FLAC", "320 kbps", "Creative Commons". */
    val detail: String = ""
)

/** A folder, album or playlist, for sources that have such a thing. */
data class SourceItem(
    val id: String,
    val name: String,
    val artworkUrl: String = "",
    val trackCount: Int = 0,
    /** Browsing this yields more items rather than tracks. */
    val isContainer: Boolean = false
)

/**
 * Where the audio actually is.
 *
 * [headers] exist because some sources only serve bytes with a referer or a
 * token attached; the app passes them through to the player untouched.
 */
data class StreamLink(
    val url: String,
    val mimeType: String = "",
    val headers: Map<String, String> = emptyMap(),
    /** Whether this link may be written to disk, if the source allows it. */
    val downloadable: Boolean = false
)

/** The api version this build of the contract represents. */
object ApiVersion {
    /**
     * Raised only when the interface changes in a way older extensions cannot
     * satisfy. Adding a method with a default body is not such a change.
     */
    const val CURRENT = 1
}

/**
 * The entry point an extension exposes.
 *
 * One package can carry several sources — a service with separate regional
 * endpoints, say — so the app asks for a list rather than assuming one.
 */
interface SourceFactory {
    fun create(): List<MusicSource>
}
