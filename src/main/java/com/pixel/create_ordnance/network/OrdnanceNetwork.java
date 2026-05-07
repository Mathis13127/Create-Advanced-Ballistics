package com.pixel.create_ordnance.network;

import com.pixel.create_ordnance.CreateOrdnance;
import com.pixel.create_ordnance.content.debug.OrdnanceClientDebugRenderer;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.phys.Vec3;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Registers all custom {@link net.minecraft.network.protocol.common.custom.CustomPacketPayload}
 * types used by CreateOrdnance.
 *
 * <p>Called once during mod initialization via
 * {@link RegisterPayloadHandlersEvent}.
 * Both server-bound and client-bound packets are registered here with their
 * respective {@link StreamCodec StreamCodec} and
 * handler methods.</p>
 */
public class OrdnanceNetwork {

        public static final StreamCodec<FriendlyByteBuf, Vec3> VEC3_STREAM_CODEC = StreamCodec
                        .of(
                                        (buf, vec) -> {
                                                buf.writeDouble(vec.x);
                                                buf.writeDouble(vec.y);
                                                buf.writeDouble(vec.z);
                                        },
                                        buf -> new Vec3(buf.readDouble(), buf.readDouble(),
                                                        buf.readDouble()));

        @SubscribeEvent
        public static void register(RegisterPayloadHandlersEvent event) {
                final PayloadRegistrar registrar = event.registrar(CreateOrdnance.MODID)
                                .versioned("1.0.0");

                // ClientboundProjectileSyncPacket (Server -> Client)
                registrar.playToClient(
                                ClientboundProjectileSyncPacket.TYPE,
                                ClientboundProjectileSyncPacket.STREAM_CODEC,
                                ClientboundProjectileSyncPacket::handle);

                // ClientboundScrapperDebugPacket (Server -> Client)
                registrar.playToClient(
                                ClientboundScrapperDebugPacket.TYPE,
                                ClientboundScrapperDebugPacket.STREAM_CODEC,
                                (packet, context) -> {
                                        // Enqueue work on the client thread
                                        context.enqueueWork(() -> {
                                                OrdnanceClientDebugRenderer
                                                                .setDebugBox(packet.min(), packet.max(),
                                                                                packet.duration(), packet.scanPos(),
                                                                                packet.centerOfMass(),
                                                                                packet.centerOfPressure());
                                        });
                                });

                // ClientboundAttractionSyncPacket (Server -> Client)
                registrar.playToClient(
                                ClientboundAttractionSyncPacket.TYPE,
                                ClientboundAttractionSyncPacket.STREAM_CODEC,
                                ClientboundAttractionSyncPacket::handle);

                // ServerboundRequestScanPacket (Client -> Server)
                registrar.playToServer(
                                ServerboundRequestScanPacket.TYPE,
                                ServerboundRequestScanPacket.STREAM_CODEC,
                                ServerboundRequestScanPacket::handle);

                // ServerboundToolModePacket (Client -> Server)
                registrar.playToServer(
                                ServerboundToolModePacket.TYPE,
                                ServerboundToolModePacket.STREAM_CODEC,
                                ServerboundToolModePacket::handle);

        }
}