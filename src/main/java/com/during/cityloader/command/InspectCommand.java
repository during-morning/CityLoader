package com.during.cityloader.command;

import com.during.cityloader.CityLoaderPlugin;
import com.during.cityloader.generator.CityBlockPopulator;
import com.during.cityloader.util.ChunkCoord;
import com.during.cityloader.worldgen.IDimensionInfo;
import com.during.cityloader.worldgen.gen.GlobalCompletionQueue;
import com.during.cityloader.worldgen.lost.BuildingInfo;
import com.during.cityloader.worldgen.lost.City;
import com.during.cityloader.worldgen.lost.cityassets.CityStyle;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 决策检查命令
 * 输出指定区块的城市判定与后处理队列指标。
 */
public class InspectCommand implements SubCommand {

    private final CityLoaderPlugin plugin;

    public InspectCommand(CityLoaderPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "inspect";
    }

    @Override
    public String getDescription() {
        return "输出区块城市决策摘要与后处理队列指标";
    }

    @Override
    public String getUsage() {
        return "/cityloader inspect [chunkX chunkZ] 或 /cityloader inspect <world> <chunkX> <chunkZ>";
    }

    @Override
    public String getPermission() {
        return "cityloader.inspect";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Target target = resolveTarget(sender, args);
        if (target == null) {
            return true;
        }

        CityBlockPopulator populator = plugin.getCityBlockPopulator();
        if (populator == null) {
            sender.sendMessage("§c生成器尚未初始化");
            return true;
        }

        IDimensionInfo dimensionInfo = populator.getOrCreateDimensionInfo(target.world());
        if (dimensionInfo == null) {
            sender.sendMessage("§c无法创建维度上下文");
            return true;
        }

        ChunkCoord coord = new ChunkCoord(dimensionInfo.dimension(), target.chunkX(), target.chunkZ());
        BuildingInfo info = BuildingInfo.getBuildingInfo(coord, dimensionInfo);
        float cityFactor = City.getCityFactor(coord, dimensionInfo, dimensionInfo.getProfile());
        GlobalCompletionQueue.Snapshot queue = GlobalCompletionQueue.snapshot(target.world());
        CityStyle style = info.getCityStyle();

        sender.sendMessage("§6§l=== CityLoader Inspect ===");
        sender.sendMessage("§7world=§f" + target.world().getName()
                + " §7chunk=§f[" + target.chunkX() + ", " + target.chunkZ() + "]");
        sender.sendMessage("§7cityFactor=§f" + String.format(Locale.ROOT, "%.4f", cityFactor)
                + " §7threshold=§f" + String.format(Locale.ROOT, "%.4f", dimensionInfo.getProfile().getCityThreshold()));
        sender.sendMessage("§7isCity=§f" + info.isCity
                + " §7hasBuilding=§f" + info.hasBuilding
                + " §7cityLevel=§f" + info.getCityLevel());
        sender.sendMessage("§7ground=§f" + info.groundLevel
                + " §7cityGround=§f" + info.getCityGroundLevel()
                + " §7water=§f" + info.waterLevel);
        sender.sendMessage("§7floors=§f" + info.floors
                + " §7cellars=§f" + info.cellars
                + " §7maxY=§f" + info.getMaxHeight());
        sender.sendMessage("§7cityStyle=§f" + (style == null ? "<none>" : style.getName())
                + " §7building=§f" + (info.buildingType == null ? "<none>" : info.buildingType.getName()));
        sender.sendMessage("§7todo.palette=§f" + info.getPalettePostTodoCount()
                + " §7todo.post=§f" + info.getPostTodoCount());
        sender.sendMessage("§7queue.pending=§f" + queue.pending()
                + " §7enqueued=§f" + queue.totalEnqueued()
                + " §7executed=§f" + queue.totalExecuted()
                + " §7requeued=§f" + queue.totalRequeued());
        sender.sendMessage("§6§l==========================");
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            if (sender instanceof Player player) {
                completions.add(String.valueOf(player.getLocation().getChunk().getX()));
            }
            Bukkit.getWorlds().forEach(world -> completions.add(world.getName()));
            return completions;
        }
        if (args.length == 2 && sender instanceof Player player) {
            completions.add(String.valueOf(player.getLocation().getChunk().getX()));
            completions.add(String.valueOf(player.getLocation().getChunk().getZ()));
            return completions;
        }
        if (args.length == 3 && sender instanceof Player player) {
            completions.add(String.valueOf(player.getLocation().getChunk().getZ()));
        }
        return completions;
    }

    private Target resolveTarget(CommandSender sender, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("§c控制台请使用: /cityloader inspect <world> <chunkX> <chunkZ>");
                return null;
            }
            return new Target(player.getWorld(), player.getLocation().getChunk().getX(), player.getLocation().getChunk().getZ());
        }

        if (args.length == 2) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("§c控制台请使用: /cityloader inspect <world> <chunkX> <chunkZ>");
                return null;
            }
            Integer x = parseInt(args[0]);
            Integer z = parseInt(args[1]);
            if (x == null || z == null) {
                sender.sendMessage("§cchunkX/chunkZ 必须是整数");
                return null;
            }
            return new Target(player.getWorld(), x, z);
        }

        if (args.length == 3) {
            World world = Bukkit.getWorld(args[0]);
            if (world == null) {
                sender.sendMessage("§c找不到世界: " + args[0]);
                return null;
            }
            Integer x = parseInt(args[1]);
            Integer z = parseInt(args[2]);
            if (x == null || z == null) {
                sender.sendMessage("§cchunkX/chunkZ 必须是整数");
                return null;
            }
            return new Target(world, x, z);
        }

        sender.sendMessage("§e用法: " + getUsage());
        return null;
    }

    private Integer parseInt(String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private record Target(World world, int chunkX, int chunkZ) {
    }
}
