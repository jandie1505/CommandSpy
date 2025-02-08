package net.jandie1505.commandspy.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import net.jandie1505.commandspy.CommandSpy;
import net.jandie1505.commandspy.data.SpyData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class SpyCommand implements SimpleCommand {
    @NotNull private final CommandSpy plugin;

    public SpyCommand(@NotNull CommandSpy plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {

        CommandSource sender = invocation.source();
        CommandSource messageSender = sender;
        String[] args = Arrays.copyOf(invocation.arguments(), invocation.arguments().length);

        if (sender == this.plugin.getProxy().getConsoleCommandSource()) {

            if (args.length < 1) {
                messageSender.sendMessage(Component.text("Console Usage: spy <PLAYER> info/addPlayer/removePlayer/allPlayers/currentServer/commands/proxyCommands/chat [args]", NamedTextColor.RED));
                return;
            }

            Player player;

            try {
                player = this.plugin.getProxy().getPlayer(UUID.fromString(args[0])).orElse(null);
            } catch (IllegalArgumentException e) {
                player = this.plugin.getProxy().getPlayer(args[0]).orElse(null);
            }

            if (player == null) {
                messageSender.sendMessage(Component.text("Player not found", NamedTextColor.RED));
                return;
            }

            String[] newArgs = new String[args.length - 1];
            for (int i = 1; i < args.length; i++) {
                newArgs[i - 1] = args[i];
            }

            args = newArgs;

            sender = player;
            messageSender = this.plugin.getProxy().getConsoleCommandSource();

        }

        if (!(sender instanceof Player player)) {
            return;
        }

        if (args.length < 1) {
            args = new String[]{"info"};
        }

        SpyData spyData = this.plugin.getSpyingPlayer(player.getUniqueId());

        switch (args[0]) {
            case "info" -> {
                Component text1 = Component.empty()
                        .append(Component.text("Spying targets:", NamedTextColor.GOLD)
                                .hoverEvent(HoverEvent.showText(Component.text("The selection from which players events are shown", NamedTextColor.GRAY)))
                        )
                        .append(Component.newline())
                        .append(Component.text("All players: ")
                                .color(NamedTextColor.GRAY) // gray color
                                .hoverEvent(HoverEvent.showText(Component.text("Use ").color(NamedTextColor.GRAY)
                                        .append(Component.text("/spy all true/false", NamedTextColor.AQUA))
                                        .append(Component.text(" to edit").color(NamedTextColor.GRAY)))
                                )
                                .clickEvent(ClickEvent.suggestCommand("/spy all " + !spyData.isSpyAllPlayers()))
                                .append(Component.text(String.valueOf(spyData.isSpyAllPlayers()))
                                        .color(spyData.isSpyAllPlayers() ? NamedTextColor.GREEN : NamedTextColor.RED))
                        )
                        .append(Component.newline())
                        .append(Component.text("Current server: ")
                                .color(NamedTextColor.GRAY)
                                .hoverEvent(HoverEvent.showText(Component.text("Use ").color(NamedTextColor.GRAY)
                                        .append(Component.text("/spy currentserver true/false", NamedTextColor.AQUA))
                                        .append(Component.text(" to edit").color(NamedTextColor.GRAY)))
                                )
                                .clickEvent(ClickEvent.suggestCommand("/spy currentserver " + !spyData.isSpyCurrentServerPlayers()))
                                .append(Component.text(String.valueOf(spyData.isSpyCurrentServerPlayers()))
                                        .color(spyData.isSpyCurrentServerPlayers() ? NamedTextColor.GREEN : NamedTextColor.RED))
                        )
                        .append(Component.newline())
                        .append(Component.text("Custom targets: ")
                                .color(NamedTextColor.GRAY) // gray color
                                .append(Component.text(spyData.getTargets().size() + " targets")
                                        .color(!spyData.getTargets().isEmpty() ? NamedTextColor.AQUA : NamedTextColor.GRAY))
                                .hoverEvent(HoverEvent.showText(Component.text("Use ").color(NamedTextColor.GRAY)
                                        .append(Component.text("/spy getplayers", NamedTextColor.AQUA))
                                        .append(Component.text(" to see a list", NamedTextColor.GRAY)))
                                )
                                .clickEvent(ClickEvent.suggestCommand("/spy getplayers"))
                        );

                Component text2 = Component.empty()
                        .append(Component.text("Spying filter:", NamedTextColor.GOLD)
                                .hoverEvent(HoverEvent.showText(Component.text("The type of events that should be shown", NamedTextColor.GRAY)))
                        )
                        .append(Component.newline())
                        .append(Component.text("Chat messages: ", NamedTextColor.GRAY)
                                .hoverEvent(HoverEvent.showText(Component.text("Use ", NamedTextColor.GRAY)
                                        .append(Component.text("/spy chat true/false", NamedTextColor.AQUA))
                                        .append(Component.text(" to edit", NamedTextColor.GRAY)))
                                )
                                .clickEvent(ClickEvent.suggestCommand("/spy chat " + !spyData.isSpyChat()))
                                .append(Component.text(String.valueOf(spyData.isSpyChat()))
                                        .color(spyData.isSpyChat() ? NamedTextColor.GREEN : NamedTextColor.RED)
                                )
                        )
                        .append(Component.newline())
                        .append(Component.text("Server Commands: ")
                                .color(NamedTextColor.GRAY)
                                .hoverEvent(HoverEvent.showText(Component.text("Use ", NamedTextColor.GRAY)
                                        .append(Component.text("/spy server-commands true/false", NamedTextColor.AQUA))
                                        .append(Component.text(" to edit", NamedTextColor.GRAY)))
                                )
                                .clickEvent(ClickEvent.suggestCommand("/spy server-commands " + !spyData.isSpyServerCommands()))
                                .append(Component.text(String.valueOf(spyData.isSpyServerCommands()))
                                        .color(spyData.isSpyServerCommands() ? NamedTextColor.GREEN : NamedTextColor.RED)
                                )
                        )
                        .append(Component.newline())
                        .append(Component.text("Proxy Commands: ", NamedTextColor.GRAY)
                                .hoverEvent(HoverEvent.showText(Component.text("Use ", NamedTextColor.GRAY)
                                        .append(Component.text("/spy proxy-commands true/false", NamedTextColor.AQUA))
                                        .append(Component.text(" to edit", NamedTextColor.GRAY)))
                                )
                                .clickEvent(ClickEvent.suggestCommand("/spy proxy-commands " + !spyData.isSpyProxyCommands()))
                                .append(Component.text(String.valueOf(spyData.isSpyProxyCommands()))
                                        .color(spyData.isSpyProxyCommands() ? NamedTextColor.GREEN : NamedTextColor.RED)
                                )
                        );

                messageSender.sendMessage(text1);
                messageSender.sendMessage(text2);

            }
            case "get-players" -> {
                Component text = Component.empty()
                        .append(Component.text("Specific spying targets:")
                                .color(NamedTextColor.GOLD)
                                .hoverEvent(HoverEvent.showText(Component.text("This is a list of the specific spying targets").color(NamedTextColor.GRAY)))
                        );

                if (spyData.getTargets().isEmpty()) {
                    text = text.append(Component.newline())
                            .append(Component.text("--- no entries ---").color(NamedTextColor.GRAY));
                }

                for (UUID playerId : spyData.getTargets()) {
                    Player p = this.plugin.getProxy().getPlayer(playerId).orElse(null);

                    text = text.append(Component.newline());

                    if (p == null) {
                        text = text.append(Component.text(playerId.toString()).color(NamedTextColor.GRAY));
                        continue;
                    }

                    text = text.append(Component.text(p.getUsername()).color(NamedTextColor.GRAY));
                    text = text.append(Component.text(" (" + playerId + ")").color(NamedTextColor.GRAY));
                }

                messageSender.sendMessage(text);

            }
            case "add-player" -> {

                if (args.length < 2) {
                    messageSender.sendMessage(Component.text("Usage: /spy addplayer <uuid/player>")
                            .color(NamedTextColor.RED));
                    return;
                }

                UUID playerId = this.plugin.getPlayerId(args[1]);

                if (playerId == null) {
                    messageSender.sendMessage(Component.text("Player not found (You might need to use the UUID if the player is on another proxy)")
                            .color(NamedTextColor.RED));
                    return;
                }

                if (spyData.getTargets().contains(playerId)) {
                    messageSender.sendMessage(Component.text("Target already added")
                            .color(NamedTextColor.RED));
                    return;
                }

                spyData.addTarget(playerId);
                messageSender.sendMessage(Component.text("Target successfully added")
                        .color(NamedTextColor.GREEN));

            }
            case "remove-player" -> {

                if (args.length < 2) {
                    messageSender.sendMessage(Component.text("Usage: /spy removeplayer <uuid/player>")
                            .color(NamedTextColor.RED));
                    return;
                }

                UUID playerId = this.plugin.getPlayerId(args[1]);

                if (playerId == null) {
                    messageSender.sendMessage(Component.text("Player not found (You might need to use the UUID if the player is on another proxy)")
                            .color(NamedTextColor.RED));
                    return;
                }

                if (!spyData.getTargets().contains(playerId)) {
                    messageSender.sendMessage(Component.text("Target not in target list")
                            .color(NamedTextColor.RED));
                    return;
                }

                spyData.removeTarget(playerId);
                messageSender.sendMessage(Component.text("Target successfully removed")
                        .color(NamedTextColor.GREEN));

            }
            case "clear-players" -> {

                spyData.clearTargets();
                messageSender.sendMessage(Component.text("Target list cleared")
                        .color(NamedTextColor.GREEN));

            }
            case "all" -> {

                Component text = Component.empty()
                        .append(Component.text("Spy all players", NamedTextColor.GRAY));

                if (args.length > 1) {
                    spyData.setSpyAllPlayers(Boolean.parseBoolean(args[1]));
                    text = text.append(Component.text(" was set to ")
                            .color(NamedTextColor.GRAY));
                } else {
                    text = text.append(Component.text(": ")
                            .color(NamedTextColor.GRAY));
                }

                text = text.append(Component.text(String.valueOf(spyData.isSpyAllPlayers()))
                        .color(spyData.isSpyAllPlayers() ? NamedTextColor.GREEN : NamedTextColor.RED));

                messageSender.sendMessage(text);

            }
            case "current-server" -> {

                Component text = Component.empty()
                        .append(Component.text("Spy current server")
                                .color(NamedTextColor.GRAY));

                if (args.length > 1) {
                    spyData.setSpyCurrentServerPlayers(Boolean.parseBoolean(args[1]));
                    text = text.append(Component.text(" was set to ")
                            .color(NamedTextColor.GRAY));
                } else {
                    text = text.append(Component.text(": ")
                            .color(NamedTextColor.GRAY));
                }

                text = text.append(Component.text(String.valueOf(spyData.isSpyCurrentServerPlayers()))
                        .color(spyData.isSpyCurrentServerPlayers() ? NamedTextColor.GREEN : NamedTextColor.RED));


                messageSender.sendMessage(text);

            }
            case "server-commands" -> {

                Component text = Component.empty()
                        .append(Component.text("Spy on server commands")
                                .color(NamedTextColor.GRAY));

                if (args.length > 1) {
                    spyData.setSpyServerCommands(Boolean.parseBoolean(args[1]));
                    text = text.append(Component.text(" was set to ")
                            .color(NamedTextColor.GRAY));
                } else {
                    text = text.append(Component.text(": ")
                            .color(NamedTextColor.GRAY));
                }

                text = text.append(Component.text(String.valueOf(spyData.isSpyServerCommands()))
                        .color(spyData.isSpyServerCommands() ? NamedTextColor.GREEN : NamedTextColor.RED));

                messageSender.sendMessage(text);

            }
            case "proxy-commands" -> {

                Component text = Component.empty()
                        .append(Component.text("Spy on proxy commands")
                                .color(NamedTextColor.GRAY));

                if (args.length > 1) {
                    spyData.setSpyProxyCommands(Boolean.parseBoolean(args[1]));
                    text = text.append(Component.text(" was set to ")
                            .color(NamedTextColor.GRAY));
                } else {
                    text = text.append(Component.text(": ")
                            .color(NamedTextColor.GRAY));
                }

                text = text.append(Component.text(String.valueOf(spyData.isSpyProxyCommands()))
                        .color(spyData.isSpyProxyCommands() ? NamedTextColor.GREEN : NamedTextColor.RED));


                messageSender.sendMessage(text);

            }
            case "chat" -> {

                Component text = Component.empty()
                        .append(Component.text("Spy on chat")
                                .color(NamedTextColor.GRAY));

                if (args.length > 1) {
                    spyData.setSpyChat(Boolean.parseBoolean(args[1]));
                    text = text.append(Component.text(" was set to ")
                            .color(NamedTextColor.GRAY));
                } else {
                    text = text.append(Component.text(": ")
                            .color(NamedTextColor.GRAY));
                }

                text = text.append(Component.text(String.valueOf(spyData.isSpyChat()))
                        .color(spyData.isSpyChat() ? NamedTextColor.GREEN : NamedTextColor.RED));


                messageSender.sendMessage(text);

            }
            default -> messageSender.sendMessage(Component.text("Unknown subcommand", NamedTextColor.RED));
        }


    }

    @Override
    public List<String> suggest(Invocation invocation) {

        if (!(invocation.source() instanceof Player)) {
            return List.of();
        }

        if (!invocation.source().hasPermission("commandspy.spy")) {
            return List.of();
        }

        CommandSource sender = invocation.source();
        String[] args = invocation.arguments();

        switch (args.length) {
            case 1 -> {
                return List.of("info", "add-player", "remove-player", "clear-player", "get-players", "all", "current-server", "server-commands", "proxy-commands", "chat");
            }
            case 2 -> {

                switch (args[0]) {
                    case "add-player" -> {
                        SpyData spyData = this.plugin.getSpyingPlayer(((Player) sender).getUniqueId());
                        List<String> players = new ArrayList<>();

                        for (Player player : List.copyOf(this.plugin.getProxy().getAllPlayers())) {

                            if (spyData.getTargets().contains(player.getUniqueId())) {
                                continue;
                            }

                            players.add(player.getUsername());

                        }

                        return List.copyOf(players);
                    }
                    case "remove-player" -> {
                        SpyData spyData = this.plugin.getSpyingPlayer(((Player) sender).getUniqueId());
                        List<String> players = new ArrayList<>();

                        for (UUID playerId : spyData.getTargets()) {
                            Player player = this.plugin.getProxy().getPlayer(playerId).orElse(null);

                            if (player == null) {
                                players.add(playerId.toString());
                                continue;
                            }

                            players.add(player.getUsername());

                        }

                        return List.copyOf(players);
                    }
                    case "all", "current-server", "server-commands", "proxy-commands", "chat" -> {
                        return List.of("false", "true");
                    }
                    default -> {
                        return List.of();
                    }
                }

            }
            default -> {
                return List.of();
            }
        }
    }

    public @NotNull CommandSpy getPlugin() {
        return plugin;
    }

}

