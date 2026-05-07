package com.pixel.create_ordnance.api.cgs;

/**
 * Defines the type of CGS (Command, Guidance, Stabilization) controller.
 * <p>
 * Each projectile entity can have at most ONE controller per CGSType.
 * This allows a single projectile to have both a thrust vectoring controller
 * and an aerodynamic control surface controller simultaneously.
 * </p>
 */
public enum CGSType {

    /**
     * Thrust Vector Control.
     * <p>
     * Controls the projectile by tilting the engine nozzle.
     * Demands are converted to nozzle tilt angles by the
     * {@link com.pixel.create_ordnance.mechanics.physics.modules.thrust.VectorialThrustModule}.
     * The effective tilt range is bounded by {@code ProjectileStats.maxNozzleTilt()}.
     * </p>
     */
    THRUST_VECTOR("thrust_vector"),

    /**
     * Aerodynamic Control (RESERVED — NOT YET IMPLEMENTED).
     * <p>
     * Will control the projectile via aerodynamic control surfaces (fins, canards).
     * Demands will be converted to fin deflection angles by a future AeroControlModule.
     * </p>
     */
    AERODYNAMIC("aerodynamic");

    private final String id;

    CGSType(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
