package io.github.sploonmc.builder

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PatchedVersionMeta(
    @SerialName("patch_file") val patchFile: String,
    @SerialName("commit_hashes") val commitHashes: CommitHashes,
    @SerialName("patch_hash") val patchHash: String,
    @SerialName("vanilla_jar_hash") val vanillaJarHash: String,
    @SerialName("patched_jar_hash") val patchedJarHash: String,
    @SerialName("vanilla_download_url") val vanillaDownloadUrl: String
)

@Serializable
data class CommitHashes(
    @SerialName("BuildData") val buildData: String,
    @SerialName("Bukkit") val bukkit: String,
    @SerialName("CraftBukkit") val craftBukkit: String,
    @SerialName("Spigot") val spigot: String
)
