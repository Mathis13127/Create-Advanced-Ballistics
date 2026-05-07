package com.pixel.create_ordnance.config;

import com.pixel.create_ordnance.CreateOrdnance;
import com.pixel.create_ordnance.api.events.ProjectileConfigReloadEvent;
import com.pixel.create_ordnance.registry.ProjectileComponentRegistry;

import net.minecraft.core.registries.BuiltInRegistries;

import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.NeoForge;

import org.apache.commons.lang3.tuple.Pair;

/**
 * Entry point for all CreateOrdnance configuration.
 *
 * <p>Manages three NeoForge config specs:
 * <ul>
 *   <li>{@link OrdnanceCommonConfig} — physics parameters, component defaults,
 *       attraction settings (synced to all clients).</li>
 *   <li>{@link OrdnanceServerConfig} — velocity limits, chunk loading, partial-tick
 *       thresholds (server-only).</li>
 *   <li>{@link OrdnanceClientConfig} — particle distance, interpolation, UI prefs
 *       (client-only).</li>
 * </ul>
 *
 * <p>Fires {@link ProjectileConfigReloadEvent}
 * on every config reload so listeners can react to value changes at runtime.</p>
 *
 * @see com.pixel.create_ordnance.mechanics.physics.core.PhysicsConstants
 */
public class OrdnanceConfigs {

    public static final ModConfigSpec COMMON_SPEC;
    public static final OrdnanceCommonConfig COMMON;

    public static final ModConfigSpec SERVER_SPEC;
    public static final OrdnanceServerConfig SERVER;

    public static final ModConfigSpec CLIENT_SPEC;
    public static final OrdnanceClientConfig CLIENT;

    // Global version counter for dynamic configuration updates
    public static long CONFIG_VERSION = 0;

    static {
        // Common
        final Pair<OrdnanceCommonConfig, ModConfigSpec> commonPair = new ModConfigSpec.Builder().configure(builder -> {
            OrdnanceCommonConfig config = new OrdnanceCommonConfig();
            config.registerAll(builder);
            return config;
        });
        COMMON_SPEC = commonPair.getRight();
        COMMON = commonPair.getLeft();
        COMMON.specification = COMMON_SPEC;

        // Server
        final Pair<OrdnanceServerConfig, ModConfigSpec> serverPair = new ModConfigSpec.Builder().configure(builder -> {
            OrdnanceServerConfig config = new OrdnanceServerConfig();
            config.registerAll(builder);
            return config;
        });
        SERVER_SPEC = serverPair.getRight();
        SERVER = serverPair.getLeft();
        SERVER.specification = SERVER_SPEC;

        // Client
        final Pair<OrdnanceClientConfig, ModConfigSpec> clientPair = new ModConfigSpec.Builder().configure(builder -> {
            OrdnanceClientConfig config = new OrdnanceClientConfig();
            config.registerAll(builder);
            return config;
        });
        CLIENT_SPEC = clientPair.getRight();
        CLIENT = clientPair.getLeft();
        CLIENT.specification = CLIENT_SPEC;
    }

    public static void register(ModLoadingContext context) {
        context.getActiveContainer().registerConfig(ModConfig.Type.COMMON, COMMON_SPEC);
        context.getActiveContainer().registerConfig(ModConfig.Type.SERVER, SERVER_SPEC);
        context.getActiveContainer().registerConfig(ModConfig.Type.CLIENT, CLIENT_SPEC);
    }

    public static void onLoad(ModConfigEvent event) {
        String modId = event.getConfig().getModId();
        boolean fireEvent = false;

        // 1. Check if it's Ordnance itself
        if (modId.equals(CreateOrdnance.MODID)) {
            fireEvent = true;
        } else {
            // 2. Check if it's an Addon (Mod that registered a projectile component)
            // We scan the registry to see if this modID owns any component block
            fireEvent = ProjectileComponentRegistry.getAll().keySet().stream()
                    .anyMatch(block -> BuiltInRegistries.BLOCK.getKey(block)
                            .getNamespace().equals(modId));
        }

        if (fireEvent && (event.getConfig().getSpec() == COMMON_SPEC || event.getConfig().getSpec() == SERVER_SPEC)) {
            // Increment global config version to signal all entities to refresh their stats
            CONFIG_VERSION++;
            CreateOrdnance.LOGGER
                    .info("Config Reload Detected. New Config Version: " + CONFIG_VERSION);

            // FIRE API EVENT
            ProjectileConfigReloadEvent apiEvent = new ProjectileConfigReloadEvent(
                    event.getConfig());
            NeoForge.EVENT_BUS.post(apiEvent);
        }
    }
}