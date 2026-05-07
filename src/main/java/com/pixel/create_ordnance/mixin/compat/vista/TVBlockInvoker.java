package com.pixel.create_ordnance.mixin.compat.vista;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.mehvahdjukaar.vista.common.tv.TVBlock;
import net.mehvahdjukaar.vista.common.tv.TVBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Mixin invoker for Vista's TVBlock {@code getMasterBlockEntity()} method.
 * <p>
 * In a multi-block TV, the {@link TVBlockEntity} only exists at the
 * {@code BOTTOM_LEFT} (master) position. This invoker allows us to
 * navigate from any clicked position to the master block entity.
 * <p>
 * {@code remap = false} because Vista is a mod class (not obfuscated).
 */
@Mixin(value = TVBlock.class, remap = false)
public interface TVBlockInvoker {

    @Invoker("getMasterBlockEntity")
    TVBlockEntity ordnance$getMasterBlockEntity(Level level, BlockPos pos, BlockState state);
}
