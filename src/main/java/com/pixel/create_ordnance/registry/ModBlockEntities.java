package com.pixel.create_ordnance.registry;

import com.pixel.create_ordnance.CreateOrdnance;
import com.pixel.create_ordnance.content.turret.KineticTurretBlockEntity;
import com.pixel.create_ordnance.content.turret.KineticTurretRenderer;

import com.simibubi.create.foundation.data.CreateRegistrate;

import com.tterrag.registrate.util.entry.BlockEntityEntry;

/**
 * Deferred block entity registration for Create: Ordnance.
 *
 * <p>Uses Create's {@link CreateRegistrate Registrate}
 * to register block entity types and bind their renderers.</p>
 */
public class ModBlockEntities {

    private static final CreateRegistrate REGISTRATE = CreateOrdnance.REGISTRATE;

    public static final BlockEntityEntry<KineticTurretBlockEntity> KINETIC_TURRET = REGISTRATE
            .blockEntity("kinetic_turret", KineticTurretBlockEntity::new)
            .validBlocks(ModBlocks.KINETIC_TURRET)
            .renderer(() -> KineticTurretRenderer::new)
            .register();

    public static void register() {
    }
}
