package com.deployagram.demo.appa

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.appendPathSegments
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** The JSON object AliceMessager sends and receives: {"message": "..."}. */
@Serializable
data class ConfigMessage(val message: String)

/** AliceMessager answered with a non-success status code, and its reply. */
class AliceMessagerRefused(val statusCode: Int, val reply: String) :
    Exception("AliceMessager answered $statusCode: $reply")

/** Calls AliceMessager's POST /AliceMessager/config. The payload is a JSON object, or no body at all. */
class AliceMessagerClient(
    private val http: HttpClient = HttpClient(AliceMessagerConfig.engine ?: OkHttp.create()),
) {
    /** Sends [payload] as {"message": payload}, or an empty body when it is null. Returns the reply, such as "World". */
    suspend fun sendConfig(payload: String?): String {
        val response = http.post(AliceMessagerConfig.baseUrl) {
            url { appendPathSegments("AliceMessager", "config") }
            if (payload != null) {
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(ConfigMessage.serializer(), ConfigMessage(payload)))
            }
        }
        val text = response.bodyAsText()
        val reply = runCatching { Json.decodeFromString(ConfigMessage.serializer(), text).message }.getOrDefault(text)
        if (!response.status.isSuccess()) throw AliceMessagerRefused(response.status.value, reply)
        return reply
    }
}
