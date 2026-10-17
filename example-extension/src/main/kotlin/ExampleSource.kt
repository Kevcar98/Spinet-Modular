package org.example.spinet

import dev.spinet.extension.Capability
import dev.spinet.extension.MusicSource
import dev.spinet.extension.SourceFactory
import dev.spinet.extension.SourceItem
import dev.spinet.extension.SourceTrack
import dev.spinet.extension.StreamLink
import java.io.File
import kotlin.math.PI
import kotlin.math.sin

class ExampleSource : MusicSource {
    override val id = "org.example.spinet.example"
    override val name = "Example"
    override val versionCode = 1
    override val capabilities = setOf(
        Capability.SEARCH,
        Capability.BROWSE,
        Capability.LOOKUP
    )

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

    override suspend fun resolve(trackId: String): StreamLink? {
        val track = track(trackId) ?: return null
        val pitch = if (track.id == "1") 440.0 else 330.0
        val file = File(System.getProperty("java.io.tmpdir"), "spinet-example-${track.id}.wav")
        if (!file.exists()) file.writeBytes(toneWav(pitch, track.durationMs))
        return StreamLink(
            url = file.toURI().toString(),
            mimeType = "audio/wav",

            downloadable = false
        )
    }

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

class ExampleFactory : SourceFactory {
    override fun create(): List<MusicSource> = listOf(ExampleSource())
}
