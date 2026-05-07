package com.pixel.create_ordnance.api.events;

import com.pixel.create_ordnance.api.cgs.CGSContext;
import com.pixel.create_ordnance.api.cgs.CGSResult;
import com.pixel.create_ordnance.api.cgs.CGSType;
import com.pixel.create_ordnance.content.entity.ProjectileEntity;

import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/**
 * Fired after a CGS controller computes its base demands, but before they are
 * applied to the projectile entity.
 * <p>
 * This event allows <b>additive</b> modification of control demands. Listeners
 * can add or subtract from each demand channel independently. The final result
 * is the base controller result plus all additive modifications, clamped
 * to valid ranges.
 * <p>
 * This event is {@linkplain ICancellableEvent cancellable}.
 * Canceling this event prevents the CGS result from being applied —
 * demands remain at their previous tick values.
 * </p>
 * <p>
 * This event is fired on the
 * {@linkplain net.neoforged.neoforge.common.NeoForge#EVENT_BUS NeoForge event
 * bus},
 * only on the {@linkplain net.neoforged.fml.LogicalSide#SERVER logical server}.
 * </p>
 *
 * <b>Example addon usage:</b>
 * <pre>{@code
 * @SubscribeEvent
 * public static void onCGS(ProjectileCGSEvent event) {
 *     // Add wind disturbance to yaw
 *     event.addYaw(0.05f);
 *
 *     // Reduce throttle in certain conditions
 *     if (event.getContext().speed() < 0.1) {
 *         event.addThrottle(-0.3f);
 *     }
 * }
 * }</pre>
 */
public class ProjectileCGSEvent extends Event implements ICancellableEvent {

    private final ProjectileEntity projectile;
    private final CGSContext context;
    private final CGSType type;
    private final CGSResult baseResult;

    // Additive modifications (start at 0, accumulate across listeners)
    private float additionalPitch = 0.0f;
    private float additionalYaw = 0.0f;
    private float additionalRoll = 0.0f;
    private float additionalThrottle = 0.0f;

    public ProjectileCGSEvent(ProjectileEntity projectile, CGSContext context,
            CGSType type, CGSResult baseResult) {
        this.projectile = projectile;
        this.context = context;
        this.type = type;
        this.baseResult = baseResult;
    }

    // =========================================================
    // READ-ONLY ACCESSORS
    // =========================================================

    public ProjectileEntity getProjectile() {
        return projectile;
    }

    public CGSContext getContext() {
        return context;
    }

    public CGSType getType() {
        return type;
    }

    /**
     * The base result from the controller, before any additive modifications.
     */
    public CGSResult getBaseResult() {
        return baseResult;
    }

    // =========================================================
    // ADDITIVE MODIFIERS
    // =========================================================

    /**
     * Adds a delta to the pitch demand.
     * Multiple listeners can call this — deltas accumulate.
     */
    public void addPitch(float delta) {
        this.additionalPitch += delta;
    }

    /**
     * Adds a delta to the yaw demand.
     * Multiple listeners can call this — deltas accumulate.
     */
    public void addYaw(float delta) {
        this.additionalYaw += delta;
    }

    /**
     * Adds a delta to the roll demand.
     * Multiple listeners can call this — deltas accumulate.
     */
    public void addRoll(float delta) {
        this.additionalRoll += delta;
    }

    /**
     * Adds a delta to the throttle.
     * Multiple listeners can call this — deltas accumulate.
     */
    public void addThrottle(float delta) {
        this.additionalThrottle += delta;
    }

    // =========================================================
    // ADDITIVE GETTERS
    // =========================================================

    public float getAdditionalPitch() {
        return additionalPitch;
    }

    public float getAdditionalYaw() {
        return additionalYaw;
    }

    public float getAdditionalRoll() {
        return additionalRoll;
    }

    public float getAdditionalThrottle() {
        return additionalThrottle;
    }

    // =========================================================
    // RESULT COMPUTATION
    // =========================================================

    /**
     * Computes the final CGS result: base + all additives, clamped to valid ranges.
     * <p>
     * Called by the CGSManager after all event listeners have run.
     *
     * @return The final clamped CGSResult
     */
    public CGSResult getFinalResult() {
        return baseResult
                .withAdditives(additionalPitch, additionalYaw, additionalRoll, additionalThrottle)
                .clamped();
    }
}
