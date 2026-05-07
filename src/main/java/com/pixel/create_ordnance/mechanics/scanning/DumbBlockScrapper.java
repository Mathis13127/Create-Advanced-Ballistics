package com.pixel.create_ordnance.mechanics.scanning;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import com.pixel.create_ordnance.registry.ProjectileComponentRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;

import net.minecraft.resources.ResourceLocation;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * A "dumb" scrapper that performs a flood-fill search (6 directions) to find
 * all
 * connected projectile components.
 * <p>
 * This scrapper is intended for STRESS TESTING the geometry validation logic in
 * {@link ProjectileAnalyzer}.
 * Avoid using this in production as it may pick up neighboring projectiles.
 */
public class DumbBlockScrapper implements IProjectileScrapper {

    private final Level level;
    private final BlockPos startPos;
    private final Direction facing;

    public DumbBlockScrapper(Level level, BlockPos startPos, Direction facing) {
        this.level = level;
        this.startPos = startPos;
        this.facing = facing;
    }

    @Override
    public ScrapResult scan(float scale) {
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();
        List<BlockPos> foundBlocks = new ArrayList<>();

        queue.add(startPos);
        visited.add(startPos);

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();

            // Validation and Collection
            BlockState state = level.getBlockState(current);
            if (ProjectileComponentRegistry.isComponent(state.getBlock())) {
                foundBlocks.add(current);

                // Check 6 neighbors
                for (Direction dir : Direction.values()) {
                    BlockPos neighbor = current.relative(dir);
                    if (!visited.contains(neighbor)) {
                        visited.add(neighbor);
                        queue.add(neighbor);
                    }
                }
            }
        }

        // Conversion to IDs
        List<ResourceLocation> ids = new ArrayList<>();
        BlockPos min = startPos;
        BlockPos max = startPos;

        for (BlockPos pos : foundBlocks) {
            ids.add(BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()));

            // Update Bounds (Simple bounding box)
            min = new BlockPos(
                    Math.min(min.getX(), pos.getX()),
                    Math.min(min.getY(), pos.getY()),
                    Math.min(min.getZ(), pos.getZ()));
            max = new BlockPos(
                    Math.max(max.getX(), pos.getX()),
                    Math.max(max.getY(), pos.getY()),
                    Math.max(max.getZ(), pos.getZ()));
        }

        return new ScrapResult(
                ids,
                Vec3.atLowerCornerOf(min),
                min,
                max,
                facing,
                scale);
    }
}