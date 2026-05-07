package com.pixel.create_ordnance.mechanics.scanning;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.pixel.create_ordnance.api.events.ProjectileAnalyzeEvent;
import com.pixel.create_ordnance.config.OrdnanceCommonConfig.ThrustType;
import com.pixel.create_ordnance.config.OrdnanceConfigs;
import com.pixel.create_ordnance.mechanics.physics.core.PhysicsConstants;
import com.pixel.create_ordnance.registry.ProjectileComponentRegistry;
import com.pixel.create_ordnance.registry.ProjectileComponentType;
import com.pixel.create_ordnance.registry.ProjectileData;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

import net.neoforged.neoforge.common.NeoForge;

/**
 * The "Supreme Analyzer" (V3.5).
 * Responsible for validating, interpreting, and calculating physics from raw
 * linear data.
 * Adheres to the "Universal Protocol":
 * 1. Strict Registry Validation (Crash on unknown)
 * 2. Strict Structure Rules (Max 1 Tail, Max 1 Payload)
 * 3. Trimming Logic (Tail=Start, Payload=End)
 * 4. Normalization (Align to UP)
 */
public class ProjectileAnalyzer {

    // Cache: Fingerprint (List<ResourceLocation> Hash) -> ProjectileStats
    private static final Map<Integer, ProjectileStats> STATS_CACHE = new HashMap<>();
    private static long LAST_CACHE_VERSION = -1;

    public static synchronized ProjectileStats analyze(ScrapResult result) {
        // 1. Check Version & Flush Cache
        if (OrdnanceConfigs.CONFIG_VERSION != LAST_CACHE_VERSION) {
            STATS_CACHE.clear();
            LAST_CACHE_VERSION = OrdnanceConfigs.CONFIG_VERSION;
        }

        // 2. Check Cache
        int fingerprint = result.blockIds().hashCode();
        if (STATS_CACHE.containsKey(fingerprint)) {
            return STATS_CACHE.get(fingerprint);
        }

        // 3. Perform Analysis
        ProjectileStats stats = performStrictAnalysis(result.blockIds());

        // 4. FIRE COMPONENT EVENT (V3.5)
        // This allows addons to modify stats (e.g. adding lift, weight)
        ProjectileAnalyzeEvent event = new ProjectileAnalyzeEvent(
                result, stats);
        NeoForge.EVENT_BUS.post(event);
        stats = event.createFinalStats();

        // 5. Cache Result
        STATS_CACHE.put(fingerprint, stats);
        return stats;
    }

