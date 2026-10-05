package org.example.spinet

import org.bgdynamix.spinet.extension.Capability
import org.bgdynamix.spinet.extension.MusicSource
import org.bgdynamix.spinet.extension.SourceFactory
import org.bgdynamix.spinet.extension.SourceItem
import org.bgdynamix.spinet.extension.SourceTrack
import org.bgdynamix.spinet.extension.StreamLink
import java.io.File
import kotlin.math.PI
import kotlin.math.sin

/**
 * A worked example of the whole contract, with no network in it.
 *
 * Deliberately not a real service: this exists to be read, copied and compiled
 * against, and a template that scraped something would answer the question
 * "what should my extension do" with an example of the thing the host app is
 * careful not to do.
 *
 * A real extension replaces the list below with whatever it talks to — an http
 * client, a local index, a hardware device — and is otherwise this shape.
 */
class ExampleSource : MusicSource {

    override val id = "org.example.spinet.example"
    override val name = "Example"
    override val versionCode = 1
    override val capabilities = setOf(
        Capability.SEARCH,
        Capability.BROWSE,
        Capability.LOOKUP
    )

    // Whatever this source has. A real one asks a server at this point.
    private val catalogue = listOf(
        SourceTrack(
            id = "1",
            title = "A Tone",
            artist = "Example",
            album = "Examples",
            durationMs = 30_000,
            detail = "Public domain"
        ),
        SourceTrack(
            id = "2",
            title = "Another Tone",
            artist = "Example",
            album = "Examples",
            durationMs = 45_000,
            detail = "Public domain"
        )
    )

    override suspend fun search(query: String, page: Int): List<SourceTrack> {
        // Paging is the caller's contract, not a suggestion: returning the whole
        // catalogue on every page would make the app ask forever.
        if (page > 1) return emptyList()
        val needle = query.trim()
        if (needle.isBlank()) return catalogue
        return catalogue.filter {
            it.title.contains(needle, ignoreCase = true) ||
                it.artist.contains(needle, ignoreCase = true)
        }
    }

    override suspend fun browse(parentId: String?): List<SourceItem> =
        if (parentId == null) {
            listOf(SourceItem(id = "all", name = "Everything", trackCount = catalogue.size))
        } else {
            emptyList()
        }

    override suspend fun track(trackId: String): SourceTrack? =
        catalogue.firstOrNull { it.id == trackId }

    /**
     * Resolved one at a time, when playback is about to start. A real source
     * does its token dance here, which is why this is not done in bulk.
     *
     * This one writes its tone to a temporary WAV file and hands back that
     * file, so the example plays without touching the network.
     */
    override suspend fun resolve(trackId: String): StreamLink? {
        val track = track(trackId) ?: return null
        val pitch = if (track.id == "1") 440.0 else 330.0
        val file = File(System.getProperty("java.io.tmpdir"), "spinet-example-${track.id}.wav")
        if (!file.exists()) file.writeBytes(toneWav(pitch, track.durationMs))
        return StreamLink(
            url = file.toURI().toString(),
            mimeType = "audio/wav",
            // Saving is offered only where whoever wrote the extension says it
            // may be. The app does not decide this.
            downloadable = false
        )
    }

    /** A quiet sine wave as 16-bit mono PCM in a WAV container. */
    private fun toneWav(hz: Double, durationMs: Long): ByteArray {
        val rate = 22_050
        val samples = (rate * durationMs / 1000).toInt()
        val data = ByteArray(samples * 2)
        for (n in 0 until samples) {
            val v = (sin(2 * PI * hz * n / rate) * 6_000).toInt()
            data[2 * n] = v.toByte()
            data[2 * n + 1] = (v shr 8).toByte()
        }
        fun le(value: Int, bytes: Int) = ByteArray(bytes) { (value shr (8 * it)).toByte() }
        return "RIFF".toByteArray() + le(36 + data.size, 4) + "WAVE".toByteArray() +
            "fmt ".toByteArray() + le(16, 4) + le(1, 2) + le(1, 2) + le(rate, 4) +
            le(rate * 2, 4) + le(2, 2) + le(16, 2) +
            "data".toByteArray() + le(data.size, 4) + data
    }
}

/**
 * The entry point. On Android this class is named in the manifest; on the
 * desktop it goes in META-INF/services/org.bgdynamix.spinet.extension.SourceFactory.
 */
class ExampleFactory : SourceFactory {
    override fun create(): List<MusicSource> = listOf(ExampleSource())
}
