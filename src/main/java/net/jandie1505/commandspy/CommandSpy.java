package net.jandie1505.commandspy;

import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.command.CommandExecuteEvent;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import net.jandie1505.commandspy.commands.SpyCommand;
import net.jandie1505.commandspy.data.SpyData;
import net.jandie1505.commandspy.data.SpyEvent;
import net.jandie1505.commandspy.redisbungee.RedisBungeeHook;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Plugin(
        id = "net-jandie1505-commandspy",
        name = "CommandSpy",
        version = "1.0-SNAPSHOT",
        url = "https://github.com/jandie1505/CommandSpy",
        description = "Allows admins to spy on commands players use",
        authors = {"jandie1505"},
        dependencies = {
                @Dependency(
                        id = "redisbungee",
                        optional = true
                )
        }
)
public class CommandSpy {
    private final ProxyServer proxy;
    private final Logger logger;
    private final SpyData consoleSpyData;
    private final Map<UUID, SpyData> spyingPlayers;
    @Nullable private final RedisBungeeHook redisBungeeHook;

    @Inject
    public CommandSpy(ProxyServer proxyServer, Logger logger) {
        this.proxy = proxyServer;
        this.logger = logger;
        this.consoleSpyData = new SpyData();
        this.spyingPlayers = new HashMap<>();
        this.redisBungeeHook = isRedisBungeeAvail() ? new RedisBungeeHook(this) : null;
    }

    // ----- SPY EVENT -----

    /**
     * This is called when an event listener creates a new spy event.<br/>
     * This will send the event to all other proxies and call the handle method on this proxy.
     * @param event spy event
     */
    private void sendSpyEvent(@NotNull SpyEvent event) {

        this.handleSpyEvent(event);

        if (redisBungeeHook != null) {
            this.redisBungeeHook.sendSpyEvent(event);
        }

    }

    /**
     * This method handles a spy event.<br/>
     * This is either called from sendSpyEvent, or if a spy event is sent from another proxy through redis.
     * @param event spy event
     */
    public void handleSpyEvent(@NotNull SpyEvent event) {

        for (Map.Entry<UUID, SpyData> entry : this.spyingPlayers.entrySet()) {
            Player player = this.proxy.getPlayer(entry.getKey()).orElse(null);
            if (player == null) continue;
            if (!entry.getValue().isSpyingOn(this.proxy, player, event)) continue;

            Component message = Component.empty()
                    .append(Component.text("[", NamedTextColor.GRAY))
                    .append(Component.text("SPY", NamedTextColor.GOLD))
                    .append(Component.text("]", NamedTextColor.GRAY))
                    .appendSpace()
                    .append(Component.text("[", NamedTextColor.GRAY))
                    .append(Component.text(event.proxy(), NamedTextColor.AQUA))
                    .append(Component.text("]", NamedTextColor.GRAY))
                    .appendSpace()
                    .append(Component.text("[", NamedTextColor.GRAY))
                    .append(Component.text(event.server(), NamedTextColor.AQUA))
                    .append(Component.text("]", NamedTextColor.GRAY))
                    .appendSpace()
                    .append(Component.text("[", NamedTextColor.GRAY));

            switch (event.type()) {
                case CHAT_MESSAGE -> message = message.append(Component.text("CHAT", NamedTextColor.YELLOW));
                case PROXY_COMMAND -> message = message.append(Component.text("PROXY COMMAND", NamedTextColor.DARK_AQUA));
                case SERVER_COMMAND -> message = message.append(Component.text("SERVER COMMAND", NamedTextColor.DARK_AQUA));
            }

            message = message.append(Component.text("]", NamedTextColor.GRAY)).appendSpace();

            Player target = this.proxy.getPlayer(event.sender()).orElse(null);
            String targetName = target != null ? target.getUsername() : event.sender().toString();

            message = message.append(Component.text(targetName + ":", NamedTextColor.GRAY)).appendNewline()
                    .append(Component.text("- Content: " + event.content(), NamedTextColor.GRAY)).appendNewline();

            String response = event.result().orElse(null);
            if (response != null) {
                message = message.append(Component.text("- Response: " + response, NamedTextColor.GRAY)).appendNewline();
            }

            message = message.append(Component.text("- Allowed: " + event.allowed(), NamedTextColor.GRAY));

            player.sendMessage(message);
        }

    }

