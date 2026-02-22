package com.during.cityloader.listener;

import com.during.cityloader.generator.CityBlockPopulator;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldInitEvent;

import java.util.function.Predicate;
import java.util.logging.Logger;

/**
 * 世界初始化监听器
 * 负责将CityBlockPopulator添加到新加载的世界中
 * 
 * @author During
 * @since 1.4.0
 */
public class WorldInitListener implements Listener {

    private final Logger logger;
    private final CityBlockPopulator cityBlockPopulator;
    private final Predicate<World> worldGenerationPredicate;

    public WorldInitListener(Logger logger,
                             CityBlockPopulator cityBlockPopulator,
                             Predicate<World> worldGenerationPredicate) {
        this.logger = logger;
        this.cityBlockPopulator = cityBlockPopulator;
        this.worldGenerationPredicate = worldGenerationPredicate;
    }

    @EventHandler
    public void onWorldInit(WorldInitEvent event) {
        World world = event.getWorld();

        logger.info("========================================");
        logger.info("检测到世界初始化事件: " + world.getName());
        logger.info("世界环境: " + world.getEnvironment());
        logger.info("世界类型: " + world.getWorldType());
        
        if (shouldEnableGeneration(world)) {
            world.getPopulators().add(cityBlockPopulator);
            logger.info("✓ 已将CityBlockPopulator添加到世界: " + world.getName());
            logger.info("当前世界的BlockPopulator数量: " + world.getPopulators().size());
        } else {
            logger.info("✗ 根据当前配置跳过世界: " + world.getName() + " (环境: " + world.getEnvironment() + ")");
        }
        
        logger.info("========================================");
    }

    private boolean shouldEnableGeneration(World world) {
        if (worldGenerationPredicate == null) {
            return world.getEnvironment() == World.Environment.NORMAL;
        }
        try {
            return worldGenerationPredicate.test(world);
        } catch (Exception ignored) {
            return false;
        }
    }
}
