package com.pixel.create_ordnance.foundation.events;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.pixel.create_ordnance.CreateOrdnance;
import com.pixel.create_ordnance.mechanics.physics.attraction.SpatialForceManager;
import com.pixel.create_ordnance.mechanics.scanning.AutoScanHandler;

import net.minecraft.core.BlockPos;

import net.minecraft.resources.ResourceKey;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import net.neoforged.bus.api.SubscribeEvent;

import net.neoforged.fml.common.EventBusSubscriber;

import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

@EventBusSubscriber(modid = CreateOrdnance.MODID)
public class OrdnanceServerEvents {

    // Map<Dimension, Map<BlockPos, TicksRemaining>>
    private static final Map<ResourceKey<Level>, Map<BlockPos, Integer>> SCHEDULED_REMOVALS = new ConcurrentHashMap<>();

    public static void scheduleBlockRemoval(Level level, BlockPos pos, int ticks) {
        if (level.isClientSide)
            return;
        SCHEDULED_REMOVALS.computeIfAbsent(level.dimension(), k -> new ConcurrentHashMap<>())
                .put(pos, ticks);
    }

    // =========================================================
    // SERVER LIFECYCLE: Attraction System
    // =========================================================

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        SpatialForceManager.init(event.getServer());
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        SpatialForceManager.shutdown();
    }

    // =========================================================
    // SERVER TICK
    // =========================================================

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        // Tick attraction sources for all loaded dimensions
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null && SpatialForceManager.getInstance() != null) {
            for (ServerLevel level : server.getAllLevels()) {
                SpatialForceManager.getInstance().tick(level);
            }
        }

        // Existing: AutoScanHandler tick
        AutoScanHandler.tick();

        // Existing: Scheduled block removals
        if (SCHEDULED_REMOVALS.isEmpty()) {
            return;
        }

        Iterator<Map.Entry<ResourceKey<Level>, Map<BlockPos, Integer>>> dimIterator = SCHEDULED_REMOVALS.entrySet()
                .iterator();

        while (dimIterator.hasNext()) {
            Map.Entry<ResourceKey<Level>, Map<BlockPos, Integer>> dimEntry = dimIterator.next();
            ResourceKey<Level> dimKey = dimEntry.getKey();
            Map<BlockPos, Integer> removals = dimEntry.getValue();

            if (server == null)
                return;

            ServerLevel level = server.getLevel(dimKey);
            if (level == null) {
                dimIterator.remove();
                continue;
            }

            Iterator<Map.Entry<BlockPos, Integer>> blockIterator = removals.entrySet().iterator();
            while (blockIterator.hasNext()) {
                Map.Entry<BlockPos, Integer> entry = blockIterator.next();
                int ticks = entry.getValue();

                if (ticks <= 0) {
                    BlockPos pos = entry.getKey();
                    if (level.getBlockState(pos).is(Blocks.REDSTONE_BLOCK)) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                    }
                    blockIterator.remove();
                } else {
                    entry.setValue(ticks - 1);
                }
            }

            if (removals.isEmpty()) {
                dimIterator.remove();
            }
        }
    }
}