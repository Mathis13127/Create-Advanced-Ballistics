package com.pixel.create_ordnance.mechanics.physics.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.pixel.create_ordnance.mechanics.physics.logic.BlackBox;
import com.pixel.create_ordnance.mechanics.physics.modules.aero.LiftModule;
import com.pixel.create_ordnance.mechanics.physics.modules.aero.SurfaceDragModule;
import com.pixel.create_ordnance.mechanics.physics.modules.attraction.AttractionModule;
import com.pixel.create_ordnance.mechanics.physics.modules.environment.BuoyancyModule;
import com.pixel.create_ordnance.mechanics.physics.modules.environment.GravityModule;
import com.pixel.create_ordnance.mechanics.physics.modules.mass.ViscousDampingModule;
import com.pixel.create_ordnance.mechanics.physics.modules.thrust.VectorialThrustModule;

import net.minecraft.world.phys.Vec3;

/**
 * The Central Physics Engine (V2 Protocol).
 * <p>
 * Orchestrates the execution of all registered {@link IPhysicsModule} instances
 * using a <b>3-phase pipeline</b>:
 * <ol>
 * <li><b>Phase 1 — FORCE:</b> All force-only modules compute independently
 * on the immutable snapshot. Emergent torques are derived from 2-point
 * forces.</li>
 * <li><b>Phase 2 — TORQUE:</b> Torque-aware modules receive the cumulative
 * Phase 1
 * torque as additional input (e.g. viscous damping reacts to angular
 * forces).</li>
 * <li><b>Phase 3 — ACCUMULATION:</b> All forces (Phase 1) and all torques
 * (Phase 1 emergent + Phase 2 direct) are summed into final vectors.</li>
 * </ol>
 * <p>
 * <b>Architecture:</b> The engine does NOT manage collisions, hitboxes, or
 * entity state.
 * It only computes net force and torque vectors. The
 * {@link com.pixel.create_ordnance.mechanics.physics.logic.SimulationEngine}
 * handles integration and state updates.
 */
public class PhysicsEngine {

    private static final PhysicsEngine INSTANCE = new PhysicsEngine();

    private final List<IPhysicsModule> forceModules = new ArrayList<>();
    private final List<IPhysicsModule> torqueModules = new ArrayList<>();
    private final Set<String> disabledModules = new HashSet<>();
    private BlackBox blackBox;

    public static PhysicsEngine getInstance() {
        return INSTANCE;
    }

    private PhysicsEngine() {
        // Modules registered via registerDefaultModules() at mod startup
    }

    /**
     * Registers all default physics modules.
     * Must be called once during mod initialization (FMLCommonSetupEvent).
     */
    public static void registerDefaultModules() {
        PhysicsEngine engine = getInstance();

        // FORCE phase — order: environment first, then aero, then propulsion
        engine.registerModule(new GravityModule());
        engine.registerModule(new BuoyancyModule());
        engine.registerModule(new SurfaceDragModule());
        engine.registerModule(new LiftModule());
        engine.registerModule(
                new VectorialThrustModule());
        engine.registerModule(new AttractionModule());

        // TORQUE phase — runs after all forces are accumulated
        engine.registerModule(new ViscousDampingModule());

        INSTANCE.blackBox = new BlackBox();
    }

    /**
     * Registers a physics module. The module is automatically sorted into
     * the correct execution phase based on {@link IPhysicsModule#getPhase()}.
     *
     * @param module The module to register.
     */
    public void registerModule(IPhysicsModule module) {
        switch (module.getPhase()) {
            case FORCE -> forceModules.add(module);
            case TORQUE -> torqueModules.add(module);
        }
    }

    /**
     * Sets the BlackBox diagnostic system for per-module snapshot capture.
     *
     * @param blackBox The BlackBox instance to use for logging.
     */
    public void setBlackBox(BlackBox blackBox) {
        this.blackBox = blackBox;
    }

    // =========================================================
    // DYNAMIC MODULE TOGGLES
    // =========================================================

    public void setModuleStatus(String moduleId, boolean enabled) {
        if (enabled) {
            disabledModules.remove(moduleId);
        } else {
            disabledModules.add(moduleId);
        }
    }

    public boolean getModuleStatus(String moduleId) {
        return !disabledModules.contains(moduleId);
    }

