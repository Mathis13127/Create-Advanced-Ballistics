package com.pixel.create_ordnance.api.events;

import com.pixel.create_ordnance.content.entity.ProjectileEntity;

import net.minecraft.world.phys.Vec3;

import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/**
 * Fired at the beginning of a projectile's tick.
 * <p>
 * This event is {@linkplain ICancellableEvent
 * cancellable}.
 * Canceling this event skips the projectile's tick logic for that tick.
 * </p>
 * <p>
 * This event is fired on the
 * {@linkplain net.neoforged.neoforge.common.NeoForge#EVENT_BUS NeoForge event
 * bus},
 * only on the {@linkplain net.neoforged.fml.LogicalSide#SERVER logical server}.
 * </p>
 */
public class ProjectilePreTickEvent extends Event implements ICancellableEvent {

    private final ProjectileEntity projectile;

    public ProjectilePreTickEvent(ProjectileEntity projectile) {
        this.projectile = projectile;
    }

    public ProjectileEntity getProjectile() {
        return projectile;
    }

    public Vec3 getVelocity() {
        return projectile.getDeltaMovement();
    }
}