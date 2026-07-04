package com.pingoptimizer.pro.data

/**
 * A network target used for latency testing. Since raw ICMP ping requires root on
 * Android, we measure latency via TCP connect time to a known open port instead -
 * this closely mirrors the "connection setup" delay a game would experience.
 */
data class PingTarget(
    val label: String,
    val host: String,
    val port: Int = 443
)

/**
 * A curated set of general-purpose, publicly reachable test endpoints spread across
 * regions/providers. These are NOT specific game-company servers (those change often
 * and are not publicly documented) - they are reliable anycast/CDN endpoints that give
 * a good real-world estimate of your route quality to nearby data centers.
 *
 * Users can also add any custom host (e.g. a game's own status-page host) manually.
 */
object DefaultTargets {
    // These are real, well-known, always-on anycast endpoints. Because they're anycast,
    // your device automatically connects to the nearest physical point-of-presence -
    // which makes them a genuinely useful proxy for "how good is my route to a nearby
    // data center right now". Users can add any custom host/IP as well (e.g. a specific
    // game server IP they know, or their home router).
    val list = listOf(
        PingTarget("Cloudflare (1.1.1.1)", "1.1.1.1", 443),
        PingTarget("Google DNS (8.8.8.8)", "8.8.8.8", 443),
        PingTarget("Quad9 (9.9.9.9)", "9.9.9.9", 443),
        PingTarget("OpenDNS (208.67.222.222)", "208.67.222.222", 443),
        PingTarget("Cloudflare CDN (cloudflare.com)", "cloudflare.com", 443),
        PingTarget("Google (google.com)", "google.com", 443)
    )
}

data class DnsProvider(
    val name: String,
    val primary: String,
    val secondary: String,
    val description: String
)

object DnsProviders {
    val list = listOf(
        DnsProvider("Cloudflare", "1.1.1.1", "1.0.0.1", "ලෝකයේ වේගවත්ම DNS අතරින් එකක්"),
        DnsProvider("Google Public DNS", "8.8.8.8", "8.8.4.4", "ස්ථාවර සහ විශ්වාසදායක"),
        DnsProvider("Quad9", "9.9.9.9", "149.112.112.112", "Malware-blocking DNS"),
        DnsProvider("OpenDNS", "208.67.222.222", "208.67.220.220", "Family-safe filtering සමඟ"),
        DnsProvider("AdGuard DNS", "94.140.14.14", "94.140.15.15", "Ad-block built-in")
    )
}

data class AppNetInfo(
    val packageName: String,
    val appName: String,
    val isForeground: Boolean,
    val rxBytes: Long,
    val txBytes: Long
)
