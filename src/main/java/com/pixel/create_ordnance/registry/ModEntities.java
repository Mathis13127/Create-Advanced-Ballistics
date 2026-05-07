package com.pixel.create_ordnance.registry;

import com.pixel.create_ordnance.CreateOrdnance;
import com.pixel.create_ordnance.content.entity.ProjectileEntity;
import com.pixel.create_ordnance.content.renderer.ProjectileEntityRenderer;

import com.simibubi.create.content.contraptions.render.ContraptionVisual;
import com.simibubi.create.foundation.data.CreateRegistrate;

import com.tterrag.registrate.util.entry.EntityEntry;

import net.minecraft.world.entity.MobCategory;

/**
 * Deferred entity-type registration for CreateOrdnance.
 *
 * <p>Registers {@link ProjectileEntity}
 * with appropriate tracking range, sync interval, and renderer binding via
 * Create's Registrate.</p>
 */
public class ModEntities {

        private static final CreateRegistrate REGISTRATE = CreateOrdnance.REGISTRATE;

        public static final EntityEntry<ProjectileEntity> PROJECTILE_ENTITY = REGISTRATE
                        .entity("projectile", ProjectileEntity::new, MobCategory.MISC)
                        .visual(() -> ContraptionVisual::new)
                        .renderer(() -> ProjectileEntityRenderer::new)
                        // Tracking: updateInterval=1 = sync every tick (20x/sec) for smooth movement
                        // Range 256 blocks for long-range visibility
                        .properties(p -> p
                                        .sized(1.0f, 1.0f)
                                        .clientTrackingRange(16) // Chunks (16*16 = 256 blocks)
                                        .updateInterval(1) // Sync every tick for smooth client movement
                                        .setShouldReceiveVelocityUpdates(true)) // Required for smooth interpolation
                        .register();

        public static void register() {
        }
}