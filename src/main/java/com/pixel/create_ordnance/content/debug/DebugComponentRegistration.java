package com.pixel.create_ordnance.content.debug;

import com.pixel.create_ordnance.config.OrdnanceConfigs;
import com.pixel.create_ordnance.mechanics.logic.behaviors.ExplosiveBehavior;
import com.pixel.create_ordnance.registry.ModBlocks;
import com.pixel.create_ordnance.registry.ProjectileComponentRegistry;

public class DebugComponentRegistration {

        public static void register() {
                // Afterburner Tail
                ProjectileComponentRegistry.entry(ModBlocks.DEBUG_TAIL.get())
                                .asTail()
                                .setWeight(OrdnanceConfigs.COMMON.debugComponents.tails.afterburner.weight::get, true)
                                .setStabilityFactor(
                                                OrdnanceConfigs.COMMON.debugComponents.tails.afterburner.stabilityFactor::get,
                                                true)
                                .setBaseThrust(OrdnanceConfigs.COMMON.debugComponents.tails.afterburner.baseThrust::get,
                                                true)
                                .setMaxNozzleTilt(
                                                OrdnanceConfigs.COMMON.debugComponents.tails.afterburner.maxNozzleTilt::get,
                                                true)
                                .setFuelConsumption(
                                                OrdnanceConfigs.COMMON.debugComponents.tails.afterburner.fuelConsumption::get,
                                                true)
                                .setThrustType(OrdnanceConfigs.COMMON.debugComponents.tails.afterburner.thrustType::get,
                                                true)
                                .setOptimalDensity(
                                                OrdnanceConfigs.COMMON.debugComponents.tails.afterburner.optimalDensity::get,
                                                true)
                                .setDensityBandwidth(
                                                OrdnanceConfigs.COMMON.debugComponents.tails.afterburner.densityBandwidth::get,
                                                true)
                                .register();

                // Propeller Tail
                ProjectileComponentRegistry.entry(ModBlocks.DEBUG_PROPELLER_TAIL.get())
                                .asTail()
                                .setWeight(OrdnanceConfigs.COMMON.debugComponents.tails.propeller.weight::get, true)
                                .setStabilityFactor(
                                                OrdnanceConfigs.COMMON.debugComponents.tails.propeller.stabilityFactor::get,
                                                true)
                                .setBaseThrust(OrdnanceConfigs.COMMON.debugComponents.tails.propeller.baseThrust::get,
                                                true)
                                .setMaxNozzleTilt(
                                                OrdnanceConfigs.COMMON.debugComponents.tails.propeller.maxNozzleTilt::get,
                                                true)
                                .setFuelConsumption(
                                                OrdnanceConfigs.COMMON.debugComponents.tails.propeller.fuelConsumption::get,
                                                true)
                                .setThrustType(OrdnanceConfigs.COMMON.debugComponents.tails.propeller.thrustType::get,
                                                true)
                                .setOptimalDensity(
                                                OrdnanceConfigs.COMMON.debugComponents.tails.propeller.optimalDensity::get,
                                                true)
                                .setDensityBandwidth(
                                                OrdnanceConfigs.COMMON.debugComponents.tails.propeller.densityBandwidth::get,
                                                true)
                                .register();

                // Fuel Tank
                ProjectileComponentRegistry.entry(ModBlocks.DEBUG_FUEL_TANK.get())
                                .asFuelTank()
                                .setWeight(OrdnanceConfigs.COMMON.debugComponents.fuelTank.weight::get, true)
                                .setStabilityFactor(OrdnanceConfigs.COMMON.debugComponents.fuelTank.stabilityFactor::get,
                                                true)
                                .setCapacity(OrdnanceConfigs.COMMON.debugComponents.fuelTank.capacity::get, true)
                                .register();

                // Payload — uses ExplosiveBehavior for on-impact explosion
                ProjectileComponentRegistry.entry(ModBlocks.DEBUG_PAYLOAD.get())
                                .asPayload()
                                .setWeight(OrdnanceConfigs.COMMON.debugComponents.payload.weight::get, true)
                                .setStabilityFactor(OrdnanceConfigs.COMMON.debugComponents.payload.stabilityFactor::get,
                                                true)
                                .behavior(new ExplosiveBehavior())
                                .addParam("explosion_power",
                                                OrdnanceConfigs.COMMON.debugComponents.payload.explosionPower::get, true)
                                .register();

                // Optional Module
                ProjectileComponentRegistry.entry(ModBlocks.DEBUG_OPTIONAL_MODULE.get())
                                .asOptionalModule()
                                .setWeight(OrdnanceConfigs.COMMON.debugComponents.optionalModule.weight::get, true)
                                .setStabilityFactor(
                                                OrdnanceConfigs.COMMON.debugComponents.optionalModule.stabilityFactor::get,
                                                true)
                                .register();

                // Ballast Module
                ProjectileComponentRegistry.entry(ModBlocks.DEBUG_BALLAST.get())
                                .asOptionalModule()
                                .setWeight(OrdnanceConfigs.COMMON.debugComponents.ballast.weight::get, true)
                                .setStabilityFactor(OrdnanceConfigs.COMMON.debugComponents.ballast.stabilityFactor::get,
                                                true)
                                .register();

                // Fins Module
                ProjectileComponentRegistry.entry(ModBlocks.DEBUG_FINS.get())
                                .asOptionalModule()
                                .setWeight(OrdnanceConfigs.COMMON.debugComponents.fins.weight::get, true)
                                .setStabilityFactor(OrdnanceConfigs.COMMON.debugComponents.fins.stabilityFactor::get,
                                                true)
                                .register();
        }

}
