package com.pixel.create_ordnance.mechanics.scanning;

import java.util.ArrayList;
import java.util.List;

import com.pixel.create_ordnance.network.OrdnanceNetwork;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import net.minecraft.resources.ResourceLocation;

import net.minecraft.world.phys.Vec3;

/**
 * Result of a raw projectile scan.
 * V3.5 Universal Protocol: Only contains a linear list of Block IDs.
 * V3.6: Includes optional world bounds (minPos/maxPos) for precise rendering.
 */
public record ScrapResult(
                List<ResourceLocation> blockIds,
                Vec3 worldOrigin, // Precision pos for physics
                BlockPos minPos, // Bound for rendering/AABB
                BlockPos maxPos, // Bound for rendering/AABB
                Direction worldFacing,
                float scale) {

        public ScrapResult{if(blockIds==null)throw new IllegalArgumentException("Bad argument: blockIds is null");if(worldOrigin==null)worldOrigin=Vec3.ZERO;if(worldFacing==null)worldFacing=Direction.UP;
        }

        /**
         * Creates a virtual result (no world bounds needed, debug renderer ignored)
         */
        public static ScrapResult virtual(List<ResourceLocation> ids, float scale) {
                return new ScrapResult(ids, Vec3.ZERO, null, null, Direction.UP, scale);
        }

        public static final StreamCodec<RegistryFriendlyByteBuf, ScrapResult> STREAM_CODEC = StreamCodec.of(
                        (buf, result) -> {
                                ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()).encode(buf,
                                                result.blockIds());
                                OrdnanceNetwork.VEC3_STREAM_CODEC.encode(buf, result.worldOrigin());

                                boolean hasBounds = result.minPos() != null;
                                buf.writeBoolean(hasBounds);
                                if (hasBounds) {
                                        buf.writeBlockPos(result.minPos());
                                        buf.writeBlockPos(result.maxPos());
                                }

                                buf.writeVarInt(result.worldFacing().ordinal());
                                buf.writeFloat(result.scale());
                        },
                        buf -> {
                                List<ResourceLocation> ids = ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list())
                                                .decode(buf);
                                Vec3 origin = OrdnanceNetwork.VEC3_STREAM_CODEC.decode(buf);

                                boolean hasBounds = buf.readBoolean();
                                BlockPos min = null;
                                BlockPos max = null;
                                if (hasBounds) {
                                        min = buf.readBlockPos();
                                        max = buf.readBlockPos();
                                }

                                Direction facing = Direction.values()[buf.readVarInt()];
                                float scale = buf.readFloat();
                                return new ScrapResult(ids, origin, min, max, facing, scale);
                        });

        public CompoundTag writeToNBT() {
                CompoundTag tag = new CompoundTag();
                ListTag list = new ListTag();
                for (ResourceLocation id : blockIds) {
                        list.add(StringTag.valueOf(id.toString()));
                }
                tag.put("blockIds", list);
                tag.putDouble("originX", worldOrigin.x);
                tag.putDouble("originY", worldOrigin.y);
                tag.putDouble("originZ", worldOrigin.z);

                if (minPos != null && maxPos != null) {
                        tag.putLong("minPos", minPos.asLong());
                        tag.putLong("maxPos", maxPos.asLong());
                }

                tag.putInt("facing", worldFacing.ordinal());
                tag.putFloat("scale", scale);
                return tag;
        }

        public static ScrapResult readFromNBT(CompoundTag tag) {
                if (tag == null || !tag.contains("blockIds")) {
                        throw new IllegalStateException("[Ordnance] ScrapResult NBT is missing blockIds!");
                }

                List<ResourceLocation> ids = new ArrayList<>();
                ListTag list = tag.getList("blockIds", 8);
                for (int i = 0; i < list.size(); i++) {
                        ids.add(ResourceLocation.parse(list.getString(i)));
                }

                Vec3 origin = new Vec3(tag.getDouble("originX"), tag.getDouble("originY"), tag.getDouble("originZ"));

                BlockPos min = tag.contains("minPos") ? BlockPos.of(tag.getLong("minPos")) : null;
                BlockPos max = tag.contains("maxPos") ? BlockPos.of(tag.getLong("maxPos")) : null;

                Direction facing = Direction.values()[tag.getInt("facing")];
                float scale = tag.getFloat("scale");

                return new ScrapResult(ids, origin, min, max, facing, scale);
        }
}