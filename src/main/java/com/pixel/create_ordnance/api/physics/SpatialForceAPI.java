package com.pixel.create_ordnance.api.physics;

import java.util.Collections;
import java.util.Set;

import com.pixel.create_ordnance.mechanics.physics.attraction.AttractionSource;
import com.pixel.create_ordnance.mechanics.physics.attraction.SpatialForceManager;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Public API for creating and managing spatial attraction/repulsion sources.
 * <p>
 * This is the <b>intended entry point for addons and external code</b>.
 * All methods delegate to the internal {@code SpatialForceManager}.
 * <p>
 * <b>Quick start:</b>
 * <pre>{@code
 * // Spawn a black hole preset
 * int id = SpatialForceAPI.spawnBlackhole(level, pos);
 *
 * // Spawn with custom parameters
 * AttractionParams custom = AttractionParams.builder()
 *     .strength(-20.0)   // negative = repulsion
 *     .maxRange(48.0)
 *     .build();
 * int id2 = SpatialForceAPI.spawn(custom, level, pos);
 *
 * // Make it follow an entity
 * SpatialForceAPI.track(id, targetEntity);
 *
 * // Modify parameters live
 * SpatialForceAPI.configure(id, AttractionParams.builder()
 *     .strength(100.0)
 *     .maxRange(64.0)
 *     .build());
 *
 * // Query state
 * Vec3 pos = SpatialForceAPI.getPosition(id);
 * AttractionParams params = SpatialForceAPI.getParams(id);
 *
 * // Clean up
 * SpatialForceAPI.remove(id);
 * }</pre>
 *
 * @see AttractionParams
 * @see AttractionParams.Builder
 */
public class SpatialForceAPI {

    private SpatialForceAPI() {} // Static API — no instantiation

    // =========================================================
    // SPAWN
    // =========================================================

    /**
     * Spawns a new attraction source with the given parameters.
     *
     * @return Source ID (positive), or -1 if manager is not initialized or max sources reached
     */
    public static int spawn(AttractionParams params, ResourceKey<Level> dimension, Vec3 pos) {
        SpatialForceManager manager = SpatialForceManager.getInstance();
        if (manager == null) return -1;
        return manager.spawn(params, dimension, pos);
    }

    /**
     * Spawns a new attraction source with a direct level reference.
     */
    public static int spawn(AttractionParams params, Level level, Vec3 pos) {
        SpatialForceManager manager = SpatialForceManager.getInstance();
        if (manager == null) return -1;
        return manager.spawn(params, level.dimension(), pos);
    }

    /**
     * Spawns a new attraction source with {@link AttractionParams#defaultAttraction()} parameters.
     */
    public static int spawnDefault(Level level, Vec3 pos) {
        return spawn(AttractionParams.defaultAttraction(), level, pos);
    }

    /**
     * Spawns a <b>Black Hole</b> preset: violent inward pull with spiral vortex.
     *
     * @see AttractionParams#blackhole()
     */
    public static int spawnBlackhole(Level level, Vec3 pos) {
        return spawn(AttractionParams.blackhole(), level, pos);
    }

    /**
     * Spawns a <b>Deflector</b> preset: repulsive force that pushes projectiles away.
     *
     * @see AttractionParams#deflector()
     */
    public static int spawnDeflector(Level level, Vec3 pos) {
        return spawn(AttractionParams.deflector(), level, pos);
    }

    /**
     * Spawns a <b>Magnet</b> preset: attracts and holds projectiles in orbit at ~3 blocks.
     *
     * @see AttractionParams#magnet()
     */
    public static int spawnMagnet(Level level, Vec3 pos) {
        return spawn(AttractionParams.magnet(), level, pos);
    }

    // =========================================================
    // MODIFY
    // =========================================================

    /**
     * Makes a source follow an entity's position each tick.
     * The source will update its position to match the entity every tick.
     * If the entity is removed, tracking is automatically cleared.
     */
    public static void track(int id, Entity entity) {
        SpatialForceManager manager = SpatialForceManager.getInstance();
        if (manager == null) return;
        AttractionSource source = manager.get(id);
        if (source != null) {
            source.setTrackedEntity(entity);
        }
    }

    /**
     * Instantly moves a source to a new position and breaks any entity tracking.
     */
    public static void teleport(int id, Vec3 pos) {
        SpatialForceManager manager = SpatialForceManager.getInstance();
        if (manager == null) return;
        AttractionSource source = manager.get(id);
        if (source != null) {
            source.setPosition(pos);
        }
    }

    /**
     * Updates the parameters of an existing source.
     * <p>
     * Changes are persisted to disk automatically.
     *
     * @return true if the source was found and updated, false otherwise
     */
    public static boolean configure(int id, AttractionParams params) {
        SpatialForceManager manager = SpatialForceManager.getInstance();
        if (manager == null) return false;
        return manager.configure(id, params);
    }

    /**
     * Removes an attraction source by ID.
     */
    public static void remove(int id) {
        SpatialForceManager manager = SpatialForceManager.getInstance();
        if (manager == null) return;
        manager.remove(id);
    }

    // =========================================================
    // QUERY
    // =========================================================

    /**
     * Returns the parameters of a source, or null if not found.
     */
    public static AttractionParams getParams(int id) {
        SpatialForceManager manager = SpatialForceManager.getInstance();
        if (manager == null) return null;
        AttractionSource source = manager.get(id);
        return source != null ? source.getParams() : null;
    }

    /**
     * Returns the current world position of a source, or null if not found.
     */
    public static Vec3 getPosition(int id) {
        SpatialForceManager manager = SpatialForceManager.getInstance();
        if (manager == null) return null;
        AttractionSource source = manager.get(id);
        return source != null ? source.getPosition() : null;
    }

    /**
     * Returns the dimension a source lives in, or null if not found.
     */
    public static ResourceKey<Level> getDimension(int id) {
        SpatialForceManager manager = SpatialForceManager.getInstance();
        if (manager == null) return null;
        AttractionSource source = manager.get(id);
        return source != null ? source.getDimension() : null;
    }

    /**
     * Returns whether a source is currently tracking an entity.
     */
    public static boolean isTracking(int id) {
        SpatialForceManager manager = SpatialForceManager.getInstance();
        if (manager == null) return false;
        AttractionSource source = manager.get(id);
        return source != null && source.getTrackedEntityId() != null;
    }

    /**
     * Returns true if a source with the given ID exists.
     */
    public static boolean exists(int id) {
        SpatialForceManager manager = SpatialForceManager.getInstance();
        if (manager == null) return false;
        return manager.get(id) != null;
    }

    /**
     * Returns the set of all active source IDs.
     *
     * @return Unmodifiable set of active IDs, or empty set if manager not initialized
     */
    public static Set<Integer> getActiveIds() {
        SpatialForceManager manager = SpatialForceManager.getInstance();
        if (manager == null) return Collections.emptySet();
        return Collections.unmodifiableSet(manager.getActiveIds());
    }

    /**
     * Returns the number of active sources.
     */
    public static int getSourceCount() {
        SpatialForceManager manager = SpatialForceManager.getInstance();
        if (manager == null) return 0;
        return manager.getSourceCount();
    }
}
