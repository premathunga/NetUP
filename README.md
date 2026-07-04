# ⚡ NetUP

Mobile ගේමර්ලා වෙනුවෙන් නිර්මාණය කරන ලද, **Android-only**, **open-source**, network toolkit
එකක්. Kotlin + Jetpack Compose වලින් 100% native විදියට හදලා තියෙනවා.

## 🆕 මේ update එකේ මොනවද වෙනස් වුනේ

**Fixed (integrity issues):**
- ❌➡️✅ "Advanced Diagnostics" screen එකේ තිබ්බ **fully scripted/fake ping log එක** (hardcoded
  IP, hardcoded "success" message) සම්පූර්ණයෙන් අයින් කරලා, **real multi-target diagnostic**
  එකකින් replace කළා (`PingUtils.runDiagnostic`) - දැන් loss/timeout තිබුනොත් ඒක අවංකවම පෙන්නනවා.
- ❌➡️✅ Radar card එකේ hardcoded "0% loss / 2ms jitter / Excellent / 5G-WiFi6" text - දැන් real
  signal + ping data වලින් ගණනය කරනවා.
- ❌➡️✅ "Performance Profile" (POWER SAVE/BALANCED/EXTREME) buttons - කලින් state එකක් විතරක්
  මාරු කළේ, දැන් screen brightness/keep-awake/sustained-performance-mode/DND වලට real effect
  එකක් දෙනවා.
- ❌➡️✅ "Thermal Sync" switch - කලින් කිසිම API එකක් call කළේ නෑ, දැන් `PowerManager`
  thermal-status listener එකෙන් (Android 10+) device එකේ **real** thermal state එක පෙන්නනවා.
- ❌➡️✅ "FPS Monitor" card - තුන්වන පාර්ශවීය app එකකට root නැතුව අනිත් app එකක **සත්‍ය FPS** එක
  කියවන්න බැහැ (ඒකත් fake claim එකක් වෙනවා), ඒක නිසා genuinely measurable දේවලට (**Ping, RAM%,
  Battery temp**) පදනම් වූ **Performance HUD** floating overlay එකකින් replace කළා.
- ❌➡️✅ "Crosshair" card - දැන් සත්‍යවශයෙන්ම screen එකේ center එකේ click-through crosshair
  overlay එකක් ඇද්දලා පෙන්නනවා.
- Code duplication - screens 3ක copy-paste වෙලා තිබ්බ `Runtime.exec("ping...")` logic එක
  `PingUtils.smartPing()` කියලා එකම තැනකට centralize කළා (real ICMP via shell + TCP-connect
  fallback, both honest measurements).

**New: Permanent background app cleanup (Shizuku + Accessibility)**

ඔයා මුලින් කිව්ව "apps තත්පර 5කින් ආයෙත් on වෙනවා" ප්‍රශ්නයට තමයි මේක විසඳුම:

| ක්‍රමය | විස්තරය | Setup |
|---|---|---|
| ⚡ **Shizuku (Ultimate)** | `adb shell` level privilege එකෙන් `forceStopPackage()` කෙලින්ම call කරනවා - `am force-stop` command එකම. Permanent, root අවශ්‍ය නෑ. | [Shizuku app](https://shizuku.rikka.app) install කරලා, wireless-debugging pairing එකෙන් start කරලා, NetUP එකට permission දෙන්න |
| 🛡️ **Accessibility (fallback)** | Real Settings "Force stop" button එකම auto-tap කරනවා (Greenify approach එකමයි) | Settings > Accessibility > NetUP on කරන්න |
| ⚠️ **Basic (last resort)** | `killBackgroundProcesses()` - OS එකට suggestion එකක් විතරයි | Setup එකක් නෑ, ඒත් weak |

Game Booster screen එකේ "RAM Overclock" card එකේ දැන් **කුමන method එකද actually run වුනේ**
කියලා පැහැදිලිව පෙන්නනවා (fake "SUCCESS" message එකක් නෙවෙයි).

## 📦 මොනවද තියෙන්නේ

| Feature | විස්තරය |
|---|---|
| 🏠 **Home** | Live ping graph (real ICMP + TCP fallback), RAM usage, quick booster toggle |
| 📡 **Network Optimizer** | Real multi-target diagnostic, gaming DNS switcher, live signal radar |
| 🚀 **Game Booster** | Local VPN tunnel priority routing + tiered real background cleanup |
| 🛠️ **Tools** | DND, Keep-Screen-On, Brightness Lock, Immersive Mode, Performance Profiles, real Thermal status, Performance HUD overlay, Crosshair overlay |
| ⚙️ **Settings** | App preferences |

## ⚠️ තාක්ෂණික අවංක සටහනක් (Important — please read)

Android එකක root නැතුව **"real" ping physically අඩු කරන්න කිසිම app එකකට බැහැ** - ඒක network
path එකේ physical distance/routing එකෙන් තීරණය වෙන දෙයක්. මේ app එකේ ඇත්තටම වටිනා දේවල්:

1. **Real ICMP ping** (`/system/bin/ping` shell-out, TCP-connect fallback) - කිසිම දෙයක් fabricate කරන්නේ නෑ
2. **Faster DNS resolution** — connection setup වේගවත් කරයි
3. **Priority routing + genuine, permanent background app cleanup** (Shizuku/Accessibility)
4. **Real signal/thermal diagnostics** — device එකේ ඇත්තටම මොකද වෙන්නේ කියලා පෙන්නනවා

## 🛠️ Build කරන විදිය

1. [Android Studio](https://developer.android.com/studio) install කරගන්න (Hedgehog හෝ ඊට අලුත්)
2. මේ folder එක Android Studio එකෙන් **Open** කරන්න (`File > Open`)
3. Gradle sync එක automatic වෙනකම් ඉන්න (dependencies download වෙනවා - Shizuku-API එකත් ඇතුළුව)
4. Device/emulator එකක Run කරන්න

## 🔐 App එකට අවශ්‍ය Permissions

- **VPN permission** — Network Booster/DNS tunnel එකට (local only)
- **Location (Fine)** — Wi-Fi SSID/RSSI විස්තර
- **Usage Access** — background apps list කරගන්න
- **Notification Policy Access** — Do Not Disturb
- **Phone State** — cellular signal/network type
- **Display over other apps (SYSTEM_ALERT_WINDOW)** — Performance HUD / Crosshair overlays
- **Accessibility Service** (optional) — permanent background-app force-stop fallback
- **Shizuku** (optional, separate app) — permanent background-app force-stop (recommended method)

## 📁 Project Structure

```
app/src/main/java/com/pingoptimizer/pro/
├── MainActivity.kt
├── PingOptimizerApp.kt
├── data/                  # Models, DataStore prefs
├── network/
│   └── PingUtils.kt       # ONE real ping implementation, used everywhere
├── service/
│   └── GameBoosterVpnService.kt
├── shizuku/               # Shizuku "Ultimate" force-stop integration
│   ├── IUserService.aidl
│   ├── UserService.kt
│   └── ShizukuBoosterManager.kt
├── accessibility/
│   └── BoostAccessibilityService.kt   # Fallback force-stop automation
├── overlay/               # Performance HUD + Crosshair floating overlays
├── utils/                 # GameModeManager (tiered cleanup), signal readers, app list
└── ui/
    ├── theme/
    └── screens/           # Home, NetworkOptimizer, GameBooster, Tools, Settings
```

## 🚧 Roadmap idea (future contributions welcome)

- [ ] Full IP-packet level NAT/routing in the VPN tunnel (currently a simplified passthrough)
- [ ] Per-app real-time bandwidth graphs (`NetworkStatsManager`)
- [ ] Widget/quick-settings tile to toggle Game Mode from anywhere
- [ ] Accessibility label matching for non-English Settings UIs (Sinhala/Tamil ROMs)
- [ ] Localization: full Sinhala + Tamil + English string resources

---

Made with ❤️ for Sri Lankan mobile gamers.