    private static ProjectileStats performStrictAnalysis(List<ResourceLocation> rawIds) {
        if (rawIds.isEmpty()) {
            throw new ProjectileAnalysisException("Projectile structure is empty!");
        }

        // --- STEP 1: REGISTRY VALIDATION & MAPPING ---
        List<ProjectileData> validData = new ArrayList<>();
        List<ResourceLocation> validIds = new ArrayList<>(); // Kept for debugging/fingerprinting if needed later

        for (ResourceLocation id : rawIds) {
            Optional<Block> blockOp = BuiltInRegistries.BLOCK.getOptional(id);
            if (blockOp.isEmpty()) {
                throw new ProjectileAnalysisException(
                        "CRITICAL: Block ID '" + id + "' not found in Minecraft Registry!");
            }

            Optional<ProjectileData> dataOp = ProjectileComponentRegistry.getData(blockOp.get());
            if (dataOp.isEmpty()) {
                throw new ProjectileAnalysisException(
                        "CRITICAL: Block '" + id + "' is not registered in Ordnance ProjectileRegistry!");
            }

            validData.add(dataOp.get());
            validIds.add(id);
        }

        // --- STEP 2: STRUCTURE SCAN (Find Anchors & Validate Uniqueness) ---
        int tailIndex = -1;
        int payloadIndex = -1;

        for (int i = 0; i < validData.size(); i++) {
            ProjectileData data = validData.get(i);

            if (data.type() == ProjectileComponentType.TAIL) {
                if (tailIndex != -1) {
                    throw new CriticalGeometryError(
                            "CRITICAL: Multiple TAIL components detected! A projectile can only have ONE engine.");
                }
                tailIndex = i;
            } else if (data.type() == ProjectileComponentType.PAYLOAD) {
                if (payloadIndex != -1) {
                    throw new CriticalGeometryError(
                            "CRITICAL: Multiple PAYLOAD components detected! A projectile can only have ONE warhead.");
                }
                payloadIndex = i;
            }
        }

        if (tailIndex != -1 && payloadIndex != -1 && tailIndex > payloadIndex) {
            throw new CriticalGeometryError(
                    "CRITICAL: Invalid Geometry! TAIL is placed AFTER PAYLOAD. Engines must be at the back.");
        }

        // --- STEP 3: STRICT GEOMETRY VALIDATION (V3.6) ---
        // Engine must be the absolute back, Payload must be the absolute front.
        // This prevents "fainéant" scrappers from grabbing extra components.

        if (tailIndex != -1) {
            for (int i = 0; i < tailIndex; i++) {
                if (ProjectileComponentRegistry.isComponent(BuiltInRegistries.BLOCK.get(rawIds.get(i)))) {
                    throw new CriticalGeometryError(generateDetailedReport(rawIds, validData, tailIndex, payloadIndex,
                            "Component '" + rawIds.get(i) + "' found BEHIND the Engine (Index " + i + ").\n" +
                                    "RULE: The Engine (TAIL) MUST be the point-of-origin (Index 0). Nothing is allowed behind it."));
                }
            }
        }

        if (payloadIndex != -1) {
            for (int i = payloadIndex + 1; i < validData.size(); i++) {
                if (ProjectileComponentRegistry.isComponent(BuiltInRegistries.BLOCK.get(rawIds.get(i)))) {
                    throw new CriticalGeometryError(generateDetailedReport(rawIds, validData, tailIndex, payloadIndex,
                            "Component '" + rawIds.get(i) + "' found AFTER the Payload (Index " + i + ").\n" +
                                    "RULE: The Payload (WARHEAD) MUST be the absolute front-most component. Nothing is allowed after it."));
                }
            }
        }

        // --- STEP 4: TRIMMING (Define Physics Body) ---
        // Start = Tail Index (or 0 if no tail)
        // End = Payload Index (or last index if no payload)

        int startIndex = (tailIndex != -1) ? tailIndex : 0;
        int endIndex = (payloadIndex != -1) ? payloadIndex : validData.size() - 1;

        List<ProjectileData> body = new ArrayList<>();
        for (int i = startIndex; i <= endIndex; i++) {
            body.add(validData.get(i));
        }

        // --- STEP 5: CALCULATION (Physics) ---
        return calculateStats(body);
    }

