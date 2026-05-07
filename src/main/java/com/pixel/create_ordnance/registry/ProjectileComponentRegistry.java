package com.pixel.create_ordnance.registry;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import com.pixel.create_ordnance.config.OrdnanceCommonConfig;
import com.pixel.create_ordnance.mechanics.logic.ProjectileBehavior;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;

/**
 * Central registry mapping Minecraft blocks to their projectile component data.
 *
 * <p>Every block used in a projectile assembly must be registered here with its
 * {@link ProjectileComponentType} and associated physical properties (mass, thrust,
 * drag coefficients, etc.).</p>
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * ProjectileComponentRegistry.entry(MyBlocks.ROCKET_TAIL.get())
 *     .asTail()
 *     .setWeight(() -> 5.0, true)
 *     .setBaseThrust(() -> 80.0, true)
 *     .behavior(new MyCustomBehavior())
 *     .register();
 * }</pre>
 *
 * <h3>Addon developers</h3>
 * <p>Addons can register their own blocks as projectile components using the same
 * fluent builder API. Registration should happen during mod initialization.
 * Custom behaviors are implemented via {@link ProjectileBehavior}.</p>
 *
 * @see ProjectileComponentType
 * @see ProjectileData
 * @see ProjectileBehavior
 */
public class ProjectileComponentRegistry {

    // Internal storage: Block -> ProjectileData
    private static final Map<Block, ProjectileData> REGISTRY = new HashMap<>();

    /**
     * Start registering a block as a projectile component.
     *
     * @param block The block instance to register.
     * @return A builder to configure the component.
     */
    public static Builder entry(Block block) {
        return new Builder(block);
    }

    /**
     * Get the registered data for a block.
     */
    public static Optional<ProjectileData> getData(Block block) {
        return Optional.ofNullable(REGISTRY.get(block));
    }

    /**
     * Get the functional type of a block.
     */
    public static Optional<ProjectileComponentType> getType(Block block) {
        return getData(block).map(ProjectileData::type);
    }

    /**
     * Check if a block is a registered component.
     */
    public static boolean isComponent(Block block) {
        return REGISTRY.containsKey(block);
    }

    public static Map<Block, ProjectileComponentType> getAll() {
        Map<Block, ProjectileComponentType> view = new HashMap<>();
        REGISTRY.forEach((b, d) -> view.put(b, d.type()));
        return Collections.unmodifiableMap(view);
    }

    public static Map<Block, ProjectileData> getRegistry() {
        return Collections.unmodifiableMap(REGISTRY);
    }

    public static Collection<ProjectileData> getAllData() {
        return Collections.unmodifiableCollection(REGISTRY.values());
    }

    // --- Builder Class ---

    public static class Builder {
        private final Block block;

        private Builder(Block block) {
            this.block = block;
        }

        public TailBuilder asTail() {
            return new TailBuilder(block);
        }

        public FuelTankBuilder asFuelTank() {
            return new FuelTankBuilder(block);
        }

        public PayloadBuilder asPayload() {
            return new PayloadBuilder(block);
        }

        public OptionalModuleBuilder asOptionalModule() {
            return new OptionalModuleBuilder(block);
        }
    }

    // --- Abstract Builder (Reduces Duplication) ---

    public abstract static class AbstractComponentBuilder<T extends AbstractComponentBuilder<T>> {
        protected final Block block;
        protected ProjectileBehavior behavior;
        protected ProjectileData.ProjectileProperty<Double> weight;
        protected ProjectileData.ProjectileProperty<Double> stability_factor;
        protected final Map<String, ProjectileData.ProjectileProperty<?>> customProps = new HashMap<>();

        protected AbstractComponentBuilder(Block block) {
            this.block = block;
        }

        @SuppressWarnings("unchecked")
        public T setWeight(Supplier<Double> val, boolean showTooltip) {
            this.weight = new ProjectileData.ProjectileProperty<>(val, showTooltip);
            return (T) this;
        }

        @SuppressWarnings("unchecked")
        public T setStabilityFactor(Supplier<Double> val, boolean showTooltip) {
            this.stability_factor = new ProjectileData.ProjectileProperty<>(val, showTooltip);
            return (T) this;
        }

        @SuppressWarnings("unchecked")
        public T setStabilityFactor(double val, boolean showTooltip) {
            this.stability_factor = new ProjectileData.ProjectileProperty<>(() -> val, showTooltip);
            return (T) this;
        }

        /**
         * Sets the behavior for this component. The behavior defines what happens
         * during SETUP, TICK, and TRIGGER lifecycle phases.
         *
         * @param behavior the behavior implementation (or null for no behavior)
         * @return this builder for chaining
         * @see ProjectileBehavior
         */
        @SuppressWarnings("unchecked")
        public T behavior(ProjectileBehavior behavior) {
            this.behavior = behavior;
            return (T) this;
        }

