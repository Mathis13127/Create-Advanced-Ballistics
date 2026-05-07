package com.pixel.create_ordnance.mechanics.scanning;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.pixel.create_ordnance.CreateOrdnance;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CurrentProjectileManager {

    // Cache: Map every block of a projectile to the ScrapResult
    private static final Map<BlockPos, ScrapResult> BLOCK_TO_PROJECTILE = new HashMap<>();
    private static final List<ScrapResult> KNOWN_PROJECTILES = new ArrayList<>();

    // The projectile currently being looked at (for Overlay/Debug)
    private static ScrapResult activeProjectile;

    public static void updateResult(ScrapResult newResult) {
        if (newResult == null || newResult.blockIds().isEmpty())
            return;

        // 1. Identify Overlaps: Find existing projectiles that share blocks with the
        // new one
        Set<ScrapResult> toRemove = new HashSet<>();
        List<BlockPos> newBlockPositions = new ArrayList<>();

        // Linear assembly logic (V3.5):
        // Start at worldOrigin (normalized to BlockPos).
        // Blocks are placed in the direction of worldFacing.
        BlockPos startPos = BlockPos.containing(newResult.worldOrigin());
        Direction scanDir = newResult.worldFacing();

        for (int i = 0; i < newResult.blockIds().size(); i++) {
            BlockPos worldPos = startPos.relative(scanDir, i);
            newBlockPositions.add(worldPos);

            if (BLOCK_TO_PROJECTILE.containsKey(worldPos)) {
                toRemove.add(BLOCK_TO_PROJECTILE.get(worldPos));
            }
        }

        // 2. Remove old versions (Merged or Updated projectiles)
        for (ScrapResult old : toRemove) {
            KNOWN_PROJECTILES.remove(old);
            BlockPos oldStart = BlockPos.containing(old.worldOrigin());
            Direction oldScanDir = old.worldFacing().getOpposite();

            for (int i = 0; i < old.blockIds().size(); i++) {
                BlockPos oldPos = oldStart.relative(oldScanDir, i);
                if (BLOCK_TO_PROJECTILE.get(oldPos) == old) {
                    BLOCK_TO_PROJECTILE.remove(oldPos);
                }
            }
        }

        // 3. Add new version
        KNOWN_PROJECTILES.add(newResult);
        for (BlockPos pos : newBlockPositions) {
            BLOCK_TO_PROJECTILE.put(pos, newResult);
        }

        // Update active if it was replaced
        if (toRemove.contains(activeProjectile)) {
            activeProjectile = newResult;
        }
    }

    public static ScrapResult getCached(BlockPos pos) {
        return BLOCK_TO_PROJECTILE.get(pos);
    }

    public static void setActiveProjectile(ScrapResult result) {
        activeProjectile = result;
    }

    public static ScrapResult getActiveProjectile() {
        return activeProjectile;
    }

    public static void clearActiveProjectile() {
        activeProjectile = null;
    }

    public static void clear() {
        BLOCK_TO_PROJECTILE.clear();
        KNOWN_PROJECTILES.clear();
        activeProjectile = null;
    }

    /**
     * Re-analyzes all cached projectiles to account for configuration changes.
     */
    public static void reanalyzeAll() {
        if (KNOWN_PROJECTILES.isEmpty())
            return;

        List<ScrapResult> oldResults = new ArrayList<>(KNOWN_PROJECTILES);
        clear(); // Wipe and rebuild

        for (ScrapResult old : oldResults) {
            try {
                // Re-analysis in V3.5 just uses the same IDs but triggers cache flush in
                // Analyzer
                ProjectileAnalyzer.analyze(old);
                updateResult(old);
            } catch (Exception e) {
                CreateOrdnance.LOGGER
                        .error("Failed to re-analyze projectile during config reload: " + e.getMessage());
            }
        }
    }

    // Legacy support (optional, can remove if all usages updated)
    public static ScrapResult getLatestResult() {
        return activeProjectile;
    }
}