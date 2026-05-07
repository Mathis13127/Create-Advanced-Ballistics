package com.pixel.create_ordnance.compat.vista;

import net.minecraft.world.phys.Vec3;

/**
 * Marker interface for virtual ViewFinderBlockEntities that support
 * sub-block-precision camera positioning.
 * <p>
 * Vista's default rendering pipeline uses {@code tile.getBlockPos().getCenter()}
 * which snaps to integer block coordinates. Implementations of this interface
 * provide a precise floating-point position that the
 * {@link com.pixel.create_ordnance.mixin.compat.vista.VistaLevelRendererMixin}
 * uses to override the camera position after Vista's normal setup.
 */
public interface IPrecisePositionCamera {

    /**
     * @return The precise world-space camera position (sub-block precision),
     *         or null if the default block-center position should be used.
     */
    Vec3 getPrecisePosition();
}
