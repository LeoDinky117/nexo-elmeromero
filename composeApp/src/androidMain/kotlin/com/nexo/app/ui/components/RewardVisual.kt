package com.nexo.app.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexo.app.util.ApiConfig
import com.nexo.app.util.client
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val ICONO_REMOTO_VALIDO = Regex("^/admin/icons/[0-9a-f-]{36}\\.(png|jpg|webp)$", RegexOption.IGNORE_CASE)

@Composable
fun RewardVisual(
    visual: String,
    modifier: Modifier = Modifier.size(36.dp),
    emojiFontSize: TextUnit = 30.sp
) {
    if (!ICONO_REMOTO_VALIDO.matches(visual)) {
        Text(visual, modifier = modifier, fontSize = emojiFontSize, textAlign = TextAlign.Center)
        return
    }

    val imagen = produceState<ImageBitmap?>(initialValue = null, key1 = visual) {
        value = runCatching {
            withContext(Dispatchers.IO) {
                val url = ApiConfig.BASE_URL.trimEnd('/') + visual
                val bytes = client.get(url).body<ByteArray>()
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            }
        }.getOrNull()
    }.value

    if (imagen != null) {
        Image(
            bitmap = imagen,
            contentDescription = "Icono de perfil",
            modifier = modifier,
            contentScale = ContentScale.Fit
        )
    } else {
        Box(modifier, contentAlignment = Alignment.Center) {
            Text("🦊", fontSize = emojiFontSize, textAlign = TextAlign.Center)
        }
    }
}
