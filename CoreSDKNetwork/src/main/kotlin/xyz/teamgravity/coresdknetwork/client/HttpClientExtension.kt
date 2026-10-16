package xyz.teamgravity.coresdknetwork.client

import io.ktor.client.HttpClient
import io.ktor.client.call.NoTransformationFoundException
import io.ktor.client.call.body
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.serialization.ContentConvertException
import io.ktor.util.reflect.TypeInfo
import io.ktor.util.reflect.typeInfo
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import timber.log.Timber
import xyz.teamgravity.coresdkandroid.result.GeneralError
import xyz.teamgravity.coresdkandroid.result.Result
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.nio.channels.UnresolvedAddressException

/**
 * Text that the cause of a ConnectException contains when the server refused the connection, for example
 * "ECONNREFUSED (Connection refused)". Any other failure to connect means the device has no working connection.
 */
private const val CONNECTION_REFUSED = "refused"

/**
 * Sends a GET request to [route] and returns the response body, or the GeneralError that describes the failure.
 * Never throws for a network, HTTP or serialization failure.
 *
 * @param route
 * Path relative to the client's base url, for example "notes".
 * @param parameters
 * Query parameters. A null value is skipped.
 */
suspend inline fun <reified Response : Any> HttpClient.safelyGet(
    route: String,
    parameters: Map<String, Any?> = emptyMap()
): Result<Response, GeneralError> {
    return safelyCall(
        typeInfo = typeInfo<Response>(),
        execute = {
            return@safelyCall get {
                url(route)
                parameters.forEach { (key, value) ->
                    parameter(key, value)
                }
            }
        }
    )
}

/**
 * Sends a POST request with [body] to [route] and returns the response body, or the GeneralError that describes the failure.
 * Never throws for a network, HTTP or serialization failure.
 *
 * @param route
 * Path relative to the client's base url, for example "notes".
 * @param body
 * Request body, serialized as JSON.
 */
suspend inline fun <reified Request, reified Response : Any> HttpClient.safelyPost(
    route: String,
    body: Request
): Result<Response, GeneralError> {
    return safelyCall(
        typeInfo = typeInfo<Response>(),
        execute = {
            return@safelyCall post {
                url(route)
                setBody(body)
            }
        }
    )
}

/**
 * Sends a PUT request with [body] to [route] and returns the response body, or the GeneralError that describes the failure.
 * Never throws for a network, HTTP or serialization failure.
 *
 * @param route
 * Path relative to the client's base url, for example "notes/1".
 * @param body
 * Request body, serialized as JSON.
 */
suspend inline fun <reified Request, reified Response : Any> HttpClient.safelyPut(
    route: String,
    body: Request
): Result<Response, GeneralError> {
    return safelyCall(
        typeInfo = typeInfo<Response>(),
        execute = {
            return@safelyCall put {
                url(route)
                setBody(body)
            }
        }
    )
}

/**
 * Sends a PATCH request with [body] to [route] and returns the response body, or the GeneralError that describes the failure.
 * Never throws for a network, HTTP or serialization failure.
 *
 * @param route
 * Path relative to the client's base url, for example "notes/1".
 * @param body
 * Request body, serialized as JSON.
 */
suspend inline fun <reified Request, reified Response : Any> HttpClient.safelyPatch(
    route: String,
    body: Request
): Result<Response, GeneralError> {
    return safelyCall(
        typeInfo = typeInfo<Response>(),
        execute = {
            return@safelyCall patch {
                url(route)
                setBody(body)
            }
        }
    )
}

/**
 * Sends a DELETE request to [route] and returns the response body, or the GeneralError that describes the failure.
 * Never throws for a network, HTTP or serialization failure.
 *
 * @param route
 * Path relative to the client's base url, for example "notes/1".
 * @param parameters
 * Query parameters. A null value is skipped.
 */
suspend inline fun <reified Response : Any> HttpClient.safelyDelete(
    route: String,
    parameters: Map<String, Any?> = emptyMap()
): Result<Response, GeneralError> {
    return safelyCall(
        typeInfo = typeInfo<Response>(),
        execute = {
            return@safelyCall delete {
                url(route)
                parameters.forEach { (key, value) ->
                    parameter(key, value)
                }
            }
        }
    )
}

/**
 * Executes the request and turns the response, or the exception it throws, into a Result.
 * Not part of the public API; it is published only because the inline helpers above call it.
 *
 * @param typeInfo
 * Type of the response body.
 * @param execute
 * Sends the request.
 */
@PublishedApi
internal suspend fun <Response> safelyCall(
    typeInfo: TypeInfo,
    execute: suspend () -> HttpResponse
): Result<Response, GeneralError> {
    try {
        val response = execute()
        if (!response.status.isSuccess()) return Result.Error(response.status.toGeneralError())
        return Result.Success(response.body(typeInfo))
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        Timber.e(e)
        return Result.Error(e.toGeneralError())
    }
}

private fun HttpStatusCode.toGeneralError(): GeneralError {
    return when (this) {
        HttpStatusCode.BadRequest -> GeneralError.IllegalArgument
        HttpStatusCode.Unauthorized -> GeneralError.Unauthorized
        HttpStatusCode.Forbidden -> GeneralError.Forbidden
        HttpStatusCode.NotFound -> GeneralError.NotFound
        HttpStatusCode.RequestTimeout -> GeneralError.Timeout
        HttpStatusCode.Conflict -> GeneralError.Conflict
        HttpStatusCode.PayloadTooLarge -> GeneralError.PayloadTooLarge
        HttpStatusCode.TooManyRequests -> GeneralError.TooManyRequests
        else -> if (value >= HttpStatusCode.InternalServerError.value) GeneralError.Server else GeneralError.Unknown
    }
}

private fun Exception.toGeneralError(): GeneralError {
    return when (this) {
        is HttpRequestTimeoutException, is ConnectTimeoutException, is SocketTimeoutException -> GeneralError.Timeout
        is UnknownHostException, is UnresolvedAddressException, is NoRouteToHostException -> GeneralError.Internet
        is ConnectException -> if (isConnectionRefused()) GeneralError.Server else GeneralError.Internet
        is ContentConvertException, is NoTransformationFoundException, is SerializationException -> GeneralError.Serialization
        else -> GeneralError.Unknown
    }
}

private fun Throwable.isConnectionRefused(): Boolean {
    return generateSequence(this) { throwable -> throwable.cause }
        .any { throwable -> throwable.message?.contains(CONNECTION_REFUSED, ignoreCase = true) == true }
}
