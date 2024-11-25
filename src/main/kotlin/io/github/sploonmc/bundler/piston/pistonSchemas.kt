package io.github.sploonmc.bundler.piston

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PistonVersion(
    val id: String,
    val url: String,
)

@Serializable
data class PistonLibrary(
    val name: String,
)

@Serializable
data class PistonVersionsResponse(
    val versions: List<PistonVersion>,
)

@Serializable
data class PistonVersionDownload(
    val url: String
)

@Serializable
data class PistonVersionDownloads(
    val server: PistonVersionDownload,
    @SerialName("server_mappings") val serverMappings: PistonVersionDownload
)

@Serializable
data class PistonVersionMeta(
    val downloads: PistonVersionDownloads,
    val libraries: List<PistonLibrary>
)