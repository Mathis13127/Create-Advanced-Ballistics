package com.pixel.create_ordnance.api.events;

import com.pixel.create_ordnance.content.entity.ProjectileEntity;
import com.pixel.create_ordnance.mechanics.scanning.ScrapResult;

import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/**
 * Fired immediately before a projectile entity is spawned into the world.
 * <p>
 * This event is {@linkplain ICancellableEvent
 * cancellable}.
 * Canceling this event prevents the projectile from spawning.
 * </p>
 * <p>
 * This event is fired on the
 * {@linkplain net.neoforged.neoforge.common.NeoForge#EVENT_BUS NeoForge event
 * bus},
 * only on the {@linkplain net.neoforged.fml.LogicalSide#SERVER logical server}.
 * </p>
 */
public class ProjectileLaunchEvent extends Event implements ICancellableEvent {

    private final ProjectileEntity projectile;
    private final ScrapResult scrapResult;

    public ProjectileLaunchEvent(ProjectileEntity projectile, ScrapResult scrapResult) {
        this.projectile = projectile;
        this.scrapResult = scrapResult;
    }

    public ProjectileEntity getProjectile() {
        return projectile;
    }

    public ScrapResult getScrapResult() {
        return scrapResult;
    }
}