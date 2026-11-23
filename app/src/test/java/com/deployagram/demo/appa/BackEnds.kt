package com.deployagram.demo.appa

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.content.OutgoingContent
import io.ktor.http.headersOf
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

/** The port BookOverflow is on, as the app's default configuration has it. */
const val BOOK_OVERFLOW_PORT = 8080

/** The port AliceMessager is on, as the app's default configuration has it. */
const val ALICE_MESSAGER_PORT = 8081

/**
 * The back-ends AppA calls, as one Ktor MockEngine. Each back-end is routed by its port, so the app can use one
 * client for both.
 */
class BackEnds {
    val bookOverflow = SuggestionAnswers()
    val aliceMessagerApp = AliceMessagerAnswers()

    fun handler(): MockRequestHandler = { request ->
        when (request.url.port) {
            ALICE_MESSAGER_PORT -> with(aliceMessagerApp) { answer(request) }
            else -> with(bookOverflow) { answer(request) }
        }
    }

    fun engine(): HttpClientEngine = MockEngine(handler())

}

/**
 * What AliceMessager answers. A POST with {"message":"Hello"} gets 200 with {"message":"World"}. Any other payload,
 * including an empty one, gets 400 with {"message":"Is someone there?"}. Each request it receives is queued for the test.
 */
class AliceMessagerAnswers {

    /** A request the double received: its path, and its body text, or null when it had no body. */
    data class Received(val path: String, val body: String?)

    private val received = LinkedBlockingQueue<Received>()

    companion object {
        const val HELLO = "{\"message\":\"Hello\"}"
        const val WORLD = "{\"message\":\"World\"}"
        const val IS_SOMEONE_THERE = "{\"message\":\"Is someone there?\"}"
    }

    fun nextRequest(): Received? = received.poll(5, TimeUnit.SECONDS)

    fun MockRequestHandleScope.answer(request: HttpRequestData): HttpResponseData {
        val body = bodyText(request.body)
        received.add(Received(request.url.encodedPath, body))
        val json = headersOf(HttpHeaders.ContentType, "application/json")
        return if (body == HELLO) {
            respond(content = WORLD, status = HttpStatusCode.OK, headers = json)
        } else {
            respond(content = IS_SOMEONE_THERE, status = HttpStatusCode.BadRequest, headers = json)
        }
    }

    private fun bodyText(body: Any?): String? =
        (body as? OutgoingContent.ByteArrayContent)?.bytes()?.decodeToString()?.takeIf { it.isNotEmpty() }
}
