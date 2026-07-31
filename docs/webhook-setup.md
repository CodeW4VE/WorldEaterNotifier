---
sidebar_position: 3
---

# Webhook Mode


Webhook mode is the default delivery method and the simplest to set up — no bot, no permissions, just a Discord webhook URL.

## 1. Create a webhook in Discord

1. Open your Discord server and select the text channel where notifications should appear.
2. Click **Edit Channel** (gear icon) → **Integrations** → **Webhooks**.
3. Click **New Webhook**, give it a name (e.g. `WorldEaterNotifier`), and optionally a channel avatar.
4. Click the webhook and press **Copy Webhook URL**. It looks like:

```
https://discord.com/api/webhooks/1234567890/abcdefghijklmnopqrstuvwxyz
```

> Treat the webhook URL like a password — anyone with it can post to that channel.

## 2. Set the webhook URL in-game

```
/worldeater settings setWebhookUrl <url>
```

The URL is shared across all three machine types, so you only set it once.

## 3. (Optional) Set the ping role

To mention a Discord role when events happen, create a role (Server Settings → Roles → Create Role) and copy its ID:

1. Enable **Developer Mode** in Discord: User Settings → Advanced → Developer Mode.
2. Right-click the role in Server Settings → **Copy Role ID**.

Then in-game:

```
/worldeater settings setPingRoleId <role-id>
```

Leave the role ID empty or set to `0` to disable all mentions.

## 4. Control which events mention the role

Each event can be independently toggled:

```
/worldeater settings discordPings enable true
/worldeater settings discordPings onStart true
/worldeater settings discordPings onStuck true
/worldeater settings discordPings onResumed true
/worldeater settings discordPings onStop true
/worldeater settings discordPings onShutdown true
```

Set any of them to `false` and that event will still be sent to Discord, but without the role mention.

## 5. Test it

Create a machine and start it:

```
/worldeater create test 0 0 0 5 5 5
/worldeater start test
```

You should see a "started" message in the channel. Stop it and a "manually stopped" message follows.

## 6. Verify your settings

```
/worldeater settings show
```

Confirms the current mode, webhook URL (masked), ping role, thresholds, and ping toggles.

## Notes

- Webhook mode is the default (`"notificationMode": "webhook"` in `config/worldeaternotifier.json`).
- Changes to the webhook URL, role, and message templates apply **immediately** — no server restart needed (latest builds).
- Webhook mode has no slash commands or toggle button; those are [bot mode](bot-setup) features.
