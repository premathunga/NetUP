# Keep VpnService and its subclasses
-keep class com.pingoptimizer.pro.service.** { *; }
-keep class com.pingoptimizer.pro.network.** { *; }

# Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**
