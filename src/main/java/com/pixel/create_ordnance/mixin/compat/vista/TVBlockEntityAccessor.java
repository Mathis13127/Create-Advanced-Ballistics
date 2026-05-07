package com.pixel.create_ordnance.mixin.compat.vista;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.mehvahdjukaar.vista.client.video_source.IVideoSource;
import net.mehvahdjukaar.vista.common.tv.TVBlockEntity;

/**
 * Mixin accessor for Vista's TVBlockEntity {@code videoSource} field.
 * <p>
 * In Vista 3.x, the {@code videoSource} field is cached — it is only set
 * in {@code updateClientVisualsOnLoad()} when the block entity syncs from
 * server. Modifying the cassette item's DataComponent client-side does NOT
 * trigger a re-read, so we must directly override this field to inject our
 * virtual camera feed.
 * <p>
 * {@code remap = false} because Vista is a mod class (not obfuscated).
 */
@Mixin(value = TVBlockEntity.class, remap = false)
public interface TVBlockEntityAccessor {

    @Accessor("videoSource")
    IVideoSource ordnance$getVideoSource();

    @Accessor("videoSource")
    void ordnance$setVideoSource(IVideoSource source);
}
