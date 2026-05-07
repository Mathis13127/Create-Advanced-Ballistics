package com.pixel.create_ordnance.mechanics.scanning;

import java.util.ArrayList;
import java.util.List;

import com.pixel.create_ordnance.config.OrdnanceConfigs;
import com.pixel.create_ordnance.foundation.utility.StateHelper;
import com.pixel.create_ordnance.registry.ProjectileComponentRegistry;
import com.pixel.create_ordnance.registry.ProjectileComponentType;
import com.pixel.create_ordnance.registry.ProjectileData;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;

import net.minecraft.resources.ResourceLocation;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

/**
 * WorldBlockScrapper V5.1 (Deterministic & Functional Flux).
 * 
 * <p>
 * This engine is responsible for converting a physical cluster of blocks in the
 * world
 * into a linear list of components for projectile analysis. It follows a strict
 * "Deterministic" philosophy where the player's interaction defines the truth.
 * </p>
 * 
 * <h2>Key Principles:</h2>
 * <ul>
 * <li><b>Source of Intent:</b> The flight direction (Forward) is defined by the
 * face the player clicks.</li>
 * <li><b>Terminal Locks:</b> The scan is bounded by the first Engine (back) and
 * first Warhead (front).</li>
 * <li><b>Functional Flux:</b> Every component in the sequence must be oriented
 * towards the Forward direction.
 * Misaligned blocks act as structural disconnects and terminate the scan.</li>
 * </ul>
 * 
 * @author Ordnance Scanning System
 */
public class WorldBlockScrapper implements IProjectileScrapper {

    private final Level level;
    private final BlockPos startPos;
    private final Direction clickedFacing;

    /**
     * Initializes the scrapper at a specific world position.
     * 
     * @param level         The world level to scan in.
     * @param startPos      The exact block position where the scan initiated.
     * @param clickedFacing The face of the block that was clicked, defining the
     *                      Forward direction.
     */
    public WorldBlockScrapper(Level level, BlockPos startPos, Direction clickedFacing) {
        this.level = level;
        this.startPos = startPos;
        this.clickedFacing = clickedFacing;
    }

