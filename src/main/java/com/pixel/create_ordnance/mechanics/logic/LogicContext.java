package com.pixel.create_ordnance.mechanics.logic;

import java.util.Map;

import com.pixel.create_ordnance.content.entity.ProjectileEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Thread-Safe Context passed to Dynamic Logic methods.
 * <br>
 * Replaces the old static field injection.
 */
public record LogicContext(
        Level level,
        Entity projectile,
        Vec3 position,
        BlockPos blockPos,
        float yaw,
        float pitch,
        Map<String, Object> params) {

    /**
     * Helper to get a parameter with a default value.
     */
    @SuppressWarnings("unchecked")
    public <T> T getParam(String key, T defaultValue) {
        if (params == null || !params.containsKey(key)) {
            return defaultValue;
        }
        try {
            Object val = params.get(key);
            // Handle number conversions if necessary (Double -> Float/Int)
            if (val instanceof Double d) {
                if (defaultValue instanceof Float)
                    return (T) Float.valueOf(d.floatValue());
                if (defaultValue instanceof Integer)
                    return (T) Integer.valueOf(d.intValue());
            }
            return (T) val;
        } catch (ClassCastException e) {
            return defaultValue;
        }
    }

    /**
     * Helper to get a required parameter (throws exception if missing).
     */
    public Object getParamOrThrow(String key) {
        if (params == null || !params.containsKey(key)) {
            throw new IllegalArgumentException("Missing required parameter: " + key);
        }
        return params.get(key);
    }

    public double getDouble(String key) {
        return ((Number) getParam(key, 0.0)).doubleValue();
    }

    public int getInt(String key) {
        return ((Number) getParam(key, 0)).intValue();
    }

    public String getString(String key) {
        return (String) getParam(key, "");
    }

    // ============================================================
    // GUIDANCE HELPERS (CGS Script API)
    // ============================================================

    /**
     * Set the target pitch demand.
     * 
     * @param val -1.0 (Down) to 1.0 (Up)
     */
    public void setPitchDemand(float val) {
        if (projectile instanceof ProjectileEntity e) {
            e.setPitchDemand(val);
        }
    }

    /**
     * Set the target yaw demand.
     * 
     * @param val -1.0 (Left) to 1.0 (Right)
     */
    public void setYawDemand(float val) {
        if (projectile instanceof ProjectileEntity e) {
            e.setYawDemand(val);
        }
    }

    /**
     * Set the target roll demand.
     * 
     * @param val -1.0 to 1.0
     */
    public void setRollDemand(float val) {
        if (projectile instanceof ProjectileEntity e) {
            e.setRollDemand(val);
        }
    }

    /**
     * Set the target engine throttle.
     * 
     * @param val 0.0 (Stop) to 1.0 (Full Thrust)
     */
    public void setThrottle(float val) {
        if (projectile instanceof ProjectileEntity e) {
            e.setThrottle(val);
        }
    }

    /**
     * Returns the physical limit of the engine's nozzle tilt.
     */
    public double getMaxNozzleTilt() {
        if (projectile instanceof ProjectileEntity e) {
            return e.getMaxNozzleTilt();
        }
        return 0;
    }
}