package com.pixel.create_ordnance.mechanics.physics.core;

import java.util.UUID;

import com.pixel.create_ordnance.config.OrdnanceCommonConfig.ThrustType;
import com.pixel.create_ordnance.content.entity.ProjectileEntity;
import com.pixel.create_ordnance.mechanics.physics.core.PhysicsRegistry.PhysicsProfile;
import com.pixel.create_ordnance.mechanics.scanning.ProjectileStats;
import com.pixel.create_ordnance.mechanics.scanning.ScrapResult;

import net.minecraft.world.phys.Vec3;

import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Immutable snapshot of all data needed by physics modules for a single tick
 * (V2 Protocol).
 * <p>
 * <b>Contract:</b> This object is created once per tick by the
 * {@link PhysicsEngine}
 * and passed to every {@link IPhysicsModule}. Modules <b>MUST NOT</b> mutate
 * any field.
 * <p>
 * Contains:
 * <ul>
 * <li>Kinematic state (position, velocity, orientation, angular velocity)</li>
 * <li>Structural data (from {@link ProjectileStats} — masses, areas, CoM,
 * CoP)</li>
 * <li>Environment (current fluid profile, density, viscosity)</li>
 * <li>Control demands (CGS inputs: pitch, yaw, roll, throttle)</li>
 * <li>Computed inputs (AoA — pre-calculated before module execution)</li>
 * <li>Lifecycle state ({@link ProjectileState})</li>
 * </ul>
 */
public class PhysicsContext {

    // =========================================================
    // KINEMATIC STATE (from entity — read-only snapshot)
    // =========================================================

    public final ProjectileEntity entity;

    /** World-space position of the projectile entity. */
    public final Vec3 position;

    /** World-space velocity vector (blocks/tick). */
    public final Vec3 velocityWorld;

    /** Local-space velocity vector (aligned to projectile axes, Nose = +Y). */
    public final Vec3 velocityLocal;

    /** Current orientation quaternion. */
    public final Quaternionf orientation;

    /** Angular velocity in local space (rad/tick). */
    public final Vector3f angularVelocity;

    /** The unique ID of the projectile being simulated. */
    public final UUID projectileId;

    /** Raw structure data. */
    public final ScrapResult scrapResult;

    /** Time step for this tick (usually 1.0, may vary for partial ticks). */
    public final double deltaTime;

    // =========================================================
    // STRUCTURAL DATA (from ProjectileStats — immutable per config reload)
    // =========================================================

    /** Full projectile statistics from the analyzer. */
    public final ProjectileStats stats;

    /** Total projectile mass (kg). Shortcut for {@code stats.totalMass()}. */
    public final double totalMass;

    /** Scale factor of the projectile (from ScrapResult). */
    public final float scale;

    // =========================================================
    // ENVIRONMENT (sampled at projectile position)
    // =========================================================

    /**
     * Physics profile of the block at the projectile's current position (Center of
     * Mass).
     */
    public final PhysicsProfile environmentProfile;

    /** Physics profile at the front application point. */
    public final PhysicsProfile frontProfile;

    /** Physics profile at the back application point. */
    public final PhysicsProfile backProfile;

    /** World-space position of the front application point. */
    public final Vec3 frontPos;

    /** World-space position of the back application point. */
    public final Vec3 backPos;

    // =========================================================
    // FUEL STATE
    // =========================================================

    /** Current remaining fuel (kL). Decremented by VectorialThrust module. */
    public final double currentFuel;

    /** Fuel consumption rate per tick (from analyzer). */
    public final double fuelConsumption;

    /** Propulsion type of the tail component. */
    public final ThrustType thrustType;

    // =========================================================
    // CGS CONTROL DEMANDS (set by pilot before Phase 1)
    // =========================================================

    /** Pitch demand from CGS: -1.0 (nose down) to 1.0 (nose up). */
    public final float pitchDemand;

    /** Yaw demand from CGS: -1.0 (left) to 1.0 (right). */
    public final float yawDemand;

    /** Roll demand from CGS: -1.0 (roll left) to 1.0 (roll right). */
    public final float rollDemand;

    /** Throttle demand from CGS: 0.0 (idle) to 1.0 (full thrust). */
    public final float throttle;

    // =========================================================
    // COMPUTED INPUTS (pre-calculated before module execution)
    // =========================================================

