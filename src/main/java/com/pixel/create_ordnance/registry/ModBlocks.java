package com.pixel.create_ordnance.registry;

import com.pixel.create_ordnance.CreateOrdnance;
import com.pixel.create_ordnance.content.blocks.DebugBallastBlock;
import com.pixel.create_ordnance.content.blocks.DebugFinsBlock;
import com.pixel.create_ordnance.content.blocks.DebugFuelTankBlock;
import com.pixel.create_ordnance.content.blocks.DebugOptionalModuleBlock;
import com.pixel.create_ordnance.content.blocks.DebugPayloadBlock;
import com.pixel.create_ordnance.content.blocks.DebugPropellerTailBlock;
import com.pixel.create_ordnance.content.blocks.DebugTailBlock;
import com.pixel.create_ordnance.content.turret.KineticTurretBlock;
import com.pixel.create_ordnance.content.turret.KineticTurretBlockItem;

import com.simibubi.create.foundation.data.CreateRegistrate;

import com.tterrag.registrate.util.entry.BlockEntry;

import net.minecraft.world.level.block.Blocks;

/**
 * Deferred block registration for CreateOrdnance.
 *
 * <p>Uses Create's {@link CreateRegistrate Registrate}
 * to register all 7 debug placeholder blocks. Each block is associated with a
 * {@link ProjectileComponentType} via {@link ProjectileComponentRegistry}.</p>
 *
 * @see ProjectileComponentRegistry
 */
public class ModBlocks {

        private static final CreateRegistrate REGISTRATE = CreateOrdnance.REGISTRATE;

        public static final BlockEntry<DebugTailBlock> DEBUG_TAIL = REGISTRATE
                        .block("debug_tail", DebugTailBlock::new)
                        .initialProperties(() -> Blocks.IRON_BLOCK)
                        .simpleItem()
                        .register();

        public static final BlockEntry<DebugPropellerTailBlock> DEBUG_PROPELLER_TAIL = REGISTRATE
                        .block("debug_propeller_tail", DebugPropellerTailBlock::new)
                        .initialProperties(() -> Blocks.IRON_BLOCK)
                        .simpleItem()
                        .register();

        public static final BlockEntry<DebugFuelTankBlock> DEBUG_FUEL_TANK = REGISTRATE
                        .block("debug_fuel_tank", DebugFuelTankBlock::new)
                        .initialProperties(() -> Blocks.IRON_BLOCK)
                        .simpleItem()
                        .register();

        public static final BlockEntry<DebugPayloadBlock> DEBUG_PAYLOAD = REGISTRATE
                        .block("debug_payload", DebugPayloadBlock::new)
                        .initialProperties(() -> Blocks.IRON_BLOCK)
                        .simpleItem()
                        .register();

        public static final BlockEntry<DebugOptionalModuleBlock> DEBUG_OPTIONAL_MODULE = REGISTRATE
                        .block("debug_optional_module", DebugOptionalModuleBlock::new)
                        .initialProperties(() -> Blocks.IRON_BLOCK)
                        .simpleItem()
                        .register();

        public static final BlockEntry<DebugBallastBlock> DEBUG_BALLAST = REGISTRATE
                        .block("debug_ballast", DebugBallastBlock::new)
                        .initialProperties(() -> Blocks.IRON_BLOCK)
                        .simpleItem()
                        .register();

        public static final BlockEntry<DebugFinsBlock> DEBUG_FINS = REGISTRATE
                        .block("debug_fins", DebugFinsBlock::new)
                        .initialProperties(() -> Blocks.IRON_BLOCK)
                        .simpleItem()
                        .register();

        // --- Kinetic Turret ---

        public static final BlockEntry<KineticTurretBlock> KINETIC_TURRET = REGISTRATE
                        .block("kinetic_turret", KineticTurretBlock::new)
                        .initialProperties(() -> Blocks.IRON_BLOCK)
                        .properties(p -> p.noOcclusion())
                        .item(KineticTurretBlockItem::new)
                        .build()
                        .register();

        public static void register() {
        }
}