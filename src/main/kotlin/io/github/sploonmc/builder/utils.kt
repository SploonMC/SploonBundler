package io.github.sploonmc.builder

import kotlinx.serialization.json.Json
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.io.path.createDirectories
import kotlin.io.path.readBytes

private val HTTP_CLIENT = HttpClient.newHttpClient()
val JSON = Json {
    ignoreUnknownKeys = true
}

inline fun <reified T> getUriJson(uri: URI): T = JSON.decodeFromString(getUri(uri))

fun downloadUri(uri: URI, output: Path) {
    output.parent.createDirectories()

    val response = HTTP_CLIENT.send(
        HttpRequest.newBuilder()
            .GET()
            .uri(uri)
            .build(),
        HttpResponse.BodyHandlers.ofFile(output)
    )

    response.body()
}

fun getUri(uri: URI): String {
    val response = HTTP_CLIENT.send(
        HttpRequest.newBuilder()
            .GET()
            .uri(uri)
            .build(),
        HttpResponse.BodyHandlers.ofString()
    )

    return response.body()
}

fun Path.sha1() = readBytes()
    .inputStream()
    .use {
        MessageDigest.getInstance("SHA-1").digest(it.readBytes())
    }.joinToString("") { "%02x".format(it) }
