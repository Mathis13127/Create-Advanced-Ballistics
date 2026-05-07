package com.pixel.create_ordnance.content.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.mojang.blaze3d.vertex.PoseStack;

import com.pixel.create_ordnance.CreateOrdnance;
import com.pixel.create_ordnance.api.events.ProjectileDeathEvent;
import com.pixel.create_ordnance.api.events.ProjectileLaunchEvent;
import com.pixel.create_ordnance.api.events.ProjectilePreTickEvent;
import com.pixel.create_ordnance.config.OrdnanceCommonConfig.ThrustType;
import com.pixel.create_ordnance.config.OrdnanceConfigs;
import com.pixel.create_ordnance.content.contraption.ProjectileContraption;
import com.pixel.create_ordnance.mechanics.cgs.CGSManager;
import com.pixel.create_ordnance.mechanics.physics.logic.SimulationEngine;
import com.pixel.create_ordnance.mechanics.scanning.ProjectileAnalyzer;
import com.pixel.create_ordnance.mechanics.scanning.ProjectileStats;
import com.pixel.create_ordnance.mechanics.scanning.ScrapResult;
import com.pixel.create_ordnance.registry.ModEntities;
import com.pixel.create_ordnance.registry.ProjectileData;

import com.simibubi.create.content.contraptions.OrientedContraptionEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;

import net.minecraft.nbt.CompoundTag;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;

import net.minecraft.util.Mth;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.common.NeoForge;

import org.joml.Quaternionf;
import org.joml.Vector3f;

public class ProjectileEntity extends OrientedContraptionEntity {

    // ============================================================
    // PROJECTILE DATA (Injected from ScrapResult, stored in NBT)
    // ============================================================
    // These values are set once at spawn and persist in NBT.
    // Physics modules read these values directly.
    // Accessible via: /data get entity @s ProjectileData
    // ============================================================

    // --- Static Data (from ScrapResult, never changes during flight) ---
    private static final EntityDataAccessor<Quaternionf> ORIENTATION = SynchedEntityData.defineId(
            ProjectileEntity.class,
            EntityDataSerializers.QUATERNION);

