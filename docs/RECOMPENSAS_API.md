# Catálogo de recompensas NEXO

El servidor conserva los puntos acumulados en `Usuarios.Puntos` y registra por separado los puntos gastados en `SaldoPuntosRecompensas`. La app muestra el saldo disponible y los canjes no reducen el nivel ni el progreso acumulado del usuario. Cada recompensa tiene código estable, tipo (`TEMA` o `ICONO`), costo y valor visual. El servidor valida el saldo disponible y registra el desbloqueo en una transacción.

## Endpoints para la app

- `GET /recompensas/usuario/{id}` devuelve puntos, artículos, artículos desbloqueados y el estilo equipado.
- `POST /recompensas/usuario/{id}/desbloquear/{premioId}` canjea puntos por un artículo.
- `POST /recompensas/usuario/{id}/equipar` recibe `{"Tipo":"TEMA","Codigo":"tema_oceano"}` o el tipo `ICONO`.

## Endpoints administrativos

El servidor debe tener configurada la variable de entorno `NEXO_ADMIN_API_KEY`. El portal administrativo envía su valor como encabezado `X-Admin-Key` en:

- `GET /admin/recompensas` para consultar el catálogo.
- `POST /admin/recompensas` para agregar un artículo.
- `GET /admin/recompensas/metricas` para consultar cuentas, catálogo activo, desbloqueos y puntos acumulados.

Ejemplo de artículo:

```json
{
  "Codigo": "tema_lavanda",
  "Nombre": "Lavanda",
  "Descripcion": "Un acento violeta suave.",
  "Tipo": "TEMA",
  "CostoPuntos": 250,
  "ValorVisual": "#9B72CF|#100D1B|#211A31|#D0B8F0"
}
```

Los temas usan cuatro colores hexadecimales separados por `|` (acento, fondo, superficie y resaltado). Los iconos usan un emoji o símbolo de hasta 12 caracteres. La clave administrativa debe permanecer en el servidor del portal; nunca debe incluirse en JavaScript público ni en la app móvil.

## Catálogo inicial

Al iniciar, el backend crea las tablas necesarias y agrega estilos Atardecer, Océano y Bosque, además de los iconos Avatar NEXO, Zorro financiero y Lobo NEXO. La inserción inicial es idempotente y no elimina los artículos ya existentes.

## Alcance de la integración

El repositorio actual no incluye el código del portal web. Estos endpoints dejan lista la conexión del lado del servidor; el portal debe consumirlos desde su backend autenticado para no exponer la clave administrativa en el navegador. Las rutas de usuario siguen el patrón actual de la API, que identifica la cuenta mediante su ID. Antes de publicar el servidor para usuarios externos, hay que sustituir ese patrón por autenticación de sesión y comprobar la identidad en el servidor.
