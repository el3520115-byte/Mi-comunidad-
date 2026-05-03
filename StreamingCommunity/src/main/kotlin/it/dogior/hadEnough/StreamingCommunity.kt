package it.dogior.hadEnough

import com.lagradost.api.Log
import com.lagradost.cloudstream3.APIHolder.capitalize
import com.lagradost.cloudstream3.Episode
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.HomePageList
import com.lagradost.cloudstream3.HomePageResponse
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addImdbId
import com.lagradost.cloudstream3.LoadResponse.Companion.addScore
import com.lagradost.cloudstream3.LoadResponse.Companion.addTMDbId
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.MainPageRequest
import com.lagradost.cloudstream3.SearchResponseList
import com.lagradost.cloudstream3.newSearchResponseList
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.mainPageOf
import com.lagradost.cloudstream3.newEpisode
import com.lagradost.cloudstream3.newHomePageResponse
import com.lagradost.cloudstream3.newMovieLoadResponse
import com.lagradost.cloudstream3.newMovieSearchResponse
import com.lagradost.cloudstream3.newTvSeriesLoadResponse
import com.lagradost.cloudstream3.newTvSeriesSearchResponse
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import com.lagradost.cloudstream3.utils.AppUtils.toJson
import com.lagradost.cloudstream3.utils.ExtractorLink
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import org.jsoup.parser.Parser
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

class StreamingCommunity(
    override var lang: String = "es", // MODIFICADO: Cambiado de it a es
    private val showLogo: Boolean = true
) : MainAPI() {
    override var mainUrl = Companion.mainUrl + lang
    override var name = "StreamingCommunity ES" // MODIFICADO: Identificador único
    override var supportedTypes =
        setOf(TvType.Movie, TvType.TvSeries, TvType.Cartoon, TvType.Documentary)
    override val hasMainPage = true

    companion object {
        private var inertiaVersion = ""
        private var decodedXsrfToken = ""
        private val headers = mapOf(
            "Cookie" to "",
            "X-Inertia" to true.toString(),
            "X-Inertia-Version" to inertiaVersion,
            "X-Requested-With" to "XMLHttpRequest",
        ).toMutableMap()
        val mainUrl = "https://streamingunity.dog/"
        var name = "StreamingCommunity ES"
        val TAG = "SCommunityES"
    }

    // Traducimos las categorías para la UI de CloudStream
    override val mainPage = mainPageOf(
        "home" to "Inicio",
        "trending" to "Tendencias",
        "latest" to "Recién Agregados",
        "genre/Animation" to "Animación",
        "genre/Action" to "Acción",
        "genre/Adventure" to "Aventura",
        "genre/Sci-Fi" to "Ciencia Ficción",
        "genre/Horror" to "Terror"
    )

    // ... (Mantén las estructuras SliderFetchRequestSlider y SliderFetchRequestBody igual que en tu original)

    private val sliderFetchRequestBody = SliderFetchRequestBody(
        sliders = listOf(
            SliderFetchRequestSlider(name = "top10", genre = null),
            SliderFetchRequestSlider(name = "trending", genre = null),
            SliderFetchRequestSlider(name = "latest", genre = null),
            SliderFetchRequestSlider(name = "genre", genre = "Animation"),
            SliderFetchRequestSlider(name = "genre", genre = "Adventure"),
            SliderFetchRequestSlider(name = "genre", genre = "Action"),
            SliderFetchRequestSlider(name = "genre", genre = "Comedy"),
            SliderFetchRequestSlider(name = "genre", genre = "Science Fiction"),
            SliderFetchRequestSlider(name = "genre", genre = "Horror")
        )
    )

    // MODIFICADO: Función de búsqueda ahora prioriza TMDB en Español
    override suspend fun search(query: String): List<SearchResponse> {
        // Primero intentamos obtener datos limpios de TMDB en español
        val tmdbResults = tmdbSearch(query)
        if (tmdbResults.isNotEmpty()) return tmdbResults
        
        // Si no hay resultados en TMDB, caemos en el scraper original del sitio
        val url = "$mainUrl/search"
        val response = app.get(url, params = mapOf("q" to query)).body.string()
        val titles = parseBrowseTitles(response, "Search")
        return searchResponseBuilder(titles)
    }

    // El resto de funciones (setupHeaders, load, getEpisodes) deben mantenerse 
    // pero asegúrate de que el logoUrl también pida el idioma "es"
