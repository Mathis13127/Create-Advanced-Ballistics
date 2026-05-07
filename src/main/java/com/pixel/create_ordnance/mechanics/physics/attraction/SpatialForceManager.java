package com.pixel.create_ordnance.mechanics.physics.attraction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.pixel.create_ordnance.CreateOrdnance;
import com.pixel.create_ordnance.api.physics.AttractionParams;
import com.pixel.create_ordnance.config.OrdnanceConfigs;
import com.pixel.create_ordnance.network.ClientboundAttractionSyncPacket;
import com.pixel.create_ordnance.network.ClientboundAttractionSyncPacket.AttractionData;

import net.minecraft.resources.ResourceKey;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Registry and lifecycle manager for all dynamic spatial attraction sources.
 * <p>
 * Singleton with explicit lifecycle: {@link #init(MinecraftServer)} on server start,
 * {@link #shutdown()} on server stop. Persistence is delegated to
 * {@link SpatialForceSavedData} attached to the overworld.
 * <p>
 * Sources are ticked per-dimension with a guard to prevent double-ticking.
 * Debug visualization is broadcast via {@link ClientboundAttractionSyncPacket}.
 */
public class SpatialForceManager {

    private static SpatialForceManager INSTANCE;

    private final Map<Integer, AttractionSource> activeSources = new HashMap<>();
    private int nextId = 1;
    private MinecraftServer server;

    /** Per-dimension tick guard: prevents double ticking from multiple callers. */
    private final Map<ResourceKey<Level>, Long> lastTickTimes = new HashMap<>();

    private SpatialForceManager() {
    }

    // =========================================================
    // LIFECYCLE
    // =========================================================

    /**
     * Initializes the manager and loads persisted data from the overworld.
     * Called on {@code ServerStartedEvent}.
     */
    public static void init(MinecraftServer server) {
        INSTANCE = new SpatialForceManager();
        INSTANCE.server = server;

        // Load from SavedData
        ServerLevel overworld = server.overworld();
        SpatialForceSavedData data = SpatialForceSavedData.getOrCreate(overworld);
        INSTANCE.activeSources.putAll(data.activeSources);
        INSTANCE.nextId = data.idGenerator.get();

        CreateOrdnance.LOGGER.info("[Attraction] Initialized with {} source(s), nextId={}",
                INSTANCE.activeSources.size(), INSTANCE.nextId);
    }

    /**
     * Shuts down the manager and clears all state.
     * Called on {@code ServerStoppingEvent}.
     */
    public static void shutdown() {
        if (INSTANCE != null) {
            CreateOrdnance.LOGGER.info("[Attraction] Shutdown, {} source(s) persisted.",
                    INSTANCE.activeSources.size());
        }
        INSTANCE = null;
    }

    /**
     * Returns the active instance, or null if the server has not started.
     */
    public static SpatialForceManager getInstance() {
        return INSTANCE;
    }

    // =========================================================
    // CRUD API
    // =========================================================

    /**
     * Spawns a new attraction source and returns its unique ID.
     *
     * @return Source ID, or -1 if max sources reached
     */
    public int spawn(AttractionParams params, ResourceKey<Level> dimension, Vec3 pos) {
        int maxSources = OrdnanceConfigs.COMMON.physics.attraction.maxSources.get();
        if (activeSources.size() >= maxSources) {
            CreateOrdnance.LOGGER.warn("[Attraction] Cannot spawn: max sources ({}) reached.", maxSources);
            return -1;
        }

        int id = nextId++;
        AttractionSource source = new AttractionSource(id, params, dimension, pos);
        activeSources.put(id, source);
        markDirty();

        CreateOrdnance.LOGGER.debug("[Attraction] Spawned source #{} at {} in {}", id, pos, dimension.location());
        return id;
    }

    /**
     * Removes a source by ID.
     */
    public void remove(int id) {
        AttractionSource removed = activeSources.remove(id);
        if (removed != null) {
            markDirty();
            CreateOrdnance.LOGGER.debug("[Attraction] Removed source #{}", id);
        }
    }

    /**
     * Returns a specific source by ID, or null if not found.
     */
    public AttractionSource get(int id) {
        return activeSources.get(id);
    }

    /**
     * Returns all active sources (read-only view).
     */
    public Collection<AttractionSource> getActiveSources() {
        return activeSources.values();
    }

    /**
     * Returns all active IDs (for command tab-completion).
     */
    public Set<Integer> getActiveIds() {
        return activeSources.keySet();
    }

    /**
     * Returns the number of active sources.
     */
    public int getSourceCount() {
        return activeSources.size();
    }

    /**
     * Updates the parameters of an existing source and persists the change.
     * <p>
     * This is the correct way to modify source parameters — calling
     * {@code source.setParams()} directly does NOT trigger persistence.
     *
     * @param id     Source ID
     * @param params New parameters
     * @return true if the source was found and updated
     */
    public boolean configure(int id, AttractionParams params) {
        AttractionSource source = activeSources.get(id);
        if (source == null) return false;
        source.setParams(params);
        markDirty();
        CreateOrdnance.LOGGER.debug("[Attraction] Configured source #{}: str={} exp={} range={}",
                id, params.strength(), params.exponent(), params.maxRange());
        return true;
    }

    // =========================================================
    // TICK
    // =========================================================

    /**
     * Ticks all sources in the given dimension.
     * <p>
     * Guarded to execute once per dimension per game tick. Handles:
     * <ul>
     *   <li>Late entity binding (UUID → Entity after world reload)</li>
     *   <li>Source position update from tracked entities</li>
     *   <li>Debug sync broadcast to clients</li>
     * </ul>
     */
    public void tick(Level level) {
        if (level.isClientSide || activeSources.isEmpty()) return;

        ResourceKey<Level> dim = level.dimension();
        long currentTime = level.getGameTime();

        // Per-dimension guard: prevent double ticking
        if (lastTickTimes.getOrDefault(dim, -1L) == currentTime) return;
        lastTickTimes.put(dim, currentTime);

        boolean debug = OrdnanceConfigs.COMMON.divers.debugMode.get();
        int syncInterval = OrdnanceConfigs.COMMON.physics.attraction.debugSyncInterval.get();
        List<AttractionData> syncData = debug ? new ArrayList<>() : null;

        for (AttractionSource source : activeSources.values()) {
            if (!source.getDimension().equals(dim)) continue;

            // Late binding: re-bind tracked entity after world reload
            if (source.getTrackedEntityId() != null && source.getTrackedEntity() == null
                    && level instanceof ServerLevel serverLevel) {
                Entity entity = serverLevel.getEntity(source.getTrackedEntityId());
                if (entity != null) {
                    source.bindTrackedEntity(entity);
                }
            }

            source.tick();

            if (syncData != null) {
                syncData.add(new AttractionData(
                        source.getId(), source.getPosition(), source.getParams().maxRange()));
            }
        }

        // Broadcast debug sync every N ticks
        if (syncData != null && !syncData.isEmpty()
                && currentTime % syncInterval == 0
                && level instanceof ServerLevel serverLevel) {
            ClientboundAttractionSyncPacket packet = new ClientboundAttractionSyncPacket(syncData);
            for (ServerPlayer player : serverLevel.players()) {
                PacketDistributor.sendToPlayer(player, packet);
            }
        }
    }

    // =========================================================
    // PERSISTENCE
    // =========================================================

    /**
     * Marks the SavedData as dirty so it will be saved on next world save.
     */
    private void markDirty() {
        if (server != null) {
            ServerLevel overworld = server.overworld();
            SpatialForceSavedData data = SpatialForceSavedData.getOrCreate(overworld);
            // Sync state to SavedData
            data.activeSources.clear();
            data.activeSources.putAll(activeSources);
            data.idGenerator.set(nextId);
            data.setDirty();
        }
    }
}