package net.jandie1505.commandspy.redis;

import net.jandie1505.commandspy.CommandSpy;
import org.json.JSONException;
import org.json.JSONObject;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

import java.util.UUID;
import java.util.logging.Level;

public class RedisManager {
    private final CommandSpy plugin;
    private final Jedis redis;
    private final String channelName;

    public RedisManager(CommandSpy plugin, String url, String channelName) {

        // INIT

        this.plugin = plugin;
        this.redis = new Jedis(url);
        this.channelName = channelName;

        // LISTENER

        this.redis.subscribe(new JedisPubSub() {

            @Override
            public void onMessage(String channel, String message) {

                if (!channel.equals(channelName)) {
                    return;
                }

                try {
                    handleRedisMessage(new JSONObject(message));
                } catch (JSONException e) {
                    // IGNORED
                }

            }

        }, this.channelName);

    }

    /**
     * Handles redis messages
     * @param json message
     */
    private void handleRedisMessage(JSONObject json) {

        try {

            if (json.optString("type") == null) {
                return;
            }

            if (json.optJSONObject("message") == null) {
                return;
            }

            String type = json.optString("type");
            JSONObject message = json.optJSONObject("message");

            switch (type) {
                case "spyEvent" -> {

                    String proxyName = message.optString("proxyName");
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

                    if (proxyName == null || serverName == null || sender == null || senderName == null || chatMessage == null) {
                        return;
                    }

                    this.plugin.spyEvent(proxyName, command, proxyCommand, cancelled, serverName, sender, senderName, chatMessage);

                }
            }

        } catch (Exception e) {
            this.plugin.getLogger().log(Level.WARNING, "Exception while decoding redis message", e);
        }

    }

    /**
     * Sends a spy event.
     */
    public void sendSpyEvent(String proxyName, boolean command, boolean proxyCommand, boolean cancelled, String serverName, UUID sender, String senderName, String chatMessage) {

        try {

            JSONObject message = new JSONObject();

            message.put("proxyName", proxyName);
            message.put("command", command);
            message.put("proxyCommand", proxyCommand);
            message.put("cancelled", cancelled);
            message.put("serverName", serverName);
            message.put("sender", sender.toString());
            message.put("senderName", senderName);
            message.put("message", chatMessage);

            JSONObject json = new JSONObject();

            json.put("type", "spyEvent");
            json.put("message", message);

            this.redis.publish(this.channelName, json.toString());

        } catch (Exception e) {
            this.plugin.getLogger().log(Level.WARNING, "Could not send redis message", e);
        }

    }

    /**
     * Closes redis connection.
     */
    public void close() {

        try {
            this.redis.close();
        } catch (Exception e) {
            this.plugin.getLogger().log(Level.SEVERE, "Exception while closing redis connection", e);
        }

    }
}
