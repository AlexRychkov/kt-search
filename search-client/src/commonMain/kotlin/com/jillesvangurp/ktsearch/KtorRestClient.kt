package com.jillesvangurp.ktsearch

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.content.*
import io.ktor.http.*
import kotlin.random.Random

expect fun defaultKtorHttpClient(logging: Boolean = false): HttpClient

/**
 * Ktor-client implementation of the RestClient.
 */
class KtorRestClient(
    private vararg val nodes: Node= arrayOf(Node("localhost",9200)),
    private val client: HttpClient = defaultKtorHttpClient(),
    private val https: Boolean = false,
    private val user: String? = null,
    private val password: String? = null,
    private val nodeSelector: NodeSelector = RoundRobinNodeSelector(),
) : RestClient {
    constructor(
        host: String = "localhost",
        port: Int = 9200
    ) : this(
        nodeSelector = RoundRobinNodeSelector(),
        nodes= arrayOf( Node(host, port))
    )

    override fun nextNode(): Node = nodeSelector.selectNode(nodes)

    override suspend fun doRequest(
        pathComponents: List<String>,
        httpMethod: HttpMethod,
        parameters: Map<String, Any>?,
        payload: String?,
        contentType: ContentType,
    ): RestResponse {

        val response = client.request {
            val node = nextNode()
            method = httpMethod
            url {
                host = node.host
                port = node.port
                if (!user.isNullOrBlank()) {
                    user = this@KtorRestClient.user
                }
                if (!password.isNullOrBlank()) {
                    password = this@KtorRestClient.password
                }
                protocol = if (https) URLProtocol.HTTPS else URLProtocol.HTTP
                path(pathComponents.joinToString("/"))
                if (!parameters.isNullOrEmpty()) {
                    parameters.entries.forEach { (key, value) ->
                        parameter(key, value)
                    }
                }
                if (payload != null) {
                    setBody(TextContent(payload, contentType = contentType))
                }
            }
        }

        val responseBody = response.readBytes()
        return when (response.status) {
            HttpStatusCode.OK -> RestResponse.Status2XX.OK(responseBody)
            HttpStatusCode.Created -> RestResponse.Status2XX.Created(responseBody)
            HttpStatusCode.Accepted -> RestResponse.Status2XX.Accepted(responseBody)
            HttpStatusCode.Gone -> RestResponse.Status2XX.Gone(responseBody)
            HttpStatusCode.PermanentRedirect -> RestResponse.Status3XX.PermanentRedirect(
                responseBody,
                response.headers["Location"]
            )
            HttpStatusCode.TemporaryRedirect -> RestResponse.Status3XX.TemporaryRedirect(
                responseBody,
                response.headers["Location"]
            )
            HttpStatusCode.NotModified -> RestResponse.Status3XX.NotModified(
                responseBody
            )
            HttpStatusCode.BadRequest -> RestResponse.Status4XX.BadRequest(responseBody)
            HttpStatusCode.Unauthorized -> RestResponse.Status4XX.UnAuthorized(responseBody)
            HttpStatusCode.Forbidden -> RestResponse.Status4XX.Forbidden(responseBody)
            HttpStatusCode.NotFound -> RestResponse.Status4XX.NotFound(responseBody)
            HttpStatusCode.InternalServerError -> RestResponse.Status5xx.InternalServerError(responseBody)
            HttpStatusCode.GatewayTimeout -> RestResponse.Status5xx.GatewayTimeout(responseBody)
            HttpStatusCode.ServiceUnavailable -> RestResponse.Status5xx.ServiceUnavailable(responseBody)
            else -> RestResponse.UnexpectedStatus(responseBody, response.status.value)
        }
    }
}



