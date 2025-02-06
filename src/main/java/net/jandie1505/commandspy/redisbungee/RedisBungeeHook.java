package net.jandie1505.commandspy.redisbungee;

import com.imaginarycode.minecraft.redisbungee.RedisBungeeAPI;
import com.imaginarycode.minecraft.redisbungee.events.PubSubMessageEvent;
import com.velocitypowered.api.event.Subscribe;
import net.jandie1505.commandspy.CommandSpy;
import net.jandie1505.commandspy.data.SpyEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

@SuppressWarnings("ClassCanBeRecord")
public class RedisBungeeHook {
    @NotNull private final CommandSpy plugin;

    public RedisBungeeHook(@NotNull CommandSpy plugin) {
        this.plugin = plugin;
    }

    @Subscribe
    public void onRedisPubSubMessage(PubSubMessageEvent event) {
        if (!event.getChannel().equals("net-jandie1505-commandspy")) return;

        SpyEvent spyEvent = null;
        try {
            JSONObject data = new JSONObject(event.getMessage());
            spyEvent = SpyEvent.deserialize(data);
        } catch (Exception e) {
            this.plugin.getLogger().warn("Failed to deserialize PubSubMessage to SpyEvent", e);
            return;
        }

        this.plugin.handleSpyEvent(spyEvent);
    }

    public void sendSpyEvent(SpyEvent event) {
        RedisBungeeAPI.getRedisBungeeApi().sendChannelMessage("net-jandie1505-commandspy", event.serialize().toString());
    }

    public CommandSpy getPlugin() {
        return plugin;
    }

    public static @Nullable String getProxyName() {
        return RedisBungeeAPI.getRedisBungeeApi().getProxyId();
    }

}
