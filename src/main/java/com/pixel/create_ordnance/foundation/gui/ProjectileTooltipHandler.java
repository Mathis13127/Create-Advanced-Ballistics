package com.pixel.create_ordnance.foundation.gui;

import java.util.List;

import com.pixel.create_ordnance.CreateOrdnance;
import com.pixel.create_ordnance.registry.ProjectileComponentRegistry;

import net.minecraft.ChatFormatting;

import net.minecraft.client.gui.screens.Screen;

import net.minecraft.network.chat.Component;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@EventBusSubscriber(modid = CreateOrdnance.MODID, value = Dist.CLIENT)
public class ProjectileTooltipHandler {

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof BlockItem blockItem))
            return;

        Block block = blockItem.getBlock();
        ProjectileComponentRegistry.getData(block).ifPresent(data -> {
            List<Component> tooltip = event.getToolTip();

            if (!Screen.hasShiftDown()) {
                tooltip.add(Component.empty());
                tooltip.add(Component.translatable("tooltip.create_ordnance.hold_shift",
                        Component.literal("SHIFT").withStyle(ChatFormatting.YELLOW))
                        .withStyle(ChatFormatting.GRAY));
                return;
            }

            tooltip.add(Component.empty());
            tooltip.add(
                    Component.translatable("tooltip.create_ordnance.stats_header").withStyle(ChatFormatting.GOLD));

            // Dynamically show all properties marked for tooltips
            data.allProperties().forEach((name, prop) -> {
                if (prop.showInTooltip() && prop.supplier() != null) {
                    Object val = prop.supplier().get();
                    if (val != null) {
                        Component label = getLabelComponent(name);
                        ChatFormatting color = getColorForProp(name);
                        String unit = getUnitForProp(name);

                        Component valueComp = Component.literal(String.format(getFormat(val), val))
                                .append(Component.literal(unit))
                                .withStyle(ChatFormatting.WHITE);

                        tooltip.add(label.copy().append(": ").withStyle(color)
                                .append(valueComp));
                    }
                }
            });

            tooltip.add(Component.literal("-----------------------").withStyle(ChatFormatting.GOLD));
        });
    }

    private static Component getLabelComponent(String name) {
        return switch (name) {
            case "mass" -> Component.translatable("tooltip.create_ordnance.mass");
            case "stability" -> Component.translatable("tooltip.create_ordnance.stability");
            case "base_thrust" -> Component.translatable("tooltip.create_ordnance.base_thrust");
            case "boost_thrust" -> Component.translatable("tooltip.create_ordnance.boost_thrust");
            case "boost_duration" -> Component.translatable("tooltip.create_ordnance.boost_duration");
            case "max_speed" -> Component.translatable("tooltip.create_ordnance.max_speed");

            case "max_nozzle_tilt" -> Component.translatable("tooltip.create_ordnance.max_nozzle_tilt");
            case "fuel_cons" -> Component.translatable("tooltip.create_ordnance.fuel_cons");
            case "thrust_type" -> Component.translatable("tooltip.create_ordnance.thrust_type");
            case "capacity" -> Component.translatable("tooltip.create_ordnance.capacity");
            default -> {
                String readable = name.replace("_", " ");
                yield Component.literal(readable.substring(0, 1).toUpperCase() + readable.substring(1));
            }
        };
    }

    private static String getUnitForProp(String name) {
        return switch (name) {
            case "mass" -> " kg";
            case "base_thrust", "boost_thrust" -> " N";
            case "boost_duration" -> " ticks";
            case "max_speed" -> " m/s";
            case "max_nozzle_tilt" -> " deg";
            case "capacity" -> " kL";
            default -> "";
        };
    }

    private static ChatFormatting getColorForProp(String name) {
        return switch (name) {
            case "mass" -> ChatFormatting.BLUE;
            case "stability" -> ChatFormatting.DARK_PURPLE;
            case "base_thrust", "boost_thrust" -> ChatFormatting.YELLOW;
            case "max_speed" -> ChatFormatting.AQUA;
            case "fuel_cons", "capacity" -> ChatFormatting.RED;
            default -> ChatFormatting.GRAY;
        };
    }

    private static String getFormat(Object val) {
        if (val instanceof Double || val instanceof Float) {
            return "%.2f";
        }
        return "%s";
    }
}