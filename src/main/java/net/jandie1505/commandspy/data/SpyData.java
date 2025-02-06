package net.jandie1505.commandspy.data;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import net.jandie1505.commandspy.CommandSpy;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class SpyData {
    @NotNull private final List<UUID> targets;
    private boolean spyAll;
    private boolean spyChat;
    private boolean spyServerCommands;
    private boolean spyProxyCommands;
    private boolean spyAllPlayers;
    private boolean spyCurrentServerPlayers;

    public SpyData() {
        this.targets = Collections.synchronizedList(new ArrayList<>());
        this.spyAll = false;
        this.spyChat = true;
        this.spyServerCommands = true;
        this.spyProxyCommands = true;
        this.spyAllPlayers = false;
        this.spyCurrentServerPlayers = false;
    }

    public boolean isSpyAll() {
        return spyAll;
    }

    public void setSpyAll(boolean spyAll) {
        this.spyAll = spyAll;
    }

    public List<UUID> getTargets() {
        return List.copyOf(this.targets);
    }

    public void addTarget(UUID playerId) {
        this.targets.add(playerId);
    }

    public void removeTarget(UUID playerId) {
        this.targets.remove(playerId);
    }

    public boolean isTarget(UUID playerId)  {
        return this.targets.contains(playerId);
    }

    public void clearTargets() {
        this.targets.clear();
    }

    public boolean isSpyChat() {
        return spyChat;
    }

    public void setSpyChat(boolean spyChat) {
        this.spyChat = spyChat;
    }

    public boolean isSpyServerCommands() {
        return spyServerCommands;
    }

    public void setSpyServerCommands(boolean spyServerCommands) {
        this.spyServerCommands = spyServerCommands;
    }

    public boolean isSpyProxyCommands() {
        return spyProxyCommands;
    }

    public void setSpyProxyCommands(boolean spyProxyCommands) {
        this.spyProxyCommands = spyProxyCommands;
    }

    public boolean isSpyAllPlayers() {
        return spyAllPlayers;
    }

    public void setSpyAllPlayers(boolean spyAllPlayers) {
        this.spyAllPlayers = spyAllPlayers;
    }

    public boolean isSpyCurrentServerPlayers() {
        return spyCurrentServerPlayers;
    }

    public void setSpyCurrentServerPlayers(boolean spyCurrentServerPlayers) {
        this.spyCurrentServerPlayers = spyCurrentServerPlayers;
    }

    public boolean isSpyingOn(@NotNull ProxyServer proxyServer, @NotNull Player player, SpyEvent event) {

        // Check if player is spying on that specific category
        if (!this.spyAll) {
            if (event.type() == SpyEvent.Type.CHAT_MESSAGE && !this.spyChat) return false;
            if (event.type() == SpyEvent.Type.PROXY_COMMAND && !this.spyProxyCommands) return false;
            if (event.type() == SpyEvent.Type.SERVER_COMMAND && !this.spyServerCommands) return false;
        }

        // Check if player spies on all players
        if (this.spyAllPlayers) return true;

        // Check if player is spying on the specific player that fired the event
        if (this.targets.contains(event.sender())) return true;

        String serverName = CommandSpy.getServerName(player);

        Player target = proxyServer.getPlayer(event.sender()).orElse(null);
        String targetServerName = target != null ? CommandSpy.getServerName(target) : null;

        // Check if the player is spying on all players on the server
        return this.spyCurrentServerPlayers && serverName != null && serverName.equals(targetServerName);
    }

}