    private static ProjectileStats calculateStats(List<ProjectileData> body) {
        double totalMass = 0;
        double rX = 0; // Center X at 0.5 (block center)
        double rY = 0;
        double rZ = 0; // Center Z at 0.5 (block center)

        double totalStability = 0;
        double sX = 0;
        double sY = 0;
        double sZ = 0;

        // Fuel
        double totalFuel = 0;
        double totalFuelConsumption = 0;

        // Tail Params
        ThrustType thrustType = ThrustType.NONE;
        double baseThrust = 0;
        double maxNozzleTilt = 0;
        double optDensity = 0;
        double bandWidth = 0;

        boolean hasTail = false;

        // Iterate body. Index 0 is the "Bottom/Back" of the projectile (y=0)
        for (int y = 0; y < body.size(); y++) {
            ProjectileData data = body.get(y);

            double mass = data.weight().get();
            double stability = data.stability_factor().get();

            // CoM Accumulation (Linear Y + Block Center XZ)
            double centerY = y + 0.5;
            totalMass += mass;
            rX += 0.5 * mass;
            rY += centerY * mass;
            rZ += 0.5 * mass;

            // CoP Accumulation
            totalStability += stability;
            sX += 0.5 * stability;
            sY += centerY * stability;
            sZ += 0.5 * stability;

            // Fuel
            if (data.allProperties().containsKey("capacity")) {
                Object capObj = data.allProperties().get("capacity").get();
                if (capObj instanceof Number n)
                    totalFuel += n.doubleValue();
            }
            if (data.fuelEfficiency() != null) {
                totalFuelConsumption += data.fuelEfficiency().get();
            }

            // Tail Extraction (Always at Index 0 if present due to trimming)
            if (data.type() == ProjectileComponentType.TAIL) {
                hasTail = true;
                thrustType = ThrustType.valueOf(getMandatoryString(data, "thrust_type"));
                baseThrust = getMandatoryDouble(data, "base_thrust");
                maxNozzleTilt = getMandatoryDouble(data, "max_nozzle_tilt");
                optDensity = getMandatoryDouble(data, "optimal_density");
                bandWidth = getMandatoryDouble(data, "density_bandwidth");
            }
        }

        if (totalFuel <= 0)
            thrustType = ThrustType.NONE;

        // Finalize Centers
        if (totalMass <= 0)
            throw new ProjectileAnalysisException("Total Mass is <= 0");

        double comY = rY / totalMass;
        double comX = rX / totalMass;
        double comZ = rZ / totalMass;
        Vec3 centerOfMass = new Vec3(comX, comY, comZ);

        Vec3 centerOfPressure;
        if (totalStability > 0) {
            centerOfPressure = new Vec3(sX / totalStability, sY / totalStability, sZ / totalStability);
        } else {
            // Fallback if no stability components (e.g. just raw blocks)
            centerOfPressure = centerOfMass;
        }

        // Terminal Velocity (Placeholder)
        double termVel = 0;

        // --- STEP 6: UNIFIED DUAL-POINT CALCULATION ---
        int bodyCount = body.size();
        double halfCount = bodyCount / 2.0;

        double massBack = 0, massFront = 0;
        double volBack = 0, volFront = 0;
        double rXAreaBack = 0, rXAreaFront = 0;
        double rYAreaBack = 0, rYAreaFront = 0;
        double rZAreaBack = 0, rZAreaFront = 0;
        double latAreaBack = 0, latAreaFront = 0;
        double frontAreaBack = 1.0;
        double frontAreaFront = 1.0;

        double baseVol = PhysicsConstants.BASE_BLOCK_VOLUME;
        for (int i = 0; i < bodyCount; i++) {
            ProjectileData data = body.get(i);
            double blockCenter = i + 0.5;
            double blockMass = data.weight().get();
            double blockVol = baseVol;
            double stability = data.stability_factor().get();

            // V2.1: Decouple priority (Stability) from force (Area) via global scale
            double blockLatArea = (1.0 + stability) * PhysicsConstants.getAeroAreaScale();

            if (i < (int) halfCount) {
                massBack += blockMass;
                volBack += blockVol;
                latAreaBack += blockLatArea;
                rXAreaBack += 0.5 * blockLatArea;
                rYAreaBack += blockCenter * blockLatArea;
                rZAreaBack += 0.5 * blockLatArea;
            } else if (i > (int) halfCount - 1 && i < (int) halfCount + 1 && (halfCount % 1 != 0)) {
                double halfMass = blockMass * 0.5;
                double halfVol = blockVol * 0.5;
                double halfLat = blockLatArea * 0.5;

                massBack += halfMass;
                volBack += halfVol;
                latAreaBack += halfLat;
                rXAreaBack += 0.5 * halfLat;
                rYAreaBack += blockCenter * halfLat;
                rZAreaBack += 0.5 * halfLat;

                massFront += halfMass;
                volFront += halfVol;
                latAreaFront += halfLat;
                rXAreaFront += 0.5 * halfLat;
                rYAreaFront += blockCenter * halfLat;
                rZAreaFront += 0.5 * halfLat;
            } else {
                massFront += blockMass;
                volFront += blockVol;
                latAreaFront += blockLatArea;
                rXAreaFront += 0.5 * blockLatArea;
                rYAreaFront += blockCenter * blockLatArea;
                rZAreaFront += 0.5 * blockLatArea;
            }
        }

        // Finalize centroids
        Vec3 centerBack = new Vec3(
                latAreaBack > 0 ? (rXAreaBack / latAreaBack) - comX : 0.0,
                latAreaBack > 0 ? (rYAreaBack / latAreaBack) - comY : -0.5,
                latAreaBack > 0 ? (rZAreaBack / latAreaBack) - comZ : 0.0);

        Vec3 centerFront = new Vec3(
                latAreaFront > 0 ? (rXAreaFront / latAreaFront) - comX : 0.0,
                latAreaFront > 0 ? (rYAreaFront / latAreaFront) - comY : 0.5,
                latAreaFront > 0 ? (rZAreaFront / latAreaFront) - comZ : 0.0);

        return new ProjectileStats(
                totalMass,
                centerOfMass,
                totalStability,
                centerOfPressure,
                totalFuel,
                totalFuelConsumption,
                thrustType,
                baseThrust,
                maxNozzleTilt,
                termVel,
                optDensity,
                bandWidth,
                massFront,
                massBack,
                volFront,
                volBack,
                frontAreaFront,
                frontAreaBack,
                latAreaFront,
                latAreaBack,
                centerFront,
                centerBack,
                latAreaFront + latAreaBack); // Total Lateral
    }

