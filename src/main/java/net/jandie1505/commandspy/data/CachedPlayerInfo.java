package net.jandie1505.commandspy.data;

import java.util.UUID;

public class CachedPlayerInfo {
    private final UUID uniqueId;
    private final String name;
    private final String serverName;

    public CachedPlayerInfo(UUID uniqueId, String name, String serverName) {
        this.uniqueId = uniqueId;
        this.name = name;
        this.serverName = serverName;
    }

    public UUID getUniqueId() {
        return uniqueId;
    }

    public String getName() {
        return name;
    }

    public String getServerName() {
        return serverName;
    }
}
