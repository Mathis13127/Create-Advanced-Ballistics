package com.pixel.create_ordnance.mechanics.scanning;

import javax.annotation.Nullable;

import com.pixel.create_ordnance.foundation.utility.StateHelper;
import com.pixel.create_ordnance.registry.ProjectileComponentRegistry;
import com.pixel.create_ordnance.registry.ProjectileData;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public record ScrappedComponent(
                ProjectileData data,
                BlockPos relativePos,
                @Nullable Object source,
                BlockState state) {

        public static final StreamCodec<RegistryFriendlyByteBuf, ScrappedComponent> STREAM_CODEC = StreamCodec
                        .of(
                                        (buf, val) -> {
                                                buf.writeBlockPos(val.relativePos());
                                                ByteBufCodecs.idMapper(Block.BLOCK_STATE_REGISTRY).encode(buf,
                                                                val.state());
                                        },
                                        buf -> {
                                                BlockPos pos = buf.readBlockPos();
                                                BlockState s = ByteBufCodecs.idMapper(Block.BLOCK_STATE_REGISTRY)
                                                                .decode(buf);
                                                ProjectileData d = ProjectileComponentRegistry.getData(s.getBlock())
                                                                .orElse(null);
                                                return new ScrappedComponent(d, pos, null, s);
                                        });

        /**
         * Calculates the absolute world position of this component's center.
         * 
         * @param scanPos The projectile's geometric center (Scan Pos).
         * @param yaw     The projectile's Yaw rotation.
         * @param pitch   The projectile's Pitch rotation.
         * @return The absolute world position of this component's center.
         */
        public Vec3 getAbsolutePos(Vec3 scanPos, float yaw, float pitch) {
                if (source instanceof BlockPos worldPos) {
                        return new Vec3(worldPos.getX() + 0.5, worldPos.getY() + 0.5, worldPos.getZ() + 0.5);
                }

                // Fallback: Denormalize relativePos (UP) to World Frame
                Direction forward = StateHelper.getDirectionFromRotation(pitch, yaw);
                BlockPos denormalized = StateHelper.rotatePosFromUp(relativePos, forward);

                // Assuming relativePos is relative to center if source is missing?
                // Currently scanPos is Center. If relativePos was 0 (Anchor), then we are at
                // Anchor.
                // We add denormalized offset.
                return scanPos.add(denormalized.getX(), denormalized.getY(), denormalized.getZ());
        }
}