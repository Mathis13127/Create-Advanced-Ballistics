package com.pixel.create_ordnance.network;

import com.pixel.create_ordnance.CreateOrdnance;

import net.minecraft.core.BlockPos;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.minecraft.resources.ResourceLocation;

import net.minecraft.world.phys.Vec3;

/**
 * Server→Client packet carrying debug visualization data after a projectile scan.
 *
 * <p>Contains the bounding box ({@code min}/{@code max}), scan origin,
 * center of mass, and center of pressure — rendered as overlay boxes and
 * markers by
 * {@link com.pixel.create_ordnance.content.debug.OrdnanceClientDebugRenderer}.</p>
 */
public record ClientboundScrapperDebugPacket(
                BlockPos min,
                BlockPos max,
                int duration,
                Vec3 scanPos,
                Vec3 centerOfMass,
                Vec3 centerOfPressure) implements CustomPacketPayload { // V13

        public static final Type<ClientboundScrapperDebugPacket> TYPE = new Type<>(
                        ResourceLocation.fromNamespaceAndPath(CreateOrdnance.MODID, "scrapper_debug"));

        public static final StreamCodec<FriendlyByteBuf, ClientboundScrapperDebugPacket> STREAM_CODEC = StreamCodec
                        .composite(
                                        BlockPos.STREAM_CODEC, ClientboundScrapperDebugPacket::min,
                                        BlockPos.STREAM_CODEC, ClientboundScrapperDebugPacket::max,
                                        ByteBufCodecs.VAR_INT, ClientboundScrapperDebugPacket::duration,
                                        OrdnanceNetwork.VEC3_STREAM_CODEC, ClientboundScrapperDebugPacket::scanPos,
                                        OrdnanceNetwork.VEC3_STREAM_CODEC, ClientboundScrapperDebugPacket::centerOfMass,
                                        OrdnanceNetwork.VEC3_STREAM_CODEC,
                                        ClientboundScrapperDebugPacket::centerOfPressure, // V13
                                        ClientboundScrapperDebugPacket::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
                return TYPE;
        }
}