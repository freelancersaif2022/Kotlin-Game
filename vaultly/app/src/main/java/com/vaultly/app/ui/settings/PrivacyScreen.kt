package com.vaultly.app.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.vaultly.app.BuildConfig
import com.vaultly.app.ui.home.RoundIcon
import com.vaultly.app.ui.theme.Palette
import com.vaultly.app.ui.theme.VaultlyTheme

private const val EFFECTIVE = "October 20, 2026"

private data class Section(val title: String, val body: List<String>)

private fun contact(): String = BuildConfig.SUPPORT_EMAIL.ifBlank { "the developer email shown on Vaultly's Google Play page" }

private val sections: List<Section> get() = listOf(
    Section(
        "1. Who we are",
        listOf(
            "Vaultly (“Vaultly”, “we”, “us”) is a mobile app that helps you keep receipts, track return windows and warranties, and create claim documents. This policy explains what information the app handles, why, and the choices you have.",
            "By using Vaultly you agree to this policy. If you do not agree, please do not use the app.",
        ),
    ),
    Section(
        "2. Information you store in Vaultly",
        listOf(
            "• Purchase details you enter or that are read from a receipt: item name, store, price, currency, purchase date, return window, warranty length, serial or order number, category and notes.",
            "• Images: receipt scans and proof photos you capture or choose.",
            "• App settings: reminder preferences, default currency, scanner mode and purchase status.",
            "This information is stored only in Vaultly's private storage on your device. We do not operate servers that receive your receipts, photos or purchase details, and we cannot see them.",
        ),
    ),
    Section(
        "3. Camera and photos",
        listOf(
            "The camera is used only while you are scanning a receipt. Live frames are analysed on your device to find the edges of the document and are never recorded or uploaded.",
            "When you choose an image from your photos, Vaultly receives only that image through Android's photo picker. Vaultly does not access the rest of your gallery.",
        ),
    ),
    Section(
        "4. Text recognition",
        listOf(
            "Text on receipts is read with Google ML Kit Text Recognition, which runs on your device through Google Play services. Receipt images are not sent to us for processing. Google may collect limited diagnostic and usage information about the ML Kit service as described in Google's terms.",
        ),
    ),
    Section(
        "5. Notifications",
        listOf(
            "If you allow notifications, Vaultly checks your deadlines on your device each day and shows reminders before a return window or warranty ends. No reminder data leaves your device.",
        ),
    ),
    Section(
        "6. Advertising",
        listOf(
            "The free version of Vaultly shows ads provided by Google AdMob. To serve and measure ads, Google may collect and process information such as your device's advertising ID, IP address, device model, operating system, app interactions with ads, and approximate location derived from the IP address.",
            "Where required by law (for example in the EEA, the UK and certain US states), Vaultly asks for your consent through Google's consent tool before personalised ads are shown. You can change your choice at any time in Settings › Ad privacy choices, and you can reset or delete your advertising ID in your Android settings.",
            "Ads are not shown to Vaultly Pro subscribers or users who bought Remove Ads.",
            "Learn more at policies.google.com/technologies/ads.",
        ),
    ),
    Section(
        "7. Purchases and subscriptions",
        listOf(
            "Payments for Vaultly Pro and Remove Ads are processed by Google Play. We never receive your card or payment details. Google Play shares with the app only what is needed to confirm your purchase, such as the product and a purchase token.",
            "Subscriptions renew automatically until cancelled. You can cancel at any time in Google Play › Payments & subscriptions › Subscriptions. Refunds are handled under Google Play's refund policy.",
        ),
    ),
    Section(
        "8. Sharing your information",
        listOf(
            "We do not sell your personal information and we do not share your receipts, photos or purchase details with anyone.",
            "A claim pack PDF is created only when you ask for it, and is shared only with the app or person you choose in Android's share sheet.",
            "Service providers described above (Google AdMob, Google Play, Google ML Kit) process data under their own privacy policies.",
        ),
    ),
    Section(
        "9. Backups",
        listOf(
            "If Android backup is turned on for your Google account, your vault (database, receipt images and settings) may be included in your device's encrypted backup to Google and restored when you set up a new phone. You control this in your Android settings.",
        ),
    ),
    Section(
        "10. Data retention and deletion",
        listOf(
            "Your data stays on your device until you delete it. Deleting a purchase in Vaultly removes its details, receipt image, photos and reminders. Uninstalling Vaultly or clearing its storage deletes all of your Vaultly data from the device.",
        ),
    ),
    Section(
        "11. Security",
        listOf(
            "Your vault is kept in app-private storage that other apps cannot read. Exported claim packs are shared through a temporary secure link that only the app you choose can open. No method of storage is completely secure, so please keep your device protected with a screen lock.",
        ),
    ),
    Section(
        "12. Children",
        listOf(
            "Vaultly is not directed to children under 13 (or the minimum age in your country), and we do not knowingly collect personal information from children.",
        ),
    ),
    Section(
        "13. Your rights",
        listOf(
            "Because your vault stays on your device, you can view, correct, export (as a claim pack) or delete your information directly in the app at any time.",
            "Depending on where you live (for example under the GDPR, UK GDPR or US state privacy laws), you may have rights to access, correct, delete or restrict the processing of personal information, to object to processing and to withdraw consent. For advertising data handled by Google, you can use the Ad privacy choices option and Google's own tools.",
        ),
    ),
    Section(
        "14. Changes to this policy",
        listOf(
            "We may update this policy from time to time. The new version will appear in the app with a new effective date. Significant changes will be highlighted in the app or on the Google Play listing.",
        ),
    ),
    Section(
        "15. Contact",
        listOf("Questions or requests about privacy? Contact us at ${contact()}."),
    ),
)

