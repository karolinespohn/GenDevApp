package org.gendev25.project

import java.io.File

internal fun generateOffersHtml(
    street: String,
    houseNumber: String,
    city: String,
    zip: String,
    country: Country,
    connectionType: ConnectionType?,
    installationService: Boolean?,
    offers: List<PresentableOffer>
): String {
    val address = "$street $houseNumber, $zip $city, ${country.presentableName}"
    val filters = buildString {
        if (connectionType != null) append("Connection: ${connectionType.name} • ")
        if (installationService != null) append("Installation: ${if (installationService) "Included" else "Not Included"}")
    }

    return """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Internet Offers for $address</title>
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }
        
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            line-height: 1.6;
            color: #333;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            min-height: 100vh;
            padding: 20px;
        }
        
        .container {
            max-width: 1200px;
            margin: 0 auto;
            background: white;
            border-radius: 16px;
            box-shadow: 0 20px 40px rgba(0,0,0,0.1);
            overflow: hidden;
        }
        
        .header {
            background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
            color: white;
            padding: 40px;
            text-align: center;
        }
        
        .header h1 {
            font-size: 2.5rem;
            margin-bottom: 10px;
            font-weight: 700;
        }
        
        .address {
            font-size: 1.2rem;
            opacity: 0.9;
            margin-bottom: 10px;
        }
        
        .filters {
            font-size: 1rem;
            opacity: 0.8;
            background: rgba(255,255,255,0.1);
            padding: 8px 16px;
            border-radius: 8px;
            display: inline-block;
        }
        
        .content {
            padding: 40px;
        }
        
        .offers-grid {
            display: grid;
            grid-template-columns: repeat(auto-fill, minmax(400px, 1fr));
            gap: 24px;
        }
        
        .offer-card {
            border: 1px solid #e2e8f0;
            border-radius: 12px;
            padding: 24px;
            background: #f8fafc;
            transition: all 0.3s ease;
            position: relative;
            overflow: hidden;
        }
        
        .offer-card:hover {
            transform: translateY(-4px);
            box-shadow: 0 12px 24px rgba(0,0,0,0.1);
            border-color: #6366f1;
        }
        
        .company-badge {
            position: absolute;
            top: 0;
            right: 0;
            background: #6366f1;
            color: white;
            padding: 8px 16px;
            border-bottom-left-radius: 8px;
            font-size: 0.8rem;
            font-weight: 600;
            text-transform: uppercase;
        }
        
        .offer-name {
            font-size: 1.4rem;
            font-weight: 700;
            color: #1e293b;
            margin-bottom: 16px;
            padding-right: 100px;
        }
        
        .speed-connection {
            display: flex;
            align-items: center;
            gap: 12px;
            margin-bottom: 16px;
        }
        
        .speed {
            background: linear-gradient(135deg, #10b981 0%, #059669 100%);
            color: white;
            padding: 8px 16px;
            border-radius: 8px;
            font-weight: 600;
            font-size: 1.1rem;
        }
        
        .connection-type {
            background: #e0e7ff;
            color: #3730a3;
            padding: 6px 12px;
            border-radius: 6px;
            font-weight: 500;
            text-transform: uppercase;
            font-size: 0.9rem;
        }
        
        .price-section {
            background: white;
            border-radius: 8px;
            padding: 16px;
            margin: 16px 0;
            border-left: 4px solid #6366f1;
        }
        
        .main-price {
            font-size: 2rem;
            font-weight: 700;
            color: #1e293b;
        }
        
        .price-after {
            color: #64748b;
            font-size: 0.9rem;
            margin-top: 4px;
        }
        
        .details-grid {
            display: grid;
            grid-template-columns: repeat(2, 1fr);
            gap: 12px;
            margin-top: 16px;
        }
        
        .detail-item {
            background: white;
            padding: 12px;
            border-radius: 6px;
            border: 1px solid #e2e8f0;
        }
        
        .detail-label {
            font-size: 0.8rem;
            color: #64748b;
            text-transform: uppercase;
            font-weight: 600;
            letter-spacing: 0.5px;
        }
        
        .detail-value {
            font-weight: 600;
            color: #1e293b;
            margin-top: 2px;
        }
        
        .discount-badge {
            background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
            color: white;
            padding: 8px 12px;
            border-radius: 6px;
            font-weight: 600;
            margin-top: 12px;
            display: inline-block;
        }
        
        .footer {
            background: #f1f5f9;
            padding: 20px;
            text-align: center;
            color: #64748b;
            font-size: 0.9rem;
        }
        
        @media (max-width: 768px) {
            .offers-grid {
                grid-template-columns: 1fr;
            }
            
            .header h1 {
                font-size: 2rem;
            }
            
            .content {
                padding: 20px;
            }
            
            .details-grid {
                grid-template-columns: 1fr;
            }
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>🌐 Internet Offers</h1>
            <div class="address">📍 $address</div>
            ${if (filters.isNotEmpty()) "<div class=\"filters\">🔍 $filters</div>" else ""}
        </div>
        
        <div class="content">
            <div class="offers-grid">
                ${offers.joinToString("") { offer -> generateOfferCard(offer) }}
            </div>
        </div>
        
        <div class="footer">
            Generated on ${
        java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' HH:mm"))
    } • 
            Found ${offers.size} offers
        </div>
    </div>
</body>
</html>
    """.trimIndent()
}

