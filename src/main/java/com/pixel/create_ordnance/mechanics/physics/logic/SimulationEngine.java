package com.pixel.create_ordnance.mechanics.physics.logic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.pixel.create_ordnance.CreateOrdnance;
import com.pixel.create_ordnance.config.OrdnanceConfigs;
import com.pixel.create_ordnance.content.entity.ProjectileEntity;
import com.pixel.create_ordnance.mechanics.physics.core.PhysicsConstants;
import com.pixel.create_ordnance.mechanics.physics.core.PhysicsContext;
import com.pixel.create_ordnance.mechanics.physics.core.PhysicsEngine;
import com.pixel.create_ordnance.mechanics.physics.core.PhysicsRegistry;
import com.pixel.create_ordnance.mechanics.physics.core.PhysicsRegistry.PhysicsProfile;
import com.pixel.create_ordnance.mechanics.physics.core.ProjectileState;
import com.pixel.create_ordnance.mechanics.scanning.ProjectileStats;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * High-level simulation orchestrator (V2 Protocol).
 * Bridges ProjectileEntity and PhysicsEngine.
 *
 * Two modes:
 * - Normal: speed below threshold, full tick in 1 step.
 * - Partial: speed above threshold, trajectory split into N segments,
 * 1 segment checked per server tick, chunk loaded on-demand.
 */
public class SimulationEngine {

    private static final SimulationEngine INSTANCE = new SimulationEngine();
    private final Map<UUID, PartialSimState> partialStates = new HashMap<>();
    private final BlackBox blackBox = new BlackBox();

    public static SimulationEngine getInstance() {
        return INSTANCE;
    }

    private SimulationEngine() {
        PhysicsEngine.getInstance().setBlackBox(blackBox);
    }

    public BlackBox getBlackBox() {
        return blackBox;
    }

    // =========================================================
    // MAIN ENTRY POINT
    // =========================================================

    public void tick(ProjectileEntity entity) {
        if (entity.level().isClientSide)
            return;

        // V6.2: Apply special interaction rules before physics
        PhysicsInteractionRules.applyRules(entity);

        ProjectileState currentState = getState(entity);
        if (currentState == ProjectileState.EXPLODED)
            return;
        if (currentState == ProjectileState.STUCK)
            return;

        UUID entityUUID = entity.getUUID();
        if (partialStates.containsKey(entityUUID)) {
            tickPartialContinue(entity, partialStates.get(entityUUID));
            return;
        }

        double speed = entity.getDeltaMovement().length();
        double threshold = PhysicsConstants.getPartialTickVelocityThreshold();

        if (speed >= threshold && PhysicsConstants.isPartialTicksEnabled()) {
            tickPartialStart(entity, speed, threshold);
        } else {
            tickNormal(entity);
        }
    }

    // =========================================================
    // MODE NORMAL
    // =========================================================

    private void tickNormal(ProjectileEntity entity) {
        PhysicsContext context = buildContext(entity, 1.0);

        PhysicsEngine.TickResult result = PhysicsEngine.getInstance().tick(context);

        Vec3 newVelocityLocal = integrateLinear(context, result.netForce());
        Vector3f newAngularVelocityLocal = integrateAngular(context, result.netTorque());
        double newFuel = consumeFuel(entity, context);

        // --- PROTOCOL V6.6: Unified Collision Check ---
        // Calculate world velocity for collision check
        Quaternionf orientation = new Quaternionf(entity.getOrientation());
        Vector3f velWorldF = new Vector3f((float) newVelocityLocal.x, (float) newVelocityLocal.y,
                (float) newVelocityLocal.z);
        orientation.transform(velWorldF);
        Vec3 newVelocityWorld = new Vec3(velWorldF.x, velWorldF.y, velWorldF.z);

        Vec3 startPos = entity.position();
        Vec3 endPos = startPos.add(newVelocityWorld);
        ServerLevel level = (ServerLevel) entity.level();

        if (checkSegmentCollision(level, startPos, endPos)) {
            entity.setPos(endPos.x, endPos.y, endPos.z);
            entity.impact();
            blackBox.recordImpactPartition(0);
            return;
        }

        applyToEntity(entity, newVelocityLocal, newAngularVelocityLocal, newFuel, context.deltaTime);
        evaluateStateTransitions(entity, newVelocityLocal);

        // V6.5: Updated "Sticky" Viscosity Hysteresis (Decay logic)
        double currentEnvVisc = context.getEnvironmentViscosity();
        double previousResidVal = entity.getResidualViscosity();
        double decayFactor = PhysicsConstants.getStickyViscosityDecay();

        // Decay the residual but never below the current environment
        double newResidual = Math.max(currentEnvVisc, previousResidVal * decayFactor);
        entity.setResidualViscosity(newResidual);
    }

