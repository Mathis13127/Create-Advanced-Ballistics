package com.pixel.create_ordnance.config;

import com.pixel.create_ordnance.config.OrdnanceCommonConfig.ThrustType;

import net.createmod.catnip.config.ConfigBase;

/**
 * Common (synced) configuration for CreateOrdnance.
 *
 * <p>
 * Contains all physics tuning parameters organized in nested groups:
 * <ul>
 * <li><b>Physics</b> — gravity, drag, lift, buoyancy coefficients.</li>
 * <li><b>Aerodynamics</b> — angle-of-attack curves, stall thresholds.</li>
 * <li><b>MassDynamics</b> — fuel burn rate, mass loss behavior.</li>
 * <li><b>Inertia</b> — rotational damping, moment of inertia parameters.</li>
 * <li><b>Thrust</b> — engine types, vectoring angles, Lorentz factors.</li>
 * <li><b>Attraction</b> — spatial force defaults, vortex factors.</li>
 * <li><b>DebugComponents</b> — property overrides for each debug block.</li>
 * </ul>
 *
 * <p>
 * Values are read at runtime via
 * {@link com.pixel.create_ordnance.mechanics.physics.core.PhysicsConstants}
 * and automatically hot-reloaded.
 * </p>
 *
 * @see OrdnanceConfigs
 */
public class OrdnanceCommonConfig extends ConfigBase {

        public final ConfigInt maxProjectileLength = i(64, 2, 1000, "maxProjectileLength",
                        Comments.maxProjectileLength);

        public final ConfigInt projectileMaxLifetime = i(180, 1, 99999, "projectileMaxLifetime",
                        "Maximum time (in seconds) a projectile can exist before auto-despawning. 0 = Infinite.");

        @Override
        public String getName() {
                return "common";
        }

        public final Physics physics = nested(0, Physics::new, Comments.physics);

        public class Physics extends ConfigBase {

                public final Environment environment = nested(0, Environment::new, Comments.environment);
                public final Aerodynamics aerodynamics = nested(0, Aerodynamics::new, Comments.aerodynamics);
                public final MassDynamics massDynamics = nested(0, MassDynamics::new, Comments.massDynamics);

                public class Environment extends ConfigBase {
                        public final ConfigFloat gravity = f(0.05f, 0.0f, 10.0f, "gravity",
                                        "Realistic gravity strength.");
                        public final ConfigFloat globalBuoyancyMultiplier = f(0.1f, 0.0f, 100.0f,
                                        "globalBuoyancyMultiplier",
                                        "Multiplier for the upward force in fluids. 1.0 = realistic volume displacement.");
                        public final ConfigBool pressureEnabled = b(true, "pressureEnabled",
                                        "Enable or disable dynamic pressure calculations.");

                        @Override
                        public String getName() {
                                return "environment";
                        }
                }

                public class Aerodynamics extends ConfigBase {
                        public final ConfigFloat frontalDragMultiplier = f(0.025f, 0.0f, 20.0f, "frontalDragMultiplier",
                                        "Multiplier for 'head-on' resistance. Affects top speed in straight lines.");
                        public final ConfigFloat lateralDragMultiplier = f(0.1f, 0.0f, 20.0f, "lateralDragMultiplier",
                                        "Multiplier for 'sideways' resistance. Affects speed loss during turns (AoA > 0).");
                        public final ConfigFloat aeroAreaScale = f(0.005f, 0.0f, 1.0f, "aeroAreaScale",
                                        "Conversion factor (Stability Points -> m²). Directly scales Lift and Drag forces.");
                        public final ConfigFloat globalLiftMultiplier = f(0.2f, 0.0f, 100.0f, "globalLiftMultiplier",
                                        "Multiplier for Lift force only. Depends on Speed², Air Density, and Angle of Attack (AoA).");
                        public final ConfigFloat stallAngle = f(20.0f, 1.0f, 90.0f, "stallAngle",
                                        "Angle of attack (degrees) at which lift stalls.");
                        public final ConfigFloat viscousDragMultiplier = f(1.0f, 0.0f, 100.0f, "viscousDragMultiplier",
                                        "Multiplier for linear viscosity friction (Skin friction). Affects low-speed resistance in fluids.");

                        @Override
                        public String getName() {
                                return "aerodynamics";
                        }
                }