internal fun generateOfferCard(offer: PresentableOffer): String {
    return """
        <div class="offer-card">
            <div class="company-badge">${offer.nameInfo.company.name}</div>
            <div class="offer-name">${offer.nameInfo.offerName}</div>
            
            <div class="speed-connection">
                <div class="speed">${offer.speed} Mbps</div>
                <div class="connection-type">${offer.connectionType.name}</div>
            </div>
            
            <div class="price-section">
                <div class="main-price">€${String.format("%.2f", offer.priceInfo.price)}/month</div>
                ${
        offer.priceInfo.monthlyPriceAfter2Years?.let {
            "<div class=\"price-after\">After 24 months: €${String.format("%.2f", it)}/month</div>"
        } ?: ""
    }
            </div>
            
            <div class="details-grid">
                ${
        offer.durationInMonths?.let {
            "<div class=\"detail-item\"><div class=\"detail-label\">Contract</div><div class=\"detail-value\">$it months</div></div>"
        } ?: ""
    }
                
                ${
        offer.installationService?.let {
            "<div class=\"detail-item\"><div class=\"detail-label\">Installation</div><div class=\"detail-value\">${if (it) "✅ Included" else "❌ Not included"}</div></div>"
        } ?: ""
    }
                
                ${
        offer.tv?.let {
            "<div class=\"detail-item\"><div class=\"detail-label\">TV Package</div><div class=\"detail-value\">$it</div></div>"
        } ?: ""
    }
                
                ${
        offer.disclaimerInfo?.limitFrom?.let {
            "<div class=\"detail-item\"><div class=\"detail-label\">Limit From</div><div class=\"detail-value\">$it GB</div></div>"
        } ?: ""
    }
                
                ${
        offer.disclaimerInfo?.maxAge?.let {
            "<div class=\"detail-item\"><div class=\"detail-label\">Max Age</div><div class=\"detail-value\">$it years</div></div>"
        } ?: ""
    }
            </div>
            
            ${
        offer.discountInfo?.let { discount ->
            val discountText = when {
                discount.absoluteDiscount != null -> "Save €${String.format("%.2f", discount.absoluteDiscount)}"
                discount.relativeDiscount != null -> "Save ${discount.relativeDiscount}%"
                else -> "Discount available"
            }
            "<div class=\"discount-badge\">🎉 $discountText</div>"
        } ?: ""
    }
        </div>
    """
}

fun saveHtmlFile(htmlContent: String, street: String, houseNumber: String, city: String) {
    try {
        val downloadsDir = File(System.getProperty("user.home"), "Downloads")
        val fileName = "internet-offers-${street.replace(" ", "-")}-$houseNumber-${city.replace(" ", "-")}.html"
        val file = File(downloadsDir, fileName)

        file.writeText(htmlContent, Charsets.UTF_8)
        println("HTML file saved to: ${file.absolutePath}")

        if (java.awt.Desktop.isDesktopSupported()) {
            java.awt.Desktop.getDesktop().browse(file.toURI())
        }
    } catch (e: Exception) {
        println("Failed to save HTML file: ${e.message}")
    }
}