    // ----- EVENT LISTENERS -----

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {

        this.proxy.getScheduler().buildTask(this, () -> {

            for (UUID playerId : Map.copyOf(this.spyingPlayers).keySet()) {

                Player player = this.proxy.getPlayer(playerId).orElse(null);
                if (player == null) {
                    this.spyingPlayers.remove(playerId);
                    continue;
                }

                if (!player.hasPermission("commandspy.use")) {
                    this.spyingPlayers.remove(playerId);
                    continue;
                }

            }

        }).repeat(30, TimeUnit.SECONDS).schedule();

        this.proxy.getCommandManager().register(
                this.proxy.getCommandManager().metaBuilder("spy").build(),
                new SpyCommand(this)
        );

        if (this.redisBungeeHook != null) {
            this.proxy.getEventManager().register(this, this.redisBungeeHook);
        }

        this.logger.info("CommandSpy (by jandie1505) has been initialized.");
    }

    @Subscribe(priority = Short.MAX_VALUE)
    public void onCommandExecute(CommandExecuteEvent event) {
        if (!(event.getCommandSource() instanceof Player player)) return;

        String proxy = getProxyName();
        if (proxy == null) proxy = "Proxy";

        String server = getServerName(player);
        if (server == null) server = "unknown";

        this.sendSpyEvent(new SpyEvent(
                event.getResult().isForwardToServer() ? SpyEvent.Type.SERVER_COMMAND : SpyEvent.Type.PROXY_COMMAND,
                proxy,
                server,
                player.getUniqueId(),
                event.getCommand(),
                event.getResult().getCommand(),
                event.getResult().isAllowed()
        ));
    }

    @Subscribe(priority = Short.MAX_VALUE)
    public void onPlayerChat(PlayerChatEvent event) {

        String proxy = getProxyName();
        if (proxy == null) proxy = "Proxy";

        String server = getServerName(event.getPlayer());
        if (server == null) server = "unknown";

        this.sendSpyEvent(new SpyEvent(
                SpyEvent.Type.CHAT_MESSAGE,
                proxy,
                server,
                event.getPlayer().getUniqueId(),
                event.getMessage(),
                Optional.empty(),
                event.getResult().isAllowed()
        ));
    }

    // ----- OTHER -----

    public ProxyServer getProxy() {
        return proxy;
    }

    public Logger getLogger() {
        return logger;
    }

    public Map<UUID, SpyData> getSpyingPlayers() {
        return spyingPlayers;
    }

    public SpyData getSpyingPlayer(@NotNull UUID playerId) {
        return this.spyingPlayers.computeIfAbsent(playerId, k -> new SpyData());
    }

    public UUID getPlayerId(String playerName) {

        try {
            return UUID.fromString(playerName);
        } catch (IllegalArgumentException e) {
            // continue if not a valid uuid
        }

        Player player = this.getProxy().getPlayer(playerName).orElse(null);

        if (player != null) {
            return player.getUniqueId();
        }

        return null;
    }

    // ----- UTILITIES -----

    public static @Nullable String getServerName(@NotNull Player player) {
        ServerConnection connection = player.getCurrentServer().orElse(null);
        if (connection == null) return null;
        return connection.getServerInfo().getName();
    }

    public static @Nullable String getProxyName() {
        if (!isRedisBungeeAvail()) return null;
        return RedisBungeeHook.getProxyName();
    }

    public static boolean isRedisBungeeAvail() {
        try {
            Class.forName("com.imaginarycode.minecraft.redisbungee.RedisBungeeAPI");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

}
