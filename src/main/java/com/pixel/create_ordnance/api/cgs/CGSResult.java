package com.pixel.create_ordnance.api.cgs;

import net.minecraft.util.Mth;

/**
 * Immutable result of a CGS controller computation.
 * <p>
 * All demands are normalized:
 * <ul>
 *   <li>{@code pitchDemand}: -1.0 (full nose down) to 1.0 (full nose up)</li>
 *   <li>{@code yawDemand}:   -1.0 (full left) to 1.0 (full right)</li>
 *   <li>{@code rollDemand}:  -1.0 (roll left) to 1.0 (roll right)</li>
 *   <li>{@code throttle}:     0.0 (idle/cut) to 1.0 (full thrust)</li>
 * </ul>
 * <p>
 * Values are NOT clamped at construction — clamping happens at application time
 * in {@link com.pixel.create_ordnance.mechanics.cgs.CGSManager} after all
 * additive modifications from the {@link com.pixel.create_ordnance.api.events.ProjectileCGSEvent}
 * are accumulated.
 *
 * @see ICGSController
 */
public record CGSResult(float pitchDemand, float yawDemand, float rollDemand, float throttle) {

    /** Neutral result: no steering, full throttle. */
    public static final CGSResult NEUTRAL = new CGSResult(0.0f, 0.0f, 0.0f, 1.0f);

    /** Neutral result: no steering, zero throttle (coasting). */
    public static final CGSResult IDLE = new CGSResult(0.0f, 0.0f, 0.0f, 0.0f);

    /**
     * Creates a new result with additive deltas applied.
     * The returned result is NOT clamped — clamping is deferred to application time.
     *
     * @param dPitch    Additional pitch demand
     * @param dYaw      Additional yaw demand
     * @param dRoll     Additional roll demand
     * @param dThrottle Additional throttle
     * @return New CGSResult with deltas added
     */
    public CGSResult withAdditives(float dPitch, float dYaw, float dRoll, float dThrottle) {
        return new CGSResult(
                pitchDemand + dPitch,
                yawDemand + dYaw,
                rollDemand + dRoll,
                throttle + dThrottle);
    }

    /**
     * Returns a new result with all demands clamped to their valid ranges.
     * Pitch/Yaw/Roll: [-1.0, 1.0], Throttle: [0.0, 1.0].
     */
    public CGSResult clamped() {
        return new CGSResult(
                Mth.clamp(pitchDemand, -1.0f, 1.0f),
                Mth.clamp(yawDemand, -1.0f, 1.0f),
                Mth.clamp(rollDemand, -1.0f, 1.0f),
                Mth.clamp(throttle, 0.0f, 1.0f));
    }
}
