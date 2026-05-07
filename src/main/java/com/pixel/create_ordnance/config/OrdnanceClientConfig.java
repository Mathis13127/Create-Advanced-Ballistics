package com.pixel.create_ordnance.config;

import net.createmod.catnip.config.ConfigBase;

/**
 * Client-only configuration for CreateOrdnance.
 *
 * <p>Controls rendering and UI preferences:
 * <ul>
 *   <li><b>UI</b> — goggles overlay, stat display format.</li>
 *   <li><b>Particles</b> — max render distance, trail density.</li>
 *   <li><b>Interpolation</b> — client-side smoothing parameters.</li>
 * </ul>
 *
 * <p>These values are <b>not</b> synced to the server.</p>
 *
 * @see OrdnanceConfigs
 */
public class OrdnanceClientConfig extends ConfigBase {

        public final UI ui = nested(0, UI::new, Comments.ui);

        public final ConfigFloat maxParticleDistance = f(256.0f, 0.0f, 1024.0f, "maxParticleDistance",
                        Comments.maxParticleDistance);

        public final ConfigInt positionInterpolationSteps = i(1, 1, 20, "positionInterpolationSteps",
                        "Number of ticks used for position smoothing. (Default: 3)");

        public final ConfigInt rotationInterpolationSteps = i(3, 1, 20, "rotationInterpolationSteps",
                        "Number of ticks used for rotation smoothing. (Default: 3)");

        public class UI extends ConfigBase {
                public final QuickConfigButton quickConfigButton = nested(0, QuickConfigButton::new,
                                Comments.quickConfigButton);

                @Override
                public String getName() {
                        return "ui";
                }

                public class QuickConfigButton extends ConfigBase {
                        public final ConfigBool showConfigInTitleScreen = b(true, "showConfigInTitleScreen",
                                        "Show Create: Ordnance config button on Title Screen");
                        public final ConfigBool showConfigInPauseMenu = b(true, "showConfigInPauseMenu",
                                        "Show Create: Ordnance config button on Pause Menu");

                        public final ConfigInt titleScreenOffsetX = i(-4, -100, 100, "titleScreenOffsetX",
                                        "Offset X for the config button on Title Screen");
                        public final ConfigInt titleScreenOffsetY = i(-24, -100, 100, "titleScreenOffsetY",
                                        "Offset Y for the config button on Title Screen");

                        public final ConfigInt pauseMenuOffsetX = i(-4, -100, 100, "pauseMenuOffsetX",
                                        "Offset X for the config button on Pause Menu");
                        public final ConfigInt pauseMenuOffsetY = i(24, -100, 100, "pauseMenuOffsetY",
                                        "Offset Y for the config button on Pause Menu");

                        @Override
                        public String getName() {
                                return "quickConfigButton";
                        }
                }
        }

        public final GogglesStats gogglesStats = nested(0, GogglesStats::new, Comments.gogglesStats);

        public class GogglesStats extends ConfigBase {
                public final StabilityThresholds stabilityThresholds = nested(0, StabilityThresholds::new,
                                Comments.stabilityThresholds);

                @Override
                public String getName() {
                        return "gogglesStats";
                }

                public class StabilityThresholds extends ConfigBase {
                        public final ConfigFloat stableMargin = f(0.5f, 0.0f, 10.0f, "stableMargin",
                                        "CoM-CoP margin below which projectile is 'Stable' (yellow). Below 0 = UNSTABLE.");
                        public final ConfigFloat veryStableMargin = f(1.5f, 0.0f, 10.0f, "veryStableMargin",
                                        "CoM-CoP margin above which projectile is 'Very Stable' (green). Above this = Extremely Stable.");

                        @Override
                        public String getName() {
                                return "stabilityThresholds";
                        }
                }
        }

        @Override
        public String getName() {
                return "client";
        }

        private static class Comments {
                static String ui = "User Interface settings";
                static String quickConfigButton = "Settings for the Quick Config Button on menus";
                static String maxParticleDistance = "Maximum distance (in blocks) at which particles from this mod are rendered on the client.";
                static String gogglesStats = "Goggles HUD overlay statistics configuration";
                static String stabilityThresholds = "Stability status thresholds for the Goggles HUD display";
        }
}
