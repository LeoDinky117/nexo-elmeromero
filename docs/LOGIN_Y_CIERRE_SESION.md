# Inicio y cierre de sesión en Nexo

## Inicio de la app

Nexo exige iniciar sesión cada vez que comienza un proceso nuevo de la app. `MainActivity` limpia primero la sesión local y muestra brevemente un indicador de carga. Después crea la navegación, cuya ruta inicial es `login`.

La limpieza elimina del dispositivo el ID activo, los puntos, la racha y el tema/icono de la cuenta anterior. No borra usuarios, movimientos, metas ni otros registros de MariaDB. También conserva el seguimiento de la guía de bienvenida.

Los ViewModels reciben el ID `0` mientras se muestra el login, por lo que no consultan datos de ningún usuario. Las consultas privadas empiezan cuando el servidor acepta el login y la app guarda el ID recibido.

## Iniciar sesión

1. El usuario escribe su correo y contraseña.
2. Al pulsar **Iniciar sesión**, `LoginViewModel` envía `POST /login` al servidor.
3. Si las credenciales son válidas, guarda el ID de usuario localmente y carga el estilo de recompensas.
4. Los ViewModels observan el ID activo y cargan los datos correspondientes. La app navega a Inicio y, cuando corresponde, muestra la guía de bienvenida.
5. Si las credenciales son incorrectas o no hay conexión, se muestra un mensaje y no se navega a las pantallas privadas.

La pantalla también muestra una recomendación para crear contraseñas nuevas. El texto es una sugerencia: no impone una regla nueva al servidor. El ejemplo que aparece en pantalla no debe reutilizarse como contraseña.

## Cerrar sesión

1. Desde **Perfil**, el usuario pulsa **Cerrar sesión** y confirma la acción.
2. La app elimina localmente el ID y los valores temporales asociados a la cuenta.
3. Los ViewModels vacían los datos en memoria al recibir el ID `0`.
4. La navegación vuelve al login y retira las pantallas privadas de la pila. El botón Atrás no regresa al perfil.

El cierre de sesión no elimina datos del servidor. En la versión actual el login no crea un token de servidor, así que el cierre invalida la sesión guardada en el dispositivo y evita que la app siga consultando con ese ID.

## Comprobación manual

- Cierra la app por completo y vuelve a abrirla: debe aparecer el login y no deben salir consultas privadas antes de enviar las credenciales.
- Inicia sesión: el Logcat debe mostrar el `POST /login` y, después de recibir `200 OK`, las consultas de datos del usuario.
- Cierra sesión desde Perfil: debe regresar al login. Al abrir o navegar por las pantallas públicas no deben consultarse datos del usuario anterior.
- Inicia sesión con otra cuenta: solo deben aparecer los datos de esa cuenta.

## Nota de seguridad pendiente

La implementación actual del servidor compara las contraseñas directamente con el valor almacenado en MariaDB; todavía no usa hash de contraseña ni token de sesión. No reutilices una contraseña personal importante en este entorno. Antes de publicar el servicio, se debe migrar el almacenamiento a un hash seguro y revisar la autenticación del servidor.
