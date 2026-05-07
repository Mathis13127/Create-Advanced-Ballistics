package com.pixel.create_ordnance.network;

import com.pixel.create_ordnance.mechanics.scanning.CurrentProjectileManager;
import com.pixel.create_ordnance.mechanics.scanning.ScrapResult;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server→Client packet synchronizing the
 * {@link ScrapResult ScrapResult}
 * of a scanned projectile structure.
 *
 * <p>Allows the client to reconstruct the projectile's component list
 * for UI display and debug overlays.</p>
 *
 * @see CurrentProjectileManager
 */
public record ClientboundProjectileSyncPacket(ScrapResult result) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ClientboundProjectileSyncPacket> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath("create_ordnance", "sync_projectile"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundProjectileSyncPacket> STREAM_CODEC = StreamCodec
            .composite(
                    ScrapResult.STREAM_CODEC,
                    ClientboundProjectileSyncPacket::result,
                    ClientboundProjectileSyncPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ClientboundProjectileSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            // Client-side execution
            CurrentProjectileManager.updateResult(packet.result());
        });
    }
}