    // =========================================================
    // MODE PARTIAL — START
    // =========================================================

    private void tickPartialStart(ProjectileEntity entity, double speed, double threshold) {
        PhysicsContext context = buildContext(entity, 1.0);
        PhysicsEngine.TickResult result = PhysicsEngine.getInstance().tick(context);

        Vec3 fullTickVelocityLocal = integrateLinear(context, result.netForce());
        Vector3f fullTickAngVel = integrateAngular(context, result.netTorque());
        double fullTickFuel = consumeFuel(entity, context);

        // --- Geometric Fix: Rotate Velocity to World Space ---
        Quaternionf orientation = context.orientation;
        Vector3f velWorldF = new Vector3f((float) fullTickVelocityLocal.x, (float) fullTickVelocityLocal.y,
                (float) fullTickVelocityLocal.z);
        orientation.transform(velWorldF);
        Vec3 fullTickVelocityWorld = new Vec3(velWorldF.x, velWorldF.y, velWorldF.z);

        double fullDistance = fullTickVelocityWorld.length();
        int partitionCount = (int) Math.ceil(fullDistance / threshold);
        partitionCount = Math.max(2, partitionCount);
        partitionCount = Math.min(partitionCount, PhysicsConstants.getMaxChunkLoaded());

        Vec3 startPos = entity.position();
        Vec3 step = fullTickVelocityWorld.scale(1.0 / partitionCount);
        List<Vec3> waypoints = new ArrayList<>();
        waypoints.add(startPos);

        for (int i = 1; i <= partitionCount; i++) {
            waypoints.add(startPos.add(step.scale(i)));
        }

        PartialSimState state = new PartialSimState(
                waypoints, 0,
                fullTickVelocityWorld, fullTickAngVel,
                fullTickFuel, new Quaternionf(entity.getOrientation()));

        partialStates.put(entity.getUUID(), state);
        tickPartialContinue(entity, state);
    }

    // =========================================================
    // MODE PARTIAL — CONTINUE (1 segment per tick)
    // =========================================================

    private void tickPartialContinue(ProjectileEntity entity, PartialSimState state) {
        UUID entityUUID = entity.getUUID();
        ServerLevel level = (ServerLevel) entity.level();

        int nextPartition = state.currentPartition + 1;
        if (nextPartition >= state.waypoints.size()) {
            finalizePartialSim(entity, state);
            partialStates.remove(entityUUID);
            return;
        }

        Vec3 segmentStart = state.waypoints.get(state.currentPartition);
        Vec3 segmentEnd = state.waypoints.get(nextPartition);

        ChunkPos destChunk = new ChunkPos(BlockPos.containing(segmentEnd));
        boolean chunkWasForced = false;

        if (!level.hasChunk(destChunk.x, destChunk.z)) {
            if (PhysicsConstants.isChunkloaderEnabled()) {
                forceLoadChunk(level, destChunk, entity, true);
                chunkWasForced = true;
            } else {
                state.currentPartition = nextPartition;
                return;
            }
        }

        boolean collisionDetected = checkSegmentCollision(level, segmentStart, segmentEnd);

        if (collisionDetected) {
            entity.setPos(segmentEnd.x, segmentEnd.y, segmentEnd.z);
            entity.impact();
            blackBox.recordImpactPartition(nextPartition);
            partialStates.remove(entityUUID);
            if (chunkWasForced) {
                forceLoadChunk(level, destChunk, entity, false);
            }
            return;
        }

        if (chunkWasForced) {
            forceLoadChunk(level, destChunk, entity, false);
        }

        state.currentPartition = nextPartition;

        // Player resync: if final chunk is already loaded, fast-forward
        ChunkPos finalChunk = new ChunkPos(BlockPos.containing(
                state.waypoints.get(state.waypoints.size() - 1)));
        if (level.hasChunk(finalChunk.x, finalChunk.z)) {
            for (int i = state.currentPartition + 1; i < state.waypoints.size(); i++) {
                Vec3 checkStart = state.waypoints.get(i - 1);
                Vec3 checkEnd = state.waypoints.get(i);
                if (checkSegmentCollision(level, checkStart, checkEnd)) {
                    entity.setPos(checkEnd.x, checkEnd.y, checkEnd.z);
                    entity.impact();
                    blackBox.recordImpactPartition(i);
                    partialStates.remove(entityUUID);
                    return;
                }
            }
            finalizePartialSim(entity, state);
            partialStates.remove(entityUUID);
        }

        // V6.5: Updated "Sticky" Viscosity Hysteresis (Decay logic)
        double currentEnvVisc = PhysicsRegistry
                .get(entity.level().getBlockState(BlockPos.containing(entity.position()))).viscosity();
        double previousResidVal = entity.getResidualViscosity();
        double decayFactor = PhysicsConstants.getStickyViscosityDecay();
        double newResidual = Math.max(currentEnvVisc, previousResidVal * decayFactor);
        entity.setResidualViscosity(newResidual);
    }

