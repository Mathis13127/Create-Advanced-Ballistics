package com.pixel.create_ordnance.foundation.events;

import com.pixel.create_ordnance.CreateOrdnance;
import com.pixel.create_ordnance.api.events.ProjectileConfigReloadEvent;
import com.pixel.create_ordnance.content.turret.KineticTurretModel;
import com.pixel.create_ordnance.foundation.gui.GogglesProjectileOverlay;

import net.createmod.catnip.config.ui.BaseConfigScreen;

import net.minecraft.resources.ResourceLocation;

import net.neoforged.bus.api.SubscribeEvent;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;

import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

public class OrdnanceClientEvents {

    @SubscribeEvent
    public static void onLoadComplete(FMLLoadCompleteEvent event) {
        ModContainer container = ModList.get()
                .getModContainerById(CreateOrdnance.MODID)
                .orElse(null);

        if (container == null) {
            CreateOrdnance.LOGGER
                    .error("Ordnance mod container missing on LoadComplete. Config screen registration skipped.");
            return;
        }

        // Register the Create-style config screen
        container.registerExtensionPoint(IConfigScreenFactory.class,
                (mc, previousScreen) -> new BaseConfigScreen(previousScreen, CreateOrdnance.MODID));

        CreateOrdnance.LOGGER.info("Registered Create-style Config Screen for Ordnance");
    }
    @SubscribeEvent
    public static void onConfigReload(ModConfigEvent event) {
        if (event.getConfig().getModId().equals(CreateOrdnance.MODID)) {
            // FIRE API EVENT
            ProjectileConfigReloadEvent apiEvent = new ProjectileConfigReloadEvent(
                    event.getConfig());
            NeoForge.EVENT_BUS.post(apiEvent);
        }
    }

    @SubscribeEvent
    public static void onRegisterLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(KineticTurretModel.LAYER, KineticTurretModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(
                ResourceLocation.fromNamespaceAndPath(CreateOrdnance.MODID,
                        "tool_selection"),
                (graphics, deltaTracker) -> {
                    com.pixel.create_ordnance.foundation.events.OrdnanceClientInputHandler.renderPassive(graphics,
                            deltaTracker.getGameTimeDeltaPartialTick(false));
                });

        event.registerAboveAll(
                ResourceLocation.fromNamespaceAndPath(CreateOrdnance.MODID,
                        "projectile_overlay"),
                (graphics, deltaTracker) -> {
                    GogglesProjectileOverlay.renderOverlay(graphics);
                });
    }
}