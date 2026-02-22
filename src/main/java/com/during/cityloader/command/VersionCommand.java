package com.during.cityloader.command;

import com.during.cityloader.CityLoaderPlugin;
import com.during.cityloader.version.VersionManager;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

/**
 * 版本命令
 * 显示插件版本和服务器信息
 * 
 * @author During
 * @since 1.4.0
 */
public class VersionCommand implements SubCommand {
    
    private final CityLoaderPlugin plugin;
    private final VersionManager versionManager;
    
    /**
     * 构造函数
     * 
     * @param plugin 插件实例
     * @param versionManager 版本管理器
     */
    public VersionCommand(CityLoaderPlugin plugin, VersionManager versionManager) {
        this.plugin = plugin;
        this.versionManager = versionManager;
    }
    
    @Override
    public String getName() {
        return "version";
    }
    
    @Override
    public String getDescription() {
        return "显示插件版本信息";
    }
    
    @Override
    public String getUsage() {
        return "/cityloader version";
    }
    
    @Override
    public String getPermission() {
        return "cityloader.version";
    }
    
    @Override
    public boolean execute(CommandSender sender, String[] args) {
        // 显示版本信息
        sender.sendMessage(ChatColor.GOLD + "=================================");
        sender.sendMessage(ChatColor.AQUA + "CityLoader " + ChatColor.WHITE + "v" + versionManager.getPluginVersion());
        sender.sendMessage(ChatColor.GRAY + "作者: During");
        sender.sendMessage(ChatColor.GRAY + "描述: 季节性城市生成器");
        sender.sendMessage("");
        sender.sendMessage(ChatColor.YELLOW + "服务器信息:");
        sender.sendMessage(ChatColor.GRAY + "  " + versionManager.getServerInfo());
        sender.sendMessage(ChatColor.GOLD + "=================================");
        
        return true;
    }
    
    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return new ArrayList<>();
    }
}
