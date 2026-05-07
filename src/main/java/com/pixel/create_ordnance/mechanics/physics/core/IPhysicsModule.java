package com.pixel.create_ordnance.mechanics.physics.core;

/**
 * Base interface for all physics modules (V2 Protocol).
 * <p>
 * <b>Contract:</b>
 * <ul>
 * <li>Modules are <b>strictly decoupled</b>: they read from an immutable
 * {@link PhysicsContext} snapshot and return a {@link ModuleOutput}.</li>
 * <li>Modules <b>NEVER</b> mutate the context or any external state.</li>
 * <li>The {@link PhysicsEngine} collects all outputs and performs
 * accumulation + torque derivation in a separate phase.</li>
 * </ul>
 * <p>
 * <b>Execution order:</b> Modules are categorized into phases by the engine.
 * A module does not need to know its phase — the engine handles ordering.
 *
 * @see ModuleOutput
 * @see PhysicsEngine
 */
public interface IPhysicsModule {

    /**
     * Entry point for physics computation.
     * <p>
     * <b>CONTRACT:</b> Every module MUST return its {@link ModuleOutput} in
     * the <b>LOCAL Reference Frame</b> of the projectile (Nose = +Y).
     * The engine performs all accumulation in local space for physical accuracy.
     * <p>
     * 
     * @param context Immutable snapshot of the world and projectile state
     * @return Computed force/torque (local space)
     */
    ModuleOutput compute(PhysicsContext context);

    /**
     * Unique identifier for this module.
     * <p>
     * Used by the
     * {@link com.pixel.create_ordnance.mechanics.physics.logic.BlackBox}
     * diagnostic system for per-module logging. Must be a stable, human-readable
     * string (e.g. {@code "gravity"}, {@code "surface_drag"}, {@code "lift"}).
     *
     * @return Module identifier string.
     */
    String getModuleId();

    /**
     * Returns the execution phase of this module.
     * <p>
     * <ul>
     * <li>{@link Phase#FORCE}: Module computes forces only (no torque inputs
     * needed).</li>
     * <li>{@link Phase#TORQUE}: Module needs Phase 1 torque results as input
     * (e.g. viscous damping reacts to accumulated angular forces).</li>
     * </ul>
     *
     * @return The execution phase for this module.
     */
    Phase getPhase();

    /**
     * Describes the physical contribution of this module.
     * Used for dynamic registry status reporting and diagnostics.
     *
     * @return The capability of this module.
     */
    ModuleCapability getCapability();

    /**
     * Execution phase for the 3-pass engine pipeline.
     */
    enum Phase {
        /**
         * Phase 1: Force-only computation.
         * Modules in this phase do NOT depend on torque results from other modules.
         * Includes: Gravity, Thrust, Attraction, SurfaceDrag, Lift, Buoyancy.
         */
        FORCE,

        /**
         * Phase 2: Torque-aware computation.
         * Modules in this phase may read the cumulative torque from Phase 1.
         * Includes: ViscousDamping, VectorialThrust (torque component).
         */
        TORQUE
    }

    /**
     * Categorization of module outputs for dynamic reporting.
     */
    enum ModuleCapability {
        /** CONTRIBUTES A SINGLE GLOBAL FORCE VECTOR. */
        FORCE_SIMPLE("Force (Simple)"),
        /**
         * CONTRIBUTES TWO FORCES (COA FRONT/BACK) — POTENTIALLY CREATES EMERGENT
         * TORQUE.
         */
        FORCE_DUAL("Force (2-Points)"),
        /** CONTRIBUTES PURE ROTATIONAL TORQUE ONLY. */
        TORQUE_ONLY("Torque"),
        /** CONTRIBUTES BOTH LINEAR FORCE AND ROTATIONAL TORQUE. */
        FORCE_WITH_TORQUE("Force + Torque");

        private final String label;

        ModuleCapability(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }
}
