---
sidebar_position: 4
---

# Bot Mode


Bot mode replaces the webhook with a real Discord bot. It adds **slash commands**, an interactive **Toggle Ping** button, and more granular control — at the cost of a bit more setup.

## 1. Create the Discord application

1. Go to the [Discord Developer Portal](https://discord.com/developers/applications).
2. Click **New Application**, give it a name, and copy the **Application ID**.
3. Open the **Bot** tab:
   - Click **Reset Token** and copy the new **token** (this is the bot token).
   - Toggle on **SERVER MEMBERS INTENT** — required for the toggle button and role checks.

> Treat the bot token like a password. If it leaks, reset it immediately.

## 2. Invite the bot to your server

Open the **OAuth2 → URL Generator** tab:

1. Select **bot** and **applications.commands** scopes.
2. Under **Bot Permissions**, select:
   - **View Channels**
   - **Send Messages**
   - **Embed Links**
   - **Manage Roles** (required to assign/remove the ping role via the toggle button)
3. Copy the generated URL and open it in a browser to invite the bot to your server.

## 3. Get the guild and channel IDs

Enable **Developer Mode** in Discord (User Settings → Advanced).

- **Guild ID**: right-click your server name → **Copy Server ID**.
- **Channel ID**: right-click the notification channel → **Copy Channel ID**.
- **Role ID**: Server Settings → right-click a role → **Copy Role ID**.

## 4. Configure the bot in-game

```
/worldeater settings setBotToken <token>
/worldeater settings setGuildId <guild-id>
/worldeater settings setChannelId <channel-id>
/worldeater settings setPingRoleId <role-id>
/worldeater settings setNotificationMode bot
```

Setting the bot token restarts the bot automatically. Switching to `bot` mode starts the bot and registers its slash commands (this happens when the bot connects — give it a few seconds).

> **Start guard**: machines won't start until `botToken`, `guildId`, and `channelId` are all set.

## 5. Discord slash commands

Commands are registered on your server as soon as the bot connects.

### Manage machines

| Command | Description |
|---|---|
| `/worldeater start <name>` | Start a machine (name has autocomplete). |
| `/worldeater stop <name>` | Stop a machine (name has autocomplete). |
| `/worldeater list` | List world eaters with active/inactive status. |

The same applies for `/trencher` and `/bedrockbreaker`. Starting/stopping from Discord also broadcasts the event to in-game chat.

### Configure the bot (`/config`)

| Command | Description |
|---|---|
| `/config ping-role <role>` | Set the role used for pings. |
| `/config channel <channel>` | Set the notification channel. |
| `/config subscription-button <true\|false>` | Show/hide the "Toggle Ping" button on start messages. |
| `/config member-discord-role <role>` | Role that can use start/stop/list slash commands. |
| `/config pings` | Interactive ping-settings editor (see below). |

### `/config pings` interactive flow

1. Running `/config pings` shows a dropdown with **World Eater**, **Trencher**, **Bedrock Breaker**.
2. Pick a type → an embed shows the current ping settings and a dropdown of what to change (enabled, onStart, onStop, onStuck, onResumed, onShutdown).
3. Pick a setting → **True / False** buttons appear.
4. Clicking one saves the change and returns you to the setting dropdown, so you can keep adjusting without re-running the command.

## 6. The "Toggle Ping" button

On every **start** message, the bot adds a **🔔 Toggle Ping** button:

- Clicking it adds or removes the ping role for you.
- The reply is ephemeral (only you see it).
- If the bot can't manage the role, you'll get an error — the bot's role must be **above** the ping role in Server Settings → Roles.

Hide the button entirely with `/config subscription-button false` or `/worldeater settings showSubscriptionButton false`.

## 7. Discord permission commands

| Action | Who can do it |
|---|---|
| `/config` (all subcommands) | **Administrator** only |
| `/worldeater` `/trencher` `/bedrockbreaker` (start/stop/list) | Member of `member-discord-role` **or** Administrator |
| Toggle Ping button | Anyone (affects only the clicker) |

The Discord commands are **not** affected by the in-game whitelist — Discord access is based purely on Discord roles.

## Notes

- Notifications queue internally until the bot's WebSocket connects; if the bot is briefly offline, up to 50 messages are buffered and flushed on reconnect.
- Changes to the channel, roles, and messages apply **immediately** — no bot restart needed.
- On server shutdown the bot stops and a shutdown notification is sent for each active machine.
