package fun.kaituo.commands;

import fun.kaituo.Spleef;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import java.util.Collections;
import java.util.List;

public class SpleefClearMapCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        // 权限验证
        if (!sender.isOp()) {
            sender.sendMessage(ChatColor.RED + "你没有执行此命令的权限!");
            return true;
        }

        // 参数校验（无参数）
        if (args.length > 0) {
            sender.sendMessage(ChatColor.RED + "用法: /spleefclearmap");
            return true;
        }

        // 执行地图清理逻辑
        Spleef.inst().clearMap();
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        // 不需要参数补全，返回空列表
        return Collections.emptyList();
    }
}