    /**
     * Angle of Attack (radians).
     * Angle between the projectile's nose axis and its velocity vector.
     * Pre-calculated by the engine before any module runs.
     */
    public final double angleOfAttack;

    // =========================================================
    // LIFECYCLE STATE
    // =========================================================

    /** Current state of the projectile (FLYING, STUCK, FORCE_CHUNKS, EXPLODED). */
    public final ProjectileState state;

    /** Boolean flag for logic deactivation (e.g. afterburner hit water). */
    public final boolean isExtinguished;

    /** Accumulated torque from previous phases (V2 Protocol). */
    public final Vec3 accumulatedTorque;

    // --- BlackBox Telemetry (Captured mid-tick) ---
    public int currentlyLoadedChunksCount = 0;
    public int totalChunksLoadedCount = 0;
    public int partitionCount = 1;
    public int currentPartition = 0;

    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public PhysicsContext(
            ProjectileEntity entity,
            Vec3 position,
            Vec3 velocityWorld,
            Vec3 velocityLocal,
            Quaternionf orientation,
            Vector3f angularVelocity,
            UUID projectileId,
            ScrapResult scrapResult,
            double deltaTime,
            ProjectileStats stats,
            float scale,
            PhysicsProfile environmentProfile,
            PhysicsProfile frontProfile,
            PhysicsProfile backProfile,
            Vec3 frontPos,
            Vec3 backPos,
            double currentFuel,
            double fuelConsumption,
            ThrustType thrustType,
            float pitchDemand,
            float yawDemand,
            float rollDemand,
            float throttle,
            double angleOfAttack,
            ProjectileState state,
            boolean isExtinguished,
            Vec3 accumulatedTorque) {

        this.entity = entity;
        this.position = position;
        this.velocityWorld = velocityWorld;
        this.velocityLocal = velocityLocal;
        this.orientation = orientation;
        this.angularVelocity = angularVelocity;
        this.projectileId = projectileId;
        this.scrapResult = scrapResult;
        this.deltaTime = deltaTime;

        this.stats = stats;
        this.totalMass = stats.totalMass();
        this.scale = scale;

        this.environmentProfile = environmentProfile;
        this.frontProfile = frontProfile;
        this.backProfile = backProfile;
        this.frontPos = frontPos;
        this.backPos = backPos;

        this.currentFuel = currentFuel;
        this.fuelConsumption = fuelConsumption;
        this.thrustType = thrustType;

        this.pitchDemand = pitchDemand;
        this.yawDemand = yawDemand;
        this.rollDemand = rollDemand;
        this.throttle = throttle;

        this.angleOfAttack = angleOfAttack;

        this.state = state;
        this.isExtinguished = isExtinguished;
        this.accumulatedTorque = accumulatedTorque;
    }

    /**
     * Returns a new context with updated accumulated torque.
     * Used between Phase 1 and Phase 2 of the physics engine.
     */
    public PhysicsContext withAccumulatedTorque(Vec3 newTorque) {
        return new PhysicsContext(
                entity,
                position, velocityWorld, velocityLocal, orientation, angularVelocity, projectileId,
                scrapResult, deltaTime, stats, scale, environmentProfile,
                frontProfile, backProfile, frontPos, backPos, currentFuel, fuelConsumption,
                thrustType, pitchDemand, yawDemand, rollDemand, throttle,
                angleOfAttack, state, isExtinguished, newTorque);
    }

    // =========================================================
    // CONVENIENCE ACCESSORS (shortcuts into stats)
    // =========================================================

    /** Shortcut: speed magnitude (blocks/tick). */
    public double getSpeed() {
        return velocityWorld.length();
    }

    /** Shortcut: projectile nose direction (local +Y rotated by orientation). */
    public Vec3 getNoseDirection() {
        Vector3f nose = new Vector3f(0, 1, 0);
        orientation.transform(nose);
        return new Vec3(nose.x, nose.y, nose.z);
    }

    /** Shortcut: environment density (kg/m³). */
    public double getEnvironmentDensity() {
        return environmentProfile.density();
    }

    /** Shortcut: environment viscosity. */
    public double getEnvironmentViscosity() {
        return environmentProfile.viscosity();
    }

    /** Shortcut: front environment density. */
    public double getFrontDensity() {
        return frontProfile.density();
    }

    /** Shortcut: back environment density. */
    public double getBackDensity() {
        return backProfile.density();
    }
}