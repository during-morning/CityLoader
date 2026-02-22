package com.during.cityloader.command;

import com.during.cityloader.CityLoaderPlugin;
import com.during.cityloader.worldgen.lost.cityassets.AssetRegistries;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * 信息命令
 * 显示插件配置和状态信息
 * 
 * @author During
 * @since 1.4.0
 */
public class InfoCommand implements SubCommand {

    private final CityLoaderPlugin plugin;

    /**
     * 构造函数
     * 
     * @param plugin 插件实例
     */
    public InfoCommand(CityLoaderPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "info";
    }

    @Override
    public String getDescription() {
        return "显示插件信息和状态";
    }

    @Override
    public String getUsage() {
        return "/cityloader info";
    }

    @Override
    public String getPermission() {
        return "cityloader.info";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        // 检查权限
        if (!sender.hasPermission(getPermission())) {
            sender.sendMessage("§c你没有权限执行此命令");
            return false;
        }

        sender.sendMessage("§6§l=== CityLoader 信息 ===");
        sender.sendMessage("");

        // 插件版本
        sender.sendMessage("§e版本: §f" + plugin.getDescription().getVersion());

        // 配置信息
        if (plugin.getPluginConfig() != null) {
            sender.sendMessage("§e默认季节: §f" +
                    plugin.getPluginConfig().getDefaultSeason().getDisplayName());
            sender.sendMessage("§e城市密度: §f" +
                    plugin.getPluginConfig().getCityDensity());
            sender.sendMessage("§e建筑高度: §f" +
                    plugin.getPluginConfig().getMinBuildingHeight() + " - " +
                    plugin.getPluginConfig().getMaxBuildingHeight());
        }

        // 季节信息
        if (plugin.getSeasonAdapter() != null) {
            sender.sendMessage("§eRealisticSeasons: §f" +
                    (plugin.getSeasonAdapter().isAvailable() ? "§a已连接" : "§c未连接"));

            // 如果是玩家，显示当前世界的季节
            if (sender instanceof Player) {
                Player player = (Player) sender;
                World world = player.getWorld();
                sender.sendMessage("§e当前季节: §f" +
                        plugin.getSeasonAdapter().getCurrentSeason(world).getDisplayName());
            }
        }

        // 资源信息
        sender.sendMessage("");
        sender.sendMessage("§e已加载资源:");
        if (AssetRegistries.isLoaded()) {
            sender.sendMessage("§7  调色板: §f" + AssetRegistries.PALETTES.size());
            sender.sendMessage("§7  调色板加载失败数: §f" + AssetRegistries.PALETTES.getLastLoadFailureCount());
            sender.sendMessage("§7  部件: §f" + AssetRegistries.PARTS.size());
            sender.sendMessage("§7  建筑: §f" + AssetRegistries.BUILDINGS.size());
        } else {
            sender.sendMessage("§7  调色板: §f0");
            sender.sendMessage("§7  调色板加载失败数: §f0");
            sender.sendMessage("§7  部件: §f0");
            sender.sendMessage("§7  建筑: §f0");
        }

        sender.sendMessage("");
        sender.sendMessage("§6§l=====================");

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return new ArrayList<>();
    }
}
