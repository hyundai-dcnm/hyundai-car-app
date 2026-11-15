package com.dcnm.donghaeng.vehicle

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.dcnm.donghaeng.core.model.Destination
import com.google.gson.Gson
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class SavedDestinations(val recent: List<Destination> = emptyList(), val favorites: Map<String, Destination> = emptyMap())

/** Destination history may identify a home/work address. Encrypt before writing preferences. */
class DestinationStore(context: Context, storeName: String = "destinations") {
    private val prefs = context.getSharedPreferences(storeName, Context.MODE_PRIVATE)
    private val alias = "donghaeng.$storeName.v1"
    private fun key(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        return (ks.getKey(alias, null) as? SecretKey) ?: KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
    @Synchronized fun load(): SavedDestinations = runCatching {
        val data = prefs.getString("encrypted", null)?.split(":") ?: return SavedDestinations()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, Base64.decode(data[0], Base64.NO_WRAP))) }
        Gson().fromJson(cipher.doFinal(Base64.decode(data[1], Base64.NO_WRAP)).toString(Charsets.UTF_8), SavedDestinations::class.java)
    }.getOrDefault(SavedDestinations())
    @Synchronized fun save(value: SavedDestinations) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key()) }
        val encrypted = cipher.doFinal(Gson().toJson(value).toByteArray())
        prefs.edit().putString("encrypted", Base64.encodeToString(cipher.iv, Base64.NO_WRAP) + ":" + Base64.encodeToString(encrypted, Base64.NO_WRAP)).apply()
    }
}
