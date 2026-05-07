package com.pixel.create_ordnance.registry;

import com.pixel.create_ordnance.CreateOrdnance;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registers the Creative-mode tab for Create: Ordnance.
 *
 * <p>Populates the tab with all registered debug blocks.
 * The Ordnance Debugger item is placed in Minecraft's Operator Items tab.</p>
 */
public class ModCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> REGISTER = DeferredRegister
            .create(Registries.CREATIVE_MODE_TAB, CreateOrdnance.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN_TAB = REGISTER.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.create_ordnance"))
                    .icon(() -> new ItemStack(ModBlocks.KINETIC_TURRET.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModBlocks.KINETIC_TURRET.get());
                    })
                    .build());

    public static void register(IEventBus modEventBus) {
        REGISTER.register(modEventBus);
        modEventBus.addListener(ModCreativeTabs::addToOperatorTab);
    }

    /**
     * Adds the Ordnance Debugger to Minecraft's Operator Items tab.
     * This tab is only visible to players with operator privileges.
     */
    @SubscribeEvent
    public static void addToOperatorTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.OP_BLOCKS) {
            event.accept(ModItems.ORDNANCE_DEBUGGER.get());
            event.accept(ModBlocks.DEBUG_TAIL.get());
            event.accept(ModBlocks.DEBUG_PROPELLER_TAIL.get());
            event.accept(ModBlocks.DEBUG_FUEL_TANK.get());
            event.accept(ModBlocks.DEBUG_PAYLOAD.get());
            event.accept(ModBlocks.DEBUG_OPTIONAL_MODULE.get());
            event.accept(ModBlocks.DEBUG_BALLAST.get());
            event.accept(ModBlocks.DEBUG_FINS.get());
        }
    }
}