    // =========================================================
    // PARTIAL — FINALIZE
    // =========================================================

    private void finalizePartialSim(ProjectileEntity entity, PartialSimState state) {
        Vec3 finalPos = state.waypoints.get(state.waypoints.size() - 1);

        entity.setPos(finalPos.x, finalPos.y, finalPos.z);
        entity.setDeltaMovement(state.simulatedVelocity);
        entity.setAngularVelocity(state.simulatedAngularVelocity);
        entity.setOrientationWithPrev(state.simulatedOrientation);
        entity.setCurrentFuel(state.simulatedFuel);

        // --- Visual Fix: Sync Euler angles for Create Mod ---
        Vector3f noseDir = new Vector3f(0, 1, 0);
        state.simulatedOrientation.transform(noseDir);
        float yawDeg = (float) Math.toDegrees(Math.atan2(-noseDir.x, noseDir.z));
        float pitchDeg = (float) Math.toDegrees(Math.asin(-noseDir.y));
        entity.setYRot(yawDeg);
        entity.setXRot(pitchDeg);

        evaluateStateTransitions(entity, state.simulatedVelocity);

        if (OrdnanceConfigs.COMMON.divers.debugMode.get()) {
            CreateOrdnance.LOGGER.debug(
                    "[SimEngine] Partial sim finalized for {} — {} partitions, final pos: {}",
                    entity.getId(), state.waypoints.size(), finalPos);
        }
    }

    // =========================================================
    // PHYSICS INTEGRATION
    // =========================================================

    private Vec3 integrateLinear(PhysicsContext context, Vec3 netForceLocal) {
        if (context.totalMass <= PhysicsConstants.AOA_SAFETY_EPSILON) {
            return context.velocityLocal;
        }
        Vec3 accelerationLocal = netForceLocal.scale(1.0 / context.totalMass);
        return context.velocityLocal.add(accelerationLocal.scale(context.deltaTime));
    }

    private Vector3f integrateAngular(PhysicsContext context, Vec3 netTorqueLocal) {
        double comToFrontY = context.stats.centerFront().y;
        double comToBackY = context.stats.centerBack().y;
        double length = comToFrontY - comToBackY;

        if (length < PhysicsConstants.AOA_SAFETY_EPSILON)
            length = 1.0;

        // I = 1/12 * M * L^2 (approximated as a rod for pitch/yaw)
        double momentOfInertia = context.totalMass * length * length / 12.0;

        if (momentOfInertia <= PhysicsConstants.AOA_SAFETY_EPSILON) {
            return new Vector3f(context.angularVelocity);
        }

        float dt = (float) context.deltaTime;
        float invI = (float) (1.0 / momentOfInertia);

        return new Vector3f(
                context.angularVelocity.x + (float) netTorqueLocal.x * invI * dt,
                context.angularVelocity.y + (float) netTorqueLocal.y * invI * dt,
                context.angularVelocity.z + (float) netTorqueLocal.z * invI * dt);
    }

    // =========================================================
    // ENTITY STATE APPLICATION
    // =========================================================

