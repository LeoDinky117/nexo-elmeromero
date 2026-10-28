package com.example

import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals

class RecompensasRoutesTest {
    @Test
    fun catalogoAdministrativoRequiereClave() = testApplication {
        application {
            configureSerialization()
            configureRecompensas()
        }

        val response = client.get("/admin/recompensas")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun metricasAdministrativasRequierenClave() = testApplication {
        application {
            configureSerialization()
            configureRecompensas()
        }

        val response = client.get("/admin/recompensas/metricas")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }
}
