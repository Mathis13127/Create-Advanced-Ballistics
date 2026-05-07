package com.pixel.create_ordnance.registry;

import com.pixel.create_ordnance.CreateOrdnance;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Custom block and item tags used by CreateOrdnance.
 *
 * <p>Tags are defined under the {@code create_ordnance} namespace and can be
 * overridden by datapacks or addon mods.</p>
 */
public class ModTags {
    public static class Blocks {
        /**
         * Blocks in this tag cost only 1.0 base kinetic power to penetrate,
         * instead of the standard (Hardness + 15.0).
         */
        public static final TagKey<Block> CHEAP_PENETRATION = tag("cheap_penetration");

        private static TagKey<Block> tag(String name) {
            return TagKey.create(Registries.BLOCK, CreateOrdnance.asResource(name));
        }
    }
}