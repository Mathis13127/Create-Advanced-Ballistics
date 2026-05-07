package com.pixel.create_ordnance.mechanics.scanning;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.pixel.create_ordnance.CreateOrdnance;
import com.pixel.create_ordnance.network.ClientboundProjectileSyncPacket;
import com.pixel.create_ordnance.registry.ProjectileComponentRegistry;
import com.pixel.create_ordnance.registry.ProjectileScrapperRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import net.minecraft.resources.ResourceLocation;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = CreateOrdnance.MODID)
public class AutoScanHandler {

    private static final int SCAN_DELAY_TICKS = 3; // 0.15 seconds

    // Player UUID -> Scan Task
    private static final Map<UUID, ScanTask> PENDING_SCANS = new ConcurrentHashMap<>();

    private record ScanTask(ServerLevel level, BlockPos pos, int ticksRemaining) {
        public ScanTask tick() {
            return new ScanTask(level, pos, ticksRemaining - 1);
        }
    }

    @SubscribeEvent
    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel().isClientSide())
            return;
        if (!(event.getEntity() instanceof ServerPlayer player))
            return;

        BlockState state = event.getState();
        if (ProjectileComponentRegistry.isComponent(state.getBlock())) {
            // 2. Schedule Scan (Reset timer if already pending)
            BlockPos pos = event.getPos();
            if (event.getLevel() instanceof ServerLevel serverLevel) {
                PENDING_SCANS.put(player.getUUID(), new ScanTask(serverLevel, pos, SCAN_DELAY_TICKS));
            }
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel().isClientSide())
            return;
        if (!(event.getPlayer() instanceof ServerPlayer player))
            return;

        BlockState state = event.getState();
        if (ProjectileComponentRegistry.isComponent(state.getBlock())) {
            // Schedule scan for neighbors (short delay to ensure block is removed)
            if (event.getLevel() instanceof ServerLevel serverLevel) {
                for (Direction dir : Direction.values()) {
                    BlockPos neighbor = event.getPos().relative(dir);
                    BlockState neighborState = serverLevel.getBlockState(neighbor);
                    if (ProjectileComponentRegistry.isComponent(neighborState.getBlock())) {
                        PENDING_SCANS.put(player.getUUID(), new ScanTask(serverLevel, neighbor, 3));
                    }
                }
            }
        }
    }

    public static void tick() {
        PENDING_SCANS.forEach((uuid, task) -> {
            // Decrement timer
            ScanTask next = task.tick();

            if (next.ticksRemaining <= 0) {
                // Execute Scan
                executeScan(uuid, next);
                PENDING_SCANS.remove(uuid);
            } else {
                // Update task
                PENDING_SCANS.put(uuid, next);
            }
        });
    }

    private static void executeScan(UUID playerUUID, ScanTask task) {
        ServerPlayer player = task.level.getServer().getPlayerList().getPlayer(playerUUID);
        if (player == null)
            return; // Player offline
        performScanLogic(task.level, task.pos, player, ProjectileScrapperRegistry.LINEAR);
    }

    public static void performInstantScan(ServerLevel level, BlockPos pos, ServerPlayer player, ResourceLocation scrapperId) {
        performScanLogic(level, pos, player, scrapperId);
    }

    private static void performScanLogic(ServerLevel level, BlockPos pos, ServerPlayer player, ResourceLocation scrapperId) {
        BlockState state = level.getBlockState(pos);

        // Validation: Block might have been broken
        if (!ProjectileComponentRegistry.isComponent(state.getBlock()))
            return;

        Direction facing = Direction.UP;
        if (state.hasProperty(BlockStateProperties.FACING)) {
            facing = state.getValue(BlockStateProperties.FACING);
        }

        try {
            // 1. Scrap using Registry
            com.pixel.create_ordnance.mechanics.scanning.IProjectileScrapper scrapper = ProjectileScrapperRegistry
                            .get(scrapperId)
                            .orElse(ProjectileScrapperRegistry.getDefault())
                            .create(level, pos, facing);
            
            ScrapResult rawResult = scrapper.scan(1.0f); // Default scale

            // 2. Validate (By performing analysis)
            // This ensures the assembly is valid before syncing to client
            ProjectileAnalyzer.analyze(rawResult);

            // 3. Sync to Client
            PacketDistributor.sendToPlayer(player,
                    new ClientboundProjectileSyncPacket(rawResult));

        } catch (Exception e) {
            // Mercy is dead.
            CreateOrdnance.LOGGER.error("[Ordnance] Auto-Scan Failed at {}: {}", pos,
                    e.getMessage());
        }
    }
}