package com.pixel.create_ordnance.mechanics.physics.logic;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.pixel.create_ordnance.CreateOrdnance;
import com.pixel.create_ordnance.config.OrdnanceConfigs;
import com.pixel.create_ordnance.mechanics.physics.core.IPhysicsModule;
import com.pixel.create_ordnance.mechanics.physics.core.ModuleOutput;
import com.pixel.create_ordnance.mechanics.physics.core.PhysicsContext;
import com.pixel.create_ordnance.mechanics.physics.core.ProjectileState;
import com.pixel.create_ordnance.mechanics.scanning.ProjectileStats;
import com.pixel.create_ordnance.mechanics.scanning.ScrapResult;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import net.neoforged.fml.ModList;

import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Diagnostic "Black Box" system (V2 Protocol).
 * <p>
 * Records a rolling history of physics ticks. Each tick contains:
 * <ul>
 * <li>Full kinematic snapshot (position, velocity, orientation, AoA,
 * speed...)</li>
 * <li>Per-module force/torque contributions</li>
 * <li>Net force and net torque</li>
 * </ul>
 * <p>
 * One-time projectile header is set at launch via
 * {@link #setProjectileHeader(ProjectileStats, ScrapResult)}
 * and written at the top of the JSON export.
 */
public class BlackBox {

    /** Maximum number of ticks to keep in the rolling buffer. */
    private UUID focusedUUID = null;
    private final List<TickSnapshot> history = new ArrayList<>();
    private TickSnapshot currentTick;
    private boolean enabled = true;

    // One-time scrapper header (set at entity spawn, never changes during flight)
    private ProjectileHeader projectileHeader = null;

    // =========================================================
    // FOCUS & HEADER (set at spawn or manual scan)
    // =========================================================

    /**
     * Sets the focus to a specific projectile.
     * This cleared previous history and header to ensure memory efficiency.
     */
    public void setFocus(UUID uuid, ProjectileStats stats, ScrapResult scrapResult) {
        this.clear();
        this.focusedUUID = uuid;
        this.projectileHeader = new ProjectileHeader(stats, scrapResult);
    }

    public UUID getFocusedUUID() {
        return focusedUUID;
    }

    public boolean hasHeader() {
        return projectileHeader != null;
    }

    // =========================================================
    // TICK LIFECYCLE
    // =========================================================

    /**
     * Starts recording a new physics tick with full kinematic context.
     * Only records if the context's projectile matches the current focus.
     *
     * @param context Immutable physics context for this tick.
     */
    public void beginTick(PhysicsContext context) {
        if (!enabled || context == null)
            return;

        // If focus is null, the first one to tick takes the focus (Auto-Focus)
        if (focusedUUID == null) {
            setFocus(context.projectileId, context.stats, context.scrapResult);
        }

        // Only record if it's the focused projectile
        if (!context.projectileId.equals(focusedUUID)) {
            return;
        }

        currentTick = new TickSnapshot(context);
    }

    /**
     * Records a single module's output for the current tick.
     *
     * @param output The module's contribution for this tick.
     */
    public void recordModuleOutput(ModuleOutput output) {
        if (!enabled || currentTick == null)
            return;
        currentTick.moduleOutputs.add(output);
    }

    /**
     * Finalizes the current tick with the accumulated net vectors.
     * Adds the snapshot to the rolling history buffer.
     *
     * @param netForce  Total accumulated force vector.
     * @param netTorque Total accumulated torque vector.
     */
    public void finalizeTick(Vec3 netForce, Vec3 netTorque) {
        if (!enabled || currentTick == null)
            return;
        currentTick.netForce = netForce;
        currentTick.netTorque = netTorque;
        history.add(currentTick);

        // CONFIGURABLE HISTORY LIMIT (Default 3500)
        int maxHistory = OrdnanceConfigs.SERVER.thresholds.maxBlackBoxHistory.get();
        if (history.size() > maxHistory) {
            history.remove(0);
        }
        currentTick = null;
    }

    /**
     * Late-update for telemetry if impact detected during partial sim.
     */
    public void recordImpactPartition(int partition) {
        if (currentTick != null) {
            currentTick.collisionPartition = partition;
        }
    }

    /**
     * Returns the last completed tick snapshot, or {@code null} if no ticks
     * recorded.
     */
    public TickSnapshot getLastTickSnapshot() {
        return history.isEmpty() ? null : history.get(history.size() - 1);
    }

    /**
     * Returns the full tick history.
     */
    public List<TickSnapshot> getHistory() {
        return history;
    }

    /**
     * Clears all recorded history, the projectile header, and the focus.
     */
    public void clear() {
        history.clear();
        currentTick = null;
        projectileHeader = null;
        focusedUUID = null;
    }

    /**
     * Enables or disables recording.
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    // =========================================================
    // JSON EXPORT
    // =========================================================

    /**
     * Serializes the full BlackBox to a single JSON string.
     * Structure:
     * 
     * <pre>
     * {
     *   "projectile": { ... full scrapper data ... },
     *   "tickCount": N,
     *   "ticks": [
     *     {
     *       "tickIndex": 0,
     *       "kinematics": {
     *         "position": [x,y,z], "velocity": [x,y,z], "speed": s,
     *         "orientation": [x,y,z,w], "angularVelocity": [x,y,z],
     *         "aoa_deg": a, "fluidDensity": d, "viscosity": v
     *       },
     *       "modules": [ { "id": "...", "force1": [...], "force2": [...], "torque": [...] }, ... ],
     *       "netForce": [...],
     *       "netTorque": [...]
     *     },
     *     ...
     *   ]
     * }
     * </pre>
     */
    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");

        // --- HEADER ---
        sb.append("  \"projectile\": ");
        if (projectileHeader != null) {
            sb.append(projectileHeader.toJson());
        } else {
            sb.append("null");
        }
        sb.append(",\n");

        // --- TICKS ---
        sb.append("  \"tickCount\": ").append(history.size()).append(",\n");
        sb.append("  \"ticks\": [\n");

        for (int t = 0; t < history.size(); t++) {
            TickSnapshot tick = history.get(t);
            sb.append("    {\n");
            sb.append("      \"tickIndex\": ").append(t).append(",\n");

            // Kinematics
            sb.append("      \"kinematics\": ").append(tick.kinematicsToJson()).append(",\n");

            // Modules
            sb.append("      \"modules\": [\n");
            for (int m = 0; m < tick.moduleOutputs.size(); m++) {
                ModuleOutput out = tick.moduleOutputs.get(m);
                sb.append("        {\n");
                sb.append("          \"id\": \"").append(out.moduleName()).append("\"");

                boolean force1NonZero = out.force1().lengthSqr() > 1e-9;
                boolean force2NonZero = out.force2().lengthSqr() > 1e-9;
                boolean torqueNonZero = out.torque().lengthSqr() > 1e-9;

                IPhysicsModule.ModuleCapability cap = out.capability();

                if (cap == IPhysicsModule.ModuleCapability.FORCE_SIMPLE
                        || cap == IPhysicsModule.ModuleCapability.FORCE_WITH_TORQUE) {
                    if (force1NonZero) {
                        sb.append(",\n          \"force\": ").append(jsonVec(out.force1()));
                    }
                } else if (cap == IPhysicsModule.ModuleCapability.FORCE_DUAL) {
                    if (force1NonZero) {
                        sb.append(",\n          \"force1\": ").append(jsonVec(out.force1()));
                    }
                    if (force2NonZero) {
                        sb.append(",\n          \"force2\": ").append(jsonVec(out.force2()));
                    }
                }

                if (torqueNonZero) {
                    sb.append(",\n          \"torque\": ").append(jsonVec(out.torque()));
                }

                sb.append("\n        }");
                if (m < tick.moduleOutputs.size() - 1)
                    sb.append(",");
                sb.append("\n");
            }
            sb.append("      ],\n");

            sb.append("      \"netForce\": ").append(jsonVec(tick.netForce)).append(",\n");
            sb.append("      \"netTorque\": ").append(jsonVec(tick.netTorque)).append("\n");
            sb.append("    }");
            if (t < history.size() - 1)
                sb.append(",");
            sb.append("\n");
        }

        sb.append("  ]\n");
        sb.append("}");
        return sb.toString();
    }

    static String jsonVec(Vec3 v) {
        return String.format(Locale.ROOT, "[%.6f, %.6f, %.6f]", v.x, v.y, v.z);
    }

    static String jsonVec3f(Vector3f v) {
        return String.format(Locale.ROOT, "[%.6f, %.6f, %.6f]", v.x, v.y, v.z);
    }

    static String jsonQuat(Quaternionf q) {
        return String.format(Locale.ROOT, "[%.6f, %.6f, %.6f, %.6f]", q.x, q.y, q.z, q.w);
    }

    // =========================================================
    // TICK SNAPSHOT
    // =========================================================

    /**
     * Snapshot of a single physics tick.
     * Kinematic data is captured at the start of the tick via the PhysicsContext.
     */
    public static class TickSnapshot {

        // Kinematics (from PhysicsContext)
        public final Vec3 position;
        public final Vec3 velocityWorld;
        public final Vec3 velocityLocal;
        public final Quaternionf orientation;
        public final Vector3f angularVelocity;
        public final double aoa;
        public final double speed;
        public final double fluidDensity;
        public final double viscosity;

        // Per-module contributions
        public final List<ModuleOutput> moduleOutputs = new ArrayList<>();

        // Net results (filled at finalizeTick)
        public Vec3 netForce = Vec3.ZERO;
        public Vec3 netTorque = Vec3.ZERO;

        // --- BlackBox Telemetry (Extended V6.7) ---
        public boolean isExtinguished;
        public ProjectileState state;
        public int currentlyLoadedChunks;
        public int totalLoadedChunks;
        public int partitionCount;
        public int currentPartition;
        public int collisionPartition = -1; // Set if impact detected

        TickSnapshot(PhysicsContext ctx) {
            if (ctx != null) {
                this.position = ctx.position;
                this.velocityWorld = ctx.velocityWorld;
                this.velocityLocal = ctx.velocityLocal;
                this.orientation = new Quaternionf(ctx.orientation);
                this.angularVelocity = new Vector3f(ctx.angularVelocity);
                this.aoa = ctx.angleOfAttack;
                this.speed = ctx.getSpeed();
                this.fluidDensity = ctx.getEnvironmentDensity();
                this.viscosity = ctx.getEnvironmentViscosity();

                // Telemetry
                this.isExtinguished = ctx.isExtinguished;
                this.state = ctx.state;
                this.currentlyLoadedChunks = ctx.currentlyLoadedChunksCount;
                this.totalLoadedChunks = ctx.totalChunksLoadedCount;
                this.partitionCount = ctx.partitionCount;
                this.currentPartition = ctx.currentPartition;
            } else {
                this.position = Vec3.ZERO;
                this.velocityWorld = Vec3.ZERO;
                this.velocityLocal = Vec3.ZERO;
                this.orientation = new Quaternionf();
                this.angularVelocity = new Vector3f();
                this.aoa = 0;
                this.speed = 0;
                this.fluidDensity = 0;
                this.viscosity = 0;
            }
        }

        String kinematicsToJson() {
            return "{\n" +
                    "        \"position\": " + jsonVec(position) + ",\n" +
                    "        \"velocityWorld\": " + jsonVec(velocityWorld) + ",\n" +
                    "        \"velocityLocal\": " + jsonVec(velocityLocal) + ",\n" +
                    "        \"speed\": " + String.format(Locale.ROOT, "%.6f", speed) + ",\n" +
                    "        \"orientation\": " + jsonQuat(orientation) + ",\n" +
                    "        \"angularVelocity\": " + jsonVec3f(angularVelocity) + ",\n" +
                    "        \"aoa_deg\": " + String.format(Locale.ROOT, "%.4f", Math.toDegrees(aoa)) + ",\n" +
                    "        \"fluidDensity\": " + String.format(Locale.ROOT, "%.4f", fluidDensity) + ",\n" +
                    "        \"viscosity\": " + String.format(Locale.ROOT, "%.4f", viscosity) + ",\n" +
                    "        \"status\": {\n" +
                    "          \"state\": \"" + state.name() + "\",\n" +
                    "          \"isExtinguished\": " + isExtinguished + "\n" +
                    "        },\n" +
                    "        \"chunks\": {\n" +
                    "          \"currentlyLoaded\": " + currentlyLoadedChunks + ",\n" +
                    "          \"totalLoadedHistory\": " + totalLoadedChunks + "\n" +
                    "        },\n" +
                    "        \"partialSim\": {\n" +
                    "          \"partitionCount\": " + partitionCount + ",\n" +
                    "          \"currentPartition\": " + currentPartition + ",\n" +
                    "          \"impactPartition\": " + collisionPartition + "\n" +
                    "        }\n" +
                    "      }";
        }

        /** Legacy debug string (kept for compatibility). */
        public String toDebugString() {
            StringBuilder sb = new StringBuilder();
            sb.append("[BlackBox Tick] Modules: ").append(moduleOutputs.size()).append("\n");
            for (ModuleOutput output : moduleOutputs) {
                sb.append("  [").append(output.moduleName()).append("] ");
                sb.append("F1=").append(String.format(Locale.ROOT, "(%.4f,%.4f,%.4f)", output.force1().x,
                        output.force1().y, output.force1().z));
                sb.append(" F2=").append(String.format(Locale.ROOT, "(%.4f,%.4f,%.4f)", output.force2().x,
                        output.force2().y, output.force2().z));
                sb.append(" T=").append(String.format(Locale.ROOT, "(%.4f,%.4f,%.4f)", output.torque().x,
                        output.torque().y, output.torque().z));
                sb.append("\n");
            }
            sb.append("  [NET] Force=")
                    .append(String.format(Locale.ROOT, "(%.4f,%.4f,%.4f)", netForce.x, netForce.y, netForce.z));
            sb.append(" Torque=")
                    .append(String.format(Locale.ROOT, "(%.4f,%.4f,%.4f)", netTorque.x, netTorque.y, netTorque.z));
            return sb.toString();
        }
    }

    // =========================================================
    // PROJECTILE HEADER
    // =========================================================

    /**
     * One-time snapshot of the projectile's static properties at launch.
     * Contains the full ScrapResult and ProjectileStats from the analyzer.
     */
    public static class ProjectileHeader {
        public final String modVersion = ModList.get()
                .getModContainerById(CreateOrdnance.MODID)
                .map(container -> container.getModInfo().getVersion().toString())
                .orElse("0.0.0-DYNAMIC-LOOKUP-FAILED");
        public final ProjectileStats stats;
        public final ScrapResult scrapResult;

        ProjectileHeader(ProjectileStats stats, ScrapResult scrapResult) {
            this.stats = stats;
            this.scrapResult = scrapResult;
        }

        String toJson() {
            StringBuilder sb = new StringBuilder();
            sb.append("{\n");
            sb.append("    \"version\": \"").append(modVersion).append("\",\n");

            // --- ScrapResult ---
            sb.append("    \"scrapResult\": {\n");
            sb.append("      \"scale\": ").append(scrapResult.scale()).append(",\n");
            sb.append("      \"facing\": \"").append(scrapResult.worldFacing().getName()).append("\",\n");
            sb.append("      \"worldOrigin\": ").append(jsonVec(scrapResult.worldOrigin())).append(",\n");
            // Block list
            sb.append("      \"blockIds\": [");
            List<ResourceLocation> ids = scrapResult.blockIds();
            for (int i = 0; i < ids.size(); i++) {
                sb.append("\"").append(ids.get(i).toString()).append("\"");
                if (i < ids.size() - 1)
                    sb.append(", ");
            }
            sb.append("],\n");
            // Bounds
            if (scrapResult.minPos() != null && scrapResult.maxPos() != null) {
                sb.append("      \"minPos\": [").append(scrapResult.minPos().getX()).append(", ")
                        .append(scrapResult.minPos().getY()).append(", ").append(scrapResult.minPos().getZ())
                        .append("],\n");
                sb.append("      \"maxPos\": [").append(scrapResult.maxPos().getX()).append(", ")
                        .append(scrapResult.maxPos().getY()).append(", ").append(scrapResult.maxPos().getZ())
                        .append("]\n");
            } else {
                sb.append("      \"minPos\": null,\n");
                sb.append("      \"maxPos\": null\n");
            }
            sb.append("    },\n");

            // --- ProjectileStats ---
            sb.append("    \"stats\": {\n");
            sb.append("      \"totalMass\": ").append(stats.totalMass()).append(",\n");
            sb.append("      \"totalStability\": ").append(stats.totalStability()).append(",\n");
            sb.append("      \"centerOfMass\": ").append(jsonVec(stats.centerOfMass())).append(",\n");
            sb.append("      \"centerOfPressure\": ").append(jsonVec(stats.centerOfPressure())).append(",\n");
            sb.append("      \"totalFuel\": ").append(stats.totalFuel()).append(",\n");
            sb.append("      \"totalFuelConsumption\": ").append(stats.totalFuelConsumption()).append(",\n");
            sb.append("      \"thrustType\": \"").append(stats.thrustType()).append("\",\n");
            sb.append("      \"baseThrust\": ").append(stats.baseThrust()).append(",\n");
            sb.append("      \"maxNozzleTilt\": ").append(stats.maxNozzleTilt()).append(",\n");
            sb.append("      \"estimatedTerminalVelocity\": ").append(stats.estimatedTerminalVelocity()).append(",\n");
            sb.append("      \"optimalDensity\": ").append(stats.optimalDensity()).append(",\n");
            sb.append("      \"densityBandwidth\": ").append(stats.densityBandwidth()).append(",\n");
            sb.append("      \"massFront\": ").append(stats.massFront()).append(",\n");
            sb.append("      \"massBack\": ").append(stats.massBack()).append(",\n");
            sb.append("      \"volumeFront\": ").append(stats.volumeFront()).append(",\n");
            sb.append("      \"volumeBack\": ").append(stats.volumeBack()).append(",\n");
            sb.append("      \"areaFrontalFront\": ").append(stats.areaFrontalFront()).append(",\n");
            sb.append("      \"areaFrontalBack\": ").append(stats.areaFrontalBack()).append(",\n");
            sb.append("      \"areaLateralFront\": ").append(stats.areaLateralFront()).append(",\n");
            sb.append("      \"areaLateralBack\": ").append(stats.areaLateralBack()).append(",\n");
            sb.append("      \"centerFront\": ").append(jsonVec(stats.centerFront())).append(",\n");
            sb.append("      \"centerBack\": ").append(jsonVec(stats.centerBack())).append(",\n");
            sb.append("      \"totalLateralArea\": ").append(stats.totalLateralArea()).append("\n");
            sb.append("    }\n");

            sb.append("  }");
            return sb.toString();
        }
    }
}