    /**
     * Executes the scan along the axis of the clicked face.
     * 
     * @param scale Physical scale of the resulting projectile.
     * @return A {@link ScrapResult} containing the ordered list of components and
     *         metadata.
     * @throws IllegalStateException If the scan starts on a non-component block.
     */
    @Override
    public ScrapResult scan(float scale) {
        ProjectileData startData = getData(startPos);
        if (startData == null) {
            throw new IllegalStateException("[Ordnance] Projectile scan started at an invalid block: " + startPos);
        }

        int maxLen = OrdnanceConfigs.COMMON.maxProjectileLength.get();
        Direction projectileForward = clickedFacing;
        Direction.Axis axis = projectileForward.getAxis();
        Direction projectileBack = projectileForward.getOpposite();

        // ============================================================
        // STEP 1: TERMINAL LOCK SEARCH (Discover Boundaries)
        // ============================================================
        // We search in both directions from the click point to find the
        // functional ends of the projectile.

        // --- A. Backward Scan (Towards Engine) ---
        BlockPos minBound = startPos;
        // Optimization: If we clicked directly on the Tail, we are at the lock.
        boolean lockBack = (startData.type() == ProjectileComponentType.TAIL);

        if (!lockBack && matchesOrientation(level.getBlockState(startPos), projectileForward)) {
            BlockPos current = startPos.relative(projectileBack);
            for (int i = 0; i < maxLen; i++) {
                ProjectileData data = getData(current);
                BlockState state = level.getBlockState(current);

                // Stop if we hit air, non-component, or a block on the wrong axis
                if (data == null || !StateHelper.isOnAxis(state, axis))
                    break;

                // FUNCTIONAL FLUX: Stop if a component points in a different direction
                if (!matchesOrientation(state, projectileForward))
                    break;

                // PROTECTION: A Warhead cannot be behind the body
                if (data.type() == ProjectileComponentType.PAYLOAD)
                    break;

                minBound = current;
                // TERMINAL LOCK: Engine (Tail) found, this is the absolute back of the missile
                if (data.type() == ProjectileComponentType.TAIL)
                    break;

                current = current.relative(projectileBack);
            }
        }

        // --- B. Forward Scan (Towards Warhead) ---
        BlockPos maxBound = startPos;
        // Optimization: If we clicked directly on the Payload, we are at the lock.
        boolean lockFront = (startData.type() == ProjectileComponentType.PAYLOAD);

        if (!lockFront && matchesOrientation(level.getBlockState(startPos), projectileForward)) {
            BlockPos current = startPos.relative(projectileForward);
            for (int i = 0; i < maxLen; i++) {
                ProjectileData data = getData(current);
                BlockState state = level.getBlockState(current);

                if (data == null || !StateHelper.isOnAxis(state, axis))
                    break;

                // FUNCTIONAL FLUX: Stop if a component points in a different direction
                if (!matchesOrientation(state, projectileForward))
                    break;

                // PROTECTION: An Engine cannot be in front of the body
                if (data.type() == ProjectileComponentType.TAIL)
                    break;

                maxBound = current;
                // TERMINAL LOCK: Warhead (Payload) found, this is the absolute front of the
                // missile
                if (data.type() == ProjectileComponentType.PAYLOAD)
                    break;

                current = current.relative(projectileForward);
            }
        }

        // ============================================================
        // STEP 2: COLLECTION PASS (Linear Extraction)
        // ============================================================

        List<ResourceLocation> blockIds = new ArrayList<>();

        // Final sanity check: If the origin block itself doesn't match orientation,
        // we fallback to a 1-block projectile to prevent illegal scans.
        if (!matchesOrientation(level.getBlockState(startPos), projectileForward)) {
            minBound = startPos;
            maxBound = startPos;
        }

        BlockPos current = minBound;
        while (true) {
            BlockState state = level.getBlockState(current);
            blockIds.add(BuiltInRegistries.BLOCK.getKey(state.getBlock()));

            if (current.equals(maxBound))
                break;
            current = current.relative(projectileForward);
        }

        return new ScrapResult(
                blockIds,
                Vec3.atLowerCornerOf(minBound),
                minBound,
                maxBound,
                projectileForward,
                scale);
    }

    /**
     * Validates if a block's physical orientation matches the intended functional
     * flux.
     * 
     * <p>
     * Rules:
     * </p>
     * <ul>
     * <li>For Directional blocks: {@code FACING} property must match the flight
     * direction exactly.</li>
     * <li>For Pillar/Axis blocks: The {@code AXIS} must align with the flight
     * axis.</li>
     * <li>For Neutral blocks: Returns {@code true} (safe fallback).</li>
     * </ul>
     * 
     * @param state             The BlockState to check.
     * @param projectileForward The intended flight direction.
     * @return True if the block is cooperative with the flight direction.
     */
    private boolean matchesOrientation(BlockState state, Direction projectileForward) {
        // 1. Directional blocks (Tails, Payloads, oriented Tanks)
        Direction facing = StateHelper.getFacing(state);
        if (facing != null) {
            return facing == projectileForward;
        }

        // 2. Axial blocks (Pillars, simple Tanks)
        if (state.hasProperty(BlockStateProperties.AXIS)) {
            return state.getValue(BlockStateProperties.AXIS) == projectileForward.getAxis();
        }

        if (state.hasProperty(BlockStateProperties.HORIZONTAL_AXIS)) {
            if (projectileForward.getAxis().isVertical())
                return false;
            return state.getValue(BlockStateProperties.HORIZONTAL_AXIS) == projectileForward.getAxis();
        }

        // 3. Neutral components (No orientation data)
        return true;
    }

    /**
     * Internal helper to fetch registered projectile data for a position.
     * 
     * @param pos The position to check.
     * @return {@link ProjectileData} if registered, otherwise null.
     */
    private ProjectileData getData(BlockPos pos) {
        if (!level.isLoaded(pos))
            return null;
        BlockState state = level.getBlockState(pos);
        if (state.isAir())
            return null;
        return ProjectileComponentRegistry.getData(state.getBlock()).orElse(null);
    }
}