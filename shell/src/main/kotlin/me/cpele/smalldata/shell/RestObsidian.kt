package me.cpele.smalldata.shell

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import me.cpele.smalldata.core.Obsidian
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.CertificateFactory
import java.util.logging.Logger
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory

object RestObsidian : Obsidian {

    override fun notes(query: String): List<Obsidian.Finding> {
        TODO("Not yet implemented")
    }

    override suspend fun auth(): Obsidian.Details {
        val apiKey = "74b58c6979aff83c31ae20926bd823bbd8207bdf6d58ff7fa0437069ebab2bd0"
        val authUri = URI("https://127.0.0.1:27124")
        val sslCtx = SSLContext.getInstance("TLS")
        val algorithm = TrustManagerFactory.getDefaultAlgorithm()
        val trustMgrFactory = TrustManagerFactory.getInstance(algorithm)
        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
        withContext(Dispatchers.IO) {
            keyStore.load(null, null)
        }
        val certFactory = CertificateFactory.getInstance("X.509")
        val certPath = "obsidian-local-rest-api.crt"
        val certStream = javaClass.classLoader.getResourceAsStream(certPath)
        val cert = certFactory.generateCertificate(certStream)
        keyStore.setCertificateEntry("cert-alias", cert)
        trustMgrFactory.init(keyStore)
        val trustMgr = trustMgrFactory.trustManagers
        sslCtx.init(null, trustMgr, SecureRandom())
        val client = HttpClient.newBuilder().sslContext(sslCtx).build()
        val request = HttpRequest
            .newBuilder(authUri)
            .GET()
            .header("Authorization", "Bearer $apiKey")
            .build()
        val authText: String = withContext(Dispatchers.IO) {
            client.send(request, BodyHandlers.ofString()).body()
        }
        val details: Details = Json.decodeFromString(authText)
        Logger.getAnonymousLogger().info("Deserialized details: $details")
        return details
    }

    @Serializable
    data class Details(
        override val status: String,
        override val versions: Versions,
        override val service: String,
        override val authenticated: Boolean
    ) : Obsidian.Details {
        @Serializable
        data class Versions(override val obsidian: String, override val self: String) : Obsidian.Details.Versions
    }
}