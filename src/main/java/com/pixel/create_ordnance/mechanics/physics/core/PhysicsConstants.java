package com.pixel.create_ordnance.mechanics.physics.core;

import com.pixel.create_ordnance.config.OrdnanceConfigs;

/**
 * Shared mathematical and physical constants for the project-wide physics
 * engine.
 * <p>
 * <b>Architecture contract:</b>
 * <ul>
 * <li>Static {@code final} fields: pure, compile-time constants (math, unit
 * standards).</li>
 * <li>Static getter methods: runtime values backed by the
 * {@link com.pixel.create_ordnance.config.OrdnanceCommonConfig}
 * or {@link com.pixel.create_ordnance.config.OrdnanceServerConfig} — <b>never
 * hardcoded</b>.</li>
 * </ul>
 * <p>
 * <b>Density reference:</b> Any module requiring a density baseline <b>MUST</b>
 * use
 * {@link PhysicsRegistry#AIR} as its reference standard.
 */
public class PhysicsConstants {

    /**
     * Safety Epsilon: minimum angle of attack (rad) used as a divisor guard
     * to prevent mathematical divergence (NaN / div-by-zero).
     */
    public static final double AOA_SAFETY_EPSILON = 1e-4;

    /**
     * Standard block-volume factor. Used for buoyancy and area-based drag
     * calculations. Scale with the projectile's overall scale as needed.
     */
    public static final double BASE_BLOCK_VOLUME = 0.8;

    /**
     * Reference density of AIR in kg/m3, used as the baseline for all
     * physics modules that depend on a fluid density standard.
     */
    public static final double AIR_DENSITY = 1.225;

    // =========================================================
    // ENVIRONMENT GETTERS
    // =========================================================

    /** Gravity strength multiplier. Reads: common.physics.environment.gravity */
    public static double getGravityStrength() {
        return OrdnanceConfigs.COMMON.physics.environment.gravity.get();
    }

    /**
     * Global buoyancy multiplier. Reads:
     * common.physics.environment.globalBuoyancyMultiplier
     */
    public static double getGlobalBuoyancyMultiplier() {
        return OrdnanceConfigs.COMMON.physics.environment.globalBuoyancyMultiplier.get();
    }

    /**
     * Whether dynamic pressure calculations are active. Reads:
     * common.physics.environment.pressureEnabled
     */
    public static boolean isPressureEnabled() {
        return OrdnanceConfigs.COMMON.physics.environment.pressureEnabled.get();
    }

    // =========================================================
    // AERODYNAMICS GETTERS
    // =========================================================

    /**
     * Global frontal air resistance multiplier. Reads:
     * common.physics.aerodynamics.frontalDragMultiplier
     */
    public static double getFrontalDragMultiplier() {
        return OrdnanceConfigs.COMMON.physics.aerodynamics.frontalDragMultiplier.get();
    }

    /**
     * Global lateral air resistance multiplier. Reads:
     * common.physics.aerodynamics.lateralDragMultiplier
     */
    public static double getLateralDragMultiplier() {
        return OrdnanceConfigs.COMMON.physics.aerodynamics.lateralDragMultiplier.get();
    }

    /**
     * Global Lift multiplier. Reads:
     * common.physics.aerodynamics.globalLiftMultiplier
     */
    public static double getGlobalLiftMultiplier() {
        return OrdnanceConfigs.COMMON.physics.aerodynamics.globalLiftMultiplier.get();
    }

    /**
     * Global scale factor: points -> m2. Reads:
     * common.physics.aerodynamics.aeroAreaScale
     */
    public static double getAeroAreaScale() {
        return OrdnanceConfigs.COMMON.physics.aerodynamics.aeroAreaScale.get();
    }

    /** Stall angle in degrees. Reads: common.physics.aerodynamics.stallAngle */
    public static double getStallAngle() {
        return OrdnanceConfigs.COMMON.physics.aerodynamics.stallAngle.get();
    }

    /**
     * Global viscous drag multiplier. Reads:
     * common.physics.aerodynamics.viscousDragMultiplier
     */
    public static double getViscousDragMultiplier() {
        return OrdnanceConfigs.COMMON.physics.aerodynamics.viscousDragMultiplier.get();
    }

    // =========================================================
    // MASS DYNAMICS GETTERS
    // =========================================================

    /**
     * Viscous damping multiplier. Reads:
     * common.physics.massDynamics.inertia.angularDampingMultiplier
     */
    public static double getAngularDampingMultiplier() {
        return OrdnanceConfigs.COMMON.physics.massDynamics.inertia.angularDampingMultiplier.get();
    }

    /**
     * Decay factor for environment viscosity memory. Reads:
     * common.physics.massDynamics.inertia.stickyViscosityDecay
     */
    public static double getStickyViscosityDecay() {
        return OrdnanceConfigs.COMMON.physics.massDynamics.inertia.stickyViscosityDecay.get();
    }

    /**
     * Kinetic energy retention after block penetration. Reads:
     * common.physics.massDynamics.solidDrag.penetrationEnergyRetention
     */
    public static double getPenetrationEnergyRetention() {
        return OrdnanceConfigs.COMMON.physics.massDynamics.solidDrag.penetrationEnergyRetention.get();
    }

    /**
     * Base energy cost for block penetration. Reads:
     * common.physics.massDynamics.solidDrag.penetrationBaseCost
     */
    public static double getPenetrationBaseCost() {
        return OrdnanceConfigs.COMMON.physics.massDynamics.solidDrag.penetrationBaseCost.get();
    }

    // =========================================================
    // SERVER GETTERS
    // =========================================================

    /** Max speed before discard. Reads: server.thresholds.velocityLimit */
    public static double getVelocityLimit() {
        return OrdnanceConfigs.SERVER.thresholds.velocityLimit.get();
    }

    /**
     * Max chunks a projectile can force-load. Reads:
     * server.thresholds.maxChunkLoaded
     */
    public static int getMaxChunkLoaded() {
        return OrdnanceConfigs.SERVER.thresholds.maxChunkLoaded.get();
    }

    /**
     * Whether partial-tick simulation is enabled. Reads:
     * server.chunks.partialTicksEnabled
     */
    public static boolean isPartialTicksEnabled() {
        return OrdnanceConfigs.SERVER.chunks.partialTicksEnabled.get();
    }

    /**
     * Whether projectile chunk loading is enabled. Reads:
     * server.chunks.chunkloaderEnabled
     */
    public static boolean isChunkloaderEnabled() {
        return OrdnanceConfigs.SERVER.chunks.chunkloaderEnabled.get();
    }

    /**
     * Speed threshold for partial-tick activation. Reads
     * 
     * server.thresholds.partialTickVelocityThreshold
     * 
     */
    public static double getPartialTickVelocityThreshold() {
        return OrdnanceConfigs.SERVER.thresholds.partialTickVelocityThreshold.get();
    }

    /**
     * 
     * 
     * Number of ticks for position interpolation. Reads:
     * client.positionInterpolationSteps
     */
    public static int getPositionInterpolationSteps() {
        return OrdnanceConfigs.CLIENT.positionInterpolationSteps.get();
    }

    /**
     * Number of ticks for rotation interpolation. Reads:
     * client.rotationInterpolationSteps
     */
    public static int getRotationInterpolationSteps() {
        return OrdnanceConfigs.CLIENT.rotationInterpolationSteps.get();
    }
}
