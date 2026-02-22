package com.during.cityloader.command;

import com.during.cityloader.CityLoaderPlugin;
import com.during.cityloader.version.VersionManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 命令处理器
 * 负责路由和执行所有子命令
 * 
 * @author During
 * @since 1.4.0
 */
public class CommandHandler implements CommandExecutor, TabCompleter {
    
    private final CityLoaderPlugin plugin;
    private final Map<String, SubCommand> subCommands;
    
    /**
     * 构造函数
     * 
     * @param plugin 插件实例
     * @param versionManager 版本管理器
     */
    public CommandHandler(CityLoaderPlugin plugin, VersionManager versionManager) {
        this.plugin = plugin;
        this.subCommands = new HashMap<>();
        
        // 注册所有子命令
        registerSubCommands(versionManager);
    }
    
    /**
     * 注册所有子命令
     * 
     * @param versionManager 版本管理器
     */
    private void registerSubCommands(VersionManager versionManager) {
        registerSubCommand(new ReloadCommand(plugin));
        registerSubCommand(new InfoCommand(plugin));
        registerSubCommand(new InspectCommand(plugin));
        registerSubCommand(new VersionCommand(plugin, versionManager));
        registerSubCommand(new GenerateCommand(plugin));
    }
    
    /**
     * 注册子命令
     * 
     * @param subCommand 子命令
     */
    private void registerSubCommand(SubCommand subCommand) {
        subCommands.put(subCommand.getName().toLowerCase(), subCommand);
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // 如果没有参数，显示帮助信息
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        
        // 获取子命令
        String subCommandName = args[0].toLowerCase();
        SubCommand subCommand = subCommands.get(subCommandName);
        
        if (subCommand == null) {
            sender.sendMessage("§c未知命令: " + subCommandName);
            sender.sendMessage("§e使用 /cityloader help 查看帮助");
            return false;
        }
        
        // 检查权限
        if (!sender.hasPermission(subCommand.getPermission())) {
            sender.sendMessage("§c你没有权限执行此命令");
            return false;
        }
        
        // 执行子命令（移除第一个参数）
        String[] subArgs = new String[args.length - 1];
        System.arraycopy(args, 1, subArgs, 0, subArgs.length);
        
        try {
            return subCommand.execute(sender, subArgs);
        } catch (Exception e) {
            sender.sendMessage("§c命令执行时发生错误: " + e.getMessage());
            plugin.getLogger().severe("命令执行错误: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        // 第一个参数：子命令名称
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            completions = subCommands.keySet().stream()
                .filter(name -> name.startsWith(partial))
                .filter(name -> sender.hasPermission(subCommands.get(name).getPermission()))
                .collect(Collectors.toList());
            completions.add("help");
        }
        // 后续参数：委托给子命令
        else if (args.length > 1) {
            String subCommandName = args[0].toLowerCase();
            SubCommand subCommand = subCommands.get(subCommandName);
            
            if (subCommand != null && sender.hasPermission(subCommand.getPermission())) {
                String[] subArgs = new String[args.length - 1];
                System.arraycopy(args, 1, subArgs, 0, subArgs.length);
                completions = subCommand.tabComplete(sender, subArgs);
            }
        }
        
        return completions;
    }
    
    /**
     * 发送帮助信息
     * 
     * @param sender 命令发送者
     */
    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6§l=== CityLoader 命令帮助 ===");
        sender.sendMessage("");
        
        for (SubCommand subCommand : subCommands.values()) {
            if (sender.hasPermission(subCommand.getPermission())) {
                sender.sendMessage("§e" + subCommand.getUsage());
                sender.sendMessage("§7  " + subCommand.getDescription());
            }
        }
        
        sender.sendMessage("");
        sender.sendMessage("§6§l========================");
    }
}
