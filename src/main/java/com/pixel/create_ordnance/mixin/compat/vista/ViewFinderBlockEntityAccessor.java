package com.pixel.create_ordnance.mixin.compat.vista;

import java.util.UUID;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.mehvahdjukaar.vista.common.view_finder.ViewFinderBlockEntity;

/**
 * Mixin accessor for Vista's ViewFinderBlockEntity private fields.
 * Used by ProjectileCameraSource to configure a virtual camera
 * with the projectile's position and orientation.
 * <p>
 * {@code remap = false} because Vista is a mod class (not obfuscated).
 */
@Mixin(value = ViewFinderBlockEntity.class, remap = false)
public interface ViewFinderBlockEntityAccessor {

    @Accessor("pitch")
    float ordnance$getPitch();

    @Accessor("pitch")
    void ordnance$setPitch(float pitch);

    @Accessor("prevPitch")
    void ordnance$setPrevPitch(float prevPitch);

    @Accessor("yaw")
    float ordnance$getYaw();

    @Accessor("yaw")
    void ordnance$setYaw(float yaw);

    @Accessor("prevYaw")
    void ordnance$setPrevYaw(float prevYaw);

    @Accessor("myUUID")
    void ordnance$setMyUUID(UUID uuid);
}
