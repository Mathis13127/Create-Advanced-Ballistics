package com.pixel.create_ordnance.api.events;

import com.pixel.create_ordnance.mechanics.physics.core.PhysicsContext;

import net.minecraft.world.phys.Vec3;

import net.neoforged.bus.api.Event;

/**
 * Fired during the physics update tick of a projectile.
 * This event allows modifying forces or torques applied to the projectile.
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
public class ProjectilePhysicsEvent extends Event {

    private final PhysicsContext context;
    private Vec3 totalForce;
    private Vec3 totalTorque;

    public ProjectilePhysicsEvent(PhysicsContext context, Vec3 totalForce, Vec3 totalTorque) {
        this.context = context;
        this.totalForce = totalForce;
        this.totalTorque = totalTorque;
    }

    public PhysicsContext getContext() {
        return context;
    }

    /**
     * The total accumulated force that will be applied to the projectile this tick.
     * Addons can modify this vector to add custom forces (Magic, Wind, Magnetism).
     */
    public Vec3 getTotalForce() {
        return totalForce;
    }

    public void setTotalForce(Vec3 totalForce) {
        this.totalForce = totalForce;
    }

    public Vec3 getTotalTorque() {
        return totalTorque;
    }

    public void setTotalTorque(Vec3 totalTorque) {
        this.totalTorque = totalTorque;
    }
}