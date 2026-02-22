package com.during.cityloader.command;

import com.during.cityloader.CityLoaderPlugin;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * 生成命令
 * 在指定位置生成城市结构
 * 
 * @author During
 * @since 1.4.0
 */
public class GenerateCommand implements SubCommand {

    private final CityLoaderPlugin plugin;

    public GenerateCommand(CityLoaderPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "generate";
    }

    @Override
    public String getDescription() {
        return "在指定位置生成城市结构";
    }

    @Override
    public String getUsage() {
        return "/cityloader generate [x] [y] [z]";
    }

    @Override
    public String getPermission() {
        return "cityloader.generate";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "此命令只能由玩家执行");
            return true;
        }

        Player player = (Player) sender;
        Location location;

        if (args.length >= 3) {
            try {
                int x = Integer.parseInt(args[0]);
                int y = Integer.parseInt(args[1]);
                int z = Integer.parseInt(args[2]);
                location = new Location(player.getWorld(), x, y, z);
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "无效的坐标格式");
                return true;
            }
        } else {
            location = player.getLocation();
        }

        sender.sendMessage(ChatColor.YELLOW + "正在生成城市结构...");
        sender.sendMessage(ChatColor.GRAY + "位置: " + location.getBlockX() + ", " +
                location.getBlockY() + ", " + location.getBlockZ());

        // TODO: 实现手动生成
        sender.sendMessage(ChatColor.GRAY + "提示: 城市结构会在新区块生成时自动创建");
        sender.sendMessage(ChatColor.GRAY + "请传送到新区块 (例如 /tp 10000 80 10000) 查看生成效果");

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        if (sender instanceof Player) {
            Player player = (Player) sender;
            Location loc = player.getLocation();
            if (args.length == 1)
                completions.add(String.valueOf(loc.getBlockX()));
            else if (args.length == 2)
                completions.add(String.valueOf(loc.getBlockY()));
            else if (args.length == 3)
                completions.add(String.valueOf(loc.getBlockZ()));
        }
        return completions;
    }
}
