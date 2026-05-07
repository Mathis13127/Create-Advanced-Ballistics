package com.pixel.create_ordnance.mechanics.physics.core;

/**
 * Discrete state machine for projectile lifecycle (V2 Protocol).
 * <p>
 * Governs what the {@link com.pixel.create_ordnance.mechanics.physics.logic.SimulationEngine}
 * does during each tick:
 * <ul>
 *   <li>{@link #FLYING}: Normal physical simulation, chunk loaded, entity visible.</li>
 *   <li>{@link #STUCK}: Velocity below stuck threshold (excluding gravity). Physics frozen.</li>
 *   <li>{@link #FORCE_CHUNKS}: Chunkloader active — simulation runs even in unloaded chunks.</li>
 *   <li>{@link #EXPLODED}: Terminal state — payload triggered, entity scheduled for removal.</li>
 * </ul>
 */
public enum ProjectileState {

    /**
     * Normal operation: all physics modules active, entity rendered,
     * chunk is loaded by player proximity.
     */
    FLYING,

    /**
     * Projectile lodged in terrain or velocity dropped below the stuck threshold.
     * Physics modules are paused. Entity persists but does not move.
     * <p>
     * <b>Note:</b> The stuck threshold ignores passive forces (gravity) to prevent
     * hovering projectiles from being flagged as stuck at apogee.
     */
    STUCK,

    /**
     * The projectile is in an unloaded chunk. The chunkloader is active and
     * the {@link com.pixel.create_ordnance.mechanics.physics.logic.SimulationEngine}
     * runs the physics tick without a visible entity.
     */
    FORCE_CHUNKS,

    /**
     * Terminal state: payload has been triggered (impact, detonation).
     * Entity is scheduled for removal on the next tick.
     */
    EXPLODED
}
