package net.jandie1505.commandspy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class SpyData {
    private final List<UUID> targets;
    private boolean spyChat;
    private boolean spyCommands;
    private boolean spyProxyCommands;
    private boolean spyAllPlayers;
    private boolean spyCurrentServerPlayers;

    public SpyData() {
        this.targets = Collections.synchronizedList(new ArrayList<>());
        this.spyChat = true;
        this.spyCommands = true;
        this.spyProxyCommands = true;
        this.spyAllPlayers = false;
        this.spyCurrentServerPlayers = false;
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

    public boolean isSpyChat() {
        return spyChat;
    }

    public void setSpyChat(boolean spyChat) {
        this.spyChat = spyChat;
    }

    public boolean isSpyCommands() {
        return spyCommands;
    }

    public void setSpyCommands(boolean spyCommands) {
        this.spyCommands = spyCommands;
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
}
