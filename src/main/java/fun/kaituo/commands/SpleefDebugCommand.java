package fun.kaituo.commands;

import fun.kaituo.Spleef;
import fun.kaituo.states.PreparingState;
import fun.kaituo.states.WaitingState;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class SpleefDebugCommand implements CommandExecutor, TabCompleter {
    private final List<String> SUBCOMMANDS = Arrays.asList("playerlist", "playernumber", "playersurvivalstatus", "gamestate");

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        // 参数校验
        if (args.length != 2 || !args[0].equalsIgnoreCase("show")) {
            sender.sendMessage("§c用法: /spleefdebug show < playerlist | playernumber | playersurvivalstatus | gamestate>");
            return true;
        }

        String subCommand = args[1].toLowerCase();
        switch (subCommand) {
            case "playerlist":
                handlePlayerList(sender);
                break;
            case "playernumber":
                handlePlayerNumber(sender);
                break;
            case "playersurvivalstatus":
                handlePlayerSurvivalStatus(sender);
                break;
            case "gamestate":
                handleGameState(sender);
                break;
            default:
                sender.sendMessage("§c无效参数。可用选项: playerlist, playernumber, playersurvivalstatus, gamestate");
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

    private void handlePlayerList(CommandSender sender) {
        if (Spleef.inst().playerIds.isEmpty()) {
            sender.sendMessage("§6当前没有玩家数据");
            return;
        }

        sender.sendMessage("§6玩家列表:");
        for (UUID uuid : Spleef.inst().playerIds) {
            Player player = Bukkit.getPlayer(uuid);
            String name = (player != null) ? player.getName() : "未知玩家";
            sender.sendMessage("§7 - §f" + name);
            sender.sendMessage("§7    - UUID: " + uuid);
        }
    }

    private void handlePlayerSurvivalStatus(CommandSender sender) {
        if (Spleef.inst().playerSurvivalStage.isEmpty()) {
            sender.sendMessage("§6当前没有玩家存活状态数据。");
            return;
        }

        sender.sendMessage("§6玩家存活情况:");
        Spleef.inst().playerSurvivalStage.forEach((uuid, isSurvived) -> {
            Player player = Bukkit.getPlayer(uuid);
            String name = (player != null) ? player.getName() : "未知玩家";
            String color = isSurvived ? "§a" : "§c";
            sender.sendMessage(" - " + color + name);
        });
    }

    private void handlePlayerNumber(CommandSender sender) {
        sender.sendMessage("§6存活玩家数量: §f" + Spleef.inst().survivingPlayerNumber);
    }

    private void handleGameState(CommandSender sender) {
        sender.sendMessage("§6当前游戏状态: §f" + Spleef.inst().currentGameState);
    }
}