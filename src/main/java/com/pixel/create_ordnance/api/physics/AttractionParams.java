package com.pixel.create_ordnance.api.physics;

import net.minecraft.nbt.CompoundTag;

/**
 * Configuration parameters for a spatial attraction/repulsion source.
 * <p>
 * Each source has its own set of parameters controlling the force profile,
 * orbital behavior, vortex effects, and alignment torque.
 * <p>
 * <b>Quick start for addon developers:</b>
 * <pre>{@code
 * // Use a preset:
 * SpatialForceAPI.spawn(AttractionParams.blackhole(), level, pos);
 *
 * // Or build custom parameters:
 * AttractionParams custom = AttractionParams.builder()
 *     .strength(-30.0)   // negative = repulsion
 *     .exponent(2.0)
 *     .maxRange(48.0)
 *     .build();
 * SpatialForceAPI.spawn(custom, level, pos);
 * }</pre>
 *
 * @param strength       Base force multiplier (k factor). Negative values create repulsion.
 * @param exponent       Falloff power (n in 1/r^n). 2.0 = inverse-square law
 * @param maxRange       Maximum range in blocks; force is zero beyond this
 * @param targetRadius   Orbital target distance. 0 = pure attraction/repulsion, &gt;0 = PD holding orbit
 * @param kp             Proportional gain for orbital PD controller
 * @param kd             Derivative gain (damping) for orbital PD controller
 * @param vortexFactor   Tangential (cross-product) force strength for spiral orbits
 * @param massDependent  If true, force scales with mass (constant acceleration for all projectiles)
 * @param orientTowards  Alignment torque strength (0.0 = none, 1.0 = full nose-towards-source)
 *
 * @see SpatialForceAPI
 */
public record AttractionParams(
        double strength,
        double exponent,
        double maxRange,
        double targetRadius,
        double kp,
        double kd,
        double vortexFactor,
        boolean massDependent,
        double orientTowards) {

    // =========================================================
    // DEFAULTS
    // =========================================================

    /**
     * Returns sensible default parameters for a basic attraction source.
     */
    public static AttractionParams defaultAttraction() {
        return new AttractionParams(
                15.0,   // strength
                2.0,    // exponent (inverse-square)
                32.0,   // maxRange
                0.0,    // targetRadius (pure attraction)
                0.5,    // kp
                0.2,    // kd
                0.0,    // vortexFactor
                true,   // massDependent
                0.0     // orientTowards
        );
    }

    // =========================================================
    // PRESETS
    // =========================================================

    /**
     * <b>Black Hole</b> — Violent inward pull with spiral vortex and nose alignment.
     * <p>
     * High strength, wide range, slower falloff (1.5), slight tangential vortex
     * for spiral approach, and strong nose-towards-center torque.
     */
    public static AttractionParams blackhole() {
        return new AttractionParams(
                50.0,   // strength — very strong pull
                1.5,    // exponent — slower falloff than inverse-square
                64.0,   // maxRange — wide area of influence
                0.0,    // targetRadius — pure attraction, no orbit
                0.5,    // kp
                0.2,    // kd
                0.3,    // vortexFactor — slight spiral effect
                true,   // massDependent
                0.8     // orientTowards — strong nose alignment
        );
    }

    /**
     * <b>Deflector</b> — Repulsive force that pushes projectiles away.
     * <p>
     * Negative strength creates an outward force. Inverse-square falloff,
     * moderate range, no vortex or alignment.
     */
    public static AttractionParams deflector() {
        return new AttractionParams(
                -25.0,  // strength — NEGATIVE = repulsion
                2.0,    // exponent — classic inverse-square
                24.0,   // maxRange
                0.0,    // targetRadius — pure repulsion
                0.5,    // kp
                0.2,    // kd
                0.0,    // vortexFactor — no spiral
                true,   // massDependent
                0.0     // orientTowards — no alignment
        );
    }

    /**
     * <b>Magnet</b> — Attracts and holds projectiles in orbit at ~3 blocks.
     * <p>
     * Moderate attraction with a PD orbital controller that maintains a
     * holding distance. Good damping for stable capture.
     */
    public static AttractionParams magnet() {
        return new AttractionParams(
                15.0,   // strength — moderate pull
                2.0,    // exponent — inverse-square
                32.0,   // maxRange
                3.0,    // targetRadius — orbit at 3 blocks
                0.8,    // kp — aggressive PD correction
                0.3,    // kd — good damping
                0.0,    // vortexFactor — no spiral
                true,   // massDependent
                0.5     // orientTowards — moderate nose alignment
        );
    }

    // =========================================================
    // BUILDER (fluent API for addon devs)
    // =========================================================

    /**
     * Creates a new builder initialized with {@link #defaultAttraction()} values.
     * <p>
     * <b>Example:</b>
     * <pre>{@code
     * AttractionParams params = AttractionParams.builder()
     *     .strength(-20.0)
     *     .maxRange(48.0)
     *     .vortexFactor(0.5)
     *     .build();
     * }</pre>
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private double strength = 15.0;
        private double exponent = 2.0;
        private double maxRange = 32.0;
        private double targetRadius = 0.0;
        private double kp = 0.5;
        private double kd = 0.2;
        private double vortexFactor = 0.0;
        private boolean massDependent = true;
        private double orientTowards = 0.0;

        public Builder strength(double strength) { this.strength = strength; return this; }
        public Builder exponent(double exponent) { this.exponent = exponent; return this; }
        public Builder maxRange(double maxRange) { this.maxRange = maxRange; return this; }
        public Builder targetRadius(double targetRadius) { this.targetRadius = targetRadius; return this; }
        public Builder kp(double kp) { this.kp = kp; return this; }
        public Builder kd(double kd) { this.kd = kd; return this; }
        public Builder vortexFactor(double vortexFactor) { this.vortexFactor = vortexFactor; return this; }
        public Builder massDependent(boolean massDependent) { this.massDependent = massDependent; return this; }
        public Builder orientTowards(double orientTowards) { this.orientTowards = orientTowards; return this; }

        public AttractionParams build() {
            return new AttractionParams(strength, exponent, maxRange, targetRadius,
                    kp, kd, vortexFactor, massDependent, orientTowards);
        }
    }

    // =========================================================
    // NBT SERIALIZATION
    // =========================================================

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putDouble("strength", strength);
        tag.putDouble("exponent", exponent);
        tag.putDouble("maxRange", maxRange);
        tag.putDouble("targetRadius", targetRadius);
        tag.putDouble("kp", kp);
        tag.putDouble("kd", kd);
        tag.putDouble("vortexFactor", vortexFactor);
        tag.putBoolean("massDependent", massDependent);
        tag.putDouble("orientTowards", orientTowards);
        return tag;
    }

    public static AttractionParams deserializeNBT(CompoundTag tag) {
        return new AttractionParams(
                tag.getDouble("strength"),
                tag.getDouble("exponent"),
                tag.getDouble("maxRange"),
                tag.getDouble("targetRadius"),
                tag.getDouble("kp"),
                tag.getDouble("kd"),
                tag.getDouble("vortexFactor"),
                tag.getBoolean("massDependent"),
                tag.getDouble("orientTowards"));
    }
}
