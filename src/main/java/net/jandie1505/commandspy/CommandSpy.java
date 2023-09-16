package net.jandie1505.commandspy;

import net.jandie1505.commandspy.commands.SpyCommand;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;
import org.json.JSONObject;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class CommandSpy extends Plugin implements Listener {
    private String proxyName;
    private JSONObject config;
    private Map<UUID, SpyData> spyingPlayers;
    private Map<UUID, CachedPlayerInfo> cachedPlayers;

    @Override
    public void onEnable() {

        // INIT

        this.config = new JSONObject();
        this.config.put("enableRedis", false);
        this.config.put("redisHost", "127.0.0.1");
        this.config.put("cachePlayerNames", true);

        this.spyingPlayers = Collections.synchronizedMap(this.spyingPlayers);
        this.cachedPlayers = Collections.synchronizedMap(this.cachedPlayers);

        // LISTENER

        this.getProxy().getPluginManager().registerListener(this, this);

        // COMMANDS

        this.getProxy().getPluginManager().registerCommand(this, new SpyCommand(this));

        // TASKS

        this.getProxy().getScheduler().schedule(this, () -> {

            for (UUID playerId : Map.copyOf(this.spyingPlayers).keySet()) {
                ProxiedPlayer player = this.getProxy().getPlayer(playerId);

                if (player == null) {
                    this.spyingPlayers.remove(playerId);
                }

            }

        }, 1, 30, TimeUnit.SECONDS);

        // FINISHED

    }

    @Override
    public void onDisable() {
        this.spyingPlayers = null;
        this.cachedPlayers = null;
    }

    public void updateSpyingPlayer(UUID playerId, SpyData spyData) {

        if (playerId == null) {
            return;
        }

        if (spyData == null) {
            this.spyingPlayers.remove(playerId);
            return;
        }

        this.spyingPlayers.put(playerId, spyData);
    }

    public SpyData getSpyData(UUID playerId) {
        SpyData spyData = this.spyingPlayers.get(playerId);

        if (spyData == null) {
            return new SpyData();
        }

        return spyData;
    }

    /**
     * Spy event.
     * This method will be called by the event listener or the redis system.
     * @param proxyName Name of the proxy (null for this proxy)
     * @param command If the event is a command
     * @param proxyCommand If the event is a valid proxy command
     * @param cancelled If the event has been cancelled
     * @param serverName The server name (not null)
     * @param sender The uuid of the sender (not null)
     * @param senderName The name of the sender (can be null)
     * @param message The message (not null)
     */
    public void spyEvent(final String proxyName, final boolean command, final boolean proxyCommand, final boolean cancelled, final String serverName, final UUID sender, final String senderName, final String message) {

        if (serverName == null || sender == null || message == null) {
            return;
        }

        for (ProxiedPlayer player : List.copyOf(this.getProxy().getPlayers())) {

            if (!player.hasPermission("commandspy.spy")) {
                continue;
            }

            SpyData spyData = this.getSpyData(player.getUniqueId());

            if (!command && !proxyCommand && !spyData.isSpyChat()) {
                continue;
            }

            if (command && !proxyCommand && !spyData.isSpyCommands()) {
                continue;
            }

            if (proxyCommand && !spyData.isSpyProxyCommands()) {
                continue;
            }

            if (!spyData.isSpyAllPlayers() && !(spyData.isSpyCurrentServerPlayers() && player.getServer().getInfo().getName().equals(serverName)) && !spyData.isTarget(sender)) {
                continue;
            }

            ComponentBuilder text = new ComponentBuilder()
                    .append("[")
                    .color(ChatColor.GRAY)
                    .append("SPY")
                    .color(ChatColor.GOLD)
                    .append("] [")
                    .color(ChatColor.GRAY)
                    .append(serverName);

            if (serverName.equals(player.getServer().getInfo().getName())) {
                text.color(ChatColor.GREEN);
            } else {
                text.color(ChatColor.DARK_GRAY);
            }

            text.append("] [").color(ChatColor.GRAY);

            if (proxyName == null) {

                if (this.proxyName != null && !this.proxyName.equals("")) {
                    text.append("EXT").color(ChatColor.AQUA).event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder().append("Current Proxy").create()));
                    text.append("] [").color(ChatColor.GRAY);
                }

            } else {

                if (!proxyName.equals("")) {
                    text.append(proxyName).event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder().append("Other Proxy").create()));
                } else {
                    text.append("EXT").event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder().append("Other Proxy (with empty name)").create()));
                }

                text.color(ChatColor.DARK_AQUA);
                text.append("] [").color(ChatColor.GRAY);

            }

            if (proxyCommand) {
                text.append("COMMAND").color(ChatColor.GOLD).event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder().append("Proxy Command").color(ChatColor.GOLD).create()));
            } else if (command) {
                text.append("COMMAND").color(ChatColor.DARK_AQUA);
            } else {
                text.append("CHAT").color(ChatColor.DARK_GRAY);
            }

            text.append("] ").color(ChatColor.GRAY);

            ProxiedPlayer otherPlayer = this.getProxy().getPlayer(sender);

            if (sender == null) {
                text.append(senderName);
            } else {
                text.append(otherPlayer.getName());
            }

            text.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder().append("UUID: " + sender + "\nOn this proxy: " + (otherPlayer != null)).create()));
            text.color(ChatColor.DARK_GRAY);
            text.append(": ").color(ChatColor.GRAY);
            text.append(message).color(ChatColor.GRAY);

            if (cancelled) {
                text.strikethrough(true);
                text.append(" cancelled").color(ChatColor.RED);
            }

            player.sendMessage(text.create());

        }

    }

    public CachedPlayerInfo getCachedPlayer(UUID playerId) {
        ProxiedPlayer player = this.getProxy().getPlayer(playerId);

        if (player != null) {
            CachedPlayerInfo cachedPlayerInfo = this.cachedPlayers.get(playerId);

            if (cachedPlayerInfo == null) {
                cachedPlayerInfo = new CachedPlayerInfo(player.getUniqueId(), player.getName(), player.getServer().getInfo().getName());
                this.cachedPlayers.put(player.getUniqueId(), cachedPlayerInfo);
            }

            return cachedPlayerInfo;
        }

        return this.cachedPlayers.get(playerId);
    }

    public UUID getPlayerId(String playerName) {

        try {
            return UUID.fromString(playerName);
        } catch (IllegalArgumentException e) {
            // continue if not a valid uuid
        }

        ProxiedPlayer player = this.getProxy().getPlayer(playerName);

        if (player != null) {
            return player.getUniqueId();
        }

        if (!this.config.optBoolean("cachePlayerNames", false)) {
            return null;
        }

        for (UUID otherPlayerId : Map.copyOf(this.cachedPlayers).keySet()) {
            CachedPlayerInfo otherCachedPlayerInfo = this.cachedPlayers.get(otherPlayerId);

            if (otherCachedPlayerInfo == null) {
                continue;
            }

            if (otherCachedPlayerInfo.getName().equals(playerName)) {
                return otherPlayerId;
            }

        }

        return null;
    }

    public Map<UUID, CachedPlayerInfo> getCachedPlayers() {
        return Map.copyOf(this.cachedPlayers);
    }

    /**
     * This method handles the local command spy.
     */
    @EventHandler
    public void onChat(ChatEvent event) {

        if (!(event.getSender() instanceof ProxiedPlayer)) {
            return;
        }

        ProxiedPlayer sender = (ProxiedPlayer) event.getSender();

        this.spyEvent(null, event.isCommand(), event.isProxyCommand(), event.isCancelled(), sender.getServer().getInfo().getName(), sender.getUniqueId(), sender.getName(), event.getMessage());

    }

    @EventHandler
    public void onPostLogin(PostLoginEvent event) {

        if (!this.config.optBoolean("cachePlayerNames", false)) {
            return;
        }

        this.cachedPlayers.put(event.getPlayer().getUniqueId(), new CachedPlayerInfo(event.getPlayer().getUniqueId(), event.getPlayer().getName(), null));

    }

    @EventHandler
    public void onDisconnect(PlayerDisconnectEvent event) {

        this.spyingPlayers.remove(event.getPlayer().getUniqueId());

        if (!this.config.optBoolean("cachePlayerNames", false)) {
            return;
        }

        this.cachedPlayers.remove(event.getPlayer().getUniqueId());

    }

}