                public class MassDynamics extends ConfigBase {
                        public final Inertia inertia = nested(0, Inertia::new, "Inertial settings.");
                        public final SolidDrag solidDrag = nested(0, SolidDrag::new,
                                        "Settings for moving through solid blocks.");

                        public class Inertia extends ConfigBase {
                                public final ConfigFloat angularDampingMultiplier = f(1.0f, 0.0f, 100.0f,
                                                "angularDampingMultiplier",

                                                "Multiplier for rotation friction. Torque depends on Spin Rate, Mass, and Environment Viscosity.");
                                public final ConfigFloat stickyViscosityDecay = f(0.8f, 0.0f, 10.0f,
                                                "stickyViscosityDecay",
                                                "History decay factor for environment viscosity. 1.0 = permanent memory, 0.0 = no memory.");

                                @Override
                                public String getName() {
                                        return "inertia";
                                }
                        }

                        public class SolidDrag extends ConfigBase {
                                public final ConfigFloat penetrationEnergyRetention = f(0.45f, 0.0f, 1.0f,
                                                "penetrationEnergyRetention",
                                                "Percentage of kinetic energy kept when breaking a block. 1.0 = no slowdown.");
                                public final ConfigFloat penetrationBaseCost = f(15.0f, 0.0f, 1000.0f,
                                                "penetrationBaseCost", "Base energy cost to break any block.");

                                @Override
                                public String getName() {
                                        return "solidDrag";
                                }
                        }

                        @Override
                        public String getName() {
                                return "massDynamics";
                        }
                }

                public final Attraction attraction = nested(0, Attraction::new, Comments.attraction);

                public class Attraction extends ConfigBase {
                        public final ConfigBool enabled = b(true, "enabled",
                                        "Enable or disable the attraction/spatial force system globally.");
                        public final ConfigInt maxSources = i(16, 1, 256, "maxSources",
                                        "Maximum number of active attraction sources allowed simultaneously.");
                        public final ConfigInt debugSyncInterval = i(5, 1, 100, "debugSyncInterval",
                                        "Interval (in ticks) between debug sync packets to clients. Lower = smoother debug rendering.");

                        @Override
                        public String getName() {
                                return "attraction";
                        }
                }

                public String getName() {
                        return "physics";
                }
        }

        public final Divers divers = nested(0, Divers::new, Comments.divers);

        public class Divers extends ConfigBase {
                public final TransformerTester transformerTester = nested(0, TransformerTester::new,
                                Comments.transformerTester);

                public final ConfigBool debugMode = b(true, "debugMode", Comments.debugMode);

                @Override
                public String getName() {
                        return "divers";
                }

                public class TransformerTester extends ConfigBase {
                        public final ConfigFloat testerScale = f(1.0f, 0.1f, 100.0f, "testerScale",
                                        Comments.testerScale);
                        public final ConfigFloat initialSpeed = f(2.0f, 0.0f, 500.0f, "initialSpeed",
                                        Comments.testerInitialSpeed);

                        @Override
                        public String getName() {
                                return "transformerTester";
                        }
                }

                public class Mixins extends ConfigBase {
                        public final ConfigBool mixinDebug = b(false, "mixinDebug",
                                        Comments.mixinDebug);
                        public final ConfigBool triggerPayloadOnExplosion = b(false, "triggerPayloadOnExplosion",
                                        Comments.triggerPayloadOnExplosion);

                        @Override
                        public String getName() {
                                return "mixins";
                        }
                }

                public final Mixins mixins = nested(0, Mixins::new, Comments.mixins);
        }

        public final DebugComponents debugComponents = nested(0, DebugComponents::new, Comments.debugComponents);

        public class DebugComponents extends ConfigBase {
                public final Tails tails = nested(1, Tails::new, Comments.tails);
                public final FuelTank fuelTank = nested(1, FuelTank::new, Comments.fuelTank);
                public final OptionalModule optionalModule = nested(1, OptionalModule::new, Comments.optionalModule);
                public final Ballast ballast = nested(1, Ballast::new, Comments.ballast);
                public final Fins fins = nested(1, Fins::new, Comments.fins);
                public final Payload payload = nested(1, Payload::new, Comments.payload);

                @Override
                public String getName() {
                        return "debugComponents";
                }

