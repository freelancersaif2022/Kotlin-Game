package com.tvremote.universal.data

/** Step-by-step help shown on the pairing screen and in Help. */
data class PlatformGuide(
    val pairing: List<String>,
    val powerOn: List<String>,
    val troubleshooting: List<String>,
    val findIp: String,
)

object Guides {

    private const val SAME_WIFI = "Turn on your TV and connect it to the same Wi-Fi network as this phone."

    fun of(platform: TvPlatform): PlatformGuide = when (platform) {
        TvPlatform.ANDROID_TV -> PlatformGuide(
            pairing = listOf(
                SAME_WIFI,
                "Select your TV in the list (or enter its IP address).",
                "A 6-character code (letters A–F and numbers) appears on your TV screen.",
                "Type the code in the app and tap Pair. Done — the code is only needed once.",
            ),
            powerOn = listOf(
                "On the TV open Settings › System › Power & energy (or Settings › Network & Internet).",
                "Turn on \"Network standby\", \"Remote start\", \"Wake on Wi-Fi\" or \"Quick start\" (name depends on brand).",
                "In this app open the TV's settings (⋮ on the remote screen) and enter the TV's MAC address from Settings › Network › About / Status.",
            ),
            troubleshooting = listOf(
                "No code on the TV? Update \"Android TV Remote Service\" in the Play Store on the TV, then restart the TV.",
                "Still nothing? On the TV: Settings › Apps › See all apps › Show system apps › Android TV Remote Service › Clear data.",
                "Make sure the phone is not on a guest Wi-Fi and the router has no \"AP / client isolation\".",
            ),
            findIp = "Settings › Network & Internet › your Wi-Fi network (or Settings › About › Status).",
        )
        TvPlatform.SAMSUNG -> PlatformGuide(
            pairing = listOf(
                SAME_WIFI,
                "Select your Samsung TV in the list.",
                "A popup appears on the TV asking to allow \"Universal Remote\".",
                "Select \"Allow\" with your TV remote within 30 seconds. Done!",
            ),
            powerOn = listOf(
                "On the TV open Settings › General › Network › Expert Settings.",
                "Turn on \"Power On with Mobile\" (2016–2017 models: \"Samsung Smart View / Mobile TV On\").",
                "The app saves your TV's MAC address automatically while it's on — then Power works even when the TV is off.",
            ),
            troubleshooting = listOf(
                "Pressed \"Deny\" by mistake? Settings › General › External Device Manager › Device Connection Manager › Device List — allow \"Universal Remote\".",
                "Set \"Access Notification\" to \"First Time Only\" in Device Connection Manager.",
                "Make sure \"IP Remote\" is on (Settings › General › Network › Expert Settings).",
            ),
            findIp = "Settings › General › Network › Network Status › IP Settings.",
        )
        TvPlatform.LG -> PlatformGuide(
            pairing = listOf(
                SAME_WIFI,
                "Select your LG TV in the list.",
                "A popup appears on the TV asking to accept the connection.",
                "Select \"Accept\" with your TV remote. Done!",
            ),
            powerOn = listOf(
                "On the TV open Settings › All Settings › General › Devices › External Devices (or Connection) › \"Mobile TV On\".",
                "Turn on \"Turn on via Wi-Fi\" (older models: Settings › Network › \"LG Connect Apps\" ON).",
                "The app saves your TV's MAC address automatically while it's on.",
            ),
            troubleshooting = listOf(
                "No popup? Turn on Settings › Network › \"LG Connect Apps\" (or \"Mobile TV On\").",
                "Restart the TV by unplugging it for 30 seconds, then try again.",
            ),
            findIp = "Settings › All Settings › Network › Wi-Fi Connection › Advanced Wi-Fi Settings.",
        )
        TvPlatform.ROKU -> PlatformGuide(
            pairing = listOf(
                SAME_WIFI,
                "Select your Roku in the list.",
                "No code needed — Roku connects instantly.",
            ),
            powerOn = listOf(
                "Roku TVs: Settings › System › Power › turn on \"Fast TV start\".",
                "Roku sticks/boxes turn the TV on through HDMI-CEC: Settings › System › Control other devices (CEC) › \"1-touch play\".",
            ),
            troubleshooting = listOf(
                "Can't control? Settings › System › Advanced system settings › Control by mobile apps › Network access › \"Default\" or \"Permissive\".",
            ),
            findIp = "Settings › Network › About.",
        )
        TvPlatform.VIZIO -> PlatformGuide(
            pairing = listOf(
                SAME_WIFI,
                "Select your Vizio TV in the list.",
                "A 4-digit PIN appears on your TV screen.",
                "Type the PIN in the app and tap Pair. Done!",
            ),
            powerOn = listOf(
                "On the TV open Settings › System › Power Mode and choose \"Quick Start\".",
            ),
            troubleshooting = listOf(
                "No PIN? Make sure the TV is fully on (not in standby) and SmartCast is up to date (Settings › System › Check for updates).",
            ),
            findIp = "Settings › Network › Network Test (or Network Connection Status).",
        )
        TvPlatform.FIRE_TV -> PlatformGuide(
            pairing = listOf(
                "On the Fire TV open Settings › My Fire TV › About and click your device name 7 times until it says you're a developer.",
                "Go back to Settings › My Fire TV › Developer options and turn on \"ADB debugging\".",
                "Make sure the Fire TV and this phone are on the same Wi-Fi.",
                "Tap Connect. On the TV popup \"Allow USB debugging?\" check \"Always allow\" and select OK.",
            ),
            powerOn = listOf(
                "Fire TV wakes from sleep with the Power button while it's connected.",
                "For TVs controlled by a Fire TV Stick, enable HDMI-CEC on the TV (Anynet+, SimpLink, Bravia Sync…).",
            ),
            troubleshooting = listOf(
                "Can't connect? Turn \"ADB debugging\" off and on again, then retry.",
                "Didn't see the popup? Restart the Fire TV and try again — the popup only appears while the TV is awake.",
            ),
            findIp = "Settings › My Fire TV › About › Network.",
        )
    }