    private static String generateDetailedReport(List<ResourceLocation> ids, List<ProjectileData> data, int tailIdx,
            int payloadIdx, String violation) {
        StringBuilder sb = new StringBuilder();
        sb.append("VIOLATION: ").append(violation).append("\n\n");
        sb.append("--- PROJECTILE SCAN SUMMARY ---\n");
        sb.append("Total Blocks Scanned: ").append(ids.size()).append("\n");
        sb.append("Engine (Tail) Index: ").append(tailIdx == -1 ? "MISSING" : tailIdx).append("\n");
        sb.append("Warhead (Payload) Index: ").append(payloadIdx == -1 ? "MISSING" : payloadIdx).append("\n");
        sb.append("\n--- DETAILED COMPONENT MAP ---\n");

        for (int i = 0; i < ids.size(); i++) {
            String prefix = "  [" + i + "] ";
            sb.append(prefix).append(ids.get(i));

            if (i == tailIdx)
                sb.append(" <--- [ENGINE/TAIL]");
            if (i == payloadIdx)
                sb.append(" <--- [WARHEAD/PAYLOAD]");

            // Highlight blocks "outside" the bounds
            if (tailIdx != -1 && i < tailIdx)
                sb.append(" !! ILLEGAL POSITION (BEHIND ENGINE) !!");
            if (payloadIdx != -1 && i > payloadIdx)
                sb.append(" !! ILLEGAL POSITION (AFTER PAYLOAD) !!");

            sb.append("\n");
        }

        return sb.toString();
    }

    private static double getMandatoryDouble(ProjectileData data, String key) {
        if (!data.allProperties().containsKey(key))
            throw new ProjectileAnalysisException("Missing property '" + key + "' on component " + data.type());
        Object val = data.allProperties().get(key).get();
        if (val instanceof Number n)
            return n.doubleValue();
        throw new ProjectileAnalysisException("Property '" + key + "' is not a number!");
    }

    private static String getMandatoryString(ProjectileData data, String key) {
        if (!data.allProperties().containsKey(key))
            throw new ProjectileAnalysisException("Missing property '" + key + "' on component " + data.type());
        Object val = data.allProperties().get(key).get();
        return val.toString();
    }
}