package com.pixel.create_ordnance.foundation.events;

import com.pixel.create_ordnance.CreateOrdnance;
import com.pixel.create_ordnance.api.events.ProjectileConfigReloadEvent;
import com.pixel.create_ordnance.content.items.OrdnanceDebuggerItem;
import com.pixel.create_ordnance.mechanics.scanning.CurrentProjectileManager;
import com.pixel.create_ordnance.registry.ModCreativeTabs;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@EventBusSubscriber(modid = CreateOrdnance.MODID, value = Dist.CLIENT)
public class OrdnanceClientGameEvents {

    @SubscribeEvent
    public static void onApiConfigReload(ProjectileConfigReloadEvent event) {
        if (!event.getConfig().getModId().equals(CreateOrdnance.MODID))
            return;

        if (event.isClient()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null) {
                mc.execute(() -> {
                    if (mc.player == null)
                        return;

                    // Construct parameters manually
                    CreativeModeTab.ItemDisplayParameters params = new CreativeModeTab.ItemDisplayParameters(
                            mc.player.connection.enabledFeatures(),
                            mc.options.operatorItemsTab().get(),
                            mc.player.level().registryAccess());

                    // Rebuild ALL tabs to pick up config changes
                    CreativeModeTabs.tabs().forEach(tab -> tab.buildContents(params));

                    // Explicitly rebuild our tab
                    try {
                        CreativeModeTab myTab = ModCreativeTabs.MAIN_TAB.get();
                        myTab.buildContents(params);
                    } catch (Exception ignored) {
                    }

                    // Refresh Projectile Overlay Stats
                    CurrentProjectileManager.reanalyzeAll();

                    // If in Creative Inventory, refresh the screen
                    if (mc.screen instanceof net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen creativeScreen) {
                        creativeScreen.resize(mc, mc.getWindow().getGuiScaledWidth(),
                                mc.getWindow().getGuiScaledHeight());
                    }
                });
            }
        }
    }

    @SubscribeEvent
    public static void onRenderGuiLayer(RenderGuiLayerEvent.Pre event) {
        if (event.getName().equals(VanillaGuiLayers.SELECTED_ITEM_NAME)) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.player.getMainHandItem()
                    .getItem() instanceof OrdnanceDebuggerItem) {
                event.setCanceled(true);
            }
        }
    }
}
