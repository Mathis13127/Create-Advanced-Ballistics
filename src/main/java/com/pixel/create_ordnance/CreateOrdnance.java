package com.pixel.create_ordnance;

import com.mojang.logging.LogUtils;

import com.pixel.create_ordnance.compat.Mods;
import com.pixel.create_ordnance.config.OrdnanceConfigs;
import com.pixel.create_ordnance.content.debug.DebugComponentRegistration;
import com.pixel.create_ordnance.foundation.events.OrdnanceClientEvents;
import com.pixel.create_ordnance.mechanics.physics.core.PhysicsEngine;
import com.pixel.create_ordnance.network.OrdnanceNetwork;
import com.pixel.create_ordnance.registry.ModBlockEntities;
import com.pixel.create_ordnance.registry.ModBlocks;
import com.pixel.create_ordnance.registry.ModContraptionTypes;
import com.pixel.create_ordnance.registry.ModCreativeTabs;
import com.pixel.create_ordnance.registry.ModEntities;
import com.pixel.create_ordnance.registry.ModItems;
import com.pixel.create_ordnance.registry.ProjectileScrapperRegistry;

import com.simibubi.create.foundation.data.CreateRegistrate;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;

import net.neoforged.api.distmarker.Dist;

import net.neoforged.bus.api.IEventBus;

import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;

import org.slf4j.Logger;

@Mod(CreateOrdnance.MODID)
public class CreateOrdnance {
    public static final String MODID = "create_ordnance";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final CreateRegistrate REGISTRATE = CreateRegistrate.create(MODID)
            .defaultCreativeTab((ResourceKey<CreativeModeTab>) null);

    public CreateOrdnance(IEventBus modEventBus) {

        ModCreativeTabs.register(modEventBus);

        REGISTRATE.registerEventListeners(modEventBus);
        modEventBus.addListener(this::commonSetup);

        // Register Config
        OrdnanceConfigs.register(ModLoadingContext.get());
        modEventBus.addListener(OrdnanceConfigs::onLoad);

        ModBlocks.register();
        ModBlockEntities.register();
        ModItems.register();
        ModEntities.register();
        ModContraptionTypes.register(modEventBus);

        // Manual MOD bus registrations to avoid @EventBusSubscriber issues with Bus.MOD
        modEventBus.addListener(OrdnanceNetwork::register);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.addListener(OrdnanceClientEvents::onLoadComplete);
            modEventBus.addListener(OrdnanceClientEvents::onConfigReload);
            modEventBus.addListener(
                    OrdnanceClientEvents::onRegisterGuiLayers);
            modEventBus.addListener(
                    OrdnanceClientEvents::onRegisterLayerDefinitions);
        }
    }

    public static ResourceLocation asResource(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            DebugComponentRegistration.register();
            ProjectileScrapperRegistry.init();
            // Register all default physics modules (Gravity, Drag, Lift, Thrust, etc.)
            PhysicsEngine.registerDefaultModules();

            // Optional mod integrations (no-op if mod is absent)
            Mods.VISTA.executeIfInstalled(() -> com.pixel.create_ordnance.compat.vista.VistaCompat::init);
        });
    }
}