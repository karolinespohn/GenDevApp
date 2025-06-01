package org.gendev25.project

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.gendev25.project.components.CountryFlag
import org.gendev25.project.components.OfferCard
import org.gendev25.project.components.ShareButton
import org.jetbrains.compose.ui.tooling.preview.Preview
import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.*

@Serializable
internal data class OffersRequestDto(
    val address: AddressRequestDto,
    val wantsFiber: Boolean = false,
    val installation: Boolean = false,
    val connectionType: String = "DSL"
)

@Serializable
internal data class AddressRequestDto(
    val street: String,
    val number: String,
    val city: String,
    val zip: String,
    val country: String
)

// Response data classes
@Serializable
internal data class ApiResponse(
    val servusSpeed: List<InternetOffer.ServusSpeedOffer>? = null,
    val byteMe: List<InternetOffer.ByteMeOffer>? = null,
    val pingPerfect: List<InternetOffer.PingPerfectOffer>? = null,
    val verbynDich: List<InternetOffer.VerbynDichOffer>? = null,
    val webWunder: List<InternetOffer.WebWunderOffer>? = null
)

// HTTP Client configuration
private val httpClient = HttpClient(CIO) {
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })
    }
    install(HttpTimeout) {
        requestTimeoutMillis = 600_000  // 10 minutes
        connectTimeoutMillis = 30_000   // 30 seconds
        socketTimeoutMillis = 600_000   // 10 minutes
    }
}

enum class SortOption(val displayName: String) {
    PRICE_LOW_TO_HIGH("Price (Low to High)"),
    SPEED_HIGH_TO_LOW("Speed (High to Low)"),
    BEST_VALUE("Best Value (Speed/Price)"),
    NONE(displayName = "None")
}