    /** Standardized Control Demands (CGS Range: -1.0 to 1.0) */
    private static final EntityDataAccessor<Float> PITCH_DEMAND = SynchedEntityData.defineId(
            ProjectileEntity.class,
            EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> YAW_DEMAND = SynchedEntityData.defineId(
            ProjectileEntity.class,
            EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> ROLL_DEMAND = SynchedEntityData.defineId(
            ProjectileEntity.class,
            EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> THROTTLE = SynchedEntityData.defineId(
            ProjectileEntity.class,
            EntityDataSerializers.FLOAT);

    // --- PROTOCOL V3.5: ID Persistence & Config Sync ---
    protected ScrapResult scrapResult;
    public long lastConfigVersion = -1;

    // PHYSICAL STATS (Updated dynamically on config reload)
    protected int projectileLength;
    protected int totalBlockCount;
    protected float projectileScale;
    protected BlockPos scanPos;
    protected double initialYaw;
    protected double initialPitch;

    protected double totalMass;
    protected Vec3 centerOfMass = Vec3.ZERO;
    protected double totalStability;
    protected Vec3 centerOfPressure = Vec3.ZERO;
    protected double maxFuel;
    protected double currentFuel;
    protected double fuelConsumption;
    protected ThrustType thrustType;

    // --- Physical parameters used by Guidance & Analysis ---
    public double baseThrust;
    public double maxNozzleTilt;
    public double optimalDensity;
    public double densityBandwidth;
    public double totalLateralArea;

    // Unified Dual-Point Stats (V5.0)
    protected double massFront;
    protected double massBack;
    protected double volumeFront;
    protected double volumeBack;
    protected double areaFrontalFront;
    protected double areaFrontalBack;
    protected double areaLateralFront;
    protected double areaLateralBack;
    protected Vec3 centerFront = Vec3.ZERO;
    protected Vec3 centerBack = Vec3.ZERO;

    // --- PROTOCOL V6.2: Logic Flags ---
    protected boolean isExtinguished = false;
    protected double residualViscosity = 0.02;
    protected int totalChunksLoadedCount = 0; // BlackBox Telemetry

    // --- COMPAT: Vista TV Link (nullable, client reads from NBT) ---
    protected BlockPos linkedTvPos = null;

    /**
     * Pending TV link position, set before spawn() to be injected
     * before the entity enters the world and its sync packet is sent.
     */
    private static BlockPos pendingLinkedTvPos = null;

    protected ProjectileVisualManager visualManager;
    Quaternionf orientation = new Quaternionf();
    Quaternionf prevOrientation = new Quaternionf();
    Vector3f angularVelocity = new Vector3f(); // Rad/tick in LOCAL space

    // ============================================================
    // CLIENT INTERPOLATION & VISUALS (Delegated to VisualManager)
    // ============================================================

    // --- DEBUG LOGGING SYSTEM (Delegated to BlackBox) ---
    public static final List<String> debugLogBuffer = new ArrayList<>();
    private static UUID focusedProjectileUUID = null;

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ORIENTATION, new Quaternionf());
        builder.define(PITCH_DEMAND, 0.0f);
        builder.define(YAW_DEMAND, 0.0f);
        builder.define(ROLL_DEMAND, 0.0f);
        builder.define(THROTTLE, 1.0f);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (level().isClientSide && visualManager != null) {
            visualManager.onSyncedDataUpdated(key, ORIENTATION);
        }
    }

    @Override
    public void lerpTo(double x, double y, double z, float yaw, float pitch, int steps) {
        if (visualManager != null) {
            visualManager.lerpTo(x, y, z, yaw, pitch, steps);
        }
    }

    public ProjectileEntity(EntityType<?> type, Level level) {
        super(type, level);
        // Disable vanilla physics (collision with blocks during move)
        this.noPhysics = true;
        this.visualManager = new ProjectileVisualManager(this);
    }

    public static ProjectileEntity create(Level level, ProjectileContraption contraption, ScrapResult stats) {
        ProjectileEntity entity = new ProjectileEntity(
                ModEntities.PROJECTILE_ENTITY.get(), level);

        entity.setContraption(contraption);
        entity.scrapResult = stats; // Store for debug logging
        return entity;
    }

    /**
     * Spawns a fully assembled projectile from a ScrapResult.
     */
    public static ProjectileEntity spawn(Level level, ScrapResult result, Vec3 position, Vec3 velocity, float yaw,
            float pitch) {

        // 1. PERFORM STRICT ANALYSIS (V3.5)
        // This validates the IDs against the registry and calculates initial stats.
        ProjectileStats stats = ProjectileAnalyzer.analyze(result);

        // 2. ASSEMBLE CONTRAPTION
        ProjectileContraption contraption = new ProjectileContraption();
        contraption.assembleFromIds(result, stats);

        // 3. CREATE ENTITY
        ProjectileEntity entity = create(level, contraption, result);

        // 4. DATA INJECTION (V3.5 Logic)
        entity.setScrapResult(result);
        entity.applyStats(stats);
        entity.lastConfigVersion = OrdnanceConfigs.CONFIG_VERSION;

        // --- BLACKBOX: AUTO-FOCUS ON LAUNCH ---
        SimulationEngine.getInstance().getBlackBox()
                .setFocus(entity.getUUID(), stats, result);

        // Metadata Snapshots
        entity.projectileScale = result.scale();
        entity.scanPos = BlockPos.containing(result.worldOrigin());
        entity.initialYaw = yaw;
        entity.initialPitch = pitch;
        entity.currentFuel = entity.maxFuel; // Start full

        // 5. INITIAL TRANSFORMS
        entity.orientation.rotationYXZ((float) Math.toRadians(-yaw), (float) Math.toRadians(pitch), 0)
                .rotateX((float) Math.toRadians(90));
        entity.prevOrientation.set(entity.orientation);
        entity.setOrientation(entity.orientation);
        entity.angularVelocity.set(0, 0, 0);

        // 6. SETUP EVENTS (Trigger logic for each block on startup)
        for (int i = 0; i < result.blockIds().size(); i++) {
            entity.triggerComponentEvent(i, "SETUP");
        }

        // 7. LEVEL PLACEMENT
        // V5.2: CoM-Centric Positioning
        // The entity's world position() is now the physical Center of Mass.
        // This ensures that all rotations (visual and physical) are perfectly aligned.

        Vec3 localSpawnPoint = new Vec3(0.5, 0.5, 0.5); // Center of the Tail block
        Vec3 com = entity.centerOfMass;

        // The exact vector from CoM to the Spawn Point in local space
        //
        Vec3 offsetFromComToSpawn = localSpawnPoint.subtract(com);

        // Rotate this offset to find where the spawn point would be relative to the CoM
        // in world space
        Vector3f rotatedOffset = new Vector3f((float) offsetFromComToSpawn.x, (float) offsetFromComToSpawn.y,
                (float) offsetFromComToSpawn.z);
        entity.orientation.transform(rotatedOffset);
        //

        // 'position' is where the user/launcher wants the CENTER of the Tail.
        // Since SpawnPoint = CoM + rotatedOffset, then CoM = SpawnPoint -
        // rotatedOffset.
        Vec3 correctedPos = position.subtract(new Vec3(rotatedOffset.x, rotatedOffset.y, rotatedOffset.z));

        entity.setPos(correctedPos.x, correctedPos.y, correctedPos.z);
        entity.xOld = correctedPos.x;
        entity.yOld = correctedPos.y;
        entity.zOld = correctedPos.z;
        entity.setXRot(pitch);
        entity.setYRot(yaw);
        entity.xRotO = pitch;
        entity.yRotO = yaw;
        entity.startAtYaw(yaw);
        entity.pitch = pitch;
        entity.prevPitch = pitch;

        entity.setDeltaMovement(velocity);
        entity.hasImpulse = true;

        // 7b. COMPAT: Inject pending Vista TV link (set before spawn() call)
        if (pendingLinkedTvPos != null) {
            entity.linkedTvPos = pendingLinkedTvPos;
            pendingLinkedTvPos = null;
        }

        // 8. API EVENT
        ProjectileLaunchEvent launchEvent = new ProjectileLaunchEvent(
                entity, result);
        NeoForge.EVENT_BUS.post(launchEvent);
        if (launchEvent.isCanceled()) {
            return null;
        }

        level.addFreshEntity(entity);
        return entity;
    }

    /**
     * Re-calculates physical stats from current scrapResult.
     * Called automatically when global config version changes.
     */
    public void reanalyze() {
        if (this.scrapResult == null)
            return;

        try {
            ProjectileStats stats = ProjectileAnalyzer.analyze(this.scrapResult);
            this.applyStats(stats);
            this.lastConfigVersion = OrdnanceConfigs.CONFIG_VERSION;

            // Update contraption pivot too (visual CoM rotation)
            if (getProjectileContraption() != null) {
                getProjectileContraption().assembleFromIds(this.scrapResult, stats);
            }

            CreateOrdnance.LOGGER
                    .debug("[Ordnance] Projectile {} re-analyzed due to config reload.", this.getId());
        } catch (Exception e) {
            CreateOrdnance.LOGGER.error(
                    "[Ordnance] FAILED to re-analyze projectile {} during config sync! {}", this.getId(),
                    e.getMessage());
        }
    }

    private void applyStats(ProjectileStats stats) {
        this.totalMass = stats.totalMass();
        this.centerOfMass = stats.centerOfMass();
        this.totalStability = stats.totalStability();
        this.centerOfPressure = stats.centerOfPressure();
        this.maxFuel = stats.totalFuel();
        this.fuelConsumption = stats.totalFuelConsumption();
        this.thrustType = stats.thrustType();
        this.baseThrust = stats.baseThrust();
        this.maxNozzleTilt = stats.maxNozzleTilt();

        this.optimalDensity = stats.optimalDensity();
        this.densityBandwidth = stats.densityBandwidth();

        this.totalLateralArea = stats.totalLateralArea();

        // Unified Dual-Point Stats
        this.massFront = stats.massFront();
        this.massBack = stats.massBack();
        this.volumeFront = stats.volumeFront();
        this.volumeBack = stats.volumeBack();
        this.areaFrontalFront = stats.areaFrontalFront();
        this.areaFrontalBack = stats.areaFrontalBack();
        this.areaLateralFront = stats.areaLateralFront();
        this.areaLateralBack = stats.areaLateralBack();
        this.centerFront = stats.centerFront();
        this.centerBack = stats.centerBack();

        this.totalBlockCount = scrapResult.blockIds().size();
        this.projectileLength = scrapResult.blockIds().size();
    }

    // FIX: Initialize prevYaw and prevPitch on CLIENT after receiving spawn data
    // Create's readAdditional sets yaw/pitch but NOT prevYaw/prevPitch, causing
    // lerp from 0
    // ============================================================
    // NBT SERIALIZATION (ProjectileData compound)
    // ============================================================

    @Override
    protected void writeAdditional(CompoundTag compound,
            HolderLookup.Provider registries,
            boolean spawnPacket) {
        super.writeAdditional(compound, registries, spawnPacket);
        ProjectileDataIO.writeAdditional(this, compound);
    }

    @Override
    protected void readAdditional(CompoundTag compound, boolean spawnPacket) {
        super.readAdditional(compound, spawnPacket);

        // Create rotation fix
        this.prevYaw = this.yaw;
        this.prevPitch = this.pitch;
        this.targetYaw = this.yaw;
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();

        ProjectileDataIO.readAdditional(this, compound);

        // SYNC: Force persistence data to update synced data and Euler fields
        // immediately.
        // This ensures the renderer and contraption are correctly aligned on load,
        // even if the projectile is 'stuck' and skips physics ticks.
        setOrientation(orientation);
        Vector3f noseDir = new Vector3f(0, 1, 0);
        orientation.transform(noseDir);
        float rotY = (float) Math.toDegrees(Math.atan2(-noseDir.x, noseDir.z));
        float rotX = (float) Math.toDegrees(Math.asin(-noseDir.y));

        this.setYRot(rotY);
        this.setXRot(rotX);
        this.yRotO = rotY;
        this.xRotO = rotX;
        this.yaw = rotY;
        this.pitch = rotX;
        this.prevYaw = rotY;
        this.prevPitch = rotX;
    }

    // ============================================================
    // ACCESSORS (For Physics and Renderer)
    // ============================================================

    public ScrapResult getScrapResult() {
        return scrapResult;
    }

    public void setScrapResult(ScrapResult scrapResult) {
        this.scrapResult = scrapResult;
    }

    /**
     * The absolute standard of geometric translations (V3.6 - Reverted).
     * Turns any local coordinate into a perfectly accurate World Space coordinate,
     * accounting for Create Mod's visual rotation around the Center of Mass.
     */
    public Vec3 getTransformedLocalPos(Vec3 local) {
        // V5.2: Simple transform. Since entity position() IS the CoM, we just rotate
        // the offset from CoM.
        Vector3f offsetFromCoM = new Vector3f(
                (float) (local.x - this.centerOfMass.x),
                (float) (local.y - this.centerOfMass.y),
                (float) (local.z - this.centerOfMass.z));

        getOrientation().transform(offsetFromCoM);

        return this.position().add(offsetFromCoM.x, offsetFromCoM.y, offsetFromCoM.z);
    }

    /**
     * The Holy Grail Function (V3.8 - Distance Normalization)
     * Calculates the exact absolute world coordinate for a point along the
     * projectile's core axis.
     * 
     * @param blocksFromTail The distance in blocks from the center of the Tail (0 =
     *                       Tail center, 1 = Next block center, etc.)
     * @return The exact world coordinate matching the visual 3D render.
     */
    public Vec3 getWorldPosFromTail(double blocksFromTail) {
        // 1. The Pivot / World CoM
        // V5.2: Simplified. position() IS world CoM.
        Vec3 worldCoM = this.position();

        // 2. Local distance from CoM to the target point
        // Tail center is at local Y=0.5. Block center XZ is 0.5, 0.5.
        double targetLocalY = 0.5 + blocksFromTail;
        Vector3f offsetFromCoM = new Vector3f(
                (float) (0.5 - this.centerOfMass.x),
                (float) (targetLocalY - this.centerOfMass.y),
                (float) (0.5 - this.centerOfMass.z));

        // 3. Apply the entity's current orientation to this offset
        getOrientation().transform(offsetFromCoM);

        // 4. Return the new world position
        return worldCoM.add(offsetFromCoM.x, offsetFromCoM.y, offsetFromCoM.z);
    }

    /**
     * Applies the entity's current orientation to a local offset vector.
     * Useful for finding where a local point (like a buoyancy center) is in
     * 'rotated' space.
     */
    public Vec3 getRotationAwareOffset(Vec3 localOffset) {
        Vector3f vec = new Vector3f((float) localOffset.x, (float) localOffset.y, (float) localOffset.z);
        getOrientation().transform(vec);
        return new Vec3(vec.x, vec.y, vec.z);
    }

    private void triggerComponentEvent(int index, String eventType) {
        ProjectileInteractionHandler.triggerComponentEvent(this, index, eventType);
    }

    public void triggerComponentEventWithCustomPos(ProjectileData data,
            String eventType, Vec3 customPos) {
        ProjectileInteractionHandler.triggerComponentEventWithCustomPos(this, data, eventType, customPos);
    }

    /**
     * Triggers full impact logic (Explosions, etc).
     * Called when Physics Engine (Registry) detects a solid wall collision.
     * Triggers all PAYLOAD components and destroys the entity.
     */
    public void impact() {
        ProjectileInteractionHandler.impact(this);
    }

    public float getScale() {
        return projectileScale;
    }

    public double getTotalMass() {
        return totalMass;
    }

    /**
     * Re-assembles current stats into a ProjectileStats record for physics modules.
     */
    public ProjectileStats getStats() {
        return new ProjectileStats(
                totalMass,
                centerOfMass,
                totalStability,
                centerOfPressure,
                maxFuel,
                fuelConsumption,
                thrustType,
                baseThrust,

                maxNozzleTilt,
                0.0, // Terminal velocity not needed in-flight
                optimalDensity,
                densityBandwidth,
                massFront,
                massBack,
                volumeFront,
                volumeBack,
                areaFrontalFront,
                areaFrontalBack,
                areaLateralFront,
                areaLateralBack,
                centerFront,
                centerBack,
                totalLateralArea);
    }

    @Override
    public boolean isReadyForRender() {
        // HACK: Hide the first two ticks on the client to avoid the spawning snap
        // glitch
        if (level().isClientSide && tickCount < 2) {
            return false;
        }
        return super.isReadyForRender();
    }

    public Quaternionf getOrientation() {
        return entityData.get(ORIENTATION);
    }

    public Quaternionf getPrevOrientation() {
        return prevOrientation;
    }

    public void setOrientation(Quaternionf value) {
        entityData.set(ORIENTATION, value);
    }

    /**
     * Sets the new orientation and preserves the old one for client-side
     * interpolation.
     */
    public void setOrientationWithPrev(Quaternionf value) {
        this.prevOrientation.set(getOrientation());
        setOrientation(value);
    }

    public Vector3f getAngularVelocity() {
        return angularVelocity;
    }

    public void setAngularVelocity(Vector3f value) {
        angularVelocity.set(value);
    }

    // ============================================================
    // GUIDANCE & CONTROL API (CGS Interface)
    // ============================================================

    /**
     * Sets the pitch control demand.
     * 
     * @param demand Normalized demand from -1.0 (Full Nose Down) to 1.0 (Full Nose
     *               Up).
     */
    public void setPitchDemand(float demand) {
        this.entityData.set(PITCH_DEMAND, Mth.clamp(demand, -1.0f, 1.0f));
    }

    public float getPitchDemand() {
        return this.entityData.get(PITCH_DEMAND);
    }

    /**
     * Sets the yaw control demand.
     * 
     * @param demand Normalized demand from -1.0 (Full Left) to 1.0 (Full Right).
     */
    public void setYawDemand(float demand) {
        this.entityData.set(YAW_DEMAND, Mth.clamp(demand, -1.0f, 1.0f));
    }

    public float getYawDemand() {
        return this.entityData.get(YAW_DEMAND);
    }

    /**
     * Sets the roll control demand.
     * 
     * @param demand Normalized demand from -1.0 (Roll Left) to 1.0 (Roll Right).
     */
    public void setRollDemand(float demand) {
        this.entityData.set(ROLL_DEMAND, Mth.clamp(demand, -1.0f, 1.0f));
    }

    public float getRollDemand() {
        return this.entityData.get(ROLL_DEMAND);
    }

    /**
     * Sets the throttle demand.
     * 
     * @param demand Normalized throttle from 0.0 (Idle/Cut) to 1.0 (Full Thrust).
     */
    public void setThrottle(float demand) {
        this.entityData.set(THROTTLE, Mth.clamp(demand, 0.0f, 1.0f));
    }

    public float getThrottle() {
        return this.entityData.get(THROTTLE);
    }

    /**
     * Returns the maximum nozzle tilt angle (degrees) supported by the tail.
     * Useful for guidance systems to calculate turning radius.
     */
    public double getMaxNozzleTilt() {
        return maxNozzleTilt;
    }

    // ============================================================
    // CREATE BUG FIX #1: Redirect velocity to Create's motion system
    // ============================================================
    // AbstractContraptionEntity blocks standard Entity movement.
    // We must use setContraptionMotion() instead of setDeltaMovement().
    @Override
    public void setDeltaMovement(Vec3 motion) {
        // Don't call super - it would be blocked by Create
        this.setContraptionMotion(motion);
    }

    // ============================================================
    // PHYSICS: Override tickContraption() instead of tick()
    // AbstractContraptionEntity.tick() does:
    // 1. xo/yo/zo = position (for interpolation)
    // 2. tickContraption() ← WE PUT PHYSICS HERE
    // 3. super.tick()
    // This ensures proper interpolation setup BEFORE physics changes position
    // ============================================================
    @Override
    public void tick() {
        // EVENT: ProjectilePreTickEvent (ADDON HOOK)
        if (!level().isClientSide) {
            ProjectilePreTickEvent event = new ProjectilePreTickEvent(
                    this);
            NeoForge.EVENT_BUS.post(event);
            if (event.isCanceled()) {
                return; // Cancelled: Skip physics/tick logic
            }

            // CGS: Update guidance demands BEFORE physics reads them
            CGSManager.getInstance().update(this);
        }

        // Record previous orientation (server and client)
        if (level().isClientSide) {
            this.prevOrientation.set(getOrientation());
        }

        // Call inherited contraption logic (Handles xo/yo/zo setup and tickContrap
        // ion)
        // xo/yo/zo MUST capture the state BEFORE interpolation for partialTicks to
        // work.
        super.tick();

        if (level().isClientSide && visualManager != null) {
            // Yaw/Pitch sync for legacy renderers/overlays (not contraption rotation)
            this.yaw = this.getYRot();
            this.pitch = this.getXRot();
        }

        if (!level().isClientSide) {
            // Auto-Despawn Logic (Optimization)
            int maxLifeSecs = OrdnanceConfigs.COMMON.projectileMaxLifetime.get();
            if (maxLifeSecs > 0 && this.tickCount > maxLifeSecs * 20) {
                this.discard();
            }
        }
    }

    @Override
    protected void tickContraption() {
        // V2 Integration Point: delegate all physics to the SimulationEngine.
        // The engine builds an immutable PhysicsContext, runs the 3-phase pipeline,
        // integrates forces (F=ma), manages fuel, and handles partial tick subdivision.
        if (!level().isClientSide) {
            SimulationEngine.getInstance().tick(this);
        } else if (visualManager != null) {
            // Client side: Interpolation happens in the physics slot to follow server tick
            // timing
            this.prevYaw = this.yaw;
            this.prevPitch = this.pitch;
            visualManager.tickInterpolation();
        }
    }

    @Override
    public void setPos(double x, double y, double z) {
        super.setPos(x, y, z);
        if (getContraption() != null && visualManager != null) {
            visualManager.refreshBoundingBox();
        }
    }

    protected void refreshBoundingBox() {
        if (visualManager != null) {
            visualManager.refreshBoundingBox();
        }
    }

    @Override
    public Vec3 applyRotation(Vec3 localPos, float partialTicks) {
        if (visualManager != null) {
            return visualManager.applyRotation(localPos, partialTicks);
        }
        return super.applyRotation(localPos, partialTicks);
    }

    @Override
    public Vec3 reverseRotation(Vec3 globalPos, float partialTicks) {
        if (visualManager != null) {
            return visualManager.reverseRotation(globalPos, partialTicks);
        }
        return super.reverseRotation(globalPos, partialTicks);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void applyLocalTransforms(PoseStack matrixStack, float partialTicks) {
        if (visualManager != null) {
            visualManager.applyLocalTransforms(matrixStack, partialTicks);
        } else {
            super.applyLocalTransforms(matrixStack, partialTicks);
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide) {

            // EVENT: ProjectileDeathEvent (ADDON HOOK)
            NeoForge.EVENT_BUS
                    .post(new ProjectileDeathEvent(this, reason));

            // CGS: Cleanup controllers for this entity
            CGSManager.getInstance().cleanup(this);
        }
        super.remove(reason);
    }

    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean canCollideWith(Entity e) {
        return false;
    }

    // NBT Handling delegation to Contraption

    public ProjectileContraption getProjectileContraption() {
        if (getContraption() instanceof ProjectileContraption pc) {
            return pc;
        }
        return null; // Should not happen usually
    }

    public Vec3 getAnchorVec() {
        // V5.2: In oriented contraptions, the anchor defines the origin of the block
        // grid
        // relative to the entity's world position BEFORE rotation.
        // Since our entity is at the world CoM, and we want block (0.5, 0, 0.5)
        // to be the Tail base, the anchor must be exactly the negative of the CoM
        // plus the block center adjustment.
        return position().subtract(centerOfMass);
    }

    // ============================================================
    // DAMAGE HANDLING (Explosions, Player Hits)
    // ============================================================

    boolean isRemoving = false;

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.isRemoving || this.isInvulnerableTo(source))
            return false;

        if (ProjectileInteractionHandler.handleHurt(this, source, amount)) {
            return true;
        }

        return super.hurt(source, amount);
    }

