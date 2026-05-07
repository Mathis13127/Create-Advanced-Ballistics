package com.pixel.create_ordnance.api.cgs;

import java.util.UUID;

import com.pixel.create_ordnance.content.entity.ProjectileEntity;
import com.pixel.create_ordnance.mechanics.cgs.CGSManager;

/**
 * Public API for the CGS (Command, Guidance, Stabilization) system.
 * <p>
 * This is the <b>intended entry point for addons and external code</b> to
 * attach, detach, and query guidance controllers on projectile entities.
 * All methods delegate to the internal {@code CGSManager}.
 * <p>
 * <b>Quick start — Creating and attaching a custom controller:</b>
 * <pre>{@code
 * // 1. Implement ICGSController
 * public class ProNavController implements ICGSController {
 *     private final Vec3 targetPos;
 *
 *     public ProNavController(Vec3 target) { this.targetPos = target; }
 *
 *     @Override
 *     public CGSResult compute(CGSContext ctx) {
 *         Vec3 toTarget = targetPos.subtract(ctx.position()).normalize();
 *         Vec3 nose = ctx.noseDirection();
 *         float yaw = (float) nose.cross(toTarget).y;
 *         return new CGSResult(0, yaw, 0, 1.0f);
 *     }
 *
 *     @Override public String getId() { return "myaddon:pro_nav"; }
 *     @Override public CGSType getType() { return CGSType.THRUST_VECTOR; }
 * }
 *
 * // 2. Attach to a projectile (with lifecycle callbacks)
 * CGSAPI.attach(projectileEntity, new ProNavController(targetPos));
 *
 * // 3. Query state
 * ICGSController ctrl = CGSAPI.getController(entity.getUUID(), CGSType.THRUST_VECTOR);
 *
 * // 4. Detach when done
 * CGSAPI.detach(projectileEntity, CGSType.THRUST_VECTOR);
 * }</pre>
 * <p>
 * <b>Event hook:</b> To modify control demands from <i>any</i> controller (without
 * implementing your own), listen to
 * {@link com.pixel.create_ordnance.api.events.ProjectileCGSEvent}:
 * <pre>{@code
 * @SubscribeEvent
 * public static void onCGS(ProjectileCGSEvent event) {
 *     event.addYaw(0.1f);  // additive wind effect
 * }
 * }</pre>
 *
 * @see ICGSController
 * @see CGSResult
 * @see CGSContext
 * @see CGSType
 * @see com.pixel.create_ordnance.api.events.ProjectileCGSEvent
 */
public class CGSAPI {

    private CGSAPI() {} // Static API — no instantiation

    // =========================================================
    // ATTACH
    // =========================================================

    /**
     * Attaches a CGS controller to a projectile entity.
     * <p>
     * Calls {@link ICGSController#onAttach(ProjectileEntity)} immediately.
     * If a controller of the same {@link CGSType} is already attached,
     * it is detached first (with {@code onDetach} callback).
     * <p>
     * Each entity can have at most <b>one controller per CGSType</b>.
     *
     * @param entity     The projectile entity
     * @param controller The controller to attach
     */
    public static void attach(ProjectileEntity entity, ICGSController controller) {
        CGSManager.getInstance().attach(entity, controller);
    }

    /**
     * Attaches a CGS controller to an entity by UUID.
     * <p>
     * <b>Note:</b> This overload does NOT trigger {@code onAttach/onDetach}
     * lifecycle callbacks. Prefer {@link #attach(ProjectileEntity, ICGSController)}
     * when you have a direct entity reference.
     *
     * @param entityId   UUID of the projectile entity
     * @param controller The controller to attach
     */
    public static void attach(UUID entityId, ICGSController controller) {
        CGSManager.getInstance().attach(entityId, controller);
    }

    // =========================================================
    // DETACH
    // =========================================================

    /**
     * Detaches the controller of a specific type from a projectile entity.
     * Calls {@link ICGSController#onDetach(ProjectileEntity)}.
     *
     * @param entity The projectile entity
     * @param type   The CGS type to detach
     */
    public static void detach(ProjectileEntity entity, CGSType type) {
        CGSManager.getInstance().detach(entity, type);
    }

    /**
     * Detaches the controller of a specific type from an entity by UUID.
     * <p>
     * <b>Note:</b> This overload does NOT trigger {@code onDetach}.
     *
     * @param entityId UUID of the projectile entity
     * @param type     The CGS type to detach
     */
    public static void detach(UUID entityId, CGSType type) {
        CGSManager.getInstance().detach(entityId, type);
    }

    /**
     * Detaches ALL controllers from a projectile entity.
     * Calls {@link ICGSController#onDetach(ProjectileEntity)} for each.
     *
     * @param entity The projectile entity
     */
    public static void detachAll(ProjectileEntity entity) {
        CGSManager.getInstance().detachAll(entity);
    }

    /**
     * Detaches ALL controllers from an entity by UUID.
     * Does NOT trigger {@code onDetach} callbacks.
     *
     * @param entityId UUID of the projectile entity
     */
    public static void detachAll(UUID entityId) {
        CGSManager.getInstance().detachAll(entityId);
    }

    // =========================================================
    // QUERY
    // =========================================================

    /**
     * Returns the controller attached for a specific type, or null.
     *
     * @param entityId UUID of the projectile entity
     * @param type     The CGS type to query
     * @return The attached controller, or null if none
     */
    public static ICGSController getController(UUID entityId, CGSType type) {
        return CGSManager.getInstance().getController(entityId, type);
    }

    /**
     * Returns true if any controller is attached to this entity.
     *
     * @param entityId UUID of the projectile entity
     * @return true if at least one controller is attached
     */
    public static boolean hasController(UUID entityId) {
        return CGSManager.getInstance().hasController(entityId);
    }
}
