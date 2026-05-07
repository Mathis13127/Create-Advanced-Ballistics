package com.pixel.create_ordnance.api.events;

import com.pixel.create_ordnance.content.entity.ProjectileEntity;

import net.minecraft.world.entity.Entity.RemovalReason;

import net.neoforged.bus.api.Event;

/**
 * Fired when a projectile entity is destroyed or "dies".
 * <p>
 * This event is not {@linkplain net.neoforged.bus.api.ICancellableEvent
 * cancellable}.
 * </p>
 * <p>
 * This event is fired on the
 * {@linkplain net.neoforged.neoforge.common.NeoForge#EVENT_BUS NeoForge event
 * bus},
 * only on the {@linkplain net.neoforged.fml.LogicalSide#SERVER logical server}.
 * </p>
 */
public class ProjectileDeathEvent extends Event {

    private final ProjectileEntity projectile;
    private final RemovalReason reason;

    public ProjectileDeathEvent(ProjectileEntity projectile, RemovalReason reason) {
        this.projectile = projectile;
        this.reason = reason;
    }

    public ProjectileEntity getProjectile() {
        return projectile;
    }

    public RemovalReason getReason() {
        return reason;
    }
}