                public class Ballast extends ConfigBase {
                        public final ConfigFloat weight = f(300.0f, 0.0f, 100000.0f, "weight",
                                        "Weight of the ballast module");
                        public final ConfigFloat stabilityFactor = f(3.0f, 0.0f, 1000.0f, "stabilityFactor",
                                        Comments.stabilityFactor);

                        @Override
                        public String getName() {
                                return "ballast";
                        }
                }

                public class Fins extends ConfigBase {
                        public final ConfigFloat weight = f(25.0f, 0.0f, 100000.0f, "weight",
                                        "Weight of the fins module");
                        public final ConfigFloat stabilityFactor = f(19.0f, 0.0f, 1000.0f, "stabilityFactor",
                                        Comments.stabilityFactor);

                        @Override
                        public String getName() {
                                return "fins";
                        }
                }

                public class Tails extends ConfigBase {
                        public final Afterburner afterburner = nested(1, Afterburner::new, Comments.afterburnerTail);
                        public final Propeller propeller = nested(1, Propeller::new, Comments.propellerTail);

                        @Override
                        public String getName() {
                                return "tails";
                        }

                        public class Afterburner extends ConfigBase {
                                public final ConfigFloat baseThrust = f(375.0f, 0.0f, 1000.0f, "baseThrust",
                                                Comments.tailBaseThrust);
                                public final ConfigFloat fuelConsumption = f(1.0f, 0.0f, 100.0f, "fuelConsumption",
                                                Comments.tailFuelConsumption);
                                public final ConfigEnum<ThrustType> thrustType = e(ThrustType.AFTERBURNER, "thrustType",
                                                Comments.tailThrustType);
                                public final ConfigFloat maxNozzleTilt = f(45.0f, 0.0f, 90.0f, "maxNozzleTilt",
                                                Comments.tailMaxNozzleTilt);
                                public final ConfigFloat weight = f(15.0f, 0.0f, 100000.0f, "weight",
                                                Comments.tailWeight);
                                public final ConfigFloat stabilityFactor = f(20.0f, 0.0f, 1000.0f, "stabilityFactor",
                                                Comments.stabilityFactor);
                                public final ConfigFloat optimalDensity = f(0.1f, 0.0f, 10000.0f, "optimalDensity",
                                                Comments.tailOptimalDensity);
                                public final ConfigFloat densityBandwidth = f(0.5f, 0.001f, 10000.0f,
                                                "densityBandwidth",
                                                Comments.tailDensityBandwidth);

                                @Override
                                public String getName() {
                                        return "afterburner";
                                }
                        }

                        public class Propeller extends ConfigBase {
                                public final ConfigFloat baseThrust = f(100.0f, 0.0f, 1000.0f, "baseThrust",
                                                Comments.tailBaseThrust);
                                public final ConfigFloat fuelConsumption = f(0.01f, 0.0f, 100.0f, "fuelConsumption",
                                                Comments.tailFuelConsumption);
                                public final ConfigEnum<ThrustType> thrustType = e(ThrustType.PROPELLER, "thrustType",
                                                Comments.tailThrustType);
                                public final ConfigFloat maxNozzleTilt = f(45.0f, 0.0f, 90.0f, "maxNozzleTilt",
                                                Comments.tailMaxNozzleTilt);
                                public final ConfigFloat weight = f(25.0f, 0.0f, 100000.0f, "weight",
                                                Comments.tailWeight);
                                public final ConfigFloat stabilityFactor = f(5.0f, 0.0f, 1000.0f, "stabilityFactor",
                                                Comments.stabilityFactor);
                                public final ConfigFloat optimalDensity = f(1000.0f, 0.0f, 10000.0f, "optimalDensity",
                                                Comments.tailOptimalDensity);
                                public final ConfigFloat densityBandwidth = f(100.0f, 0.001f, 10000.0f,
                                                "densityBandwidth",
                                                Comments.tailDensityBandwidth);

                                @Override
                                public String getName() {
                                        return "propeller";
                                }
                        }
                }

                public class FuelTank extends ConfigBase {
                        public final ConfigFloat capacity = f(1000.0f, 0.0f, 100000.0f, "capacity",
                                        Comments.fuelTankCapacity);
                        public final ConfigFloat weight = f(60.0f, 0.0f, 100000.0f, "weight", Comments.fuelTankWeight);
                        public final ConfigFloat stabilityFactor = f(3.0f, 0.0f, 1000.0f, "stabilityFactor",
                                        Comments.stabilityFactor);

