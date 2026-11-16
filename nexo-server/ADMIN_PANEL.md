# Panel web de administración NEXO

El panel se sirve en `/admin/` desde los recursos del servidor. Trabaja con el catálogo y las métricas existentes en `Recompensas.kt`: crea temas e iconos, y muestra únicamente conteos agregados. No consulta correos, contraseñas ni movimientos individuales.

## Configuración

- `NEXO_ADMIN_API_KEY`: clave aleatoria de al menos 32 caracteres. Se guarda solo como variable de entorno del servidor; no debe agregarse al repositorio, al HTML ni al JavaScript. Para generar una clave localmente se puede usar `openssl rand -hex 32`.
- `NEXO_ADMIN_PUBLIC_ORIGIN`: origen exacto desde el que se abre el panel. Para desarrollo local: `http://localhost:8080`. En un servidor publicado: el origen HTTPS, por ejemplo `https://admin.ejemplo.com`.

El panel intercambia la clave por una sesión aleatoria de 30 minutos. El navegador la conserva en una cookie `HttpOnly`, `SameSite=Strict` y, cuando el origen es HTTPS, `Secure`. Las operaciones de escritura también comprueban el encabezado `Origin`. Las claves no se guardan en `localStorage` ni se incluyen en los archivos del portal.

En producción, publica el portal solo detrás de HTTPS y limita el acceso de red al servidor de aplicaciones. No configures un origen HTTP público: el único HTTP aceptado es `localhost` para desarrollo.

## Advertencia sobre la API móvil actual

La sesión descrita arriba protege el panel administrativo. En el código actual, varias rutas que usa la app móvil todavía reciben el ID del usuario desde el cliente y no validan una sesión propia. Por eso no se debe publicar el servidor completo en Internet con datos reales hasta agregar autenticación y autorización por usuario a esas rutas. El panel no usa ni expone esos endpoints.

## Límites actuales

El panel crea premios de tipo `ICONO` o `TEMA` y lee las métricas agregadas que ya ofrece la aplicación. La edición, pausa y papelera del diseño recibido no se muestran, porque el backend actual no ofrece esas operaciones. El historial de actividad tampoco se muestra porque no existe esa métrica en el modelo actual.