    public double getTotalStability() {
        return totalStability;
    }

    public Vec3 getCenterOfMass() {
        return centerOfMass;
    }

    public Vec3 getCenterOfPressure() {
        return centerOfPressure;
    }

    public int getProjectileLength() {
        return projectileLength;
    }

    public int getTotalChunksLoadedCount() {
        return totalChunksLoadedCount;
    }

    public void incrementTotalChunksLoaded() {
        this.totalChunksLoadedCount++;
    }

    public int getTotalBlockCount() {
        return totalBlockCount;
    }

    public double getBaseThrust() {
        return baseThrust;
    }

    public double getOptimalDensity() {
        return optimalDensity;
    }

    public double getDensityBandwidth() {
        return densityBandwidth;
    }

    public double getTotalLateralArea() {
        return totalLateralArea;
    }

    public double getMaxFuel() {
        return maxFuel;
    }

    public double getFuelConsumption() {
        return fuelConsumption;
    }

    public double getEstimatedTerminalVelocity() {
        return getStats().estimatedTerminalVelocity();
    }

    // ============================================================
    // PHYSICS V2: Additional accessors for SimulationEngine
    // ============================================================

    public Vec3 getCenterBack() {
        return centerBack;
    }

    public double getCurrentFuel() {
        return currentFuel;
    }

