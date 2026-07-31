---
sidebar_position: 6
---

# FAQ


## Los comandos de barra de Discord no aparecen

Los comandos se registran cuando el bot se conecta. Si acabas de cambiar a modo bot o actualizaste el bot, reinicia el servidor para que el bot se reconecte y vuelva a registrar los comandos. Si aún no aparecen:

1. Asegúrate de que `notificationMode` sea `bot`.
2. Verifica que `botToken`, `guildId` y `channelId` estén configurados y sean correctos.
3. Confirma que el bot fue invitado con el alcance `applications.commands`.

## "You won't get pinged" / "Failed to assign role" al hacer clic en Toggle Ping

El bot necesita el permiso **Manage Roles**, y el rol del bot debe estar **por encima** del rol de ping en Configuración del servidor → Roles. Sube el rol del bot e inténtalo de nuevo.

## Las máquinas no arrancan ("delivery not configured")

El mod se niega a iniciar máquinas si el modo actual no está configurado:

- **Modo webhook**: `webhookUrl` debe estar configurada (`/worldeater settings setWebhookUrl <url>`).
- **Modo bot**: `botToken`, `guildId` y `channelId` deben estar configurados.

## ¿Puedo usar el modo webhook y el modo bot a la vez?

No. `notificationMode` es `webhook` o `bot` — solo uno está activo a la vez.

## ¿Quién puede usar los comandos?

**En el juego**: los jugadores op siempre; los jugadores no-op solo si están en la whitelist (`settings whitelist add <jugador>`).

**En Discord**: `/config` es solo para Administradores. `/worldeater` `/trencher` `/bedrockbreaker` start/stop/list requieren el `member-discord-role` **o** ser Administrador. El botón Toggle Ping está abierto para todos (solo afecta a quien hace clic).

La whitelist del juego **no** se aplica a los comandos de Discord.

## El bot no se conecta

- Verifica el token dos veces (restablécelo en el Portal de desarrolladores si dudas).
- Revisa los logs del servidor para ver el error del bot al iniciar.
- Asegúrate de que el servidor tenga acceso a internet (API de Discord).
- Confirma que `guildId` coincide con el servidor al que fue invitado el bot.

## No se detecta nada aunque la máquina esté funcionando

Revisa los umbrales:

- **World eater**: `minTntCount` — cuántas TNT encendidas deben estar en la región cada segundo. Si tu máquina usa menos, bájalo.
- **Trencher / bedrock breaker**: `minBlocksBroken` — bloques destruidos por explosión. Los bedrock breakers usan 1 por defecto.
- **`stopTimeout`**: segundos sin actividad antes de que salte el "stuck". Las máquinas grandes pueden pausarse legítimamente; sube el tiempo si tienes avisos de atascado falsos.

## Las máquinas se reinician inactivas tras un reinicio

Es por diseño. Al iniciar el servidor, cada máquina se carga como **inactiva** y debe iniciarse manualmente con `/worldeater start <nombre>` (o el comando de barra de Discord). Las máquinas activas se detienen automáticamente al apagar y se envía una notificación de apagado.
