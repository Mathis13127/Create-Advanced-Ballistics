package com.pixel.create_ordnance.mixin.compat.vista;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.pixel.create_ordnance.compat.vista.IPrecisePositionCamera;

import net.mehvahdjukaar.vista.client.renderer.VistaLevelRenderer;
import net.mehvahdjukaar.vista.common.view_finder.ViewFinderBlockEntity;

import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;

/**
 * Mixin into Vista's VistaLevelRenderer to override camera position
 * with sub-block precision for projectile cameras.
 * <p>
 * Vista's default {@code setupSceneCamera()} uses
 * {@code tile.getBlockPos().getCenter()} which snaps to integer block
 * coordinates, causing the camera to "stutter" as it moves across blocks.
 * <p>
 * This mixin injects at the RETURN of {@code setupSceneCamera()} and,
 * if the tile implements {@link IPrecisePositionCamera}, overrides both
 * the dummy entity position and the camera position with the precise Vec3.
 */
@Mixin(value = VistaLevelRenderer.class, remap = false)
public class VistaLevelRendererMixin {

    @Inject(method = "setupSceneCamera", at = @At("RETURN"), remap = false)
    private static void ordnance$overridePrecisePosition(
            ViewFinderBlockEntity tile, Camera dummyCamera, float partialTicks,
            CallbackInfo ci) {

        if (tile instanceof IPrecisePositionCamera cam) {
            Vec3 precisePos = cam.getPrecisePosition();
            if (precisePos != null) {
                // Override the entity position (Vista sets this from getBlockPos().getCenter())
                dummyCamera.getEntity().setPos(precisePos);
                // Override the camera position used for rendering
                // Camera.setPosition() is protected — use mixin invoker
                ((CameraInvoker) dummyCamera).ordnance$setPosition(precisePos);
            }
        }
    }
}
