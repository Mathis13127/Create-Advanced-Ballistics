package com.pixel.create_ordnance.mixin.compat.vista;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Mixin accessor for vanilla BlockEntity's {@code worldPosition} field.
 * <p>
 * Used by {@link com.pixel.create_ordnance.compat.vista.ProjectileCameraSource.VirtualViewFinderBE}
 * to dynamically update the virtual camera's position as the projectile moves.
 * <p>
 * {@code @Mutable} is required because {@code worldPosition} is {@code final}
 * in MC 1.21.1 — Mixin removes the final modifier at load time so the setter
 * accessor can work.
 */
@Mixin(BlockEntity.class)
public interface BlockEntityAccessor {

    @Mutable
    @Accessor("worldPosition")
    void ordnance$setWorldPosition(BlockPos pos);
}
