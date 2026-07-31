---
sidebar_position: 2
---

# Comandos de Minecraft


Los tres tipos de máquina tienen la misma estructura de comandos. Los comandos están limitados por el [sistema de permisos](faq) — los jugadores op siempre tienen acceso; los jugadores no-op deben estar en la whitelist.

- `/worldeater` — máquinas basadas en TNT
- `/trencher` — máquinas por destrucción de bloques
- `/bedrockbreaker` — máquinas por destrucción de bloques

Cada comando admite los siguientes subcomandos (usando `/worldeater` como ejemplo):

## Create

```
/worldeater create <nombre> <x1> <y1> <z1> <x2> <y2> <z2>
```

Define una región rectangular de monitoreo. Las dos esquinas se normalizan, así que el orden no importa. La dimensión es la dimensión en la que estás cuando ejecutas el comando.

## Start / Stop

```
/worldeater start <nombre>
/worldeater stop <nombre>
```

`start` inicia el monitoreo y envía una notificación "iniciado" a Discord. `stop` finaliza el monitoreo y envía una notificación "detenido manualmente".

> **Protección de inicio**: el mod se niega a iniciar una máquina si el modo de notificación actual no está configurado (el modo webhook necesita una `webhookUrl`; el modo bot necesita `botToken` + `guildId` + `channelId`).

## List / Delete

```
/worldeater list
/worldeater delete <nombre>
```

`list` muestra todas las máquinas con su estado activo/inactivo. `delete` elimina la definición de una máquina.

## Settings

```
/worldeater settings show
```

Muestra los ajustes actuales de ese tipo de máquina.

### Ajustes de entrega

| Comando | Modo | Descripción |
|---|---|---|
| `settings setWebhookUrl <url>` | webhook | Establece la URL del webhook de Discord (compartida entre los tres tipos). |
| `settings setPingRoleId <idRol>` | ambos | Rol que se menciona cuando los pings están habilitados. `0` o vacío deshabilita las menciones. |
| `settings setBotToken <token>` | bot | Establece el token del bot y reinicia el bot. |
| `settings setGuildId <id>` | bot | Establece el ID del servidor (guild) de Discord. |
| `settings setChannelId <id>` | bot | Establece el canal de Discord para las notificaciones. |
| `settings setMemberDiscordRole <idRol>` | bot | Rol que puede usar los comandos de barra start/stop/list en Discord. |
| `settings setNotificationMode <webhook\|bot>` | ambos | Cambia el modo de entrega y actualiza los comandos visibles. |
| `settings showSubscriptionButton <true\|false>` | bot | Muestra/oculta el botón "Toggle Ping" en los mensajes de inicio. |

Los comandos de ajustes irrelevantes para el modo actual se ocultan del autocompletado. Por ejemplo, los ajustes solo de bot solo aparecen cuando `notificationMode` es `bot`.

### Umbrales de detección

| Comando | Aplica a | Predeterminado |
|---|---|---|
| `settings setStopTimeout <segundos>` | todos | 60 (world eater / bedrock breaker), 180 (trencher) |
| `settings setMinTntCount <cantidad>` | world eater | 20 |
| `settings setMinBlocksBroken <cantidad>` | trencher / bedrock breaker | 3 / 1 |

### Alternadores de ping en Discord

```
/worldeater settings discordPings show
/worldeater settings discordPings enable <true|false>
/worldeater settings discordPings onStart <true|false>
/worldeater settings discordPings onStop <true|false>
/worldeater settings discordPings onStuck <true|false>
/worldeater settings discordPings onResumed <true|false>
/worldeater settings discordPings onShutdown <true|false>
```

Cada alternador controla si el rol de ping se menciona para ese evento. El `enable` global debe estar en `true` para que cualquier alternador por evento tenga efecto. Estos se guardan en el JSON de configuración entre reinicios.

### Whitelist

```
/worldeater settings whitelist list
/worldeater settings whitelist add <jugador>
/worldeater settings whitelist remove <jugador>
```

La whitelist es **compartida** entre los tres comandos. Los jugadores op siempre tienen acceso; los jugadores no-op deben estar en la lista. Solo los jugadores op pueden añadir o eliminar nombres.
