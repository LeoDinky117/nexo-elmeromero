package com.nexo.app.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavController
import com.nexo.app.navigation.Routes
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import nexo.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.painterResource
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.shape.RoundedCornerShape
import nexo.composeapp.generated.resources.icon_trabajo
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.TextButton
import com.nexo.app.ui.viewModel.MovimientosViewModel
import nexo.composeapp.generated.resources.icon_check



@Composable
fun SuccessScreen(navController: NavController,
                  movimientosVM: MovimientosViewModel
) {


    val colorFondo = Color(0xFF0B1019)
    val colorMorado = Color(0xFFBE2EDD)
    val degradadoRosa = Brush.horizontalGradient(listOf(Color(0xFFC837AB),
        Color(0xFFFF508E)))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorFondo)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // --- SECCIÓN ICONO CHECK ---
        Box(
            modifier = Modifier
                .size(120.dp)
                .border(4.dp, colorMorado, CircleShape),
            contentAlignment = Alignment.Center
        ) {

            Image(
                painter = painterResource(Res.drawable.icon_check),
                contentDescription = "¡Éxito!",
                modifier = Modifier.size(60.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // --- TÍTULO Y SUBTÍTULO ---
        Text(
            text = "¡Guardado!",
            color = Color.White,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Tu movimiento fue registrado y tu saldo actualizado.",
            color = Color.Gray,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp).padding(horizontal = 20.dp)
        )

        Spacer(modifier = Modifier.height(40.dp))

        // --- RESUMEN DEL MOVIMIENTO ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF161C28), RoundedCornerShape(20.dp))
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) { }

        Spacer(modifier = Modifier.height(16.dp))

        // --- NUEVO SALDO Y XP ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF161C28), RoundedCornerShape(20.dp))
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Nuevo saldo", color = Color.Gray)
            Text(
                text = "$${movimientosVM.saldoTotal}",
                color = Color(0xFFFF508E),
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Cuadro de XP simplificado
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1C2431), RoundedCornerShape(20.dp))
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("+10 XP ganados", color = Color.White, fontSize = 14.sp)
        }

        Spacer(modifier = Modifier.height(60.dp))

        // --- BOTONES DE ACCIÓN ---
        // Botón principal "Ir al inicio" con degradado
        Button(
            onClick = { navController.navigate(Routes.MAIN_HOME) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            contentPadding = PaddingValues()
        ) {
            Box(
                modifier = Modifier.fillMaxSize().background(degradadoRosa),
                contentAlignment = Alignment.Center
            ) {
                Text("Ir al inicio", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        // Botón secundario "Agregar otro"
        TextButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text("Agregar otro", color = Color.Gray)
        }
    }
}