    private void applyToEntity(ProjectileEntity entity, Vec3 newVelocityLocal,
            Vector3f newAngularVelocityLocal, double newFuel, double deltaTime) {

        // 1. Rotate VELOCITY back to World Space for Entity movement
        Quaternionf orientation = new Quaternionf(entity.getOrientation());
        Vector3f velWorld = new Vector3f((float) newVelocityLocal.x, (float) newVelocityLocal.y,
                (float) newVelocityLocal.z);
        orientation.transform(velWorld);
        Vec3 newVelocityWorld = new Vec3(velWorld.x, velWorld.y, velWorld.z);

        entity.setDeltaMovement(newVelocityWorld);
        entity.setAngularVelocity(newAngularVelocityLocal);

        // 2. Quaternion integration: dq/dt = 0.5 * omega_world * q * dt
        // Standard practice: integrate using WORLD angular velocity for global
        // persistence
        float dt = (float) deltaTime;
        float halfDt = dt * 0.5f;

        Vector3f worldAngVel = new Vector3f(newAngularVelocityLocal);
        orientation.transform(worldAngVel);

        Quaternionf spinQuat = new Quaternionf(
                worldAngVel.x * halfDt,
                worldAngVel.y * halfDt,
                worldAngVel.z * halfDt,
                0);
        spinQuat.mul(orientation);

        orientation.set(
                orientation.x + spinQuat.x,
                orientation.y + spinQuat.y,
                orientation.z + spinQuat.z,
                orientation.w + spinQuat.w);
        orientation.normalize();

        entity.setOrientationWithPrev(orientation);

        // 3. Sync Euler angles for Create Mod (based on Nose direction +Y)
        Vector3f noseDir = new Vector3f(0, 1, 0);
        orientation.transform(noseDir);
        float yawDeg = (float) Math.toDegrees(Math.atan2(-noseDir.x, noseDir.z));
        float pitchDeg = (float) Math.toDegrees(Math.asin(-noseDir.y));
        entity.setYRot(yawDeg);
        entity.setXRot(pitchDeg);

        // 4. Move entity (World space)
        entity.move(net.minecraft.world.entity.MoverType.SELF, newVelocityWorld);

        // 5. Fuel
        entity.setCurrentFuel(newFuel);
    }

    // =========================================================
    // CONTEXT CONSTRUCTION
    // =========================================================

    private PhysicsContext buildContext(ProjectileEntity entity, double deltaTime) {
        ProjectileStats stats = entity.getStats();
        Vec3 pos = entity.position();
        Quaternionf orientation = new Quaternionf(entity.getOrientation());

        // 1. Velocity Transformation (World -> Local)
        Vec3 velWorld = entity.getDeltaMovement();
        Vector3f velLocalF = new Vector3f((float) velWorld.x, (float) velWorld.y, (float) velWorld.z);
        Quaternionf invOrientation = new Quaternionf(orientation).invert();
        invOrientation.transform(velLocalF);
        Vec3 velocityLocal = new Vec3(velLocalF.x, velLocalF.y, velLocalF.z);

        // 2. Sample Environment at 3 points: CoM, Front Center, Back Center
        BlockState blockAtCom = entity.level().getBlockState(BlockPos.containing(pos));
        PhysicsProfile envProfile = PhysicsRegistry.get(blockAtCom);

        // Calculate world-space positions for front/back points
        Vector3f frontLocal = new Vector3f((float) stats.centerFront().x, (float) stats.centerFront().y,
                (float) stats.centerFront().z);
        Vector3f backLocal = new Vector3f((float) stats.centerBack().x, (float) stats.centerBack().y,
                (float) stats.centerBack().z);

        orientation.transform(frontLocal);
        orientation.transform(backLocal);

        Vec3 frontPos = pos.add(frontLocal.x, frontLocal.y, frontLocal.z);
        Vec3 backPos = pos.add(backLocal.x, backLocal.y, backLocal.z);

        PhysicsProfile frontProfile = PhysicsRegistry.get(entity.level().getBlockState(BlockPos.containing(frontPos)));
        PhysicsProfile backProfile = PhysicsRegistry.get(entity.level().getBlockState(BlockPos.containing(backPos)));

        double angleOfAttack = computeAngleOfAttack(entity);
        ProjectileState state = getState(entity);

        PhysicsContext ctx = new PhysicsContext(
                entity,
                pos,
                velWorld,
                velocityLocal,
                orientation,
                new Vector3f(entity.getAngularVelocity()),
                entity.getUUID(),
                entity.getScrapResult(),
                deltaTime,
                stats,
                entity.getProjectileScale(),
                envProfile,
                frontProfile,
                backProfile,
                frontPos,
                backPos,
                entity.getCurrentFuel(),
                entity.getFuelConsumption(),
                entity.getThrustType(),
                entity.getPitchDemand(),
                entity.getYawDemand(),
                entity.getRollDemand(),
                entity.getThrottle(),
                angleOfAttack,
                state,
                entity.isExtinguished(),
                Vec3.ZERO);

        // Populate Telemetry
        ctx.totalChunksLoadedCount = entity.getTotalChunksLoadedCount();
        if (partialStates.containsKey(entity.getUUID())) {
            PartialSimState ps = partialStates.get(entity.getUUID());
            ctx.partitionCount = ps.waypoints.size() - 1;
            ctx.currentPartition = ps.currentPartition;
            ctx.currentlyLoadedChunksCount = ps.forcedChunks.size() + 1; // +1 for the chunk it is technically in
        } else {
            ctx.currentlyLoadedChunksCount = 1;
        }

        return ctx;
    }

