package com.pixel.create_ordnance.mechanics.scanning;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/**
 * Protocol for retrieving projectile components from any source (World,
 * Inventory, Entity...).
 * <p>
 * The implementation decides HOW to find components.
 * The output is always a {@link ScrapResult} ready for aggregation.
 */
public interface IProjectileScrapper {

    /**
     * Performs the scan.
     * 
     * @param scale The geometric scale factor to apply to the projectile.
     * @return A {@link ScrapResult} containing components and geometric data.
     */
    ScrapResult scan(float scale);

    /**
     * Factory for creating scrappers with world context.
     */
    @FunctionalInterface
    public interface ScrapperFactory {
        IProjectileScrapper create(Level level, BlockPos pos,
                Direction facing);
    }

}