package com.pixel.create_ordnance.api.events;

import com.pixel.create_ordnance.content.entity.ProjectileEntity;

import net.minecraft.world.phys.HitResult;

import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/**
 * Fired when a projectile impacts a block or entity.
 * <p>
 * This event is {@linkplain ICancellableEvent
 * cancellable}.
 * Canceling this event prevents the default impact behavior (like sticking or
 * breaking blocks).
 * </p>
 * <p>
 * This event is fired on the
 * {@linkplain net.neoforged.neoforge.common.NeoForge#EVENT_BUS NeoForge event
 * bus},
 * only on the {@linkplain net.neoforged.fml.LogicalSide#SERVER logical server}.
 * </p>
 */
public class ProjectileImpactEvent extends Event implements ICancellableEvent {

    private final ProjectileEntity projectile;
    private final HitResult hitResult;

    public ProjectileImpactEvent(ProjectileEntity projectile, HitResult hitResult) {
        this.projectile = projectile;
        this.hitResult = hitResult;
    }

    public ProjectileEntity getProjectile() {
        return projectile;
    }

    public HitResult getHitResult() {
        return hitResult;
    }
}