    // =========================================================
    // COMPUTED INPUTS
    // =========================================================

    private double computeAngleOfAttack(ProjectileEntity entity) {
        Vec3 velocity = entity.getDeltaMovement();
        double speed = velocity.length();
        if (speed < PhysicsConstants.AOA_SAFETY_EPSILON)
            return 0.0;

        Vector3f nose = new Vector3f(0, 1, 0);
        entity.getOrientation().transform(nose);
        Vec3 noseDir = new Vec3(nose.x, nose.y, nose.z);

        double dot = noseDir.dot(velocity.normalize());
        dot = Math.max(-1.0, Math.min(1.0, dot));
        return Math.acos(dot);
    }

    // =========================================================
    // STATE MACHINE
    // =========================================================

    private void evaluateStateTransitions(ProjectileEntity entity, Vec3 velocity) {
        double speed = velocity.length();
        if (speed > PhysicsConstants.getVelocityLimit()) {
            CreateOrdnance.LOGGER.warn(
                    "[SimEngine] Projectile {} exceeded velocity limit ({} > {}). Discarding.",
                    entity.getId(), speed, PhysicsConstants.getVelocityLimit());
            entity.discard();
            return;
        }

        if (Double.isNaN(speed) || !Double.isFinite(speed)) {
            CreateOrdnance.LOGGER.error(
                    "[SimEngine] NaN/Infinity detected for projectile {}! Discarding.",
                    entity.getId());
            entity.discard();
        }
    }

    private ProjectileState getState(ProjectileEntity entity) {
        return ProjectileState.FLYING;
    }

    // =========================================================
    // FUEL
    // =========================================================

    private double consumeFuel(ProjectileEntity entity, PhysicsContext ctx) {
        if (ctx.currentFuel <= 0)
            return 0;
        double consumed = ctx.fuelConsumption * ctx.throttle * ctx.deltaTime;
        return Math.max(0, ctx.currentFuel - consumed);
    }

    // =========================================================
    // COLLISION
    // =========================================================

    private boolean checkSegmentCollision(ServerLevel level, Vec3 start, Vec3 end) {
        double dist = start.distanceTo(end);
        int steps = Math.max(1, (int) Math.ceil(dist));
        Vec3 sv = end.subtract(start).scale(1.0 / steps);
        for (int i = 1; i <= steps; i++) {
            Vec3 cp = start.add(sv.scale(i));
            BlockPos bp = BlockPos.containing(cp);
            if (!level.isLoaded(bp))
                continue;
            BlockState bs = level.getBlockState(bp);
            if (bs.isAir())
                continue;
            PhysicsProfile pr = PhysicsRegistry.get(bs);
            if (pr.isFluid())
                continue;
            return true;
        }
        return false;
    }

    // =========================================================
    // CHUNKLOADER
    // =========================================================

    private void forceLoadChunk(ServerLevel level, ChunkPos chunk, ProjectileEntity entity, boolean load) {
        level.setChunkForced(chunk.x, chunk.z, load);
        UUID uuid = entity.getUUID();
        if (load) {
            entity.incrementTotalChunksLoaded();
            if (partialStates.containsKey(uuid)) {
                partialStates.get(uuid).forcedChunks.add(chunk);
            }
        } else {
            if (partialStates.containsKey(uuid)) {
                partialStates.get(uuid).forcedChunks.remove(chunk);
            }
        }
        if (OrdnanceConfigs.COMMON.divers.debugMode.get()) {
            CreateOrdnance.LOGGER.debug("[SimEngine] {} chunk ({}, {}) for {}",
                    load ? "Loading" : "Unloading", chunk.x, chunk.z, uuid);
        }
    }

    // =========================================================
    // CLEANUP
    // =========================================================

    public void onProjectileRemoved(UUID uuid) {
        partialStates.remove(uuid);
    }

    // =========================================================
    // PARTIAL SIM STATE
    // =========================================================

    private static class PartialSimState {
        final List<Vec3> waypoints;
        int currentPartition;
        final Vec3 simulatedVelocity;
        final Vector3f simulatedAngularVelocity;
        final double simulatedFuel;
        final Quaternionf simulatedOrientation;
        final List<ChunkPos> forcedChunks = new ArrayList<>();

        PartialSimState(List<Vec3> wp, int cp,
                Vec3 sv, Vector3f sav,
                double sf, Quaternionf so) {
            this.waypoints = wp;
            this.currentPartition = cp;
            this.simulatedVelocity = sv;
            this.simulatedAngularVelocity = sav;
            this.simulatedFuel = sf;
            this.simulatedOrientation = so;
        }
    }
}