package com.pixel.create_ordnance.foundation.events;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.pixel.create_ordnance.CreateOrdnance;
import com.pixel.create_ordnance.mechanics.scanning.CurrentProjectileManager;
import com.pixel.create_ordnance.mechanics.scanning.ScrapResult;
import com.pixel.create_ordnance.network.ServerboundRequestScanPacket;
import com.pixel.create_ordnance.registry.ProjectileComponentRegistry;
import com.pixel.create_ordnance.registry.ProjectileScrapperRegistry;

import com.simibubi.create.content.equipment.goggles.GogglesItem;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = CreateOrdnance.MODID, value = Dist.CLIENT)
public class OrdnanceClientEventHandler {

    // Cooldown management for scan requests
    private static final Map<BlockPos, Long> SCAN_COOLDOWNS = new HashMap<>();
    private static final long COOLDOWN_MS = 350;
    private static int purgeCounter = 0;
    private static final int PURGE_INTERVAL = 100;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null)
            return;

        // Periodic cleanup of expired cooldown entries to prevent memory leak
        if (++purgeCounter >= PURGE_INTERVAL) {
            purgeCounter = 0;
            long now = System.currentTimeMillis();
            Iterator<Map.Entry<BlockPos, Long>> iterator = SCAN_COOLDOWNS.entrySet().iterator();
            while (iterator.hasNext()) {
                if (now - iterator.next().getValue() > COOLDOWN_MS) {
                    iterator.remove();
                }
            }
        }

        boolean wearingGoggles = GogglesItem.isWearingGoggles(mc.player);

        if (!wearingGoggles) {
            CurrentProjectileManager.clearActiveProjectile();
            return;
        }

        HitResult hit = mc.hitResult;
        if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
            BlockHitResult blockHit = (BlockHitResult) hit;
            BlockPos pos = blockHit.getBlockPos();
            BlockState state = mc.level.getBlockState(pos);

            if (ProjectileComponentRegistry.isComponent(state.getBlock())) {
                ScrapResult cached = CurrentProjectileManager
                        .getCached(pos);

                if (cached != null) {
                    CurrentProjectileManager
                            .setActiveProjectile(cached);
                } else {
                    // Not cached -> Request Scan (with debounce)
                    long now = System.currentTimeMillis();
                    if (!SCAN_COOLDOWNS.containsKey(pos) || now - SCAN_COOLDOWNS.get(pos) > COOLDOWN_MS) {
                        SCAN_COOLDOWNS.put(pos, now);
                        PacketDistributor.sendToServer(
                                new ServerboundRequestScanPacket(pos,
                                        ProjectileScrapperRegistry.LINEAR));
                    }
                    CurrentProjectileManager.clearActiveProjectile();
                }
            } else {
                CurrentProjectileManager.clearActiveProjectile();
            }
        } else {
            CurrentProjectileManager.clearActiveProjectile();
        }
    }
}