    public void resetModules() {
        disabledModules.clear();
    }

    public List<IPhysicsModule> getAllModules() {
        List<IPhysicsModule> all = new ArrayList<>(forceModules);
        all.addAll(torqueModules);
        return all;
    }

    /**
     * Runs the full 3-phase physics pipeline for a single tick.
     * <p>
     * <b>Input:</b> An immutable {@link PhysicsContext} snapshot.<br>
     * <b>Output:</b> A {@link TickResult} containing the net force and net torque.
     *
     * @param context The immutable snapshot for this tick.
     * @return The accumulated result of all module contributions.
     */
    public TickResult tick(PhysicsContext context) {
        List<ModuleOutput> allOutputs = new ArrayList<>();
        Vec3 netForce = Vec3.ZERO;
        Vec3 netTorque = Vec3.ZERO;

        // Capture kinematics in the BlackBox for this tick
        if (blackBox != null) {
            blackBox.beginTick(context);
        }

        // ====================
        // PHASE 1: FORCE-ONLY
        // ====================
        for (IPhysicsModule module : forceModules) {
            if (disabledModules.contains(module.getModuleId()))
                continue;

            ModuleOutput output = module.compute(context).withCapability(module.getCapability());
            allOutputs.add(output);

            // V7: Accumulate Linear Force — capability-aware summation.
            // FORCE_SIMPLE / FORCE_WITH_TORQUE: force1 == force2, take one copy.
            // FORCE_DUAL: force1 ≠ force2, sum both application-point forces.
            Vec3 linearForce = switch (output.capability()) {
                case FORCE_SIMPLE, FORCE_WITH_TORQUE -> output.force1();
                case FORCE_DUAL -> output.force1().add(output.force2());
                case TORQUE_ONLY -> Vec3.ZERO;
            };
            netForce = netForce.add(linearForce);

            // Derive emergent torque from 2-point forces
            if (!output.force1().equals(output.force2())) {
                Vec3 emergentTorque = deriveEmergentTorque(output, context);
                netTorque = netTorque.add(emergentTorque);
            }

            // Add any explicitly declared torque (Mixed modules)
            netTorque = netTorque.add(output.torque());

            // BlackBox: record per-module snapshot
            if (blackBox != null) {
                blackBox.recordModuleOutput(output);
            }
        }

        // ====================
        // PHASE 2: TORQUE-AWARE
        // ====================
        // Torque-aware modules can read the Phase 1 cumulative torque
        // via context.
        PhysicsContext torqueContext = context.withAccumulatedTorque(netTorque);

        for (IPhysicsModule module : torqueModules) {
            if (disabledModules.contains(module.getModuleId()))
                continue;

            ModuleOutput output = module.compute(torqueContext).withCapability(module.getCapability());
            allOutputs.add(output);

            // Accumulate Direct Torque
            netTorque = netTorque.add(output.torque());

            if (blackBox != null) {
                blackBox.recordModuleOutput(output);
            }
        }

        // BlackBox: finalize tick snapshot
        if (blackBox != null) {
            blackBox.finalizeTick(netForce, netTorque);
        }

        return new TickResult(netForce, netTorque, allOutputs);
    }

    /**
     * Derives emergent torque from a 2-point force differential.
     * <p>
     * Uses the lever arms from the CoM to each application point
     * (front/back centroids from
     * {@link com.pixel.create_ordnance.mechanics.scanning.ProjectileStats}).
     * <p>
     * {@code τ = r_back × F_back + r_front × F_front}
     */
    private Vec3 deriveEmergentTorque(ModuleOutput output, PhysicsContext context) {
        // Contract: All forces in output are already LOCAL.
        // Contract: centerFront/Back are LOCAL relative to CoM.
        Vec3 rBack = context.stats.centerBack();
        Vec3 rFront = context.stats.centerFront();

        // Torque per point (Local): τ = r_local × F_local
        Vec3 torqueBack = rBack.cross(output.force1());
        Vec3 torqueFront = rFront.cross(output.force2());

        return torqueBack.add(torqueFront);
    }

    /**
     * Result of a full engine tick. Contains the net force, net torque,
     * and the individual contributions from each module (for debugging).
     */
    public record TickResult(Vec3 netForce, Vec3 netTorque, List<ModuleOutput> moduleOutputs) {
    }
}