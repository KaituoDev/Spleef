package fun.kaituo.commands;

import fun.kaituo.Spleef;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class SpleefDebugCommand implements CommandExecutor, TabCompleter {
    private final List<String> SUBCOMMANDS = Arrays.asList("players", "playernum", "survival");

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        // 参数校验
        if (args.length != 2 || !args[0].equalsIgnoreCase("show")) {
            sender.sendMessage(ChatColor.RED + "用法: /spleefdebug show <players|playernum|survival>");
            return true;
        }

        String subCommand = args[1].toLowerCase();
        switch (subCommand) {
            case "players":
                handlePlayers(sender);
                break;
            case "playernum":
                handlePlayerNum(sender);
                break;
            case "survival":
                handleSurvival(sender);
                break;
            default:
                sender.sendMessage(ChatColor.RED + "无效参数。可用选项: players, playernum, survival");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) { // 第一个参数补全
            if ("show".startsWith(args[0].toLowerCase())) {
                completions.add("show");
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("show")) { // 第二个参数补全
            for (String sub : SUBCOMMANDS) {
                if (sub.startsWith(args[1].toLowerCase())) {
                    completions.add(sub);
                }
            }
        }
        return completions;
    }

    private void handlePlayers(CommandSender sender) {
        if (Spleef.inst().playerIds.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "当前没有玩家数据。");
            return;
        }

        sender.sendMessage(ChatColor.GOLD + "玩家列表:");
        for (UUID uuid : Spleef.inst().playerIds) {
            OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
            String name = (player.getName() != null) ? player.getName() : "未知玩家";
            sender.sendMessage(ChatColor.GRAY + " - " + name);
        }
    }

    private void handleSurvival(CommandSender sender) {
        if (Spleef.inst().playerSurvivalStage.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "当前没有玩家生存状态数据。");
            return;
        }

        sender.sendMessage(ChatColor.GOLD + "玩家生存状态:");
        Spleef.inst().playerSurvivalStage.forEach((uuid, isSurvived) -> {
            OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
            String name = (player.getName() != null) ? player.getName() : "未知玩家";
            ChatColor color = isSurvived ? ChatColor.GREEN : ChatColor.RED;
            sender.sendMessage(color + " - " + name + ": " + isSurvived);
        });
    }

    private void handlePlayerNum(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "存活玩家数量: " + ChatColor.WHITE + Spleef.inst().survivingPlayerNumber);
    }
}