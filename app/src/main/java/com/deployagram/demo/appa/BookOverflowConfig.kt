package com.deployagram.demo.appa

import io.ktor.client.engine.HttpClientEngine

/**
 * Where AppA finds BookOverflow, and how it sends requests.
 *
 * The default points at BookOverflow on the host machine, as seen from the Android emulator (10.0.2.2 is the
 * emulator's alias for the host's loopback). Tests replace the engine with a double, so no network is used.
 */
object BookOverflowConfig {
    @Volatile
    var baseUrl: String = "http://10.0.2.2:8080"

    /** The engine that sends the requests. Null means the real OkHttp engine. */
    @Volatile
    var engine: HttpClientEngine? = null
}
