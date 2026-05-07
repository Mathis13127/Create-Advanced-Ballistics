package com.pixel.create_ordnance.mechanics.physics.attraction;

import java.util.UUID;

import com.pixel.create_ordnance.api.physics.AttractionParams;

import net.minecraft.core.registries.Registries;

import net.minecraft.nbt.CompoundTag;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Instance of an active spatial attraction source.
 * <p>
 * Each source occupies a world-space position and applies forces to projectiles
 * within its {@link AttractionParams#maxRange()}. Sources can optionally track
 * an entity, following its position each tick.
 * <p>
 * The {@code trackedEntity} reference is transient (not persisted). On world reload,
 * {@code trackedEntityId} is used for late binding via {@link #tick()}.
 */
public class AttractionSource {

    private final int id;
    private AttractionParams params;
    private final ResourceKey<Level> dimension;
    private Vec3 position;
    private Entity trackedEntity;
    private UUID trackedEntityId;

    public AttractionSource(int id, AttractionParams params, ResourceKey<Level> dimension, Vec3 position) {
        this.id = id;
        this.params = params;
        this.dimension = dimension;
        this.position = position;
    }

    // =========================================================
    // TICK
    // =========================================================

    /**
     * Updates position from tracked entity. Clears tracking if entity is removed.
     */
    public void tick() {
        if (trackedEntity != null) {
            if (trackedEntity.isRemoved()) {
                trackedEntity = null;
                trackedEntityId = null;
            } else {
                this.position = trackedEntity.position();
            }
        }
    }

    // =========================================================
    // GETTERS
    // =========================================================

    public int getId() {
        return id;
    }

    public AttractionParams getParams() {
        return params;
    }

    public ResourceKey<Level> getDimension() {
        return dimension;
    }

    public Vec3 getPosition() {
        return position;
    }

    public UUID getTrackedEntityId() {
        return trackedEntityId;
    }

    public Entity getTrackedEntity() {
        return trackedEntity;
    }

    // =========================================================
    // SETTERS
    // =========================================================

    public void setParams(AttractionParams params) {
        this.params = params;
    }

    /**
     * Moves the source to a new position and breaks any entity tracking.
     */
    public void setPosition(Vec3 pos) {
        this.position = pos;
        this.trackedEntity = null;
        this.trackedEntityId = null;
    }

    /**
     * Makes this source follow the given entity's position each tick.
     */
    public void setTrackedEntity(Entity entity) {
        this.trackedEntity = entity;
        this.trackedEntityId = entity != null ? entity.getUUID() : null;
    }

    /**
     * Late binding: sets the tracked entity reference from a UUID (after world reload).
     * Called by {@link SpatialForceManager} when the entity becomes available.
     */
    public void bindTrackedEntity(Entity entity) {
        if (entity != null && trackedEntityId != null && entity.getUUID().equals(trackedEntityId)) {
            this.trackedEntity = entity;
        }
    }

    // =========================================================
    // NBT SERIALIZATION
    // =========================================================

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("id", id);
        tag.put("params", params.serializeNBT());
        tag.putString("dimension", dimension.location().toString());
        tag.putDouble("posX", position.x);
        tag.putDouble("posY", position.y);
        tag.putDouble("posZ", position.z);
        if (trackedEntityId != null) {
            tag.putUUID("trackedEntityId", trackedEntityId);
        }
        return tag;
    }

    public static AttractionSource deserializeNBT(CompoundTag tag) {
        int id = tag.getInt("id");
        AttractionParams params = AttractionParams.deserializeNBT(tag.getCompound("params"));
        ResourceLocation dimLoc = ResourceLocation.parse(tag.getString("dimension"));
        ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, dimLoc);
        Vec3 position = new Vec3(tag.getDouble("posX"), tag.getDouble("posY"), tag.getDouble("posZ"));

        AttractionSource source = new AttractionSource(id, params, dimension, position);
        if (tag.hasUUID("trackedEntityId")) {
            source.trackedEntityId = tag.getUUID("trackedEntityId");
        }
        return source;
    }
}