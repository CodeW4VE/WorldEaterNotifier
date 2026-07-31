---
sidebar_position: 4
---

# Modo Bot


El modo bot reemplaza el webhook con un bot real de Discord. Añade **comandos de barra**, un botón interactivo **Toggle Ping** y un control más granular — a costa de una configuración un poco más larga.

## 1. Crea la aplicación de Discord

1. Ve al [Portal de desarrolladores de Discord](https://discord.com/developers/applications).
2. Haz clic en **New Application**, ponle un nombre y copia el **Application ID**.
3. Abre la pestaña **Bot**:
   - Haz clic en **Reset Token** y copia el nuevo **token** (este es el token del bot).
   - Activa **SERVER MEMBERS INTENT** — necesario para el botón de alternancia y las comprobaciones de rol.

> Trata el token del bot como una contraseña. Si se filtra, restablécelo inmediatamente.

## 2. Invita el bot a tu servidor

Abre la pestaña **OAuth2 → URL Generator**:

1. Selecciona los alcances **bot** y **applications.commands**.
2. En **Bot Permissions**, selecciona:
   - **View Channels**
   - **Send Messages**
   - **Embed Links**
   - **Manage Roles** (requerido para asignar/quitar el rol de ping con el botón de alternancia)
3. Copia la URL generada y ábrela en un navegador para invitar el bot a tu servidor.

## 3. Obtén los IDs del servidor y del canal

Activa el **modo desarrollador** en Discord (Configuración de usuario → Avanzado).

- **Guild ID**: clic derecho en el nombre de tu servidor → **Copy Server ID**.
- **Channel ID**: clic derecho en el canal de notificaciones → **Copy Channel ID**.
- **Role ID**: Configuración del servidor → clic derecho en un rol → **Copy Role ID**.

## 4. Configura el bot en el juego

```
/worldeater settings setBotToken <token>
/worldeater settings setGuildId <id-del-servidor>
/worldeater settings setChannelId <id-del-canal>
/worldeater settings setPingRoleId <id-de-rol>
/worldeater settings setNotificationMode bot
```

Configurar el token del bot reinicia el bot automáticamente. Cambiar a modo `bot` arranca el bot y registra sus comandos de barra (esto ocurre cuando el bot se conecta — dale unos segundos).

> **Protección de inicio**: las máquinas no arrancarán hasta que `botToken`, `guildId` y `channelId` estén configurados.

## 5. Comandos de barra en Discord

Los comandos se registran en tu servidor en cuanto el bot se conecta.

### Gestionar máquinas

| Comando | Descripción |
|---|---|
| `/worldeater start <nombre>` | Inicia una máquina (el nombre tiene autocompletado). |
| `/worldeater stop <nombre>` | Detiene una máquina (el nombre tiene autocompletado). |
| `/worldeater list` | Lista los world eaters con estado activo/inactivo. |

Lo mismo aplica para `/trencher` y `/bedrockbreaker`. Iniciar/detener desde Discord también transmite el evento al chat del juego.

### Configurar el bot (`/config`)

| Comando | Descripción |
|---|---|
| `/config ping-role <rol>` | Configura el rol usado para los pings. |
| `/config channel <canal>` | Configura el canal de notificaciones. |
| `/config subscription-button <true\|false>` | Muestra/oculta el botón "Toggle Ping" en los mensajes de inicio. |
| `/config member-discord-role <rol>` | Rol que puede usar los comandos de barra start/stop/list. |
| `/config pings` | Editor interactivo de los ajustes de ping (ver abajo). |

### Flujo interactivo de `/config pings`

1. Ejecutar `/config pings` muestra un desplegable con **World Eater**, **Trencher**, **Bedrock Breaker**.
2. Elige un tipo → un embed muestra los ajustes de ping actuales y un desplegable de qué cambiar (enabled, onStart, onStop, onStuck, onResumed, onShutdown).
3. Elige un ajuste → aparecen botones **True / False**.
4. Hacer clic en uno guarda el cambio y te devuelve al desplegable de ajustes, así puedes seguir ajustando sin volver a ejecutar el comando.

## 6. El botón "Toggle Ping"

En cada mensaje de **inicio**, el bot añade un botón **🔔 Toggle Ping**:

- Al hacer clic, añade o quita el rol de ping para ti.
- La respuesta es efímera (solo tú la ves).
- Si el bot no puede gestionar el rol, verás un error — el rol del bot debe estar **por encima** del rol de ping en Configuración del servidor → Roles.

Oculta el botón por completo con `/config subscription-button false` o `/worldeater settings showSubscriptionButton false`.

## 7. Matriz de permisos en Discord

| Acción | Quién puede hacerla |
|---|---|
| `/config` (todos los subcomandos) | Solo **Administradores** |
| `/worldeater` `/trencher` `/bedrockbreaker` (start/stop/list) | Miembro de `member-discord-role` **o** Administrador |
| Botón Toggle Ping | Cualquiera (afecta solo a quien hace clic) |

Los comandos de Discord **no** se ven afectados por la whitelist del juego — el acceso en Discord se basa únicamente en los roles de Discord.

## Notas

- Las notificaciones se encolan internamente hasta que el WebSocket del bot se conecta; si el bot está brevemente desconectado, se almacenan hasta 50 mensajes y se vacían al reconectarse.
- Los cambios en el canal, roles y mensajes se aplican **inmediatamente** — sin necesidad de reiniciar el bot.
- Al apagar el servidor, el bot se detiene y se envía una notificación de apagado por cada máquina activa.