@Composable
fun PrivacyScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundIcon(Icons.AutoMirrored.Outlined.ArrowBack, "Back", onBack)
            Spacer(Modifier.width(14.dp))
            Text("Privacy policy", style = MaterialTheme.typography.headlineSmall)
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Summary card
            val shape = RoundedCornerShape(24.dp)
            Column(
                Modifier.fillMaxWidth().clip(shape).background(VaultlyTheme.colors.inkBrush)
                    .border(1.dp, Palette.Gold.copy(alpha = 0.3f), shape).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("The short version", style = MaterialTheme.typography.titleLarge, color = androidx.compose.ui.graphics.Color.White)
                Summary(Icons.Outlined.PhoneAndroid, "Your receipts, photos and purchase details stay on your phone.")
                Summary(Icons.Outlined.CloudOff, "We run no servers that collect your vault, and we never sell data.")
                Summary(Icons.Outlined.Lock, "Ads (free version) come from Google AdMob, with your consent where required.")
            }
            Text("Effective $EFFECTIVE", style = MaterialTheme.typography.labelMedium, color = VaultlyTheme.colors.muted)
            sections.forEach { section ->
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(VaultlyTheme.colors.card)
                        .border(1.dp, VaultlyTheme.colors.hairline, RoundedCornerShape(20.dp)).padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(section.title, style = MaterialTheme.typography.titleMedium)
                    section.body.forEach {
                        Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (BuildConfig.SUPPORT_EMAIL.isNotBlank()) {
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(VaultlyTheme.colors.gold.copy(alpha = 0.12f))
                        .clickable {
                            context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${BuildConfig.SUPPORT_EMAIL}")))
                        }
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) { Text("Email ${BuildConfig.SUPPORT_EMAIL}", style = MaterialTheme.typography.labelLarge, color = VaultlyTheme.colors.gold) }
            }
            Spacer(Modifier.navigationBarsPadding().height(24.dp))
        }
    }
}

@Composable
private fun Summary(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(34.dp).clip(CircleShape).background(Palette.GoldLight.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, Modifier.size(18.dp), tint = Palette.GoldLight)
        }
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.85f))
    }
}
