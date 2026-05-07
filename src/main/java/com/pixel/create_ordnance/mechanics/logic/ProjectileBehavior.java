package com.pixel.create_ordnance.mechanics.logic;

/**
 * Interface defining the behavior of a projectile component during its lifecycle.
 *
 * <p>Components register a {@code ProjectileBehavior} implementation that handles
 * three lifecycle phases:
 * <ul>
 *   <li><b>onSetup</b> — called once when the projectile spawns (per component)</li>
 *   <li><b>onTick</b> — called every server tick (for sensors, guidance, effects, etc.)</li>
 *   <li><b>onTrigger</b> — called on impact or external event (explosions, fire, etc.)</li>
 * </ul>
 *
 * <p>All methods have a default empty implementation — override only what you need.
 *
 * <p><b>Addon developers:</b> Create your own implementation and register it via
 * {@code ProjectileComponentRegistry.entry(block).behavior(new MyBehavior()).register();}
 *
 * @see LogicContext
 */
public interface ProjectileBehavior {

    /**
     * Called once when the projectile spawns, for each component that has this behavior.
     *
     * @param ctx the execution context (level, entity, position, parameters)
     */
    default void onSetup(LogicContext ctx) {}

    /**
     * Called every server tick for this component.
     * Use for continuous logic: sensors, guidance feedback, particle effects, etc.
     *
     * @param ctx the execution context (level, entity, position, parameters)
     */
    default void onTick(LogicContext ctx) {}

    /**
     * Called when the component is triggered (impact, explosion, fire, etc.).
     *
     * @param ctx the execution context (level, entity, position, parameters)
     */
    default void onTrigger(LogicContext ctx) {}

    /**
     * Returns a short identifier for this behavior, used in commands and debug display.
     * Example: {@code "explosive"}, {@code "debug"}, {@code "proximity_sensor"}
     *
     * @return a non-null behavior identifier
     */
    String getBehaviorId();
}