@Composable
@Preview
fun App(deepLinkParams: Map<String, String>? = null) {
    MaterialTheme {
        var offers by remember { mutableStateOf<List<PresentableOffer>>(emptyList()) }
        var isLoading by remember { mutableStateOf(false) }
        var error by remember { mutableStateOf<String?>(null) }
        var searchHasHappened by remember { mutableStateOf(false) }
        var selectedConnectionType by remember { mutableStateOf<ConnectionType?>(null) }
        var selectedInstallationService by remember { mutableStateOf<Boolean?>(null) }
        var latestAddress by remember { mutableStateOf<Address?>(null) }
        var latestConnectionType by remember { mutableStateOf<ConnectionType?>(null) }
        var latestInstallationService by remember { mutableStateOf<Boolean?>(null) }
        var selectedSortOption by remember { mutableStateOf(SortOption.PRICE_LOW_TO_HIGH) }

        // Form state variables - initialize from deep link if available
        var street by remember { mutableStateOf(deepLinkParams?.get("street") ?: "") }
        var houseNumber by remember { mutableStateOf(deepLinkParams?.get("houseNumber") ?: "") }
        var city by remember { mutableStateOf(deepLinkParams?.get("city") ?: "") }
        var zip by remember { mutableStateOf(deepLinkParams?.get("zip") ?: "") }
        var selectedCountry by remember {
            mutableStateOf(
                deepLinkParams?.get("country")?.let { countryName ->
                    Country.entries.find { it.name == countryName }
                } ?: Country.AUSTRIA
            )
        }

        // Address state that will be used for searching
        var address by remember {
            mutableStateOf(
                Address(
                    street = "",
                    number = "",
                    city = "",
                    zip = "",
                    country = Country.AUSTRIA
                )
            )
        }

        // Coroutine scope for search function
        val coroutineScope = rememberCoroutineScope()

        // Sort offers whenever sorting option changes
        val sortedOffers = remember(offers, selectedSortOption) {
            when (selectedSortOption) {
                SortOption.NONE -> offers
                SortOption.PRICE_LOW_TO_HIGH -> offers.sortedBy { it.priceInfo.price }
                SortOption.SPEED_HIGH_TO_LOW -> offers.sortedByDescending { it.speed }
                SortOption.BEST_VALUE -> offers.sortedByDescending {
                    if (it.priceInfo.price > 0) it.speed.toDouble() / it.priceInfo.price else 0.0
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // address search form
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Search Address",
                            style = MaterialTheme.typography.titleMedium,
                        )

                        if (offers.isNotEmpty()) {
                            ShareButton(
                                street = latestAddress!!.street,
                                houseNumber = latestAddress!!.number,
                                city = latestAddress!!.city,
                                zip = latestAddress!!.zip,
                                country = latestAddress!!.country,
                                connectionType = latestConnectionType,
                                installationService = latestInstallationService,
                                offers = offers
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // street
                        OutlinedTextField(
                            value = street,
                            onValueChange = { street = it },
                            label = { Text("Street", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            modifier = Modifier.weight(0.4f).padding(vertical = 4.dp),
                            singleLine = true,
                        )

                        // house number
                        OutlinedTextField(
                            value = houseNumber,
                            onValueChange = { houseNumber = it },
                            label = { Text("House Number", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            modifier = Modifier.weight(0.15f).padding(vertical = 4.dp),
                            singleLine = true,
                        )

                        // city
                        OutlinedTextField(
                            value = city,
                            onValueChange = { city = it },
                            label = { Text("City", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            modifier = Modifier.weight(0.3f).padding(vertical = 4.dp),
                            singleLine = true,
                        )

                        // zip
                        OutlinedTextField(
                            value = zip,
                            onValueChange = { zip = it },
                            label = { Text("Zip Code", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            modifier = Modifier.weight(0.15f).padding(vertical = 4.dp),
                            singleLine = true,
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Country:",
                            modifier = Modifier.padding(end = 16.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Austria flag
                            CountryFlag(
                                flag = "🇦🇹",
                                isSelected = selectedCountry == Country.AUSTRIA,
                                onClick = { selectedCountry = Country.AUSTRIA }
                            )

                            // Germany flag
                            CountryFlag(
                                flag = "🇩🇪",
                                isSelected = selectedCountry == Country.GERMANY,
                                onClick = { selectedCountry = Country.GERMANY }
                            )

                            // Switzerland flag
                            CountryFlag(
                                flag = "🇨🇭",
                                isSelected = selectedCountry == Country.SWITZERLAND,
                                onClick = { selectedCountry = Country.SWITZERLAND }
                            )
                        }
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Filters (Optional)",
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            Row(modifier = Modifier.padding(bottom = 8.dp)) {
                                Text(
                                    text = "Connection:",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                Spacer(Modifier.width(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FilterChip(
                                        onClick = {
                                            selectedConnectionType =
                                                if (selectedConnectionType == ConnectionType.FIBER) null else ConnectionType.FIBER
                                        },
                                        label = { Text("Fiber", style = MaterialTheme.typography.bodySmall) },
                                        selected = selectedConnectionType == ConnectionType.FIBER,
                                    )

                                    FilterChip(
                                        onClick = {
                                            selectedConnectionType =
                                                if (selectedConnectionType == ConnectionType.DSL) null else ConnectionType.DSL
                                        },
                                        label = { Text("DSL", style = MaterialTheme.typography.bodySmall) },
                                        selected = selectedConnectionType == ConnectionType.DSL,
                                    )

                                    FilterChip(
                                        onClick = {
                                            selectedConnectionType =
                                                if (selectedConnectionType == ConnectionType.CABLE) null else ConnectionType.CABLE
                                        },
                                        label = { Text("Cable", style = MaterialTheme.typography.bodySmall) },
                                        selected = selectedConnectionType == ConnectionType.CABLE,
                                    )
                                    FilterChip(
                                        onClick = {
                                            selectedConnectionType =
                                                if (selectedConnectionType == ConnectionType.MOBILE) null else ConnectionType.MOBILE
                                        },
                                        label = { Text("Mobile", style = MaterialTheme.typography.bodySmall) },
                                        selected = selectedConnectionType == ConnectionType.MOBILE,
                                    )
                                }
                            }

                            Row {
                                Text(
                                    text = "Installation:",
                                    style = MaterialTheme.typography.bodySmall,
                                )

                                Spacer(Modifier.width(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FilterChip(
                                        onClick = {
                                            selectedInstallationService =
                                                if (selectedInstallationService == true) null else true
                                        },
                                        label = { Text("Included", style = MaterialTheme.typography.bodySmall) },
                                        selected = selectedInstallationService == true,
                                    )

                                    FilterChip(
                                        onClick = {
                                            selectedInstallationService =
                                                if (selectedInstallationService == false) null else false
                                        },
                                        label = { Text("Not Included", style = MaterialTheme.typography.bodySmall) },
                                        selected = selectedInstallationService == false,
                                    )
                                }
                            }
                        }
                    }

                    val isFormValid = street.isNotEmpty() && houseNumber.isNotEmpty() &&
                            city.isNotEmpty() && zip.isNotEmpty()

                    // Search button row with sorting dropdown
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Sort dropdown
                        var sortDropdownExpanded by remember { mutableStateOf(false) }

                        Box {
                            OutlinedButton(
                                onClick = { sortDropdownExpanded = true },
                                modifier = Modifier.height(40.dp)
                            ) {
                                Text(
                                    text = "Sort by: ${selectedSortOption.displayName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            DropdownMenu(
                                expanded = sortDropdownExpanded,
                                onDismissRequest = { sortDropdownExpanded = false }
                            ) {
                                SortOption.entries.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option.displayName) },
                                        onClick = {
                                            selectedSortOption = option
                                            sortDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = {
                                address = Address(
                                    street = street,
                                    number = houseNumber,
                                    city = city,
                                    zip = zip,
                                    country = selectedCountry
                                )
                                isLoading = true
                                error = null

                                coroutineScope.launch {
                                    kotlinx.coroutines.delay(200)
                                    try {
                                        offers = emptyList()
                                        offers =
                                            getAllOffers(address, selectedConnectionType, selectedInstallationService)
                                        latestAddress = address
                                        latestConnectionType = selectedConnectionType
                                        latestInstallationService = selectedInstallationService
                                        searchHasHappened = true
                                    } catch (e: Exception) {
                                        error = e.message
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            },
                            enabled = isFormValid
                        ) {
                            Text("Search")
                        }
                    }
                }
            }

            if (searchHasHappened && address.street.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Selected Address: ${address.street} ${address.number}, ${address.zip} ${address.city}, ${address.country.presentableName}",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Loading offers for ${address.street} ${address.number}...")
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("This might take a while")
                        }
                    }
                }

                error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Error: $error")
                    }
                }

                offers.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (searchHasHappened) {
                            Text("No offers found for this address")
                        } else {
                            Text("Let's find you the best Internet offers!")
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(sortedOffers) { offer ->
                            OfferCard(presentableOffer = offer)
                        }
                    }
                }
            }
        }
    }
}

private suspend fun getAllOffers(
    address: Address,
    connectionType: ConnectionType?,
    installationService: Boolean?
): List<PresentableOffer> {
    return try {
        val requestDto = OffersRequestDto(
            address = AddressRequestDto(
                street = address.street,
                number = address.number,
                city = address.city,
                zip = address.zip,
                country = address.country.name
            ),
            wantsFiber = connectionType == ConnectionType.FIBER,
            installation = installationService ?: false,
            connectionType = connectionType?.name ?: "DSL"
        )

        val response: ApiResponse = httpClient.post("https://gendevserver-production.up.railway.app/api/all/offers") {
            contentType(ContentType.Application.Json)
            setBody(requestDto)
        }.body()

        val allOffers = mutableListOf<InternetOffer>()

        response.servusSpeed?.let { allOffers.addAll(it) }
        response.byteMe?.let { allOffers.addAll(it) }
        response.pingPerfect?.let { allOffers.addAll(it) }
        response.verbynDich?.let { allOffers.addAll(it) }
        response.webWunder?.let { allOffers.addAll(it) }

        allOffers.map { turnToPresentableOffer(it) }

    } catch (e: Exception) {
        println("Error fetching offers: ${e.message}")
        e.printStackTrace()
        throw Exception("Failed to fetch offers: ${e.message}")
    }
}