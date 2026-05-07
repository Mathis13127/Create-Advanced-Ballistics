package com.pixel.create_ordnance.registry;

import java.util.function.Supplier;

import com.pixel.create_ordnance.CreateOrdnance;
import com.pixel.create_ordnance.content.contraption.ProjectileContraption;

import com.simibubi.create.api.contraption.ContraptionType;
import com.simibubi.create.api.registry.CreateRegistries;
import com.simibubi.create.content.contraptions.Contraption;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registers custom Create contraption types used by CreateOrdnance.
 *
 * <p>Defines the {@code PROJECTILE} contraption type backed by
 * {@link ProjectileContraption},
 * which handles block collection and assembly for in-flight projectile entities.</p>
 */
public class ModContraptionTypes {

    private static final DeferredRegister<ContraptionType> CONTRAPTION_TYPES = DeferredRegister
            .create(CreateRegistries.CONTRAPTION_TYPE, CreateOrdnance.MODID);

    public static final DeferredHolder<ContraptionType, ContraptionType> PROJECTILE = register("projectile",
            ProjectileContraption::new);

    private static DeferredHolder<ContraptionType, ContraptionType> register(String name,
            Supplier<? extends Contraption> factory) {
        return CONTRAPTION_TYPES.register(name, () -> new ContraptionType(factory));
    }

    public static void register(IEventBus modEventBus) {
        CONTRAPTION_TYPES.register(modEventBus);
    }
}