    /** Brand → platforms it ships with, for the manual "Choose your TV" flow. */
    data class Brand(val name: String, val platforms: List<TvPlatform>, val note: String? = null)

    val brands = listOf(
        Brand("Samsung", listOf(TvPlatform.SAMSUNG)),
        Brand("LG", listOf(TvPlatform.LG)),
        Brand("Sony", listOf(TvPlatform.ANDROID_TV)),
        Brand("TCL", listOf(TvPlatform.ANDROID_TV, TvPlatform.ROKU)),
        Brand("Hisense", listOf(TvPlatform.ANDROID_TV, TvPlatform.ROKU), note = "Hisense VIDAA TVs are not supported."),
        Brand("Philips", listOf(TvPlatform.ANDROID_TV)),
        Brand("Xiaomi", listOf(TvPlatform.ANDROID_TV)),
        Brand("Vizio", listOf(TvPlatform.VIZIO)),
        Brand("Roku", listOf(TvPlatform.ROKU)),
        Brand("Amazon Fire TV", listOf(TvPlatform.FIRE_TV)),
        Brand("Chromecast / Google TV", listOf(TvPlatform.ANDROID_TV)),
        Brand("Nvidia Shield", listOf(TvPlatform.ANDROID_TV)),
        Brand("Sharp", listOf(TvPlatform.ANDROID_TV, TvPlatform.ROKU)),
        Brand("Toshiba", listOf(TvPlatform.FIRE_TV, TvPlatform.ANDROID_TV)),
        Brand("Insignia", listOf(TvPlatform.FIRE_TV, TvPlatform.ROKU)),
        Brand("Panasonic", listOf(TvPlatform.ANDROID_TV, TvPlatform.FIRE_TV), note = "Older Panasonic \"My Home Screen\" TVs are not supported."),
        Brand("Onn / Walmart", listOf(TvPlatform.ROKU, TvPlatform.ANDROID_TV)),
        Brand("Skyworth / Other Android TV", listOf(TvPlatform.ANDROID_TV)),
    )

    fun platformHint(platform: TvPlatform): String = when (platform) {
        TvPlatform.ANDROID_TV -> "Home screen says Google TV or Android TV"
        TvPlatform.ROKU -> "Purple Roku home screen"
        TvPlatform.FIRE_TV -> "Amazon Fire TV home screen"
        TvPlatform.SAMSUNG -> "Samsung Smart Hub"
        TvPlatform.LG -> "LG webOS home bar"
        TvPlatform.VIZIO -> "Vizio SmartCast home"
    }
}
