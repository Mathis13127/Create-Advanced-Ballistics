package com.pixel.create_ordnance.network;

import com.pixel.create_ordnance.CreateOrdnance;
import com.pixel.create_ordnance.mechanics.scanning.AutoScanHandler;

import io.netty.buffer.ByteBuf;

import net.minecraft.core.BlockPos;

import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.minecraft.resources.ResourceLocation;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client→Server packet requesting an immediate projectile scan at a
 * specific block position.
 *
 * <p>Triggered when the player uses the debug tool in scan mode.
 * The server delegates to
 * {@link AutoScanHandler}
 * and replies with a {@link ClientboundScrapperDebugPacket}.</p>
 */
public record ServerboundRequestScanPacket(BlockPos pos, ResourceLocation scrapperId) implements CustomPacketPayload {

    public static final Type<ServerboundRequestScanPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CreateOrdnance.MODID, "request_scan"));

    public static final StreamCodec<ByteBuf, ServerboundRequestScanPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            ServerboundRequestScanPacket::pos,
            ResourceLocation.STREAM_CODEC,
            ServerboundRequestScanPacket::scrapperId,
            ServerboundRequestScanPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ServerboundRequestScanPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                if (player.level() instanceof ServerLevel level) {
                    AutoScanHandler.performInstantScan(level, packet.pos(), player, packet.scrapperId());
                }
            }
        });
    }
}