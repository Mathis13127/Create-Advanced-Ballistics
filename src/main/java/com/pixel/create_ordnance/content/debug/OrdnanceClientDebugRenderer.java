package com.pixel.create_ordnance.content.debug;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.pixel.create_ordnance.CreateOrdnance;
import com.pixel.create_ordnance.config.OrdnanceConfigs;
import com.pixel.create_ordnance.mechanics.scanning.CurrentProjectileManager;
import com.pixel.create_ordnance.mechanics.scanning.ProjectileAnalyzer;
import com.pixel.create_ordnance.mechanics.scanning.ProjectileStats;
import com.pixel.create_ordnance.mechanics.scanning.ScrapResult;
import com.pixel.create_ordnance.network.ClientboundAttractionSyncPacket.AttractionData;
import com.pixel.create_ordnance.registry.ModItems;

import com.simibubi.create.content.equipment.goggles.GogglesItem;

import net.createmod.catnip.outliner.Outliner;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = CreateOrdnance.MODID, value = Dist.CLIENT)
public class OrdnanceClientDebugRenderer {

    private static AABB targetBox = null;
    private static AABB comBox = null; // Blue
    private static AABB copBox = null; // Violet
    private static int ticksRemaining = 0;

    // Attraction Source Debug Data
    private static final Map<Integer, SyncedAttraction> ATTRACTORS = new HashMap<>();

    private static class SyncedAttraction {
        final Vec3 pos;
        final double range;
        int ttl; // Time to live in ticks

        SyncedAttraction(Vec3 pos, double range) {
            this.pos = pos;
            this.range = range;
            this.ttl = 100; // Last for 5 seconds (100 ticks) if no updates
        }
    }

    public static void receiveAttractionSync(List<AttractionData> sources) {
        for (AttractionData data : sources) {
            ATTRACTORS.put(data.id(), new SyncedAttraction(data.pos(), data.range()));
        }
    }

    private static final double BOX_HALF_SIZE_COM = 0.325;
    private static final double BOX_HALF_SIZE_COP = 0.35;

    public static void setDebugBox(BlockPos min, BlockPos max, int durationTicks, Vec3 scanPos, Vec3 centerOfMass,
            Vec3 centerOfPressure) {
        // Red Box: Scanned Area
        targetBox = new AABB(min).minmax(new AABB(max)).inflate(0.05);

        // Blue Box: Center of Mass
        comBox = createBox(centerOfMass, BOX_HALF_SIZE_COM);

        // Violet Box: Center of Pressure
        copBox = createBox(centerOfPressure, BOX_HALF_SIZE_COP);

        ticksRemaining = durationTicks;
    }

    private static AABB createBox(Vec3 center, double halfSize) {
        return new AABB(
                center.x - halfSize, center.y - halfSize, center.z - halfSize,
                center.x + halfSize, center.y + halfSize, center.z + halfSize);
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null)
            return;

        boolean debugMode = OrdnanceConfigs.COMMON.divers.debugMode.get();

