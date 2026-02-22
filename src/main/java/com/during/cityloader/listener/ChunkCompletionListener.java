package com.during.cityloader.listener;

import com.during.cityloader.generator.CityBlockPopulator;
import com.during.cityloader.worldgen.ChunkFixer;
import com.during.cityloader.worldgen.IDimensionInfo;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

import java.util.function.Predicate;

/**
 * 区块补全监听器：在区块加载后执行跨区块修复。
 */
public class ChunkCompletionListener implements Listener {

    private final CityBlockPopulator cityBlockPopulator;
    private final Predicate<World> worldGenerationPredicate;

    public ChunkCompletionListener(CityBlockPopulator cityBlockPopulator,
                                   Predicate<World> worldGenerationPredicate) {
        this.cityBlockPopulator = cityBlockPopulator;
        this.worldGenerationPredicate = worldGenerationPredicate;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChunkLoad(ChunkLoadEvent event) {
        World world = event.getWorld();
        if (!shouldEnableGeneration(world) || cityBlockPopulator == null) {
            return;
        }
        Chunk chunk = event.getChunk();
        IDimensionInfo dimensionInfo = cityBlockPopulator.getOrCreateDimensionInfo(world);
        if (dimensionInfo == null) {
            return;
        }
        ChunkFixer.fix(dimensionInfo, chunk.getX(), chunk.getZ());
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
