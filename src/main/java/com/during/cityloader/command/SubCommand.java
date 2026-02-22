package com.during.cityloader.command;

import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * 子命令接口
 * 所有子命令都应该实现此接口
 * 
 * @author During
 * @since 1.4.0
 */
public interface SubCommand {
    
    /**
     * 获取命令名称
     * 
     * @return 命令名称
     */
    String getName();
    
    /**
     * 获取命令描述
     * 
     * @return 命令描述
     */
    String getDescription();
    
    /**
     * 获取命令用法
     * 
     * @return 命令用法
     */
    String getUsage();
    
    /**
     * 获取所需权限
     * 
     * @return 权限节点
     */
    String getPermission();
    
    /**
     * 执行命令
     * 
     * @param sender 命令发送者
     * @param args 命令参数
     * @return 如果执行成功返回true
     */
    boolean execute(CommandSender sender, String[] args);
    
    /**
     * Tab补全
     * 
     * @param sender 命令发送者
     * @param args 当前参数
     * @return 补全建议列表
     */
    List<String> tabComplete(CommandSender sender, String[] args);
}
