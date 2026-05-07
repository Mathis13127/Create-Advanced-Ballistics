package com.pixel.create_ordnance.mechanics.cgs;

import com.pixel.create_ordnance.CreateOrdnance;
import com.pixel.create_ordnance.api.cgs.CGSContext;
import com.pixel.create_ordnance.api.cgs.CGSResult;
import com.pixel.create_ordnance.api.cgs.CGSType;
import com.pixel.create_ordnance.api.cgs.ICGSController;
import com.pixel.create_ordnance.content.entity.ProjectileEntity;

/**
 * Debug CGS controller driven by the {@code /prj-lib projectile cgs} command.
 * <p>
 * <b>Behavior:</b> Once a property is set via command, it is <b>held indefinitely</b>
 * until explicitly cleared with {@code /prj-lib projectile cgs <targets> clear}.
 * This allows operators to observe the sustained effect of thrust vectoring in
 * real time. Each property (pitch, yaw, roll, thrust) can be set independently
 * and persists across ticks.
 * <p>
 * When no property has been set (or after a {@link #clear()}), the controller
 * is inactive and returns {@link CGSResult#NEUTRAL}, letting demands fall back
 * to their default values (0/0/0/1.0).
 * <p>
 * The controller is attached once per entity on first command usage and reused
 * for subsequent commands.
 */
public class CommandCGSController implements ICGSController {

    private static final String ID = "ordnance:command_debug";

    private float pitch = 0.0f;
    private float yaw = 0.0f;
    private float roll = 0.0f;
    private float throttle = 1.0f;

    /** True when at least one property has been set via command. */
    private boolean hasActiveCommand = false;

    /**
     * Updates a single demand channel and marks the controller as active.
     * <p>
     * The value is held indefinitely until {@link #clear()} is called or
     * a new value is set for the same property.
     *
     * @param property "pitch", "yaw", "roll", or "thrust"
     * @param value    The raw command value (pitch/yaw/roll: -1 to 1, thrust: 0 to 100)
     */
    public void setProperty(String property, float value) {
        switch (property) {
            case "pitch" -> this.pitch = value;
            case "yaw" -> this.yaw = value;
            case "roll" -> this.roll = value;
            case "thrust" -> this.throttle = value / 100.0f;
        }
        this.hasActiveCommand = true;

        CreateOrdnance.LOGGER.debug(
                "[CGS Command] Set {} = {} (current state: pitch={}, yaw={}, roll={}, throttle={})",
                property, value, pitch, yaw, roll, throttle);
    }

    /**
     * Resets all demands to neutral and deactivates the controller.
     * <p>
     * After calling this, {@link #compute(CGSContext)} returns {@link CGSResult#NEUTRAL}
     * which sets demands to (0, 0, 0, 1.0).
     */
    public void clear() {
        this.pitch = 0.0f;
        this.yaw = 0.0f;
        this.roll = 0.0f;
        this.throttle = 1.0f;
        this.hasActiveCommand = false;

        CreateOrdnance.LOGGER.debug("[CGS Command] Cleared all demands → NEUTRAL");
    }

    /**
     * Returns the current pitch demand value.
     */
    public float getPitch() {
        return pitch;
    }

    /**
     * Returns the current yaw demand value.
     */
    public float getYaw() {
        return yaw;
    }

    /**
     * Returns the current roll demand value.
     */
    public float getRoll() {
        return roll;
    }

    /**
     * Returns the current throttle demand value (0.0 to 1.0).
     */
    public float getThrottleValue() {
        return throttle;
    }

    /**
     * Returns true if any property has been set via command.
     */
    public boolean hasActiveCommand() {
        return hasActiveCommand;
    }

    @Override
    public CGSResult compute(CGSContext context) {
        if (hasActiveCommand) {
            return new CGSResult(pitch, yaw, roll, throttle);
        }
        return CGSResult.NEUTRAL;
    }

    // NOTE: We intentionally do NOT override isActive().
    // The default (always true) ensures that after clear(), compute() returns
    // NEUTRAL which applyResult() writes to the entity, resetting demands to 0/0/0/1.0.
    // If we returned false here, the manager would skip us and old demands would persist.

    @Override
    public void onAttach(ProjectileEntity entity) {
        CreateOrdnance.LOGGER.debug(
                "[CGS Command] Controller attached to entity {} (UUID: {})",
                entity.getId(), entity.getUUID());
    }

    @Override
    public void onDetach(ProjectileEntity entity) {
        CreateOrdnance.LOGGER.debug(
                "[CGS Command] Controller detached from entity {} (UUID: {})",
                entity.getId(), entity.getUUID());
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public CGSType getType() {
        return CGSType.THRUST_VECTOR;
    }
}
