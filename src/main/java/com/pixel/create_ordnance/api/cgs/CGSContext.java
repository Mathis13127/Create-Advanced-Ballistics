package com.pixel.create_ordnance.api.cgs;

import java.util.UUID;

import com.pixel.create_ordnance.content.entity.ProjectileEntity;
import com.pixel.create_ordnance.mechanics.scanning.ProjectileStats;

import net.minecraft.world.phys.Vec3;

import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Immutable snapshot of projectile state passed to CGS controllers.
 * <p>
 * Contains all data a guidance algorithm needs to compute control demands,
 * without requiring direct access to the entity (though the entity reference
 * is available for edge cases).
 * <p>
 * This mirrors the approach of
 * {@link com.pixel.create_ordnance.mechanics.physics.core.PhysicsContext}
 * but is scoped to CGS-relevant data only.
 *
 * @param entity          The projectile entity (for edge-case access)
 * @param entityId        UUID of the projectile
 * @param position        World-space position (center of mass)
 * @param velocity        World-space velocity vector (blocks/tick)
 * @param orientation     Current orientation quaternion
 * @param angularVelocity Angular velocity in local space (rad/tick)
 * @param speed           Scalar speed (blocks/tick), precomputed for convenience
 * @param angleOfAttack   Current angle of attack (radians)
 * @param noseDirection   World-space nose direction unit vector
 * @param currentFuel     Current remaining fuel
 * @param maxFuel         Maximum fuel capacity
 * @param stats           Full projectile stats (includes maxNozzleTilt, baseThrust, etc.)
 * @param deltaTime       Time step for this tick (usually 1.0)
 * @param tickCount       Entity tick count since spawn
 */
public record CGSContext(
        ProjectileEntity entity,
        UUID entityId,
        Vec3 position,
        Vec3 velocity,
        Quaternionf orientation,
        Vector3f angularVelocity,
        double speed,
        double angleOfAttack,
        Vec3 noseDirection,
        double currentFuel,
        double maxFuel,
        ProjectileStats stats,
        double deltaTime,
        int tickCount) {

    /**
     * Constructs a CGSContext by extracting all relevant data from a ProjectileEntity.
     * This is the primary factory method, called by CGSManager each tick.
     */
    public static CGSContext fromEntity(ProjectileEntity entity) {
        Vec3 velocity = entity.getDeltaMovement();
        double speed = velocity.length();

        Quaternionf orientation = new Quaternionf(entity.getOrientation());
        Vector3f nose = new Vector3f(0, 1, 0);
        orientation.transform(nose);
        Vec3 noseDirection = new Vec3(nose.x, nose.y, nose.z);

        // Compute AoA: angle between nose direction and velocity
        double angleOfAttack = 0.0;
        if (speed > 1e-6) {
            double dot = noseDirection.dot(velocity.normalize());
            dot = Math.max(-1.0, Math.min(1.0, dot));
            angleOfAttack = Math.acos(dot);
        }

        return new CGSContext(
                entity,
                entity.getUUID(),
                entity.position(),
                velocity,
                orientation,
                new Vector3f(entity.getAngularVelocity()),
                speed,
                angleOfAttack,
                noseDirection,
                entity.getCurrentFuel(),
                entity.getMaxFuel(),
                entity.getStats(),
                1.0,
                entity.tickCount);
    }
}
