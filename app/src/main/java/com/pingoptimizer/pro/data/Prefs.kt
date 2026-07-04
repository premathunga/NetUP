package com.pingoptimizer.pro.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "ping_optimizer_prefs")

object PrefsKeys {
    val PRIORITY_PACKAGE = stringPreferencesKey("priority_package")
    val PRIORITY_LABEL = stringPreferencesKey("priority_label")
    val DNS_NAME = stringPreferencesKey("dns_name")
    val DNS_PRIMARY = stringPreferencesKey("dns_primary")
    val DNS_SECONDARY = stringPreferencesKey("dns_secondary")
    val THROTTLE_ENABLED = booleanPreferencesKey("throttle_enabled")
    val BOOSTER_ACTIVE = booleanPreferencesKey("booster_active")
}

class AppPrefs(private val context: Context) {

    val priorityPackage: Flow<String?> =
        context.dataStore.data.map { it[PrefsKeys.PRIORITY_PACKAGE] }
    val priorityLabel: Flow<String?> =
        context.dataStore.data.map { it[PrefsKeys.PRIORITY_LABEL] }
    val dnsName: Flow<String> =
        context.dataStore.data.map { it[PrefsKeys.DNS_NAME] ?: "Cloudflare" }
    val dnsPrimary: Flow<String> =
        context.dataStore.data.map { it[PrefsKeys.DNS_PRIMARY] ?: "1.1.1.1" }
    val dnsSecondary: Flow<String> =
        context.dataStore.data.map { it[PrefsKeys.DNS_SECONDARY] ?: "1.0.0.1" }
    val throttleEnabled: Flow<Boolean> =
        context.dataStore.data.map { it[PrefsKeys.THROTTLE_ENABLED] ?: false }
    val boosterActive: Flow<Boolean> =
        context.dataStore.data.map { it[PrefsKeys.BOOSTER_ACTIVE] ?: false }

    suspend fun setPriorityApp(packageName: String, label: String) {
        context.dataStore.edit {
            it[PrefsKeys.PRIORITY_PACKAGE] = packageName
            it[PrefsKeys.PRIORITY_LABEL] = label
        }
    }

    suspend fun setDns(name: String, primary: String, secondary: String) {
        context.dataStore.edit {
            it[PrefsKeys.DNS_NAME] = name
            it[PrefsKeys.DNS_PRIMARY] = primary
            it[PrefsKeys.DNS_SECONDARY] = secondary
        }
    }

    suspend fun setThrottleEnabled(enabled: Boolean) {
        context.dataStore.edit { it[PrefsKeys.THROTTLE_ENABLED] = enabled }
    }

    suspend fun setBoosterActive(active: Boolean) {
        context.dataStore.edit { it[PrefsKeys.BOOSTER_ACTIVE] = active }
    }
}
