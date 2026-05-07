package com.pixel.create_ordnance.config;

import net.createmod.catnip.config.ConfigBase;

/**
 * Server-only configuration for CreateOrdnance.
 *
 * <p>Controls safety limits and performance thresholds:
 * <ul>
 *   <li><b>Thresholds</b> — max velocity, partial-tick segments, blackbox history size.</li>
 *   <li><b>Chunks</b> — chunk-loading range, simulation accuracy at chunk borders.</li>
 * </ul>
 *
 * <p>These values are <b>not</b> synced to clients.</p>
 *
 * @see OrdnanceConfigs
 */
public class OrdnanceServerConfig extends ConfigBase {

        public final Thresholds thresholds = nested(0, Thresholds::new, "Safety limits and thresholds.");
        public final Chunks chunks = nested(0, Chunks::new, "Chunk loading and simulation accuracy.");

        public class Thresholds extends ConfigBase {
                public final ConfigFloat velocityLimit = f(250.0f, 0.1f, 10000.0f, "velocityLimit",
                                "Absolute maximum speed (blocks/tick) allowed before discard.");
                public final ConfigInt maxChunkLoaded = i(100, 1, 1000, "maxChunkLoaded",
                                "Maximum number of chunks a single projectile can force load.");
                public final ConfigFloat partialTickVelocityThreshold = f(3.0f, 0.1f, 100.0f,
                                "partialTickVelocityThreshold",
                                "Speed (blocks/tick) above which partial-tick subdivision activates.");

                public final ConfigInt maxBlackBoxHistory = i(3500, 1, 100000, "maxBlackBoxHistory",
                                "Maximum number of ticks to keep in the rolling BlackBox buffer.");

                @Override
                public String getName() {
                        return "thresholds";
                }
        }

        public class Chunks extends ConfigBase {
                public final ConfigBool partialTicksEnabled = b(true, "partialTicksEnabled",
                                "Enable high-precision sub-tick simulation.");
                public final ConfigBool chunkloaderEnabled = b(false, "chunkloaderEnabled",
                                "Enable or disable chunk loading for projectiles.");

                @Override
                public String getName() {
                        return "chunks";
                }
        }

        @Override
        public String getName() {
                return "server";
        }

}
