package com.pixel.create_ordnance.network;

import com.pixel.create_ordnance.CreateOrdnance;
import com.pixel.create_ordnance.content.items.OrdnanceDebuggerItem;
import com.pixel.create_ordnance.content.items.TransformerMode;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client→Server packet sent when the player cycles the
 * {@link TransformerMode TransformerMode}
 * on their debug tool.
 *
 * <p>The server validates the mode and updates the held item's NBT accordingly.</p>
 *
 * @see OrdnanceDebuggerItem
 */
public record ServerboundToolModePacket(TransformerMode mode) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ServerboundToolModePacket> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(CreateOrdnance.MODID, "tool_mode"));

    public static final StreamCodec<ByteBuf, ServerboundToolModePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.idMapper(i -> TransformerMode.values()[i], TransformerMode::ordinal),
            ServerboundToolModePacket::mode,
            ServerboundToolModePacket::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ServerboundToolModePacket payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = context.player();
            ItemStack stack = player.getMainHandItem();
            if (stack.getItem() instanceof OrdnanceDebuggerItem) {
                OrdnanceDebuggerItem.setMode(stack, payload.mode());
            }
        });
    }
}