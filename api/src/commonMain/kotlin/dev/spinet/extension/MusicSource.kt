package dev.spinet.extension

interface MusicSource {
    val id: String

    val name: String

    val versionCode: Int

    val apiVersion: Int get() = ApiVersion.CURRENT

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

data class SourcePlaylist(
    val name: String,
    val tracks: List<SourceTrack>,

    val playable: Boolean = true
)

data class SettingField(
    val key: String,
    val label: String,

    val hint: String = "",

    val secret: Boolean = false
)

enum class Capability {
    SEARCH,
    BROWSE,
    LOOKUP,

    DOWNLOAD
}

data class SourceTrack(
    val id: String,
    val title: String,
    val artist: String = "",
    val album: String = "",
    val durationMs: Long = 0,
    val artworkUrl: String = "",

    val detail: String = ""
)

data class SourceItem(
    val id: String,
    val name: String,
    val artworkUrl: String = "",
    val trackCount: Int = 0,

    val isContainer: Boolean = false
)

data class StreamLink(
    val url: String,
    val mimeType: String = "",
    val headers: Map<String, String> = emptyMap(),

    val downloadable: Boolean = false
)

object ApiVersion {
    const val CURRENT = 1
}

interface SourceFactory {
    fun create(): List<MusicSource>
}
