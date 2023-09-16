package net.jandie1505.commandspy.commands;

import net.jandie1505.commandspy.CommandSpy;
import net.jandie1505.commandspy.data.SpyData;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SpyCommand extends Command implements TabExecutor {
    private final CommandSpy plugin;

    public SpyCommand(CommandSpy plugin) {
        super("spy", "commandspy.spy");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {

        if (sender == this.plugin.getProxy().getConsole()) {

            if (args.length < 1) {
                sender.sendMessage(new ComponentBuilder().append("Console Usage: spy <PLAYER> info/addPlayer/removePlayer/allPlayers/currentServer/commands/proxyCommands/chat [args]").color(ChatColor.RED).create());
                return;
            }

            ProxiedPlayer player;

            try {
                player = this.plugin.getProxy().getPlayer(UUID.fromString(args[0]));
            } catch (IllegalArgumentException e) {
                player = this.plugin.getProxy().getPlayer(args[0]);
            }

            if (player == null) {
                sender.sendMessage(new ComponentBuilder().append("Player not found").color(ChatColor.RED).create());
                return;
            }

            String[] newArgs = new String[args.length - 1];
            for (int i = 1; i < args.length; i++) {
                newArgs[i - 1] = args[i];
            }

            args = newArgs;

            sender = player;

        }

        if (!(sender instanceof ProxiedPlayer)) {
            return;
        }

        if (args.length < 1) {
            args = new String[]{"info"};
        }

        SpyData spyData = this.plugin.getSpyData(((ProxiedPlayer) sender).getUniqueId());

        switch (args[0]) {
            case "info" -> {
                ComponentBuilder text = new ComponentBuilder()
                        .append("Spying targets:")
                        .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder().append("The selection from which players events are shown").color(ChatColor.GRAY).create()))
                        .color(ChatColor.GOLD)
                        .append("\n")
                        .append("All players: ")
                        .color(ChatColor.GRAY)
                        .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder().append("Use ").color(ChatColor.GRAY).append("/spy all true/false").color(ChatColor.AQUA).append(" to edit").color(ChatColor.GRAY).create()))
                        .event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/spy all " + !spyData.isSpyAllPlayers()))
                        .append(String.valueOf(spyData.isSpyAllPlayers()))
                        .color(spyData.isSpyAllPlayers() ? ChatColor.GREEN : ChatColor.RED)
                        .append("\n")
                        .append("Current server: ")
                        .color(ChatColor.GRAY)
                        .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder().append("Use ").color(ChatColor.GRAY).append("/spy currentserver true/false").color(ChatColor.AQUA).append(" to edit").color(ChatColor.GRAY).create()))
                        .event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/spy currentserver " + !spyData.isSpyCurrentServerPlayers()))
                        .append(String.valueOf(spyData.isSpyCurrentServerPlayers()))
                        .color(spyData.isSpyCurrentServerPlayers() ? ChatColor.GREEN : ChatColor.RED)
                        .append("\n")
                        .append("Custom targets: ")
                        .color(ChatColor.GRAY)
                        .append(spyData.getTargets().size() + " targets")
                        .color((spyData.getTargets().size() > 0) ? ChatColor.AQUA : ChatColor.GRAY)
                        .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder().append("Use ").color(ChatColor.GRAY).append("/spy getplayers").color(ChatColor.AQUA).append(" to see a list").color(ChatColor.GRAY).create()))
                        .event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/spy getplayers"));

                ComponentBuilder text2 = new ComponentBuilder()
                        .append("Spying filter:")
                        .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder().append("The type of events that should be shown").color(ChatColor.GRAY).create()))
                        .color(ChatColor.GOLD)
                        .append("\n")
                        .append("Chat messages: ")
                        .color(ChatColor.GRAY)
                        .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder().append("Use ").color(ChatColor.GRAY).append("/spy chat true/false").color(ChatColor.AQUA).append(" to edit").color(ChatColor.GRAY).create()))
                        .event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/spy chat " + !spyData.isSpyChat()))
                        .append(String.valueOf(spyData.isSpyChat()))
                        .color(spyData.isSpyChat() ? ChatColor.GREEN : ChatColor.RED)
                        .append("\n")
                        .append("Commands: ")
                        .color(ChatColor.GRAY)
                        .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder().append("Use ").color(ChatColor.GRAY).append("/spy commands true/false").color(ChatColor.AQUA).append(" to edit").color(ChatColor.GRAY).create()))
                        .event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/spy commands " + !spyData.isSpyCommands()))
                        .append(String.valueOf(spyData.isSpyCommands()))
                        .color(spyData.isSpyCommands() ? ChatColor.GREEN : ChatColor.RED)
                        .append("\n")
                        .append("Proxy Commands: ")
                        .color(ChatColor.GRAY)
                        .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder().append("Use ").color(ChatColor.GRAY).append("/spy proxycommands true/false").color(ChatColor.AQUA).append(" to edit").color(ChatColor.GRAY).create()))
                        .event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/spy proxycommands " + !spyData.isSpyProxyCommands()))
                        .append(String.valueOf(spyData.isSpyProxyCommands()))
                        .color(spyData.isSpyProxyCommands() ? ChatColor.GREEN : ChatColor.RED);

                sender.sendMessage(text.create());
                sender.sendMessage(text2.create());

            }
            case "getplayers" -> {
                ComponentBuilder text = new ComponentBuilder()
                        .append("Specific spying targets:")
                        .color(ChatColor.GOLD)
                        .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder().append("This is a list of the specific spying targets").color(ChatColor.GRAY).create()));

                if (spyData.getTargets().isEmpty()) {
                    text.append("\n");
                    text.append("--- no entries ---").color(ChatColor.GRAY);
                }

                for (UUID playerId : spyData.getTargets()) {
                    ProxiedPlayer player = this.plugin.getProxy().getPlayer(playerId);

                    text.append("\n");

                    if (player == null) {
                        text.append(playerId.toString()).color(ChatColor.GRAY);
                        continue;
                    }

                    text.append(player.getName()).color(ChatColor.GRAY);
                    text.append(" (" + playerId + ")").color(ChatColor.GRAY);

                }

                sender.sendMessage(text.create());

            }
            case "addplayer" -> {

                if (args.length < 2) {
                    sender.sendMessage(new ComponentBuilder().append("Usage: /spy addplayer <uuid/player>").color(ChatColor.RED).create());
                    return;
                }

                UUID playerId = this.plugin.getPlayerId(args[1]);

                if (playerId == null) {
                    sender.sendMessage(new ComponentBuilder().append("Player not found").color(ChatColor.RED).create());
                    return;
                }

                if (spyData.getTargets().contains(playerId)) {
                    sender.sendMessage(new ComponentBuilder().append("Target already added").color(ChatColor.RED).create());
                    return;
                }

                spyData.addTarget(playerId);
                sender.sendMessage(new ComponentBuilder().append("Target successfully added").color(ChatColor.GREEN).create());

            }
            case "removeplayer" -> {

                if (args.length < 2) {
                    sender.sendMessage(new ComponentBuilder().append("Usage: /spy removeplayer <uuid/player>").color(ChatColor.RED).create());
                    return;
                }

                UUID playerId = this.plugin.getPlayerId(args[1]);

                if (playerId == null) {
                    sender.sendMessage(new ComponentBuilder().append("Player not found").color(ChatColor.RED).create());
                    return;
                }

                if (!spyData.getTargets().contains(playerId)) {
                    sender.sendMessage(new ComponentBuilder().append("Target not in target list").color(ChatColor.RED).create());
                    return;
                }

                spyData.removeTarget(playerId);
                sender.sendMessage(new ComponentBuilder().append("Target successfully removed").color(ChatColor.GREEN).create());

            }
            case "clearplayers" -> {

                spyData.clearTargets();
                sender.sendMessage(new ComponentBuilder().append("Target list cleared").color(ChatColor.GREEN).create());

            }
            case "all" -> {

                ComponentBuilder text = new ComponentBuilder()
                        .append("Spy all players")
                        .color(ChatColor.GRAY);

                if (args.length > 1) {

                    spyData.setSpyAllPlayers(Boolean.parseBoolean(args[1]));
                    text.append(" was set to ").color(ChatColor.GRAY);

                } else {

                    text.append(": ").color(ChatColor.GRAY);

                }

                text.append(String.valueOf(spyData.isSpyAllPlayers())).color(spyData.isSpyAllPlayers() ? ChatColor.GREEN : ChatColor.RED);

                sender.sendMessage(text.create());

            }
            case "currentserver" -> {

                ComponentBuilder text = new ComponentBuilder()
                        .append("Spy current server")
                        .color(ChatColor.GRAY);

                if (args.length > 1) {

                    spyData.setSpyCurrentServerPlayers(Boolean.parseBoolean(args[1]));
                    text.append(" was set to ").color(ChatColor.GRAY);

                } else {

                    text.append(": ").color(ChatColor.GRAY);

                }

                text.append(String.valueOf(spyData.isSpyCurrentServerPlayers())).color(spyData.isSpyCurrentServerPlayers() ? ChatColor.GREEN : ChatColor.RED);

                sender.sendMessage(text.create());

            }
            case "commands" -> {

                ComponentBuilder text = new ComponentBuilder()
                        .append("Spy on commands")
                        .color(ChatColor.GRAY);

                if (args.length > 1) {

                    spyData.setSpyCommands(Boolean.parseBoolean(args[1]));
                    text.append(" was set to ").color(ChatColor.GRAY);

                } else {

                    text.append(": ").color(ChatColor.GRAY);

                }

                text.append(String.valueOf(spyData.isSpyCommands())).color(spyData.isSpyCommands() ? ChatColor.GREEN : ChatColor.RED);

                sender.sendMessage(text.create());

            }
            case "proxycommands" -> {

                ComponentBuilder text = new ComponentBuilder()
                        .append("Spy on proxy commands")
                        .color(ChatColor.GRAY);

                if (args.length > 1) {

                    spyData.setSpyProxyCommands(Boolean.parseBoolean(args[1]));
                    text.append(" was set to ").color(ChatColor.GRAY);

                } else {

                    text.append(": ").color(ChatColor.GRAY);

                }

                text.append(String.valueOf(spyData.isSpyProxyCommands())).color(spyData.isSpyProxyCommands() ? ChatColor.GREEN : ChatColor.RED);

                sender.sendMessage(text.create());

            }
            case "chat" -> {

                ComponentBuilder text = new ComponentBuilder()
                        .append("Spy on chat")
                        .color(ChatColor.GRAY);

                if (args.length > 1) {

                    spyData.setSpyChat(Boolean.parseBoolean(args[1]));
                    text.append(" was set to ").color(ChatColor.GRAY);

                } else {

                    text.append(": ").color(ChatColor.GRAY);

                }

                text.append(String.valueOf(spyData.isSpyChat())).color(spyData.isSpyChat() ? ChatColor.GREEN : ChatColor.RED);

                sender.sendMessage(text.create());

            }
            default -> sender.sendMessage(new ComponentBuilder().append("Unknown subcommand").color(ChatColor.RED).create());
        }

    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {

        if (!(sender instanceof ProxiedPlayer)) {
            return List.of();
        }

        if (!sender.hasPermission("commandspy.spy")) {
            return List.of();
        }

        switch (args.length) {
            case 1 -> {
                return List.of("info", "addplayer", "removeplayer", "clearplayer", "getplayers", "all", "currentserver", "commands", "proxycommands", "chat");
            }
            case 2 -> {

                switch (args[0]) {
                    case "addplayer" -> {
                        SpyData spyData = this.plugin.getSpyData(((ProxiedPlayer) sender).getUniqueId());
                        List<String> players = new ArrayList<>();

                        for (ProxiedPlayer player : List.copyOf(this.plugin.getProxy().getPlayers())) {

                            if (spyData.getTargets().contains(player.getUniqueId())) {
                                continue;
                            }

                            players.add(player.getName());

                        }

                        return List.copyOf(players);
                    }
                    case "removeplayer" -> {
                        SpyData spyData = this.plugin.getSpyData(((ProxiedPlayer) sender).getUniqueId());
                        List<String> players = new ArrayList<>();

                        for (UUID playerId : spyData.getTargets()) {
                            ProxiedPlayer player = this.plugin.getProxy().getPlayer(playerId);

                            if (player == null) {
                                players.add(playerId.toString());
                                continue;
                            }

                            players.add(player.getName());

                        }

                        return List.copyOf(players);
                    }
                    case "all", "currentserver", "commands", "proxycommands", "chat" -> {
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
}
