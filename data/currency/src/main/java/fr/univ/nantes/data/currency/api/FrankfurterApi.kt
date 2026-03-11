package fr.univ.nantes.data.currency.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.serialization.Serializable

@Serializable
data class FrankfurterResponse(
    val base: String,
    val date: String,
    val rates: Map<String, Double>
)

class FrankfurterApi(private val httpClient: HttpClient) {

    suspend fun getLatestRates(base: String): FrankfurterResponse {
        return httpClient.get("https://api.frankfurter.dev/v1/latest") {
            parameter("from", base)
        }.body()
    }
}
