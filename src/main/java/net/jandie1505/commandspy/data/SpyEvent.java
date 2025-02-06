package net.jandie1505.commandspy.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Optional;
import java.util.UUID;

public record SpyEvent(
        @NotNull Type type,
        @NotNull String proxy,
        @NotNull String server,
        @NotNull UUID sender,
        @NotNull String content,
        @NotNull Optional<String> result,
        boolean allowed
) {

    public @NotNull JSONObject serialize() {
        JSONObject json = new JSONObject();

        json.put("type", this.type.toString());
        json.put("proxy", this.proxy);
        json.put("server", this.server);
        json.put("sender", this.sender.toString());
        json.put("content", this.content);
        json.put("result_available", this.result.isPresent());
        json.put("result", this.result.orElse(""));
        json.put("allowed", this.allowed);

        return json;
    }

    public static @NotNull SpyEvent deserialize(@NotNull JSONObject json) throws JSONException {
        Type type = Type.valueOf(json.getString("type"));
        String proxy = json.getString("proxy");
        String server = json.getString("server");
        UUID sender = UUID.fromString(json.getString("sender"));
        String content = json.getString("content");
        boolean resultAvailable = json.getBoolean("result_available");
        String result = json.getString("result");
        boolean allowed = json.getBoolean("allowed");
        return new SpyEvent(type, proxy, server, sender, content, resultAvailable ? Optional.of(result) : Optional.empty(), allowed);
    }

    public enum Type {
        PROXY_COMMAND(true),
        SERVER_COMMAND(true),
        CHAT_MESSAGE(false);

        private final boolean command;

        Type(boolean command) {
            this.command = command;
        }

        public boolean isCommand() {
            return command;
        }

    }

}
