package com.pixel.create_ordnance.mixin.compat.vista;

import java.util.UUID;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.pixel.create_ordnance.compat.vista.VistaCompat;

import net.mehvahdjukaar.vista.common.BroadcastManager;
import net.mehvahdjukaar.vista.common.cassette.IBroadcastProvider;

/**
 * Mixin into Vista's BroadcastManager to intercept camera UUID resolution.
 * <p>
 * When a TV requests a live feed texture and the UUID corresponds to a
 * projectile virtual camera (not a real ViewFinder block), this mixin returns
 * our virtual ViewFinderBlockEntity instead of null.
 * <p>
 * This is the critical bridge between Ordnance's projectile cameras and
 * Vista's rendering pipeline.
 * <p>
 * NOTE: This file is named LiveFeedConnectionManagerMixin for historical
 * reasons (Vista 2.x class name). It targets Vista 3.x's BroadcastManager.
 */
@Mixin(value = BroadcastManager.class, remap = false)
public class LiveFeedConnectionManagerMixin {

    /**
     * Inject at the RETURN of getBroadcast to provide virtual cameras
     * when the normal Vista lookup returns null.
     */
    @Inject(method = "getBroadcast", at = @At("RETURN"), cancellable = true, remap = false)
    private void ordnance$resolveProjectileCamera(
            UUID feedId, boolean clientSide,
            CallbackInfoReturnable<IBroadcastProvider> cir) {

        // Only intercept if Vista's normal lookup failed
        if (cir.getReturnValue() == null && feedId != null) {
            IBroadcastProvider virtualBE = VistaCompat.resolveProjectileCamera(feedId);
            if (virtualBE != null) {
                cir.setReturnValue(virtualBE);
            }
        }
    }
}
