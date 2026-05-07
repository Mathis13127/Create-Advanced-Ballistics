package com.pixel.create_ordnance.mechanics.physics.core;

import net.minecraft.world.phys.Vec3;

/**
 * Standardized output format for all physics modules (V2 Protocol).
 * <p>
 * <b>Contract:</b>
 * <ul>
 * <li><b>Force Simple:</b> {@code force1.equals(force2)} — uniform force at
 * center of mass.</li>
 * <li><b>Force 2 Points:</b> {@code !force1.equals(force2)} — differential
 * force at front/back.
 * The engine derives emergent torque from lever arms:
 * {@code τ = r_front × F_front + r_back × F_back}.</li>
 * <li><b>Torque Only:</b> {@code force1 == force2 == ZERO}, torque declared
 * directly (e.g. viscous damping).</li>
 * </ul>
 * <p>
 * {@code moduleName} is <b>mandatory</b> — it feeds directly into the
 * {@link com.pixel.create_ordnance.mechanics.physics.logic.BlackBox}
 * diagnostic system for per-module logging.
 *
 * @param force1     Force applied at the back (tail) application point.
 * @param force2     Force applied at the front (nose) application point.
 * @param torque     Direct torque contribution (Vec3, rad/tick²). Default:
 *                   {@link Vec3#ZERO}.
 * @param moduleName Unique module identifier for BlackBox logging.
 */
public record ModuleOutput(Vec3 force1, Vec3 force2, Vec3 torque, String moduleName,
        IPhysicsModule.ModuleCapability capability) {

    public ModuleOutput(Vec3 force1, Vec3 force2, Vec3 torque, String moduleName) {
        this(force1, force2, torque, moduleName, IPhysicsModule.ModuleCapability.FORCE_DUAL);
    }

    /**
     * Tethers a capability tag to this output for diagnostic formatting.
     */
    public ModuleOutput withCapability(IPhysicsModule.ModuleCapability capability) {
        return new ModuleOutput(force1, force2, torque, moduleName, capability);
    }

    /**
     * Creates a Force Simple output (uniform force at CoM).
     * Both application points receive the same force vector.
     */
    public static ModuleOutput forceSimple(String moduleName, Vec3 force) {
        return new ModuleOutput(force, force, Vec3.ZERO, moduleName, IPhysicsModule.ModuleCapability.FORCE_SIMPLE);
    }

    /**
     * Creates a Force 2-Points output (differential force at front/back).
     * The engine will derive emergent torque from the lever arm geometry.
     */
    public static ModuleOutput forceDual(String moduleName, Vec3 forceBack, Vec3 forceFront) {
        return new ModuleOutput(forceBack, forceFront, Vec3.ZERO, moduleName,
                IPhysicsModule.ModuleCapability.FORCE_DUAL);
    }

    /**
     * Creates a Torque-Only output (no linear force, direct angular contribution).
     * Used for modules like viscous damping that only affect rotation.
     */
    public static ModuleOutput torqueOnly(String moduleName, Vec3 torque) {
        return new ModuleOutput(Vec3.ZERO, Vec3.ZERO, torque, moduleName, IPhysicsModule.ModuleCapability.TORQUE_ONLY);
    }

    /**
     * Creates a Force Simple + Torque output.
     * Used for modules like Vectorial Thrust that produce both a linear force
     * AND a direct torque (from nozzle tilt offset).
     */
    public static ModuleOutput forceSimpleWithTorque(String moduleName, Vec3 force, Vec3 torque) {
        return new ModuleOutput(force, force, torque, moduleName, IPhysicsModule.ModuleCapability.FORCE_WITH_TORQUE);
    }

    /**
     * Empty output — module ran but contributed nothing this tick.
     * Useful for disabled modules or zero-contribution edge cases.
     */
    public static ModuleOutput none(String moduleName) {
        return new ModuleOutput(Vec3.ZERO, Vec3.ZERO, Vec3.ZERO, moduleName);
    }
}
