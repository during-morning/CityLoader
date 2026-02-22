package com.during.cityloader.command;

import com.during.cityloader.CityLoaderPlugin;
import com.during.cityloader.config.PluginConfig;
import com.during.cityloader.util.PaperResourceLoader;
import com.during.cityloader.worldgen.lost.BuildingInfo;
import com.during.cityloader.worldgen.lost.cityassets.AssetRegistries;
import org.bukkit.World;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

/**
 * 重载命令
 * 重载插件配置和资源
 * 
 * @author During
 * @since 1.4.0
 */
public class ReloadCommand implements SubCommand {
    
    private final CityLoaderPlugin plugin;
    
    /**
     * 构造函数
     * 
     * @param plugin 插件实例
     */
    public ReloadCommand(CityLoaderPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public String getName() {
        return "reload";
    }
    
    @Override
    public String getDescription() {
        return "重载插件配置和资源";
    }
    
    @Override
    public String getUsage() {
        return "/cityloader reload";
    }
    
    @Override
    public String getPermission() {
        return "cityloader.reload";
    }
    
    @Override
    public boolean execute(CommandSender sender, String[] args) {
        // 检查权限
        if (!sender.hasPermission(getPermission())) {
            sender.sendMessage("§c你没有权限执行此命令");
            return false;
        }
        
        sender.sendMessage("§e正在重载配置和资源...");
        
        try {
            // 重载配置
            PluginConfig reloaded = plugin.getConfigManager().reloadConfig();
            plugin.refreshRuntimeConfig(reloaded);
            sender.sendMessage("§a✓ 配置重载成功");
            sender.sendMessage("§7  外部资产目录数: " + PaperResourceLoader.getExternalDataRoots().size());
            
            // 重载新架构资产
            AssetRegistries.reset();
            BuildingInfo.resetCache();

            int preloadedWorlds = 0;
            for (World world : plugin.getServer().getWorlds()) {
                if (!plugin.shouldEnableCityGeneration(world)) {
                    continue;
                }
                AssetRegistries.load(world);
                preloadedWorlds++;
            }

            sender.sendMessage("§a✓ 资源重载成功: " + AssetRegistries.getStatistics());
            sender.sendMessage("§7  已预加载世界数: " + preloadedWorlds);
            sender.sendMessage("§7  资产覆盖冲突数: " + PaperResourceLoader.getLastScanConflicts().size());
            
            sender.sendMessage("§a重载完成！");
            return true;
            
        } catch (Exception e) {
            sender.sendMessage("§c重载失败: " + e.getMessage());
            plugin.getLogger().severe("重载失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return new ArrayList<>();
    }
}
