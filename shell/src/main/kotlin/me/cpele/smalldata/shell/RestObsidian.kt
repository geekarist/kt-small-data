package me.cpele.smalldata.shell

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import me.cpele.smalldata.core.Obsidian
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpRequest.BodyPublishers
import java.net.http.HttpResponse.BodyHandlers
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.CertificateFactory
import java.util.logging.Logger
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory

private const val API_KEY = "74b58c6979aff83c31ae20926bd823bbd8207bdf6d58ff7fa0437069ebab2bd0"
private const val BASE_URL = "https://127.0.0.1:27124"

class RestObsidian(private val json: Json) : Obsidian {

    override suspend fun notes(query: String): List<Obsidian.Finding> {
        val request =
            withContext(Dispatchers.Default) {
                val encodedQuery = URLEncoder.encode(query, Charsets.UTF_8)
                val searchUri =
                    URI(BASE_URL).resolve("/search/simple/").resolve("?query=$encodedQuery")
                HttpRequest.newBuilder(searchUri)
                    .header("Authorization", "Bearer $API_KEY")
                    .POST(BodyPublishers.noBody())
                    .build()
            }
        val client =
            withContext(Dispatchers.Default) {
                val sslCtx = buildSslContext()
                HttpClient.newBuilder().sslContext(sslCtx).build()
            }
        val responseBody =
            withContext(Dispatchers.IO) {
                val response = client.send(request, BodyHandlers.ofString())
                response.body()
            }
        val findings: List<Finding> =
            withContext(Dispatchers.Default) { json.decodeFromString(responseBody) }
        return findings
    }

    override suspend fun open(path: String) {
        Logger.getAnonymousLogger().info("Opening $path")
        val urlEncodedPath = URI(null, null, path, null).toASCIIString()
        Logger.getAnonymousLogger().info("URL-encoded path: $urlEncodedPath")
        val request =
            HttpRequest.newBuilder(URI(BASE_URL).resolve("/open/").resolve(urlEncodedPath))
                .header("Authorization", "Bearer $API_KEY")
                .POST(BodyPublishers.noBody())
                .build()
        Logger.getAnonymousLogger().info("Sending HTTP request: $request")
        val sslCtx = buildSslContext()
        val client = HttpClient.newBuilder().sslContext(sslCtx).build()
        val responseBody =
            withContext(Dispatchers.IO) { client.send(request, BodyHandlers.ofString()).body() }
        Logger.getAnonymousLogger().info("Got /open response: $responseBody")
    }

    @Serializable
    private data class Finding(private val filename: String) : Obsidian.Finding {
        override val label: String = filename
    }

    override suspend fun auth(): Obsidian.Details {
        val sslCtx = buildSslContext()
        val client = HttpClient.newBuilder().sslContext(sslCtx).build()
        val authUri = URI(BASE_URL)
        val request =
            HttpRequest.newBuilder(authUri).GET().header("Authorization", "Bearer $API_KEY").build()
        val authText: String =
            withContext(Dispatchers.IO) { client.send(request, BodyHandlers.ofString()).body() }
        val details: Details = json.decodeFromString(authText)
        Logger.getAnonymousLogger().info("Deserialized details: $details")
        return details
    }

    private suspend fun buildSslContext(): SSLContext? {
        val sslCtx = SSLContext.getInstance("TLS")
        val algorithm = TrustManagerFactory.getDefaultAlgorithm()
        val trustMgrFactory = TrustManagerFactory.getInstance(algorithm)
        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
        withContext(Dispatchers.IO) { keyStore.load(null, null) }
        val certFactory = CertificateFactory.getInstance("X.509")
        val certPath = "obsidian-local-rest-api.crt"
        val certStream = javaClass.classLoader.getResourceAsStream(certPath)
        val cert = certFactory.generateCertificate(certStream)
        keyStore.setCertificateEntry("cert-alias", cert)
        trustMgrFactory.init(keyStore)
        val trustMgr = trustMgrFactory.trustManagers
        sslCtx.init(null, trustMgr, SecureRandom())
        return sslCtx
    }

    @Serializable
    private data class Details(
        override val status: String,
        override val versions: Versions,
        override val service: String,
        override val authenticated: Boolean
    ) : Obsidian.Details {
        @Serializable
        data class Versions(override val obsidian: String, override val self: String) :
            Obsidian.Details.Versions
    }
}
