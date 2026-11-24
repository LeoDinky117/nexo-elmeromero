package com.nexo.app.model

import java.util.Locale

/** Colores e iconos compartidos por el calendario, el widget y el registro de presupuesto. */
object VisualConceptoCalendario {
    fun colorArgb(evento: EventoCalendario): Int = when (evento.tipo) {
        TipoEventoCalendario.INGRESO -> 0xFF71D58B.toInt()
        TipoEventoCalendario.CORTE -> 0xFFB985E8.toInt()
        TipoEventoCalendario.PAGO -> when (categoria(evento.concepto)) {
            "hogar" -> 0xFFFFB84D.toInt()
            "servicios" -> 0xFF57C7D4.toInt()
            "salud" -> 0xFFFF7894.toInt()
            "deuda" -> 0xFFB985E8.toInt()
            "comida" -> 0xFFEE9457.toInt()
            "transporte" -> 0xFF7CA9FF.toInt()
            "suscripcion" -> 0xFF5ED0B2.toInt()
            else -> 0xFFE477B0.toInt()
        }
    }

    fun icono(concepto: String): String = when (categoria(concepto)) {
        "ingreso" -> "💰"
        "hogar" -> "🏠"
        "servicios" -> when {
            concepto.contains("agua", true) -> "💧"
            concepto.contains("luz", true) -> "💡"
            concepto.contains("gas", true) -> "🔥"
            else -> "📶"
        }
        "salud" -> "🩺"
        "deuda" -> if (concepto.contains("tanda", true)) "🤝" else "💳"
        "comida" -> "🛒"
        "transporte" -> "🚌"
        "suscripcion" -> "📱"
        "educacion" -> "🎓"
        "compras" -> "🛍️"
        "ocio" -> "🎟️"
        "viaje" -> "✈️"
        "mascotas" -> "🐾"
        "impuestos" -> "🧾"
        "seguro" -> "🛡️"
        "cuidado_personal" -> "🧴"
        "gimnasio" -> "🏋️"
        "ahorro" -> "🐖"
        "gasto_hormiga" -> "🐜"
        else -> "🧾"
    }

    private fun categoria(concepto: String): String {
        val texto = concepto.lowercase(Locale.ROOT)
        return when {
            listOf("sueldo", "salario", "nómina", "nomina", "ingreso", "freelance", "venta", "pensión", "pension", "apoyo", "rendimiento", "interés", "interes", "tanda recibida").any(texto::contains) -> "ingreso"
            listOf("renta", "hipoteca", "hogar").any(texto::contains) -> "hogar"
            listOf("gasolina", "transporte", "camión", "camion", "uber", "metro").any(texto::contains) -> "transporte"
            listOf("luz", "agua", "gas", "internet", "teléfono", "telefono", "servicio").any(texto::contains) -> "servicios"
            listOf("salud", "medicina", "médic", "medic", "consulta", "farmacia", "doctor", "seguro médico").any(texto::contains) -> "salud"
            listOf("tarjeta", "préstamo", "prestamo", "crédito", "credito", "deuda", "tanda", "diferido").any(texto::contains) -> "deuda"
            listOf("despensa", "comida", "supermercado", "alimento").any(texto::contains) -> "comida"
            listOf("suscripción", "suscripcion", "streaming", "netflix", "spotify").any(texto::contains) -> "suscripcion"
            listOf("gastos hormiga", "café", "cafe", "snack", "antojito").any(texto::contains) -> "gasto_hormiga"
            listOf("colegiatura", "escuela", "curso", "educación", "educacion", "libro").any(texto::contains) -> "educacion"
            listOf("compra", "ropa", "calzado", "electrónico", "electronico").any(texto::contains) -> "compras"
            listOf("ocio", "cine", "entretenimiento", "concierto", "juego").any(texto::contains) -> "ocio"
            listOf("viaje", "vacaciones", "hotel", "avión", "avion").any(texto::contains) -> "viaje"
            listOf("mascota", "veterinario", "perro", "gato").any(texto::contains) -> "mascotas"
            listOf("impuesto", "predial", "tenencia").any(texto::contains) -> "impuestos"
            listOf("seguro", "póliza", "poliza").any(texto::contains) -> "seguro"
            listOf("cuidado personal", "estética", "estetica", "peluquería", "peluqueria").any(texto::contains) -> "cuidado_personal"
            listOf("gimnasio", "deporte", "entrenamiento").any(texto::contains) -> "gimnasio"
            listOf("ahorro", "meta").any(texto::contains) -> "ahorro"
            else -> "otro"
        }
    }
}
