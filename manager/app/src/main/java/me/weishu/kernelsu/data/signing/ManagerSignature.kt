package me.weishu.kernelsu.data.signing

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.security.MessageDigest

/**
 * The manager's own signing certificate, reduced to the two values a kernel module or a
 * BakaSU-style dynamic manager matches against: the DER block length and its SHA-256.
 *
 * The size is what the kernel compares against `EXPECTED_SIZE2` and the hash against
 * `EXPECTED_HASH2`, so both come straight from the certificate this APK was signed with.
 */
data class ManagerSignature(
    val sizeBytes: Int,
    val sha256: String,
) {
    /** Kernel modules print and compare the size as a four digit hex value. */
    val sizeHex: String get() = "0x%04x".format(sizeBytes)

    companion object {
        @Volatile
        private var cached: ManagerSignature? = null

        /** Reads, and memoises, the certificate of the running APK. Null when unavailable. */
        fun of(context: Context): ManagerSignature? =
            cached ?: runCatching { read(context) }.getOrNull()?.also { cached = it }

        private fun read(context: Context): ManagerSignature? {
            val pm = context.packageManager
            val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(
                        PackageManager.GET_SIGNING_CERTIFICATES.toLong(),
                    ),
                )
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            }
            val signer = info.signingInfo?.apkContentsSigners?.firstOrNull() ?: return null
            val der = signer.toByteArray()
            val digest = MessageDigest.getInstance("SHA-256").digest(der)
            return ManagerSignature(
                sizeBytes = der.size,
                sha256 = digest.joinToString(separator = "") { "%02x".format(it) },
            )
        }
    }
}
