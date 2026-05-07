package com.pixel.create_ordnance.registry;

/**
 * Enumerates the functional roles a block can have inside a projectile assembly.
 *
 * <p>These types are intentionally fixed and map directly to the 1-D scanning
 * pipeline used by
 * {@link com.pixel.create_ordnance.mechanics.scanning.ProjectileAnalyzer}.
 * Custom gameplay behaviors (guidance, sensors, special warheads…) should be
 * registered as {@link #OPTIONAL_MODULE} and implemented via optional physics
 * modules — <b>not</b> as new component types.</p>
 *
 * @see ProjectileComponentRegistry
 * @see ProjectileData
 */
public enum ProjectileComponentType {

    /** Propulsion component — sits at index 0 (bottom) of the scan axis. */
    TAIL,

    /** Fuel storage — extends burn duration and total impulse. */
    FUEL_TANK,

    /** Warhead / cargo — sits at the top of the scan axis; triggers on impact. */
    PAYLOAD,

    /**
     * Optional module slot — fins, ballast, guidance, or any addon-defined
     * behavior. This is the <b>intended extension point</b> for addon developers
     * who need custom component logic.
     */
    OPTIONAL_MODULE;
}
