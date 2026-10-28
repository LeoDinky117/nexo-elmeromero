package com.example

import com.example.com.nexo.app.model.NuevaRecompensa
import kotlin.test.Test
import kotlin.test.assertFailsWith

class RecompensasValidationTest {
    @Test
    fun aceptaTemaConCuatroColoresValidos() {
        validarPremio(
            NuevaRecompensa(
                codigo = "tema_lavanda",
                nombre = "Lavanda",
                descripcion = "Tema de prueba",
                tipo = "TEMA",
                costoPuntos = 250,
                valorVisual = "#9B72CF|#100D1B|#211A31|#D0B8F0"
            )
        )
    }

    @Test
    fun rechazaTemaConColorInvalido() {
        assertFailsWith<IllegalArgumentException> {
            validarPremio(
                NuevaRecompensa(
                    codigo = "tema_invalido",
                    nombre = "Tema inválido",
                    tipo = "TEMA",
                    costoPuntos = 100,
                    valorVisual = "#ABC|#100D1B|#211A31|#D0B8F0"
                )
            )
        }
    }

    @Test
    fun rechazaTipoDeRecompensaNoAdmitido() {
        assertFailsWith<IllegalArgumentException> {
            validarPremio(
                NuevaRecompensa(
                    codigo = "fondo_animado",
                    nombre = "Fondo animado",
                    tipo = "FONDO",
                    costoPuntos = 100,
                    valorVisual = "fondo.gif"
                )
            )
        }
    }

    @Test
    fun rechazaCostoNegativo() {
        assertFailsWith<IllegalArgumentException> {
            validarPremio(
                NuevaRecompensa(
                    codigo = "icono_prueba",
                    nombre = "Icono de prueba",
                    tipo = "ICONO",
                    costoPuntos = -1,
                    valorVisual = "⭐"
                )
            )
        }
    }
}
