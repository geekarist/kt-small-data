package me.cpele.smalldata.shell

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.GenericUrl
import com.google.api.client.http.HttpRequestInitializer
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.customsearch.v1.CustomSearchAPI
import me.cpele.smalldata.core.GoogleEffects
import java.net.URL

class RestGoogleEffects : GoogleEffects {
    private val apiKey = System.getenv("GOOGLE_API_KEY")

    /**
     * TODO: Google custom search for given query
     *
     * See
     * [Identify your application](https://developers.google.com/custom-search/v1/introduction/?apix=true#identify_your_application_to_google_with_api_key)
     * and
     * [API overview](https://developers.google.com/custom-search/v1/introduction/?apix=true#api_overview)
     */
    override suspend fun search(query: String): List<GoogleEffects.Findings> {
        val transport = GoogleNetHttpTransport.newTrustedTransport()
        val jsonFactory = GsonFactory.builder().build()
        val reqInit = HttpRequestInitializer { req ->
            val url = req.url.toURL()
            val authenticatedUrl = URL(url, "?apiKey=$apiKey")
            req.url = GenericUrl(authenticatedUrl)
        }
        val api = CustomSearchAPI.Builder(transport, jsonFactory, reqInit).build()
        val engine = api.cse()
        val response = engine.list().setQ(query).execute()
        val items = response.items
        return items.map { GoogleEffects.Findings(label = it.title) }
    }
}
