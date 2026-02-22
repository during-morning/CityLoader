package com.during.cityloader.version;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 版本管理器
 * 管理插件版本和兼容性检查
 * 
 * @author During
 * @since 1.4.0
 */
public class VersionManager {
    
    private final Logger logger;
    private final Plugin plugin;
    private final String pluginVersion;
    private final String requiredMinecraftVersion = "1.21";
    
    /**
     * 构造函数
     * 
     * @param logger 日志记录器
     * @param plugin 插件实例
     */
    public VersionManager(Logger logger, Plugin plugin) {
        this.logger = logger;
        this.plugin = plugin;
        this.pluginVersion = plugin.getDescription().getVersion();
    }
    
    /**
     * 检查版本兼容性
     * 
     * @return 是否兼容
     */
    public boolean checkCompatibility() {
        try {
            // 获取Minecraft版本
            String minecraftVersion = getMinecraftVersion();
            
            logger.info("检查版本兼容性...");
            logger.info("  Minecraft版本: " + minecraftVersion);
            logger.info("  要求最低版本: " + requiredMinecraftVersion);
            
            // 比较版本
            if (compareVersions(minecraftVersion, requiredMinecraftVersion) >= 0) {
                logger.info("版本兼容性检查通过");
                return true;
            } else {
                logger.severe("版本不兼容！");
                logger.severe("  当前Minecraft版本: " + minecraftVersion);
                logger.severe("  要求最低版本: " + requiredMinecraftVersion);
                logger.severe("  请升级服务器版本");
                return false;
            }
            
        } catch (Exception e) {
            logger.warning("版本检查失败: " + e.getMessage());
            logger.warning("将继续加载插件，但可能存在兼容性问题");
            return true; // 降级处理，允许继续加载
        }
    }
    
    /**
     * 获取Minecraft版本
     * 
     * @return Minecraft版本字符串
     */
    private String getMinecraftVersion() {
        String version = Bukkit.getVersion();
        
        // 从版本字符串中提取Minecraft版本号
        // 例如: "git-Paper-123 (MC: 1.21.1)"
        Pattern pattern = Pattern.compile("MC: ([0-9.]+)");
        Matcher matcher = pattern.matcher(version);
        
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        // 如果无法提取，返回Bukkit版本
        return Bukkit.getBukkitVersion();
    }
    
    /**
     * 比较两个版本号
     * 
     * @param version1 版本1
     * @param version2 版本2
     * @return 比较结果：正数表示version1 > version2，0表示相等，负数表示version1 < version2
     */
    private int compareVersions(String version1, String version2) {
        String[] parts1 = version1.split("\\.");
        String[] parts2 = version2.split("\\.");
        
        int maxLength = Math.max(parts1.length, parts2.length);
        
        for (int i = 0; i < maxLength; i++) {
            int v1 = i < parts1.length ? parseVersionPart(parts1[i]) : 0;
            int v2 = i < parts2.length ? parseVersionPart(parts2[i]) : 0;
            
            if (v1 != v2) {
                return v1 - v2;
            }
        }
        
        return 0;
    }
    
    /**
     * 解析版本号的一部分
     * 
     * @param part 版本号部分
     * @return 数字值
     */
    private int parseVersionPart(String part) {
        try {
            // 移除非数字字符
            String numericPart = part.replaceAll("[^0-9]", "");
            if (numericPart.isEmpty()) {
                return 0;
            }
            return Integer.parseInt(numericPart);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    
    /**
     * 获取插件版本
     * 
     * @return 插件版本字符串
     */
    public String getPluginVersion() {
        return pluginVersion;
    }
    
    /**
     * 显示版本信息
     */
    public void displayVersionInfo() {
        logger.info("=================================");
        logger.info("CityLoader v" + pluginVersion);
        logger.info("Minecraft版本: " + getMinecraftVersion());
        logger.info("Bukkit版本: " + Bukkit.getBukkitVersion());
        logger.info("服务器: " + Bukkit.getName() + " " + Bukkit.getVersion());
        logger.info("=================================");
    }
    
    /**
     * 获取服务器信息
     * 
     * @return 服务器信息字符串
     */
    public String getServerInfo() {
        return String.format("%s %s (MC: %s)",
            Bukkit.getName(),
            Bukkit.getVersion(),
            getMinecraftVersion());
    }
}
