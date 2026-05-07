package com.pixel.create_ordnance.mechanics.physics.modules.thrust;

import com.pixel.create_ordnance.config.OrdnanceCommonConfig.ThrustType;
import com.pixel.create_ordnance.mechanics.physics.core.IPhysicsModule;
import com.pixel.create_ordnance.mechanics.physics.core.ModuleOutput;
import com.pixel.create_ordnance.mechanics.physics.core.PhysicsConstants;
import com.pixel.create_ordnance.mechanics.physics.core.PhysicsContext;

import net.minecraft.world.phys.Vec3;

import org.joml.Vector3f;

/**
 * Vectorial Thrust Module (V2 Protocol) — FORCE SIMPLE + TORQUE.
 * <p>
 * Produces linear thrust along the nozzle direction (which may be tilted by
 * CGS)
 * and direct torque from the nozzle offset.
 * <p>
 * Includes:
 * <ul>
 * <li>Throttle control (0.0 → 1.0)</li>
 * <li>Fuel consumption check (no fuel = no thrust)</li>
 * <li>Optimal Density + Density Bandwidth (Lorentzian efficiency curve)</li>
 * <li>Nozzle tilt (from CGS pilot) → torque component</li>
 * </ul>
 * <p>
 * <b>CGS Dependency:</b> CGS pilot must set {@code pitchDemand} and
 * {@code yawDemand}
 * BEFORE Phase 1 execution. This module reads them from the context.
 */
public class VectorialThrustModule implements IPhysicsModule {

    private static final String MODULE_ID = "vectorial_thrust";

    @Override
    public ModuleOutput compute(PhysicsContext context) {
        // No thrust if no propulsion type
        if (context.thrustType == ThrustType.NONE) {
            return ModuleOutput.none(MODULE_ID);
        }

        // No thrust if no fuel
        if (context.currentFuel <= 0) {
            return ModuleOutput.none(MODULE_ID);
        }

        // Logic deactivation (e.g. extinguished in water)
        if (context.isExtinguished) {
            return ModuleOutput.none(MODULE_ID);
        }

        // Base thrust from analyzer
        double baseThrust = context.stats.baseThrust();
        double throttle = context.throttle;

        // Lorentzian efficiency: η = 1 / (1 + ((ρ - ρ_opt) / Γ)²)
        // V6.1: Use density at the back (where the tail is) instead of CoM
        double fluidDensity = context.getBackDensity();
        double optDensity = context.stats.optimalDensity();
        double bandwidth = context.stats.densityBandwidth();

        double densityDelta = fluidDensity - optDensity;
        double efficiency = 1.0;
        if (bandwidth > PhysicsConstants.AOA_SAFETY_EPSILON) {
            efficiency = 1.0 / (1.0 + (densityDelta / bandwidth) * (densityDelta / bandwidth));
        }

        // Effective thrust = base × throttle × efficiency
        double effectiveThrust = baseThrust * throttle * efficiency;

        // Nozzle direction: start from projectile nose (+Y local), tilt by CGS demands
        double maxTilt = Math.toRadians(context.stats.maxNozzleTilt());
        float pitchTilt = (float) (context.pitchDemand * maxTilt);
        float yawTilt = (float) (context.yawDemand * maxTilt);

        // Build nozzle direction in local space (nose = +Y, tilt around X and Z)
        Vector3f nozzleLocalF = new Vector3f(0, 1, 0);
        // Apply tilt: pitch around X, yaw around Z
        nozzleLocalF.rotateX(pitchTilt);
        nozzleLocalF.rotateZ(-yawTilt);

        Vec3 thrustForceLocal = new Vec3(nozzleLocalF.x, nozzleLocalF.y, nozzleLocalF.z)
                .scale(effectiveThrust);

        // Torque from nozzle tilt (offset from centerline) - LOCAL
        Vec3 torqueLocal = Vec3.ZERO;
        if (Math.abs(pitchTilt) > PhysicsConstants.AOA_SAFETY_EPSILON
                || Math.abs(yawTilt) > PhysicsConstants.AOA_SAFETY_EPSILON) {

            Vec3 noseDirLocal = new Vec3(0, 1, 0);
            Vec3 thrustDirLocal = new Vec3(nozzleLocalF.x, nozzleLocalF.y, nozzleLocalF.z).normalize();

            // Lever arm from tail center is negative because tail is at the back
            // However, we already have centerBack() in stats which is the tail's center
            // relative to CoM.
            double tailLeverY = context.stats.centerBack().y;

            // τ = r × F
            // The thrust creates rotation because it's not aligned with the nose axis.
            torqueLocal = noseDirLocal.cross(thrustDirLocal).scale(effectiveThrust * Math.abs(tailLeverY));
        }

        return ModuleOutput.forceSimpleWithTorque(MODULE_ID, thrustForceLocal, torqueLocal);
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
        return ModuleCapability.FORCE_WITH_TORQUE;
    }
}