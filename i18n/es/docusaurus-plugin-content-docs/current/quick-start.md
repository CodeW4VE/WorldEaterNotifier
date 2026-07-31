---
sidebar_position: 1
---

# Inicio Rápido


La forma más rápida de poner en marcha WorldEaterNotifier con un webhook (el modo de entrega predeterminado).

## Requisitos

- **Java 21** o superior
- **Minecraft 1.21.11** (servidor)
- **Fabric Loader** 0.18.1 o superior
- **Fabric API** 0.141.4+1.21.11 (o compatible)

## 1. Instala el mod

1. Compila el JAR desde el código fuente, o descárgalo de las [releases](https://github.com/CodeW4VE/WorldEaterNotifier/releases).
2. Copia `worldeaternotifier-*.jar` en la carpeta `mods` de tu servidor.
3. Inicia el servidor. El mod crea `config/worldeaternotifier.json` en el primer arranque.

## 2. Configura el webhook

1. En Discord, abre tu servidor → un canal de texto → **Editar canal → Integraciones → Webhooks → Nuevo webhook**.
2. Copia la URL del webhook (`https://discord.com/api/webhooks/...`).
3. En el juego, ejecuta:

```
/worldeater settings setWebhookUrl <url>
```

Eso es todo para la entrega. Opcionalmente, configura un rol para los pings:

```
/worldeater settings setPingRoleId <id-de-rol>
```

Consulta [Modo Webhook](webhook-setup) para la guía completa.

## 3. Crea una máquina

Las máquinas se definen con un nombre y las dos esquinas opuestas de una región rectangular (inclusiva). Las coordenadas se introducen como coordenadas absolutas de bloques.

```
/worldeater create <nombre> <x1> <y1> <z1> <x2> <y2> <z2>
/trencher create <nombre> <x1> <y1> <z1> <x2> <y2> <z2>
/bedrockbreaker create <nombre> <x1> <y1> <z1> <x2> <y2> <z2>
```

Ejemplo:

```
/worldeater create mi_eater 100 64 200 120 64 240
```

El orden de las esquinas no importa — el mod las normaliza.

## 4. Inicia el monitoreo

```
/worldeater start mi_eater
```

Se envía una notificación de "iniciado" a Discord. La máquina ahora es monitoreada:

- **World eaters**: si hay menos de `minTntCount` TNT encendidas en la región cada segundo, el "reloj de actividad" no avanza.
- **Trenchers / bedrock breakers**: si se destruyen menos de `minBlocksBroken` bloques por una explosión en la región, la actividad no avanza.
- Si no hay actividad durante `stopTimeout` segundos, se envía una notificación de **atascado**. Si la actividad se reanuda, se envía una notificación de **reanudado**.

Usa `/worldeater stop <nombre>` para detener el monitoreo manualmente.

## 5. Comportamiento al apagar

Cuando el servidor se apaga:

- Todas las máquinas activas se detienen (se ponen en `active: false` en la configuración).
- Se envía una notificación de **apagado** por cada máquina activa.
- En el próximo inicio del servidor, las máquinas están **inactivas** — debes iniciarlas manualmente de nuevo.

## Siguientes pasos

- Comandos completos en el juego: [Comandos de Minecraft](minecraft-commands)
- Modo bot de Discord en lugar de webhook: [Modo Bot](bot-setup)
- Personalizar notificaciones: [Plantillas de Mensaje](message-templates)
