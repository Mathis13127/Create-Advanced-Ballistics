package com.pixel.create_ordnance.mechanics.physics.attraction;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Persists attraction sources in the world folder.
 * <p>
 * Attached to the overworld so sources are shared across dimensions.
 * Uses NBT ListTag serialization matching the old format.
 */
public class SpatialForceSavedData extends SavedData {

    public static final String FILE_ID = "create_ordnance_spatial_forces";

    public final Map<Integer, AttractionSource> activeSources = new HashMap<>();
    public final AtomicInteger idGenerator = new AtomicInteger(1);

    public SpatialForceSavedData() {
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("next_id", idGenerator.get());

        ListTag list = new ListTag();
        for (AttractionSource source : activeSources.values()) {
            list.add(source.serializeNBT());
        }
        tag.put("sources", list);

        return tag;
    }

    public static SpatialForceSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        SpatialForceSavedData data = new SpatialForceSavedData();
        data.idGenerator.set(tag.getInt("next_id"));

        ListTag list = tag.getList("sources", 10); // 10 = TAG_COMPOUND
        for (int i = 0; i < list.size(); i++) {
            AttractionSource source = AttractionSource.deserializeNBT(list.getCompound(i));
            data.activeSources.put(source.getId(), source);
        }

        return data;
    }

    /**
     * Gets or creates the saved data from the overworld's data storage.
     */
    public static SpatialForceSavedData getOrCreate(ServerLevel overworld) {
        return overworld.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(SpatialForceSavedData::new, SpatialForceSavedData::load),
                FILE_ID);
    }
}
