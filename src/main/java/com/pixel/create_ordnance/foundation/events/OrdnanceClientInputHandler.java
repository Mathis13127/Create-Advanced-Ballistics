package com.pixel.create_ordnance.foundation.events;

import java.util.List;

import com.pixel.create_ordnance.CreateOrdnance;
import com.pixel.create_ordnance.content.items.OrdnanceDebuggerItem;
import com.pixel.create_ordnance.content.items.TransformerMode;
import com.pixel.create_ordnance.foundation.gui.OrdnanceToolSelectionScreen;
import com.pixel.create_ordnance.network.ServerboundToolModePacket;

import com.simibubi.create.AllKeys;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

import net.neoforged.api.distmarker.Dist;

import net.neoforged.bus.api.SubscribeEvent;

import net.neoforged.fml.common.EventBusSubscriber;

import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = CreateOrdnance.MODID, value = Dist.CLIENT)
public class OrdnanceClientInputHandler {

    private static OrdnanceToolSelectionScreen selectionScreen;
    private static int passiveRenderTimer;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null)
            return;

        if (selectionScreen != null) {
            selectionScreen.update();
            if (passiveRenderTimer > 0)
                passiveRenderTimer--;
        }

        boolean keyHeld = AllKeys.TOOL_MENU.isPressed();
        ItemStack stack = mc.player.getMainHandItem();
        boolean hasItem = stack.getItem() instanceof OrdnanceDebuggerItem;

        // Initialize if missing
        if (hasItem && selectionScreen == null) {
            setupScreen(mc);
        }

        if (selectionScreen != null) {
            // Handle Focus State locally (no setScreen)
            if (hasItem && keyHeld) {
                if (!selectionScreen.focused) {
                    selectionScreen.focused = true;
                }
            } else {
                if (selectionScreen.focused) {
                    selectionScreen.focused = false;
                    selectionScreen.onClose(); // Trigger callback
                }
            }
        }
    }

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        if (selectionScreen != null && selectionScreen.focused) {
            // Inverted Scroll Direction
            selectionScreen.cycle((int) (Math.signum(event.getScrollDeltaY())));

            // Sync immediately to NBT so renderPassive doesn't override it next frame
            TransformerMode newMode = (TransformerMode) selectionScreen.getSelectedMode();
            ItemStack stack = Minecraft.getInstance().player.getMainHandItem();
            if (stack.getItem() instanceof OrdnanceDebuggerItem) {
                OrdnanceDebuggerItem.setMode(stack, newMode);

                // Sync to Server
                PacketDistributor.sendToServer(
                        new ServerboundToolModePacket(newMode));
            }

            event.setCanceled(true); // Consume event
        }
    }

    private static void setupScreen(Minecraft mc) {
        selectionScreen = new OrdnanceToolSelectionScreen(
                List.of(TransformerMode.values()),
                (mode) -> {
                    ItemStack currentStack = Minecraft.getInstance().player.getMainHandItem();
                    if (currentStack.getItem() instanceof OrdnanceDebuggerItem) {
                        OrdnanceDebuggerItem.setMode(currentStack, (TransformerMode) mode);
                    }
                });
    }

    public static void renderPassive(GuiGraphics graphics, float partialTicks) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null)
            return;
        ItemStack stack = mc.player.getMainHandItem();

        if (!(stack.getItem() instanceof OrdnanceDebuggerItem))
            return;

        if (selectionScreen == null) {
            setupScreen(mc);
        }

        TransformerMode current = OrdnanceDebuggerItem.getMode(stack);
        selectionScreen.setSelectedElement(current);

        // Always render passive - the screen handles the "focused" visualization
        // internally
        selectionScreen.renderPassive(graphics, partialTicks);
    }
}