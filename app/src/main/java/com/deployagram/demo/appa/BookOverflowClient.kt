package com.deployagram.demo.appa

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.url
import io.ktor.http.appendPathSegments
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/** BookOverflow answered with a non-success status code. */
class BookOverflowRefused(val statusCode: Int) : Exception("BookOverflow answered $statusCode")

/** Calls BookOverflow's GET /BookOverflow/suggestion?email=... with Ktor, and reports the outcome. */
class BookOverflowClient(
    private val http: HttpClient = HttpClient(BookOverflowConfig.engine ?: OkHttp.create()) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    },
) {
    suspend fun suggestionFor(email: String): Result<BookSuggestion> = runCatching {
        val response = http.get(BookOverflowConfig.baseUrl) {
            url {
                appendPathSegments("BookOverflow", "suggestion")
                parameters.append("email", email)
            }
        }
        if (!response.status.isSuccess()) throw BookOverflowRefused(response.status.value)
        response.body<BookSuggestion>()
    }
}
