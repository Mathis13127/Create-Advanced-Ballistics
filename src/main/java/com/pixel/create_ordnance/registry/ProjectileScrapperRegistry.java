package com.pixel.create_ordnance.registry;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.pixel.create_ordnance.CreateOrdnance;
import com.pixel.create_ordnance.mechanics.scanning.DumbBlockScrapper;
import com.pixel.create_ordnance.mechanics.scanning.IProjectileScrapper;
import com.pixel.create_ordnance.mechanics.scanning.WorldBlockScrapper;

import net.minecraft.resources.ResourceLocation;

/**
 * Registry mapping {@link ResourceLocation} identifiers
 * to {@link IProjectileScrapper}
 * implementations.
 *
 * <p>Built-in scrappers:
 * <ul>
 *   <li>{@code create_ordnance:world_block} —
 *       {@link WorldBlockScrapper},
 *       scans blocks directly from the world.</li>
 *   <li>{@code create_ordnance:dumb_block} —
 *       {@link DumbBlockScrapper},
 *       simplified scan for testing.</li>
 * </ul>
 *
 * <p>Addon developers can register additional scrappers via
 * {@link #register(ResourceLocation, IProjectileScrapper)}.</p>
 *
 * @see IProjectileScrapper
 */
public class ProjectileScrapperRegistry {

    private static final Map<ResourceLocation, IProjectileScrapper.ScrapperFactory> REGISTRY = new HashMap<>();

    public static final ResourceLocation LINEAR = ResourceLocation.fromNamespaceAndPath(CreateOrdnance.MODID,
            "linear");
    public static final ResourceLocation DUMB = ResourceLocation.fromNamespaceAndPath(CreateOrdnance.MODID, "dumb");

    static {
        // Register default scrappers
        register(LINEAR, WorldBlockScrapper::new);
        register(DUMB, DumbBlockScrapper::new);
    }

    /**
     * Register a new scrapper factory.
     */
    public static void register(ResourceLocation id, IProjectileScrapper.ScrapperFactory factory) {
        REGISTRY.put(id, factory);
    }

    /**
     * Get a scrapper factory by ID.
     */
    public static Optional<IProjectileScrapper.ScrapperFactory> get(ResourceLocation id) {
        return Optional.ofNullable(REGISTRY.get(id));
    }

    /**
     * Get the default linear scrapper factory.
     */
    public static IProjectileScrapper.ScrapperFactory getDefault() {
        return REGISTRY.get(LINEAR);
    }

    public static void init() {
        // Initialization trigger
    }
}