    public void setCurrentFuel(double fuel) {
        this.currentFuel = fuel;
    }

    public ThrustType getThrustType() {
        return thrustType;
    }

    public float getProjectileScale() {
        return projectileScale;
    }

    public boolean isExtinguished() {
        return isExtinguished;
    }

    public double getResidualViscosity() {
        return residualViscosity;
    }

    public void setResidualViscosity(double residualViscosity) {
        this.residualViscosity = residualViscosity;
    }

    public void setExtinguished(boolean extinguished) {
        this.isExtinguished = extinguished;
    }

    // ============================================================
    // COMPAT: Vista TV Link
    // ============================================================

    /**
     * Returns the position of the Vista TV linked to this projectile,
     * or null if no TV is linked.
     * <p>
     * Set at spawn time by the OrdnanceDebugger when a TV was
     * previously shift-clicked. Used client-side by VistaCompat to
     * create a camera feed on the TV.
     */
    public BlockPos getLinkedTvPos() {
        return linkedTvPos;
    }

    public void setLinkedTvPos(BlockPos pos) {
        this.linkedTvPos = pos;
    }

    /**
     * Sets a pending TV link position to be injected into the next
     * projectile spawned via {@link #spawn}. Must be called BEFORE spawn().
     * The pending value is consumed (cleared) automatically.
     */
    public static void setPendingLinkedTvPos(BlockPos pos) {
        pendingLinkedTvPos = pos;
    }

    public static void clearPendingLinkedTvPos() {
        pendingLinkedTvPos = null;
    }
}