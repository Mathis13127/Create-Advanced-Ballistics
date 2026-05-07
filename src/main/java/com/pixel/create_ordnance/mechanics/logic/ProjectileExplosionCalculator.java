package com.pixel.create_ordnance.mechanics.logic;

import java.util.Optional;

import net.minecraft.core.BlockPos;

import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

/**
 * Custom explosion calculator that ignores fluid blast resistance.
 * This allows projectiles to explode effectively underwater or in lava.
 */
public class ProjectileExplosionCalculator extends ExplosionDamageCalculator {

    public static final ProjectileExplosionCalculator INSTANCE = new ProjectileExplosionCalculator();

    @Override
    public Optional<Float> getBlockExplosionResistance(Explosion explosion, BlockGetter reader, BlockPos pos,
            BlockState state, FluidState fluid) {
        // If it's a fluid, ignore its resistance (return 0.0)
        // This allows the explosion power to propagate without being dampened by
        // water/lava.
        if (!fluid.isEmpty()) {
            return Optional.of(0.0f);
        }
        // Otherwise use default behavior
        return super.getBlockExplosionResistance(explosion, reader, pos, state, fluid);
    }

    @Override
    public boolean shouldBlockExplode(Explosion explosion, BlockGetter reader, BlockPos pos, BlockState state,
            float power) {
        // DO NOT DESTROY FLUIDS.
        // Even if resistance is 0, we don't want the explosion to replace water/lava
        // with air blocks.
        if (!state.getFluidState().isEmpty()) {
            return false;
        }
        return super.shouldBlockExplode(explosion, reader, pos, state, power);
    }
}