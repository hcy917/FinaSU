package me.weishu.kernelsu.ui.screen.home

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.weishu.kernelsu.R
import me.weishu.kernelsu.data.signing.ManagerSignature
import me.weishu.kernelsu.ui.theme.FolkType

/**
 * Shows the certificate this APK is signed with: the size and SHA-256 that a kernel module
 * matches, and that a BakaSU-style dynamic manager asks for when the app is registered.
 *
 * Replaces the install/patch entry points the manager used to offer on Home; long-pressing the
 * card copies both values, so they can be pasted into whichever tool needs them.
 */
@Composable
internal fun HomeSignatureCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val signature by produceState<ManagerSignature?>(initialValue = null) {
        value = withContext(Dispatchers.IO) { ManagerSignature.of(context) }
    }
    val info = signature ?: return

    HomeCard(
        modifier = modifier.fillMaxWidth(),
        onLongClick = { context.copySignature(info) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.home_signature_title),
                style = FolkType.Title,
            )
            SignatureRow(
                label = stringResource(R.string.home_signature_size),
                value = "${info.sizeHex} (${info.sizeBytes})",
            )
            SignatureRow(
                label = stringResource(R.string.home_signature_hash),
                value = info.sha256,
                monospace = true,
            )
            Text(
                text = stringResource(R.string.home_signature_copy_hint),
                style = FolkType.Caption,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SignatureRow(
    label: String,
    value: String,
    monospace: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            style = FolkType.Caption,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(96.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = value,
            style = if (monospace) FolkType.Caption.copy(fontFamily = FontFamily.Monospace) else FolkType.Summary,
        )
    }
}

private fun Context.copySignature(signature: ManagerSignature) {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val payload = "${signature.sizeHex} ${signature.sizeBytes}\n${signature.sha256}"
    clipboard.setPrimaryClip(ClipData.newPlainText("FinaSU signing certificate", payload))
    Toast.makeText(this, R.string.home_signature_copied, Toast.LENGTH_SHORT).show()
}
