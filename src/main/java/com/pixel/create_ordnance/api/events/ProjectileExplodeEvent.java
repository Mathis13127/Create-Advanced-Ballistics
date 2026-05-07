package com.pixel.create_ordnance.api.events;

import com.pixel.create_ordnance.content.contraption.ProjectileContraption;
import com.pixel.create_ordnance.content.entity.ProjectileEntity;

import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/**
 * Fired when a projectile explodes.
 * <p>
 * This event is {@linkplain ICancellableEvent
 * cancellable}.
 * Canceling this event prevents the explosion from occurring.
 * </p>
 * <p>
 * This event is fired on the
 * {@linkplain net.neoforged.neoforge.common.NeoForge#EVENT_BUS NeoForge event
 * bus},
 * only on the {@linkplain net.neoforged.fml.LogicalSide#SERVER logical server}.
 * </p>
 */
public class ProjectileExplodeEvent extends Event implements ICancellableEvent {

    private final ProjectileEntity projectile;
    private final ProjectileContraption contraption;

    public ProjectileExplodeEvent(ProjectileEntity projectile, ProjectileContraption contraption) {
        this.projectile = projectile;
        this.contraption = contraption;
    }

    public ProjectileEntity getProjectile() {
        return projectile;
    }

    public ProjectileContraption getContraption() {
        return contraption;
    }
}