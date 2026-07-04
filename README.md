# ⚡ NetUP

Mobile ගේමර්ලා වෙනුවෙන් නිර්මාණය කරන ලද, **Android-only**, **open-source**, network toolkit
එකක්. Kotlin + Jetpack Compose වලින් 100% native විදියට හදලා තියෙනවා.

## 📦 මොනවද තියෙන්නේ

| Feature | විස්තරය |
|---|---|
| 📡 **Ping Monitor** | TCP-connect based real-time latency test (min/avg/max/jitter/packet-loss) |
| 🚀 **Network Booster** | Local VPN tunnel එකකින් priority game එකට direct route + background app throttling |
| 🌐 **DNS Switcher** | Cloudflare, Google, Quad9, OpenDNS, AdGuard අතරින් වේගවත්ම DNS server එක speed-test කරලා තෝරගන්න |
| 🎮 **Game Mode** | Do Not Disturb, Keep-Screen-On, background app cleanup |
| 📶 **Signal Analyzer** | Wi-Fi (RSSI/link speed/band) + Mobile Data (network type/signal level) සජීවී පරීක්ෂාව |

## ⚠️ තාක්ෂණික අවංක සටහනක් (Important — please read)

Android එකක root නැතුව (root නොකළ device එකක) **"real" ICMP ping එක physically අඩු කරන්න
කිසිම app එකකට බැහැ** — ඒක network path එකේ physical distance/routing එකෙන් තීරණය වෙන දෙයක්.
කිසිම legit tool එකකට (paid හෝ free) මේ limitation එක මගහරින්න බැහැ.

මේ app එක ඇත්තටම කරන, **සත්‍ය වටිනාකමක් ඇති** දේවල්:

1. **Faster DNS resolution** — connection setup වේගවත් කරයි (real, measurable)
2. **Priority routing** — VPN tunnel එකෙන් game app එක bypass කරලා direct route දෙනවා, අනිත්
   apps tunnel එකෙන් throttle කරනවා (real bandwidth prioritization)
3. **Background app cleanup** — RAM/CPU/bandwidth අල්ලාගෙන ඉන්න apps clear කරනවා
4. **DND + signal diagnostics** — distraction-free environment + connection quality visibility

මේ සියල්ලම එකතුවෙලා **perceived lag/jitter** සැලකිය යුතු ලෙස අඩු කරන්න පුළුවන් - ඒත් "0 ping"
වගේ දේවල් promise කරන Play Store apps බොහොමයක් marketing hype විතරයි කියලා දැනගෙන ඉන්න.

## 🛠️ Build කරන විදිය

1. [Android Studio](https://developer.android.com/studio) install කරගන්න (Hedgehog හෝ ඊට අලුත්)
2. මේ folder එක Android Studio එකෙන් **Open** කරන්න (`File > Open`)
3. Gradle sync එක automatic වෙනකම් intewait කරන්න (පළමු වතාවේදී dependencies download වෙනවා -
   internet connection එකක් අවශ්‍යයි)
4. USB/Wireless debugging සක්‍රීය කරපු Android device එකක් හෝ emulator එකක් connect කරන්න
5. `Run ▶` click කරන්න

> Gradle wrapper jar file එක මේ repo එකේ නෑ (binary file එකක් නිසා). Android Studio එකෙන් open
> කරාම එය automatic ලෙස generate කරගන්නවා. Command line එකෙන් build කරනවානම් කලින්
> `gradle wrapper` run කරන්න.

## 🔐 App එකට අවශ්‍ය Permissions

- **VPN permission** — Network Booster/DNS Switcher tunnel එකට (local only, no remote server)
- **Location (Fine)** — Android requirement එකක් Wi-Fi SSID/RSSI විස්තර බලාගන්න
- **Usage Access** — background running apps list කරගන්න (Game Mode cleanup සඳහා)
- **Notification Policy Access** — Do Not Disturb toggle කරන්න
- **Phone State** — cellular signal/network type බලාගන්න

සියලුම permissions app එක ඇතුලේම (in-app) request කරනවා, පළමු වතාවේ පාවිච්චි කරන feature එකට
navigate කරාම.

## 📁 Project Structure

```
app/src/main/java/com/pingoptimizer/pro/
├── MainActivity.kt              # Navigation shell (bottom nav + NavHost)
├── PingOptimizerApp.kt          # Application class, notification channels
├── data/
│   ├── Models.kt                 # Ping targets, DNS providers
│   └── Prefs.kt                  # DataStore-backed persisted settings
├── network/
│   └── PingUtils.kt              # TCP-connect latency measurement
├── service/
│   └── GameBoosterVpnService.kt  # Local VPN tunnel (DNS override + priority routing)
├── utils/
│   ├── AppListUtils.kt           # Installed-app / game detection
│   ├── GameModeManager.kt        # DND + background app cleanup
│   └── NetworkSignalUtils.kt     # Wi-Fi / cellular signal readers
└── ui/
    ├── theme/                    # Compose Material3 dark gaming theme
    └── screens/                  # Dashboard, Ping, Booster, DNS, GameMode, Signal
```

## 🚧 Roadmap idea (future contributions welcome)

- [ ] Full IP-packet level NAT/routing in the VPN tunnel (currently a simplified passthrough)
- [ ] Per-app real-time bandwidth graphs (`NetworkStatsManager`)
- [ ] Widget/quick-settings tile to toggle Game Mode from anywhere
- [ ] Community-maintained list of popular game "status/ping-check" endpoints
- [ ] Localization: full Sinhala + Tamil + English string resources

---

Made with ❤️ for Sri Lankan mobile gamers.
