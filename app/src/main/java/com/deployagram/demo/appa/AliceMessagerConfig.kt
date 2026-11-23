package com.deployagram.demo.appa

import io.ktor.client.engine.HttpClientEngine

/**
 * Where AppA finds AliceMessager, and how it sends requests.
 *
 * The default points at AliceMessager on the host machine, as seen from the Android emulator. Tests replace the engine
 * with a double, so no network is used.
 */
object AliceMessagerConfig {
    @Volatile
    var baseUrl: String = "http://10.0.2.2:8081"

    /** The engine that sends the requests. Null means the real OkHttp engine. */
    @Volatile
    var engine: HttpClientEngine? = null
}
