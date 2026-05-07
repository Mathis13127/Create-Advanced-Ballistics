package com.pixel.create_ordnance.api.cgs;

import com.pixel.create_ordnance.content.entity.ProjectileEntity;

/**
 * Public API interface for CGS (Command, Guidance, Stabilization) controllers.
 * <p>
 * Addons implement this interface to create custom guidance algorithms
 * (proportional navigation, beam riding, waypoint following, etc.).
 * Controllers are attached to projectile entities via
 * {@link com.pixel.create_ordnance.mechanics.cgs.CGSManager#attach(ProjectileEntity, ICGSController)}.
 * <p>
 * <b>Lifecycle:</b>
 * <ol>
 *   <li>{@link #onAttach(ProjectileEntity)} — Called when the controller is attached.</li>
 *   <li>{@link #compute(CGSContext)} — Called every server tick to compute control demands.</li>
 *   <li>{@link #onDetach(ProjectileEntity)} — Called when the controller is detached or the entity dies.</li>
 * </ol>
 * <p>
 * <b>Contract:</b>
 * <ul>
 *   <li>The {@link #compute(CGSContext)} method is called on the server thread only.</li>
 *   <li>Controllers MUST NOT directly modify the entity — return demands via {@link CGSResult}.</li>
 * </ul>
 * <p>
 * <b>Example addon usage:</b>
 * <pre>{@code
 * public class ProNavController implements ICGSController {
 *     private Vec3 targetPos;
 *
 *     public ProNavController(Vec3 target) { this.targetPos = target; }
 *
 *     @Override
 *     public CGSResult compute(CGSContext ctx) {
 *         // ... proportional navigation math ...
 *         return new CGSResult(pitchCmd, yawCmd, 0f, 1.0f);
 *     }
 *
 *     @Override public String getId() { return "my_addon:pro_nav"; }
 *     @Override public CGSType getType() { return CGSType.THRUST_VECTOR; }
 * }
 * }</pre>
 *
 * @see CGSResult
 * @see CGSContext
 * @see CGSType
 */
public interface ICGSController {

    /**
     * Computes control demands for this tick.
     * <p>
     * Called once per server tick for each entity this controller is attached to.
     * The returned {@link CGSResult} represents the controller's <b>base</b> demands.
     * These may be further modified by addons via the
     * {@link com.pixel.create_ordnance.api.events.ProjectileCGSEvent}.
     *
     * @param context Immutable snapshot of the projectile's current state
     * @return The computed control demands
     */
    CGSResult compute(CGSContext context);

    /**
     * Unique identifier for this controller.
     * <p>
     * Recommended format: {@code "modid:controller_name"} (e.g. {@code "mymod:pro_nav"}).
     *
     * @return Controller identifier string
     */
    String getId();

    /**
     * The CGS type this controller manages.
     * <p>
     * Each entity can have at most one controller per {@link CGSType}.
     * For thrust-vectored missiles, use {@link CGSType#THRUST_VECTOR}.
     *
     * @return The CGS type
     */
    CGSType getType();

    /**
     * Called when this controller is attached to a projectile entity.
     * <p>
     * Use this for one-time initialization (e.g. capturing initial heading,
     * setting up target tracking).
     *
     * @param entity The projectile entity this controller is now managing
     */
    default void onAttach(ProjectileEntity entity) {
    }

    /**
     * Called when this controller is detached from a projectile entity.
     * <p>
     * Use this for cleanup (e.g. releasing target locks).
     * Also called when the entity dies/is removed.
     *
     * @param entity The projectile entity this controller was managing
     */
    default void onDetach(ProjectileEntity entity) {
    }

    /**
     * Determines whether this controller should compute demands this tick.
     * <p>
     * When this returns {@code false}, the controller is skipped and
     * no CGS result is produced (demands remain at their previous values).
     * The controller remains attached — use
     * {@link com.pixel.create_ordnance.mechanics.cgs.CGSManager#detach(java.util.UUID, CGSType)}
     * to permanently remove it.
     *
     * @param context Immutable snapshot of the projectile's current state
     * @return {@code true} if the controller should compute this tick
     */
    default boolean isActive(CGSContext context) {
        return true;
    }
}
