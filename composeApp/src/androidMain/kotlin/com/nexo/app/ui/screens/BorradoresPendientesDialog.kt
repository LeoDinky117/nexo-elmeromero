package com.nexo.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nexo.app.model.MovimientoBorrador
import com.nexo.app.model.CategoriaFinanciera
import java.util.Locale
import kotlin.math.abs

@Composable
fun BorradoresPendientesDialog(
    borradores: List<MovimientoBorrador>,
    categorias: List<CategoriaFinanciera>,
    guardando: Boolean,
    mensaje: String?,
    onQuitar: (String) -> Unit,
    onGuardarUno: (String) -> Unit,
    onGuardarTodos: () -> Unit,
    onCerrar: () -> Unit
) {
    Dialog(
        onDismissRequest = onCerrar,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = 680.dp)
                .background(Color(0xFF111A29), RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Movimientos por confirmar", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Revisa lo que dictaste antes de guardarlo.", color = Color.LightGray, fontSize = 13.sp)
                }
                TextButton(onClick = onCerrar, enabled = !guardando) {
                    Text("Después", color = Color(0xFFD86AD1))
                }
            }

            Spacer(Modifier.height(14.dp))
            LazyColumn(
                modifier = Modifier.weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                items(borradores, key = { it.id }) { borrador ->
                    val categoria = categorias.firstOrNull { it.idCategoria == borrador.idCategoria }?.nombre ?: "Otros"
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF202B3B))) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(borrador.descripcion, color = Color.White, fontWeight = FontWeight.SemiBold)
                                Text("${borrador.tipo} · $categoria · ${borrador.fecha}", color = Color.LightGray, fontSize = 12.sp)
                                Text(
                                    "${if (borrador.monto >= 0) "+" else "−"}$${String.format(Locale.US, "%.2f", abs(borrador.monto))}",
                                    color = if (borrador.monto >= 0) Color(0xFF70D48A) else Color(0xFFFF6B91),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                TextButton(onClick = { onGuardarUno(borrador.id) }, enabled = !guardando) {
                                    Text("Guardar", color = Color(0xFF70D48A), fontSize = 12.sp)
                                }
                                TextButton(onClick = { onQuitar(borrador.id) }, enabled = !guardando) {
                                    Text("Quitar", color = Color(0xFFFF90A9), fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            if (mensaje != null) {
                Spacer(Modifier.height(8.dp))
                Text(mensaje, color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
            }

            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = onGuardarTodos,
                    enabled = borradores.isNotEmpty() && !guardando,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC837AB))
                ) {
                    if (guardando) {
                        CircularProgressIndicator(modifier = Modifier.height(18.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(if (guardando) "Guardando" else "Guardar todo")
                }
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = onCerrar, enabled = !guardando) {
                    Text("Revisar después", color = Color.LightGray)
                }
            }
        }
    }
}