        // 1. Process Attraction Sources
        if (debugMode) {
            Iterator<Map.Entry<Integer, SyncedAttraction>> it = ATTRACTORS.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Integer, SyncedAttraction> entry = it.next();
                SyncedAttraction sa = entry.getValue();

                // 1. Render Spherical Range (Wireframe)
                renderSphere("attraction_range_" + entry.getKey(), sa.pos, sa.range, 0xFFFF00, 1 / 16f);

                // 2. Render Center Point
                Outliner.getInstance()
                        .chaseAABB("attraction_center_" + entry.getKey(), new AABB(sa.pos, sa.pos).inflate(0.25))
                        .colored(0xFFAA00) // Orange
                        .lineWidth(1 / 16f);

                sa.ttl--;
                if (sa.ttl <= 0)
                    it.remove();
            }
        }

        // 2. Process Scrapper Debug
        boolean holdingTool = mc.player.isHolding(ModItems.ORDNANCE_DEBUGGER.get());
        boolean wearingGoggles = GogglesItem.isWearingGoggles(mc.player);

        if (debugMode) {
            if (ticksRemaining > 0 && targetBox != null) {
                Outliner.getInstance().chaseAABB("scrapper_scan", targetBox)
                        .colored(0xFF0000) // Red
                        .lineWidth(1 / 32f);

                if (comBox != null) {
                    Outliner.getInstance().chaseAABB("scrapper_com", comBox)
                            .colored(0x0000FF) // Blue
                            .lineWidth(1 / 32f);
                }
                if (copBox != null) {
                    Outliner.getInstance().chaseAABB("scrapper_cop", copBox)
                            .colored(0x9900FF) // Violet
                            .lineWidth(1 / 32f);
                }

                ticksRemaining--;
                if (ticksRemaining <= 0) {
                    targetBox = null;
                    comBox = null;
                    copBox = null;
                }
            }

            if (wearingGoggles) {
                ScrapResult result = CurrentProjectileManager.getActiveProjectile();
                if (result != null && result.minPos() != null && result.maxPos() != null) {
                    AABB passiveBox = new AABB(result.minPos()).minmax(new AABB(result.maxPos())).inflate(0.05);
                    Outliner.getInstance().chaseAABB("goggles_scan", passiveBox)
                            .colored(0xFF0000)
                            .lineWidth(1 / 32f);

                    // Dynamic CoM/CoP for goggles
                    ProjectileStats stats = ProjectileAnalyzer
                            .analyze(result);
                    Vec3 tailCenter = Vec3.atCenterOf(result.minPos());
                    Vec3 facingVec = Vec3.atLowerCornerOf(result.worldFacing().getNormal());

                    double comOffset = stats.centerOfMass().y - 0.5;
                    double copOffset = stats.centerOfPressure().y - 0.5;

                    Vec3 worldCoM = tailCenter.add(facingVec.scale(comOffset));
                    Vec3 worldCoP = tailCenter.add(facingVec.scale(copOffset));

                    Outliner.getInstance().chaseAABB("goggles_com", createBox(worldCoM, BOX_HALF_SIZE_COM))
                            .colored(0x0000FF)
                            .lineWidth(1 / 32f);
                    Outliner.getInstance().chaseAABB("goggles_cop", createBox(worldCoP, BOX_HALF_SIZE_COP))
                            .colored(0x9900FF)
                            .lineWidth(1 / 32f);
                }
            }
        }
    }

    private static void renderSphere(String id, Vec3 center, double radius, int color, float lineWidth) {
        int segments = 24;
        int verticalRings = 4; // 4 rings = 8 longitudinal "bars"
        int horizontalRings = 3; // Top, Mid, Bottom latitudes

        // Vertical Rings (Longitudes)
        for (int r = 0; r < verticalRings; r++) {
            double ringAngle = r * (Math.PI / verticalRings);
            for (int i = 0; i < segments; i++) {
                double angle = i * (2 * Math.PI / segments);
                double nextAngle = (i + 1) * (2 * Math.PI / segments);

                Vec3 p1 = center.add(
                        radius * Math.cos(angle) * Math.cos(ringAngle),
                        radius * Math.sin(angle),
                        radius * Math.cos(angle) * Math.sin(ringAngle));
                Vec3 p2 = center.add(
                        radius * Math.cos(nextAngle) * Math.cos(ringAngle),
                        radius * Math.sin(nextAngle),
                        radius * Math.cos(nextAngle) * Math.sin(ringAngle));

                Outliner.getInstance().showLine(id + "_v_" + r + "_" + i, p1, p2)
                        .colored(color)
                        .lineWidth(lineWidth);
            }
        }

        // Horizontal Rings (Latitudes)
        for (int r = 0; r < horizontalRings; r++) {
            double latAngle = (r - 1) * (Math.PI / 4); // -45, 0, 45 degrees
            double latRadius = radius * Math.cos(latAngle);
            double yOffset = radius * Math.sin(latAngle);

            for (int i = 0; i < segments; i++) {
                double angle = i * (2 * Math.PI / segments);
                double nextAngle = (i + 1) * (2 * Math.PI / segments);

                Vec3 p1 = center.add(Math.cos(angle) * latRadius, yOffset, Math.sin(angle) * latRadius);
                Vec3 p2 = center.add(Math.cos(nextAngle) * latRadius, yOffset, Math.sin(nextAngle) * latRadius);

                Outliner.getInstance().showLine(id + "_h_" + r + "_" + i, p1, p2)
                        .colored(color)
                        .lineWidth(lineWidth);
            }
        }
    }
}