                        @Override
                        public String getName() {
                                return "fuelTank";
                        }
                }

                public class OptionalModule extends ConfigBase {
                        public final ConfigFloat weight = f(60.0f, 0.0f, 100000.0f, "weight",
                                        Comments.optionalModuleWeight);
                        public final ConfigFloat stabilityFactor = f(3.0f, 0.0f, 1000.0f, "stabilityFactor",
                                        Comments.stabilityFactor);

                        @Override
                        public String getName() {
                                return "optionalModule";
                        }
                }

                public class Payload extends ConfigBase {
                        public final ConfigFloat explosionPower = f(4.0f, 0.0f, 100.0f, "explosionPower",
                                        Comments.payloadExplosionPower);
                        public final ConfigFloat weight = f(20.0f, 0.0f, 100000.0f, "weight", Comments.payloadWeight);
                        public final ConfigFloat stabilityFactor = f(3.0f, 0.0f, 1000.0f, "stabilityFactor",
                                        Comments.stabilityFactor);

                        @Override
                        public String getName() {
                                return "payload";
                        }
                }
        }

        public enum ThrustType {
                AFTERBURNER, PROPELLER, NONE
        }

        private static class Comments {

                static String debugMode = "Enable developer debug mode (Logs, Visuals)";

                static String debugComponents = "Configuration for Debug Components";
                static String tails = "Debug Tails Configuration (Afterburner vs Propeller)";
                static String afterburnerTail = "Configuration for the Afterburner (Jet) style tail";
                static String propellerTail = "Configuration for the Propeller (Aquatic/Low-speed) style tail";
                static String tailBaseThrust = "Base thrust force provided by the tail";
                static String tailDragCoefficient = "Air drag coefficient for this component";
                static String tailFuelConsumption = "Fuel consumption per tick";
                static String tailThrustType = "Type of propulsion (AFTERBURNER, PROPELLER, NONE)";
                static String tailMaxNozzleTilt = "Max nozzle tilt for thrust vectoring (degrees)";
                static String tailWeight = "Weight of the component";
                static String tailOptimalDensity = "Optimal fluid density for peak propulsive efficiency (Lorentzian resonance center). Air ~1.2, Water ~1000";
                static String tailDensityBandwidth = "Bandwidth of the Lorentzian density coupling curve (higher = more tolerant to density mismatch)";

                static String fuelTank = "Debug Fuel Tank Configuration";
                static String fuelTankCapacity = "Fuel capacity";
                static String fuelTankWeight = "Weight of the component";
                static String ballast = "Debug Ballast Configuration (Dead Weight)";
                static String fins = "Debug Fins Configuration (Stability)";

                static String divers = "Miscellaneous configuration settings";
                static String mixins = "Debug message toggles for Mixins and API event listeners";
                static String transformerTester = "Configuration for the Debug Transformer Tester item";
                static String testerScale = "Scale factor applied to scanned projectiles (default 1.0)";
                static String testerInitialSpeed = "Initial velocity scale for launched projectiles";
                static String mixinDebug = "Enable verbose logs for Mixins and internal events";
                static String triggerPayloadOnExplosion = "If enabled, projectiles will trigger their payloads when destroyed (explosion, damage, impact).";

                static String optionalModule = "Debug Optional Module Configuration";
                static String optionalModuleWeight = "Weight of the optional module";

                static String payload = "Debug Payload Configuration";
                static String payloadExplosionPower = "Explosion power (radius)";
                static String payloadWeight = "Weight of the payload";

                static String maxProjectileLength = "Maximum number of blocks to scan";
                static String physics = "Core Physics Engine Configuration";
                static String environment = "Environment Physics Configuration";
                static String aerodynamics = "Aerodynamics Physics Configuration";
                static String massDynamics = "Mass Dynamics Physics Configuration";
                static String attraction = "Attraction / Spatial Force Configuration";

                static String stabilityFactor = "Centre de Pression influence. Higher value (e.g. 20 for Tails) pulls the Center of Pressure towards this block. For stable flight, CoP must be BEHIND Center of Gravity.";

        }
}