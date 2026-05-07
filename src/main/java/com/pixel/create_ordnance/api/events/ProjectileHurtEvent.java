package com.pixel.create_ordnance.api.events;

import com.pixel.create_ordnance.content.entity.ProjectileEntity;

import net.minecraft.world.damagesource.DamageSource;

import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/**
 * Fired when a projectile entity is damaged.
 * <p>
 * This event is {@linkplain ICancellableEvent
 * cancellable}.
 * Canceling this event prevents the projectile from taking damage.
 * </p>
 * <p>
 * This event is fired on the
 * {@linkplain net.neoforged.neoforge.common.NeoForge#EVENT_BUS NeoForge event
 * bus},
 * only on the {@linkplain net.neoforged.fml.LogicalSide#SERVER logical server}.
 * </p>
 */
public class ProjectileHurtEvent extends Event implements ICancellableEvent {

    private final ProjectileEntity projectile;
    private final DamageSource source;
    private float amount;

    public ProjectileHurtEvent(ProjectileEntity projectile, DamageSource source, float amount) {
        this.projectile = projectile;
        this.source = source;
        this.amount = amount;
    }

    public ProjectileEntity getProjectile() {
        return projectile;
    }

    public DamageSource getSource() {
        return source;
    }

    public float getAmount() {
        return amount;
    }

    public void setAmount(float amount) {
        this.amount = amount;
    }
}