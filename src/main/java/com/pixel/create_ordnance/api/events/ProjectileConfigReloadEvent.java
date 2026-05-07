package com.pixel.create_ordnance.api.events;

import net.neoforged.bus.api.Event;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;

/**
 * Fired when the physics library configuration is reloaded.
 * <p>
 * This event is not {@linkplain net.neoforged.bus.api.ICancellableEvent
 * cancellable}.
 * </p>
 * <p>
 * This event is fired on the
 * {@linkplain net.neoforged.neoforge.common.NeoForge#EVENT_BUS NeoForge event
 * bus}.
 * </p>
 */
public class ProjectileConfigReloadEvent extends Event {

    private final ModConfig config;

    public ProjectileConfigReloadEvent(ModConfig config) {
        this.config = config;
    }

    public ModConfig getConfig() {
        return config;
    }

    public boolean isClient() {
        return FMLEnvironment.dist.isClient();
    }
}