package com.pixel.create_ordnance.mechanics.physics.modules.aero;

import com.pixel.create_ordnance.mechanics.physics.core.IPhysicsModule;
import com.pixel.create_ordnance.mechanics.physics.core.ModuleOutput;
import com.pixel.create_ordnance.mechanics.physics.core.PhysicsConstants;
import com.pixel.create_ordnance.mechanics.physics.core.PhysicsContext;

import net.minecraft.world.phys.Vec3;

/**
 * Lift Module (V2 Protocol) — FORCE 2 POINTS.
 * <p>
 * Computes the lift force perpendicular to the velocity vector,
 * proportional to the Angle of Attack and lateral surface area.
 * <p>
 * This module unifies the old "SideForce" and "Lift" concepts:
 * both are the same physical phenomenon in 3D (force ⊥ velocity ∝ AoA).
 * <p>
 * Differential application at front/back lateral areas creates
 * emergent stabilization/destabilization torque.
 * <p>
 * <b>Future:</b> CGS pilot can inject aileron-based lift modifiers.
 */
public class LiftModule implements IPhysicsModule {

    private static final String MODULE_ID = "lift";

    @Override
    public ModuleOutput compute(PhysicsContext context) {
        double speed = context.getSpeed();
        double aoa = context.angleOfAttack;
        double stallAngle = Math.toRadians(PhysicsConstants.getStallAngle());
        double fluidDensity = context.getEnvironmentDensity();

        // No lift without velocity
        if (speed < PhysicsConstants.AOA_SAFETY_EPSILON) {
            return ModuleOutput.none(MODULE_ID);
        }

        // Lift coefficient: linear below stall, drops after stall
        double liftCoeff;
        if (Math.abs(aoa) <= stallAngle) {
            liftCoeff = aoa / stallAngle;
        } else {
            double excess = Math.abs(aoa) - stallAngle;
            double dropoff = Math.exp(-excess);
            liftCoeff = Math.signum(aoa) * dropoff;
        }

        // Lift direction: perpendicular to velocity in the AoA plane (LOCAL SPACE)
        Vec3 noseDirLocal = new Vec3(0, 1, 0); // Nose is +Y in local
        Vec3 velNormLocal = context.velocityLocal.normalize();

        // Lift is the component of nose direction perpendicular to velocity
        Vec3 liftDirLocal = noseDirLocal.subtract(velNormLocal.scale(noseDirLocal.dot(velNormLocal)));
        if (liftDirLocal.lengthSqr() < PhysicsConstants.AOA_SAFETY_EPSILON) {
            return ModuleOutput.none(MODULE_ID);
        }
        liftDirLocal = liftDirLocal.normalize();

        // Dynamic pressure = 0.5 × ρ × v²
        double dynamicPressure = 0.5 * fluidDensity * speed * speed;

        // Lateral areas from analyzer
        double areaBack = context.stats.areaLateralBack();
        double areaFront = context.stats.areaLateralFront();

        double stabilityFactor = PhysicsConstants.getGlobalLiftMultiplier();

        // Force per half = CL × q × area × stabilityFactor
        double forceBack = liftCoeff * dynamicPressure * areaBack * stabilityFactor;
        double forceFront = liftCoeff * dynamicPressure * areaFront * stabilityFactor;

        Vec3 liftBackLocal = liftDirLocal.scale(forceBack);
        Vec3 liftFrontLocal = liftDirLocal.scale(forceFront);

        return ModuleOutput.forceDual(MODULE_ID, liftBackLocal, liftFrontLocal);
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
        return ModuleCapability.FORCE_DUAL;
    }
}
