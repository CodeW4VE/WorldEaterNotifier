---
sidebar_position: 5
---

# Plantillas de Mensaje


Todo el texto de las notificaciones de Discord es configurable por tipo de máquina. Esto funciona tanto en modo webhook como en modo bot.

## Los 5 mensajes

Cada tipo de máquina tiene su propio bloque `messages` en `config/worldeaternotifier.json` con cinco plantillas:

| Clave | Evento |
|---|---|
| `start` | Máquina iniciada. |
| `stuck` | Máquina detenida por una obstrucción (sin actividad durante `stopTimeout` segundos). |
| `resumed` | La máquina volvió a moverse tras estar atascada. |
| `manualStop` | Máquina detenida manualmente con `/stop` (o el comando de barra de Discord). |
| `shutdown` | La máquina estaba activa cuando el servidor se apagó. |

## Comodines

| Comodín | Se reemplaza con |
|---|---|
| `{type}` | El nombre del tipo de máquina (`WorldEater`, `Trencher`, `BedrockBreaker`). |
| `{name}` | El nombre de la instancia de máquina. |

## Predeterminados

```json
"messages": {
  "start": "{type} **'{name}'** has started.",
  "stuck": "{type} **'{name}'** has stopped due to an obstruction.",
  "resumed": "{type} **'{name}'** has started again.",
  "manualStop": "{type} **'{name}'** was stopped manually.",
  "shutdown": "{type} **'{name}'** was shut down with the server and may have broken."
}
```

## Cómo editarlos

1. Detén el servidor (o prepárate para reiniciarlo después).
2. Abre `config/worldeaternotifier.json` en un editor de texto.
3. Edita el bloque `messages` del tipo de máquina que quieras (bajo `worldEaterSettings`, `trencherSettings` o `bedrockBreakerSettings`).

Ejemplo — personaliza el mensaje de atascado del world eater:

```json
{
  "worldEaterSettings": {
    "messages": {
      "stuck": "{type} '{name}' is blocked! Check the front wall."
    }
  }
}
```

4. Guarda el archivo. En los builds recientes, los cambios de mensaje se aplican de inmediato; de lo contrario, reinicia el servidor.

> Cualquier campo de mensaje que falte en el JSON vuelve a su valor predeterminado.

## El markdown de Discord funciona

El markdown de Discord se admite en las plantillas — negrita, cursiva, bloques de código, menciones de canal/rol:

```json
"start": "{type} **'{name}'** is now being monitored — <@&1234567890>"
```

## Ejemplo: configuración completa con mensajes personalizados

```json
{
  "worldEaterSettings": {
    "minTntCount": 20,
    "messages": {
      "start": "{type} '{name}' has started.",
      "stuck": "{type} '{name}' has stopped.",
      "resumed": "{type} '{name}' resumed.",
      "manualStop": "{type} '{name}' stopped manually.",
      "shutdown": "{type} '{name}' was shut down with the server."
    }
  }
}
```
