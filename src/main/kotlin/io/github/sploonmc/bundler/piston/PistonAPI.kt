package io.github.sploonmc.bundler.piston

import io.github.sploonmc.bundler.getUriJson
import java.net.URI

object PistonAPI {
    private const val PISTON_META_URL = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"

    fun getPistonVersions() = getUriJson<PistonVersionsResponse>(URI(PISTON_META_URL))

    fun PistonVersionsResponse.getVersionMeta(version: String) = getUriJson<PistonVersionMeta>(
        URI(
            versions.find { pistonVersion -> pistonVersion.id == version.toString() }?.url
                ?: throw IllegalArgumentException("invalid version: $version")
        )
    )
}