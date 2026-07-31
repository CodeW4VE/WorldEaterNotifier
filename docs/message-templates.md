---
sidebar_position: 5
---

# Message Templates


Every Discord notification text is configurable per machine type. This works in both webhook and bot modes.

## The 5 messages

Each machine type has its own `messages` block in `config/worldeaternotifier.json` with five templates:

| Key | Event |
|---|---|
| `start` | Machine started. |
| `stuck` | Machine stopped due to an obstruction (no activity for `stopTimeout` seconds). |
| `resumed` | Machine started moving again after being stuck. |
| `manualStop` | Machine stopped manually with `/stop` (or Discord slash command). |
| `shutdown` | Machine was active when the server shut down. |

## Placeholders

| Placeholder | Replaced with |
|---|---|
| `{type}` | The machine type name (`WorldEater`, `Trencher`, `BedrockBreaker`). |
| `{name}` | The machine instance name. |

## Defaults

```json
"messages": {
  "start": "{type} **'{name}'** has started.",
  "stuck": "{type} **'{name}'** has stopped due to an obstruction.",
  "resumed": "{type} **'{name}'** has started again.",
  "manualStop": "{type} **'{name}'** was stopped manually.",
  "shutdown": "{type} **'{name}'** was shut down with the server and may have broken."
}
```

## How to edit

1. Stop the server (or be ready to restart it later).
2. Open `config/worldeaternotifier.json` in a text editor.
3. Edit the `messages` block for the machine type you want (under `worldEaterSettings`, `trencherSettings`, or `bedrockBreakerSettings`).

Example — customize the world eater stuck message:

```json
{
  "worldEaterSettings": {
    "messages": {
      "stuck": "{type} '{name}' is blocked! Check the front wall."
    }
  }
}
```

4. Save the file. On the latest builds, message changes apply immediately; otherwise restart the server.

> Any message field that's missing in the JSON falls back to its default.

## Discord markdown works

Discord markdown is supported in the templates — bold, italics, code blocks, emoji, channel/role mentions:

```json
"start": "{type} **'{name}'** is now being monitored — <@&1234567890>"
```

## Example: full config with custom messages

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
