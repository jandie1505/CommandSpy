package net.jandie1505.commandspy;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Content;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import org.json.JSONObject;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class CommandSpy extends Plugin {
    private JSONObject config;
    private Map<UUID, SpyData> spyingPlayers;

    @Override
    public void onEnable() {

        this.config = new JSONObject();
        this.config.put("enableRedis", false);
        this.config.put("redisHost", "127.0.0.1");

        this.spyingPlayers = Collections.synchronizedMap(this.spyingPlayers);

        this.getProxy().getScheduler().schedule(this, () -> {

            for (UUID playerId : Map.copyOf(this.spyingPlayers).keySet()) {
                ProxiedPlayer player = this.getProxy().getPlayer(playerId);

                if (player == null) {
                    this.spyingPlayers.remove(playerId);
                }

            }

        }, 1, 30, TimeUnit.SECONDS);

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

    public void spyEvent(final boolean external, final int type, final boolean command, final boolean proxyCommand, final boolean cancelled, final String serverName, final UUID sender, final String senderName, final String message) {

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

}
