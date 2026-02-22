package com.during.cityloader.worldgen.gen;

import org.bukkit.World;
import org.bukkit.block.BlockState;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 全局补全队列：承接超出 LimitedRegion 的延迟方块实体回写任务。
 */
public final class GlobalCompletionQueue {

    private static final Map<UUID, Deque<GenerationContext.BlockStateTask>> QUEUES = new ConcurrentHashMap<>();
    private static final Map<UUID, QueueStats> STATS = new ConcurrentHashMap<>();

    private GlobalCompletionQueue() {
    }

    public static void enqueue(World world, GenerationContext.BlockStateTask task) {
        if (world == null || task == null) {
            return;
        }
        QUEUES.computeIfAbsent(world.getUID(), id -> new ArrayDeque<>());
        QueueStats stats = STATS.computeIfAbsent(world.getUID(), id -> new QueueStats());
        stats.totalEnqueued.incrementAndGet();
        Deque<GenerationContext.BlockStateTask> queue = QUEUES.get(world.getUID());
        synchronized (queue) {
            queue.addLast(task);
        }
    }

    public static int drain(World world, int maxTasks) {
        if (world == null || maxTasks <= 0) {
            return 0;
        }
        Deque<GenerationContext.BlockStateTask> queue = QUEUES.get(world.getUID());
        if (queue == null) {
            return 0;
        }
        QueueStats stats = STATS.computeIfAbsent(world.getUID(), id -> new QueueStats());

        int executed = 0;
        int budget = maxTasks;
        while (budget-- > 0) {
            GenerationContext.BlockStateTask task;
            synchronized (queue) {
                task = queue.pollFirst();
            }
            if (task == null) {
                break;
            }

            if (!world.isChunkLoaded(task.x() >> 4, task.z() >> 4)) {
                synchronized (queue) {
                    queue.addLast(task);
                }
                stats.totalRequeued.incrementAndGet();
                continue;
            }

            try {
                BlockState state = world.getBlockAt(task.x(), task.y(), task.z()).getState();
                if (state != null && task.mutator().mutate(state)) {
                    state.update(true, false);
                }
                executed++;
                stats.totalExecuted.incrementAndGet();
            } catch (Exception ignored) {
                synchronized (queue) {
                    queue.addLast(task);
                }
                stats.totalRequeued.incrementAndGet();
            }
        }

        synchronized (queue) {
            if (queue.isEmpty()) {
                QUEUES.remove(world.getUID());
            }
        }
        return executed;
    }

    public static Snapshot snapshot(World world) {
        if (world == null) {
            return new Snapshot(0, 0, 0, 0);
        }
        UUID id = world.getUID();
        Deque<GenerationContext.BlockStateTask> queue = QUEUES.get(id);
        int pending = 0;
        if (queue != null) {
            synchronized (queue) {
                pending = queue.size();
            }
        }
        QueueStats stats = STATS.get(id);
        if (stats == null) {
            return new Snapshot(pending, 0, 0, 0);
        }
        return new Snapshot(
                pending,
                stats.totalEnqueued.get(),
                stats.totalExecuted.get(),
                stats.totalRequeued.get());
    }

    public static void clear() {
        QUEUES.clear();
        STATS.clear();
    }

    private static final class QueueStats {
        private final AtomicLong totalEnqueued = new AtomicLong();
        private final AtomicLong totalExecuted = new AtomicLong();
        private final AtomicLong totalRequeued = new AtomicLong();
    }

    public record Snapshot(int pending, long totalEnqueued, long totalExecuted, long totalRequeued) {
    }
}
