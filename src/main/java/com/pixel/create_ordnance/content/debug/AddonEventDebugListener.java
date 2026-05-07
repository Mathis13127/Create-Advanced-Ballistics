package com.pixel.create_ordnance.content.debug;

import com.pixel.create_ordnance.CreateOrdnance;
import com.pixel.create_ordnance.api.events.ProjectileAnalyzeEvent;
import com.pixel.create_ordnance.api.events.ProjectileCGSEvent;
import com.pixel.create_ordnance.api.events.ProjectileDeathEvent;
import com.pixel.create_ordnance.api.events.ProjectileExplodeEvent;
import com.pixel.create_ordnance.api.events.ProjectileHurtEvent;
import com.pixel.create_ordnance.api.events.ProjectileImpactEvent;
import com.pixel.create_ordnance.api.events.ProjectileLaunchEvent;
import com.pixel.create_ordnance.api.events.ProjectilePhysicsEvent;
import com.pixel.create_ordnance.api.events.ProjectilePreTickEvent;
import com.pixel.create_ordnance.config.OrdnanceConfigs;

import net.minecraft.network.chat.Component;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * DEBUG LISTENER
 * Prints chat messages when API events are fired.
 * Intended to verify the API implementation.
 */
@EventBusSubscriber(modid = CreateOrdnance.MODID)
public class AddonEventDebugListener {

    private static long lastPhysicsMsgTime = 0;
    private static long lastPreTickMsgTime = 0;
    private static long lastCGSMsgTime = 0;

    @SubscribeEvent
    public static void onAnalyze(ProjectileAnalyzeEvent event) {
        broadcast("[API] Analyze Event | Components: " + event.getScrapResult().blockIds().size());
        // Test modification
        // event.setTotalFuel(event.getTotalFuel() + 1000);
    }

    @SubscribeEvent
    public static void onLaunch(ProjectileLaunchEvent event) {
        broadcast("[API] Launch Event | Entity: " + event.getProjectile().getId());
    }

    @SubscribeEvent
    public static void onPreTick(ProjectilePreTickEvent event) {
        if (shouldPrint(lastPreTickMsgTime)) {
            broadcast("[API] PreTick Event (Throttled) | Entity: " + event.getProjectile().getId());
            lastPreTickMsgTime = System.currentTimeMillis();
        }
    }

    @SubscribeEvent
    public static void onPhysics(ProjectilePhysicsEvent event) {
        if (shouldPrint(lastPhysicsMsgTime)) {
            broadcast("[API] Physics Event (Throttled) | Force: " + event.getTotalForce());
            lastPhysicsMsgTime = System.currentTimeMillis();
        }
    }

    @SubscribeEvent
    public static void onImpact(ProjectileImpactEvent event) {
        broadcast("[API] Impact Event | Entity: " + event.getProjectile().getId());
    }

    @SubscribeEvent
    public static void onHurt(ProjectileHurtEvent event) {
        broadcast("[API] Hurt Event | Source: " + event.getSource().getMsgId() + " | Amount: " + event.getAmount());
    }

    @SubscribeEvent
    public static void onExplode(ProjectileExplodeEvent event) {
        broadcast("[API] Explode Event | Contraption: " + event.getContraption().entity.getId());
    }

    @SubscribeEvent
    public static void onCGS(ProjectileCGSEvent event) {
        if (shouldPrint(lastCGSMsgTime)) {
            broadcast("[API] CGS Event | Type: " + event.getType().getId()
                    + " | Base: P=" + String.format("%.2f", event.getBaseResult().pitchDemand())
                    + " Y=" + String.format("%.2f", event.getBaseResult().yawDemand())
                    + " T=" + String.format("%.2f", event.getBaseResult().throttle()));
            lastCGSMsgTime = System.currentTimeMillis();
        }
    }

    @SubscribeEvent
    public static void onDeath(ProjectileDeathEvent event) {
        broadcast("[API] Death Event | Reason: " + event.getReason());
    }

    private static boolean shouldPrint(long lastTime) {
        return System.currentTimeMillis() - lastTime > 2000; // 2 seconds throttle
    }

    private static void broadcast(String msg) {
        // Controlled by MixinDebug config
        if (!OrdnanceConfigs.COMMON.divers.mixins.mixinDebug.get())
            return;

        // Send to all players on server
        ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()
                .forEach(player -> player.sendSystemMessage(Component.literal("§e" + msg)));

        // Also log to console
        CreateOrdnance.LOGGER.info(msg);
    }
}