        /**
         * Adds a parameter to be passed to the behavior via {@link com.pixel.create_ordnance.mechanics.logic.LogicContext}.
         * <br>
         * <b>WARNING: Parameters are NOT validated!</b>
         * <br>
         * If the behavior expects a parameter that you do not provide here,
         * it will receive a default value (0, null, false) without warning.
         * <br>
         * Responsibility is on the developer to match the behavior's requirements.
         */
        @SuppressWarnings("unchecked")
        public T addParam(String name, Supplier<?> provider, boolean showInTooltip) {
            this.customProps.put(name, new ProjectileData.ProjectileProperty<>(provider, showInTooltip));
            return (T) this;
        }

        /**
         * Adds a parameter to be passed to the behavior via {@link com.pixel.create_ordnance.mechanics.logic.LogicContext}.
         * <br>
         * <b>WARNING: Parameters are NOT validated!</b>
         */
        @SuppressWarnings("unchecked")
        public T addParam(String name, Object value, boolean showInTooltip) {
            this.customProps.put(name, new ProjectileData.ProjectileProperty<>(() -> value, showInTooltip));
            return (T) this;
        }

        protected void validate() {
            String blockId = BuiltInRegistries.BLOCK.getKey(block).toString();

            // Validate FACING property exists (required for rendering and scanning)
            boolean hasFacing = block.defaultBlockState().getProperties().stream()
                    .anyMatch(prop -> prop.getName().equals("facing"));
            if (!hasFacing) {
                throw new IllegalStateException(
                        "Block '" + blockId + "' is missing required 'facing' property. "
                                + "All projectile components MUST have a FACING property for proper rendering and scanning.");
            }

            if (weight == null) {
                throw new IllegalStateException("Missing mandatory 'weight' for block: " + blockId);
            }

            if (stability_factor == null) {
                throw new IllegalStateException("Missing mandatory 'stability_factor' for block: " + blockId);
            }
        }

        public abstract void register();
    }

    // --- Specific Builders ---

    public static class TailBuilder extends AbstractComponentBuilder<TailBuilder> {
        private ProjectileData.ProjectileProperty<Double> baseThrust;

        private ProjectileData.ProjectileProperty<Double> maxNozzleTilt;
        private ProjectileData.ProjectileProperty<Double> fuelConsumption;
        private ProjectileData.ProjectileProperty<OrdnanceCommonConfig.ThrustType> thrustType;
        private ProjectileData.ProjectileProperty<Double> optimalDensity;
        private ProjectileData.ProjectileProperty<Double> densityBandwidth;

        private TailBuilder(Block block) {
            super(block);
        }

        public TailBuilder setBaseThrust(Supplier<Double> val, boolean showTooltip) {
            this.baseThrust = new ProjectileData.ProjectileProperty<>(val, showTooltip);
            return this;
        }

        public TailBuilder setMaxNozzleTilt(Supplier<Double> val, boolean showTooltip) {
            this.maxNozzleTilt = new ProjectileData.ProjectileProperty<>(val, showTooltip);
            return this;
        }

        public TailBuilder setFuelConsumption(Supplier<Double> val, boolean showTooltip) {
            this.fuelConsumption = new ProjectileData.ProjectileProperty<>(val, showTooltip);
            return this;
        }

        public TailBuilder setThrustType(Supplier<OrdnanceCommonConfig.ThrustType> val, boolean showTooltip) {
            this.thrustType = new ProjectileData.ProjectileProperty<>(val, showTooltip);
            return this;
        }

        public TailBuilder setOptimalDensity(Supplier<Double> val, boolean showTooltip) {
            this.optimalDensity = new ProjectileData.ProjectileProperty<>(val, showTooltip);
            return this;
        }

        public TailBuilder setDensityBandwidth(Supplier<Double> val, boolean showTooltip) {
            this.densityBandwidth = new ProjectileData.ProjectileProperty<>(val, showTooltip);
            return this;
        }

        @Override
        public void register() {
            validate();
            REGISTRY.put(block, ProjectileData.tail(
                    weight, stability_factor, baseThrust,
                    maxNozzleTilt,
                    fuelConsumption, thrustType, optimalDensity, densityBandwidth,
                    behavior,
                    customProps));
        }
    }

    public static class FuelTankBuilder extends AbstractComponentBuilder<FuelTankBuilder> {
        private ProjectileData.ProjectileProperty<Double> capacity;

        private FuelTankBuilder(Block block) {
            super(block);
        }

        public FuelTankBuilder setCapacity(Supplier<Double> val, boolean showTooltip) {
            this.capacity = new ProjectileData.ProjectileProperty<>(val, showTooltip);
            return this;
        }

        @Override
        public void register() {
            validate();
            REGISTRY.put(block,
                    ProjectileData.fuelTank(weight, stability_factor, capacity, behavior, customProps));
        }
    }

    public static class PayloadBuilder extends AbstractComponentBuilder<PayloadBuilder> {
        private PayloadBuilder(Block block) {
            super(block);
        }

        @Override
        public void register() {
            validate();
            REGISTRY.put(block,
                    ProjectileData.payload(weight, stability_factor, behavior, customProps));
        }
    }

    public static class OptionalModuleBuilder extends AbstractComponentBuilder<OptionalModuleBuilder> {
        private OptionalModuleBuilder(Block block) {
            super(block);
        }

        @Override
        public void register() {
            validate();
            REGISTRY.put(block,
                    ProjectileData.optionalModule(weight, stability_factor, behavior, customProps));
        }
    }
}
