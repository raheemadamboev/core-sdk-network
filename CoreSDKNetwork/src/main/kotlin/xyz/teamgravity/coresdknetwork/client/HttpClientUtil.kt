package xyz.teamgravity.coresdknetwork.client

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import timber.log.Timber

object HttpClientUtil {

    ///////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Creates HttpClient that serializes with [json], logs to Timber and resolves every request path against [baseUrl].
     * Non-2xx responses are not thrown, so the safe call helpers in HttpClientExtension can map them to GeneralError.
     *
     * @param engine
     * Engine that executes the requests. Pass a MockEngine in tests.
     * @param baseUrl
     * Base url of the server. It must end with a slash, and request paths must not start with one, for example
     * "https://api.example.com/v1/" and "notes".
     * @param json
     * Json instance that serializes request bodies and deserializes response bodies, usually CoreJson from core-sdk-android.
     * @param logLevel
     * How much of each request and response is logged, usually LogLevel.ALL in debug builds and LogLevel.NONE in release
     * builds. The Authorization header is never logged.
     * @param configuration
     * Extra configuration applied after the defaults, for example the Auth plugin. Do not set expectSuccess to true here,
     * otherwise non-2xx responses are thrown instead of being mapped to GeneralError.
     */
    fun create(
        engine: HttpClientEngine,
        baseUrl: String,
        json: Json,
        logLevel: LogLevel,
        configuration: HttpClientConfig<*>.() -> Unit = {}
    ): HttpClient {
        return HttpClient(engine) {
            expectSuccess = false

            install(ContentNegotiation) {
                json(json)
            }

            install(Logging) {
                logger = TimberLogger
                level = logLevel
                sanitizeHeader { header -> header == HttpHeaders.Authorization }
            }

            defaultRequest {
                url(baseUrl)
                contentType(ContentType.Application.Json)
            }

            configuration()
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Misc
    ///////////////////////////////////////////////////////////////////////////

    private object TimberLogger : Logger {
        override fun log(message: String) {
            Timber.d(message)
        }
    }
}
