package org.gendev25.project.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import org.gendev25.project.ConnectionType
import org.gendev25.project.Country
import org.gendev25.project.PresentableOffer
import org.gendev25.project.generateOffersHtml
import org.gendev25.project.saveHtmlFile

@Composable
internal fun ShareButton(
    street: String,
    houseNumber: String,
    city: String,
    zip: String,
    country: Country,
    connectionType: ConnectionType?,
    installationService: Boolean?,
    offers: List<PresentableOffer>
) {
    var showCopiedMessage by remember { mutableStateOf(false) }

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            onClick = {
                val htmlContent = generateOffersHtml(
                    street, houseNumber, city, zip, country,
                    connectionType, installationService, offers
                )
                saveHtmlFile(htmlContent, street, houseNumber, city)
                showCopiedMessage = true
            }
        ) {
            Text("Share")
        }

        if (showCopiedMessage) {
            Text(
                text = "HTML saved to Downloads!",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
            LaunchedEffect(showCopiedMessage) {
                kotlinx.coroutines.delay(3000)
                showCopiedMessage = false
            }
        }
    }
}
