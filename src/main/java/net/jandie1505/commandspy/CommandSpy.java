package net.jandie1505.commandspy;

import com.imaginarycode.minecraft.redisbungee.RedisBungeeAPI;
import com.imaginarycode.minecraft.redisbungee.events.PubSubMessageEvent;
import net.jandie1505.commandspy.commands.SpyCommand;
import net.jandie1505.commandspy.data.SpyData;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class CommandSpy extends Plugin implements Listener {
    private JSONObject config;
    private Map<UUID, SpyData> spyingPlayers;

    @Override
    public void onEnable() {

        // INIT

        this.config = new JSONObject();

        this.spyingPlayers = Collections.synchronizedMap(new HashMap<>());

        // CONFIG

        File configFile = new File(this.getDataFolder(), "config.json");

        if (!configFile.exists()) {

            try {
                configFile.getParentFile().mkdirs();
                configFile.createNewFile();
            } catch (IOException e) {
                this.getLogger().warning("Could not load config");
            }

        }

        this.loadConfig(configFile);
        this.saveConfig(configFile);

        // REDIS

        if (this.isRedisBungeeLoaded()) {
            RedisBungeeAPI.getRedisBungeeApi().registerPubSubChannels("net.jandie1505.commandspy");
        }

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

    }

    public void loadConfig(File file) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));

            StringBuilder sb = new StringBuilder();
            String line = br.readLine();

            while (line != null) {
                sb.append(line);
                sb.append(System.lineSeparator());
                line = br.readLine();
            }

            String out = sb.toString();

            JSONObject jsonConfig = new JSONObject(out);

            for (String key : jsonConfig.keySet()) {
                this.config.put(key, jsonConfig.get(key));
            }

            this.getLogger().fine("Config loaded");
        } catch (IOException | JSONException e) {
            this.getLogger().warning("Failed to load config");
        }
    }

    public void saveConfig(File file) {
        try {
            FileWriter writer = new FileWriter(file);
            writer.write(this.config.toString(4));
            writer.flush();
            writer.close();

            this.getLogger().fine("Config saved");
        } catch (IOException e) {
            this.getLogger().warning("Failed to save config");
        }
    }

    public SpyData getSpyData(UUID playerId) {
        SpyData spyData = this.spyingPlayers.get(playerId);

        if (spyData == null) {
            spyData = new SpyData();
            this.spyingPlayers.put(playerId, spyData);
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

                if (this.isRedisBungeeLoaded()) {
                    text.append(RedisBungeeAPI.getRedisBungeeApi().getProxyId()).color(ChatColor.AQUA).event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder().append("Current Proxy").create()));
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
                text.append("COMMAND").color(ChatColor.AQUA).event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder().append("Proxy Command").color(ChatColor.GOLD).create()));
            } else if (command) {
                text.append("COMMAND").color(ChatColor.DARK_AQUA);
            } else {
                text.append("CHAT").color(ChatColor.YELLOW);
            }

            text.append("] ").color(ChatColor.GRAY);

            ProxiedPlayer otherPlayer = this.getProxy().getPlayer(sender);

            if (sender == null) {
                text.append(senderName);
            } else {
                text.append(otherPlayer.getName());
            }

            text.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder().append("UUID: " + sender + "\nOn this proxy: " + (otherPlayer != null)).create()));
            text.color(ChatColor.GRAY);
            text.append(": ").color(ChatColor.GRAY);
            text.append(message).color(ChatColor.GRAY);

            if (cancelled) {
                text.strikethrough(true);
                text.append(" cancelled").color(ChatColor.RED);
            }

            player.sendMessage(text.create());

        }

    }

    public boolean isRedisBungeeLoaded() {

        try {
            Class.forName("com.imaginarycode.minecraft.redisbungee.RedisBungeeAPI");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }

    }

    /**
     * Sends a spy event.
     */
    public void sendRedisSpyEvent(boolean command, boolean proxyCommand, boolean cancelled, String serverName, UUID sender, String senderName, String chatMessage) {

        if (!isRedisBungeeLoaded()) {
            return;
        }

        try {

            JSONObject message = new JSONObject();

            message.put("command", command);
            message.put("proxyCommand", proxyCommand);
            message.put("cancelled", cancelled);
            message.put("serverName", serverName);
            message.put("sender", sender.toString());
            message.put("senderName", senderName);
            message.put("message", chatMessage);

            JSONObject json = new JSONObject();

            json.put("proxy", RedisBungeeAPI.getRedisBungeeApi().getProxyId());
            json.put("type", "spyEvent");
            json.put("message", message);

            RedisBungeeAPI.getRedisBungeeApi().sendChannelMessage("net.jandie1505.commandspy", json.toString());

        } catch (Exception e) {
            this.getLogger().log(Level.WARNING, "Could not send redis message", e);
        }

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

        return null;
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

        this.getProxy().getScheduler().runAsync(this, () -> {

            if (this.isRedisBungeeLoaded()) {
                this.sendRedisSpyEvent(event.isCommand(), event.isProxyCommand(), event.isCancelled(), sender.getServer().getInfo().getName(), sender.getUniqueId(), sender.getName(), event.getMessage());
            }

        });

    }

    @EventHandler
    public void onDisconnect(PlayerDisconnectEvent event) {
        this.spyingPlayers.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPubSubMessage(PubSubMessageEvent event) {

        if (!event.getChannel().equals("net.jandie1505.commandspy")) {
            return;
        }

        try {

            JSONObject json = new JSONObject(event.getMessage());

            if (json.optString("type") == null) {
                return;
            }

            if (json.optJSONObject("message") == null) {
                return;
            }

            String proxy = json.optString("proxy");

            if (proxy == null || proxy.equals(RedisBungeeAPI.getRedisBungeeApi().getProxyId())) {
                return;
            }

            String type = json.optString("type");
            JSONObject message = json.optJSONObject("message");

            switch (type) {
                case "spyEvent" -> {

                    boolean command = message.optBoolean("command", false);
                    boolean proxyCommand = message.optBoolean("proxyCommand", false);
                    boolean cancelled = message.optBoolean("cancelled", false);
                    String serverName = message.optString("serverName");
                    UUID sender;

                    try {
                        sender = UUID.fromString(message.optString("sender", ""));
                    } catch (IllegalArgumentException e) {
                        return;
                    }

                    String senderName = message.optString("senderName");
                    String chatMessage = message.optString("message");

                    if (serverName == null || sender == null || senderName == null || chatMessage == null) {
                        return;
                    }

                    this.spyEvent(proxy, command, proxyCommand, cancelled, serverName, sender, senderName, chatMessage);

                }
            }

        } catch (Exception e) {
            this.getLogger().log(Level.WARNING, "Exception while decoding redis message", e);
        }

    }

}
