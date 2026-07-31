---
sidebar_position: 3
---

# Modo Webhook


El modo webhook es el método de entrega predeterminado y el más simple de configurar — sin bot, sin permisos, solo una URL de webhook de Discord.

## 1. Crea un webhook en Discord

1. Abre tu servidor de Discord y selecciona el canal de texto donde deben aparecer las notificaciones.
2. Haz clic en **Editar canal** (ícono de engranaje) → **Integraciones** → **Webhooks**.
3. Haz clic en **Nuevo webhook**, ponle un nombre (p. ej. `WorldEaterNotifier`) y, opcionalmente, un avatar para el canal.
4. Haz clic en el webhook y presiona **Copiar URL de webhook**. Se ve así:

```
https://discord.com/api/webhooks/1234567890/abcdefghijklmnopqrstuvwxyz
```

> Trata la URL del webhook como una contraseña — cualquiera que la tenga puede publicar en ese canal.

## 2. Configura la URL del webhook en el juego

```
/worldeater settings setWebhookUrl <url>
```

La URL es compartida entre los tres tipos de máquina, así que solo la configuras una vez.

## 3. (Opcional) Configura el rol de ping

Para mencionar un rol de Discord cuando ocurren eventos, crea un rol (Configuración del servidor → Roles → Crear rol) y copia su ID:

1. Activa el **modo desarrollador** en Discord: Configuración de usuario → Avanzado → Modo desarrollador.
2. Haz clic derecho en el rol en Configuración del servidor → **Copiar ID de rol**.

Luego en el juego:

```
/worldeater settings setPingRoleId <id-de-rol>
```

Deja el ID de rol vacío o pon `0` para deshabilitar todas las menciones.

## 4. Controla qué eventos mencionan el rol

Cada evento se puede alternar de forma independiente:

```
/worldeater settings discordPings enable true
/worldeater settings discordPings onStart true
/worldeater settings discordPings onStuck true
/worldeater settings discordPings onResumed true
/worldeater settings discordPings onStop true
/worldeater settings discordPings onShutdown true
```

Pon cualquiera de ellos en `false` y ese evento se seguirá enviando a Discord, pero sin la mención del rol.

## 5. Pruébalo

Crea una máquina y arráncala:

```
/worldeater create prueba 0 0 0 5 5 5
/worldeater start prueba
```

Deberías ver un mensaje de "iniciado" en el canal. Deténla y un mensaje de "detenido manualmente" lo seguirá.

## 6. Verifica tus ajustes

```
/worldeater settings show
```

Confirma el modo actual, la URL del webhook (enmascarada), el rol de ping, los umbrales y los alternadores de ping.

## Notas

- El modo webhook es el predeterminado (`"notificationMode": "webhook"` en `config/worldeaternotifier.json`).
- Los cambios en la URL del webhook, el rol y las plantillas de mensaje se aplican **inmediatamente** — sin necesidad de reiniciar el servidor (builds recientes).
- El modo webhook no tiene comandos de barra ni botón de alternancia; esas son características del [modo bot](bot-setup).
