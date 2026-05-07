package com.pixel.create_ordnance.registry;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.pixel.create_ordnance.config.OrdnanceCommonConfig;
import com.pixel.create_ordnance.mechanics.logic.ProjectileBehavior;

/**
 * Immutable record holding all configurable properties of a projectile component.
 *
 * <p>Each field is a {@link ProjectileProperty} wrapping a {@link Supplier}
 * so that values can be backed by live config entries (hot-reloadable).</p>
 *
 * <p>Instances are created by
 * {@link ProjectileComponentRegistry.Builder} and stored in the central registry.</p>
 *
 * @see ProjectileComponentRegistry
 * @see ProjectileComponentType
 */
public record ProjectileData(
        ProjectileComponentType type,
        ProjectileProperty<Double> weight,
        ProjectileProperty<Double> stability_factor,
        ProjectileProperty<Double> baseThrust,
        ProjectileProperty<Double> fuelEfficiency,

        @Nullable ProjectileBehavior behavior,
        Map<String, ProjectileProperty<?>> allProperties) {

    public record ProjectileProperty<T>(Supplier<T> supplier, boolean showInTooltip) implements Supplier<T> {
        @Override
        public T get() {
            return supplier != null ? supplier.get() : null;
        }
    }

    public static ProjectileData tail(
            ProjectileProperty<Double> weight,
            ProjectileProperty<Double> stability_factor,
            ProjectileProperty<Double> baseThrust,

            ProjectileProperty<Double> maxNozzleTilt,
            ProjectileProperty<Double> fuelConsumption,
            ProjectileProperty<OrdnanceCommonConfig.ThrustType> thrustType,
            ProjectileProperty<Double> optimalDensity,
            ProjectileProperty<Double> densityBandwidth,
            @Nullable ProjectileBehavior behavior,
            Map<String, ProjectileProperty<?>> customProps) {

        Map<String, ProjectileProperty<?>> finalProps = new HashMap<>(customProps);

        // Core properties for dynamic display
        finalProps.put("mass", weight);
        finalProps.put("stability", stability_factor);
        finalProps.put("base_thrust", baseThrust);

        finalProps.put("max_nozzle_tilt", maxNozzleTilt);
        finalProps.put("fuel_cons", fuelConsumption);
        finalProps.put("thrust_type", thrustType);
        finalProps.put("optimal_density", optimalDensity);
        finalProps.put("density_bandwidth", densityBandwidth);

        return new ProjectileData(
                ProjectileComponentType.TAIL,
                weight,
                stability_factor,
                baseThrust,
                fuelConsumption,
                behavior,
                Collections.unmodifiableMap(finalProps));
    }

    public static ProjectileData fuelTank(
            ProjectileProperty<Double> weight,
            ProjectileProperty<Double> stability_factor,
            ProjectileProperty<Double> capacity,
            @Nullable ProjectileBehavior behavior,
            Map<String, ProjectileProperty<?>> customProps) {

        Map<String, ProjectileProperty<?>> finalProps = new HashMap<>(customProps);
        finalProps.put("mass", weight);
        finalProps.put("stability", stability_factor);
        finalProps.put("capacity", capacity);

        return new ProjectileData(
                ProjectileComponentType.FUEL_TANK,
                weight,
                stability_factor,
                null, null,
                behavior,
                Collections.unmodifiableMap(finalProps));
    }

    public static ProjectileData payload(
            ProjectileProperty<Double> weight,
            ProjectileProperty<Double> stability_factor,
            @Nullable ProjectileBehavior behavior,
            Map<String, ProjectileProperty<?>> customProps) {

        Map<String, ProjectileProperty<?>> finalProps = new HashMap<>(customProps);
        finalProps.put("mass", weight);
        finalProps.put("stability", stability_factor);

        return new ProjectileData(
                ProjectileComponentType.PAYLOAD,
                weight,
                stability_factor,
                null, null,
                behavior,
                Collections.unmodifiableMap(finalProps));
    }

    public static ProjectileData optionalModule(
            ProjectileProperty<Double> weight,
            ProjectileProperty<Double> stability_factor,
            @Nullable ProjectileBehavior behavior,
            Map<String, ProjectileProperty<?>> customProps) {

        Map<String, ProjectileProperty<?>> finalProps = new HashMap<>(customProps);
        finalProps.put("mass", weight);
        finalProps.put("stability", stability_factor);

        return new ProjectileData(
                ProjectileComponentType.OPTIONAL_MODULE,
                weight,
                stability_factor,
                null, null,
                behavior,
                Collections.unmodifiableMap(finalProps));
    }
}
