package net.jandie1505.commandspy;

import net.jandie1505.commandspy.commands.SpyCommand;
import net.jandie1505.commandspy.data.SpyData;
import net.jandie1505.commandspy.redis.RedisManager;
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
    private String proxyName;
    private JSONObject config;
    private Map<UUID, SpyData> spyingPlayers;
    private RedisManager redisManager;

    @Override
    public void onEnable() {

        // INIT

        this.config = new JSONObject();
        this.config.put("enableRedis", false);
        this.config.put("redisUrl", "127.0.0.1");
        this.config.put("redisUsername", "");
        this.config.put("redisPassword", "");
        this.config.put("customRedisChannel", "");
        this.config.put("proxyName", "");

        this.spyingPlayers = Collections.synchronizedMap(new HashMap<>());

        // CONFIG

        this.loadConfig(new File(this.getDataFolder(), "config.json"));
        this.saveConfig(new File(this.getDataFolder(), "config.json"));

        // PROXY NAME

        this.proxyName = null;

        if (!this.config.optString("proxyName", "").equals("")) {
            this.proxyName = this.config.optString("proxyName", "");
        }

        // REDIS

        if (this.redisManager != null) {
            this.redisManager.close();
            this.redisManager = null;
        }

        if (this.config.optBoolean("enableRedis", false)) {

            try {

                String channelName = this.config.optString("customRedisChannel", "");

                if (channelName.equals("")) {
                    channelName = "net.jandie1505.commandspy";
                }

                String username = null;
                String password = null;

                if (!this.config.optString("redisUsername", "").equals("")) {
                    username = this.config.optString("redisUsername");
                }

                if (!this.config.optString("redisPassword", "").equals("")) {
                    password = this.config.optString("redisPassword");
                }

                this.redisManager = new RedisManager(this, this.config.optString("redisUrl", ""), username, password, channelName);

            } catch (Exception e) {
                this.getLogger().log(Level.WARNING, "Could not connect to redis", e);
            }

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

        this.redisManager.close();
        this.redisManager = null;

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

        if (this.redisManager != null) {
            this.redisManager.sendSpyEvent(event.isCommand(), event.isProxyCommand(), event.isCancelled(), sender.getServer().getInfo().getName(), sender.getUniqueId(), sender.getName(), event.getMessage());
        }

    }

    @EventHandler
    public void onDisconnect(PlayerDisconnectEvent event) {
        this.spyingPlayers.remove(event.getPlayer().getUniqueId());
    }

    public String getProxyName() {
        return this.proxyName;
    }

}
