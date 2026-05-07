package com.pixel.create_ordnance.network;

import java.util.List;

import com.pixel.create_ordnance.CreateOrdnance;
import com.pixel.create_ordnance.content.debug.OrdnanceClientDebugRenderer;
import com.pixel.create_ordnance.network.ClientboundAttractionSyncPacket.AttractionData;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.minecraft.resources.ResourceLocation;

import net.minecraft.world.phys.Vec3;

import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server→Client packet synchronizing all active
 * {@link com.pixel.create_ordnance.mechanics.physics.attraction.AttractionSource AttractionSource}
 * positions for debug rendering.
 *
 * <p>Sent periodically by the server so the client can draw attraction-field
 * gizmos via
 * {@link OrdnanceClientDebugRenderer}.</p>
 *
 * @see com.pixel.create_ordnance.api.physics.SpatialForceAPI
 */
public record ClientboundAttractionSyncPacket(List<AttractionData> sources) implements CustomPacketPayload {

    public static final Type<ClientboundAttractionSyncPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CreateOrdnance.MODID, "attraction_sync_v2"));

    public record AttractionData(int id, Vec3 pos, double range) {
        public static final StreamCodec<FriendlyByteBuf, AttractionData> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, AttractionData::id,
                OrdnanceNetwork.VEC3_STREAM_CODEC, AttractionData::pos,
                ByteBufCodecs.DOUBLE, AttractionData::range,
                AttractionData::new
        );
    }

    public static final StreamCodec<FriendlyByteBuf, ClientboundAttractionSyncPacket> STREAM_CODEC = StreamCodec.composite(
            AttractionData.STREAM_CODEC.apply(ByteBufCodecs.list()), ClientboundAttractionSyncPacket::sources,
            ClientboundAttractionSyncPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(final ClientboundAttractionSyncPacket packet, final IPayloadContext context) {
        context.enqueueWork(() -> {
            OrdnanceClientDebugRenderer.receiveAttractionSync(packet.sources());
        });
    }
}