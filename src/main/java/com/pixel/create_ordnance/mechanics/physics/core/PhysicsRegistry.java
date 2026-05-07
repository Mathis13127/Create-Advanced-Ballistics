package com.pixel.create_ordnance.mechanics.physics.core;

import java.util.HashMap;
import java.util.Map;

import com.pixel.create_ordnance.mechanics.physics.core.PhysicsRegistry.PhysicsProfile;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Registry for environmental physics profiles.
 * standardizes densities and fluid properties across the project.
 */
public class PhysicsRegistry {
    private static final Map<Block, PhysicsProfile> REGISTRY = new HashMap<>();

    /**
     * The Standard Reference: AIR.
     * Viscosity is set to 0.02 (relative to Water = 1.0) to provide
     * minimal "roll grip" and damping for stabilization.
     */
    public static final PhysicsProfile AIR = new PhysicsProfile("air", PhysicsConstants.AIR_DENSITY, 0.02, false, true);

    public record PhysicsProfile(String name, double density, double viscosity, boolean isFluid, boolean compressible) {
    }

    static {
        // Core Minecraft Blocks
        register(Blocks.AIR, AIR);
        register(Blocks.WATER, new PhysicsProfile("water", 1000.0, 1.0, true, false));
        register(Blocks.LAVA, new PhysicsProfile("lava", 3100.0, 10.0, true, false));
    }

    public static void register(Block block, PhysicsProfile profile) {
        REGISTRY.put(block, profile);
    }

    public static PhysicsProfile get(BlockState state) {
        return REGISTRY.getOrDefault(state.getBlock(), AIR);
    }

    public static Map<Block, PhysicsProfile> getRegistry() {
        return REGISTRY;
    }
}