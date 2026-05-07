package com.pixel.create_ordnance.mixin.compat.vista;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;

/**
 * Mixin invoker for vanilla Camera's protected {@code setPosition()} method.
 * <p>
 * Used by {@link VistaLevelRendererMixin} to override the camera position
 * with sub-block precision after Vista's default setup.
 */
@Mixin(Camera.class)
public interface CameraInvoker {

    @Invoker("setPosition")
    void ordnance$setPosition(Vec3 pos);
}
