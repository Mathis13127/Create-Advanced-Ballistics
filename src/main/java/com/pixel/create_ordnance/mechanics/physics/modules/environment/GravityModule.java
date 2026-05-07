package com.pixel.create_ordnance.mechanics.physics.modules.environment;

import com.pixel.create_ordnance.mechanics.physics.core.IPhysicsModule;
import com.pixel.create_ordnance.mechanics.physics.core.ModuleOutput;
import com.pixel.create_ordnance.mechanics.physics.core.PhysicsConstants;
import com.pixel.create_ordnance.mechanics.physics.core.PhysicsContext;

import net.minecraft.world.phys.Vec3;

import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Gravity Module (V2 Protocol) — FORCE SIMPLE.
 * <p>
 * Applies a uniform downward gravitational force at the center of mass.
 * The force is proportional to: {@code mass × gravityStrength}.
 * <p>
 * Uses the configurable gravity multiplier from
 * {@link PhysicsConstants#getGravityStrength()}.
 */
public class GravityModule implements IPhysicsModule {

    private static final String MODULE_ID = "gravity";

    @Override
    public ModuleOutput compute(PhysicsContext context) {
        double gravityStrength = PhysicsConstants.getGravityStrength();
        double forceMagnitude = context.totalMass * gravityStrength;

        // Gravity is a WORLD FORCE: (0, -g, 0)
        Vec3 gravityWorld = new Vec3(0, -forceMagnitude, 0);

        // Transform to LOCAL Space: F_local = q_inv * F_world
        Vector3f gravityLocal = new Vector3f(
                (float) gravityWorld.x, (float) gravityWorld.y, (float) gravityWorld.z);
        Quaternionf invOrientation = new Quaternionf(context.orientation).invert();
        invOrientation.transform(gravityLocal);

        return ModuleOutput.forceSimple(MODULE_ID, new Vec3(gravityLocal.x, gravityLocal.y, gravityLocal.z));
    }

    @Override
    public String getModuleId() {
        return MODULE_ID;
    }

    @Override
    public Phase getPhase() {
        return Phase.FORCE;
    }

    @Override
    public ModuleCapability getCapability() {
        return ModuleCapability.FORCE_SIMPLE;
    }
}