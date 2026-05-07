package com.pixel.create_ordnance.mechanics.physics.modules.environment;

import com.pixel.create_ordnance.mechanics.physics.core.IPhysicsModule;
import com.pixel.create_ordnance.mechanics.physics.core.ModuleOutput;
import com.pixel.create_ordnance.mechanics.physics.core.PhysicsConstants;
import com.pixel.create_ordnance.mechanics.physics.core.PhysicsContext;
import com.pixel.create_ordnance.mechanics.physics.core.PhysicsRegistry;
import com.pixel.create_ordnance.mechanics.physics.core.PhysicsRegistry.PhysicsProfile;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Buoyancy Module (V7) — FORCE 2 POINTS.
 * <p>
 * Applies Archimedes' buoyancy as a vertical force proportional to
 * displaced fluid density. Different volume distributions at front/back
 * create emergent torque via differential lift.
 * <p>
 * {@code F_buoyancy = volume × fluidDensity × submersionFactor × buoyancyMultiplier × gravity}
 * <p>
 * <b>V7 — Continuous submersion gradient:</b> At fluid surfaces, the
 * submersion factor varies linearly with the point's depth inside the
 * surface block (0.0 at the top → 1.0 at the bottom). This eliminates
 * the discrete 0/0.5/1.0 steps that caused oscillation when a point
 * repeatedly crossed the air/fluid boundary. Deep underwater (fluid
 * above the point) the factor is always 1.0.
 * <p>
 * Uses {@link PhysicsConstants#getGlobalBuoyancyMultiplier()} and environment
 * density from {@link PhysicsContext#environmentProfile}.
 */
public class BuoyancyModule implements IPhysicsModule {

    private static final String MODULE_ID = "buoyancy";

    @Override
    public ModuleOutput compute(PhysicsContext context) {
        double buoyancyMul = PhysicsConstants.getGlobalBuoyancyMultiplier();
        double gravity = PhysicsConstants.getGravityStrength();

        // Front/back volumes from analyzer
        double volumeBack = context.stats.volumeBack() * PhysicsConstants.BASE_BLOCK_VOLUME;
        double volumeFront = context.stats.volumeFront() * PhysicsConstants.BASE_BLOCK_VOLUME;

        // V6.4: Smooth Buoyancy Gradient
        // Instead of binary density, we sample the fluid 0.5 blocks above the point
        // to determine if it's "partially" submerged.

        double backSubmersion = getSubmersionFactor(context.backProfile, context.entity.level(), context.backPos);
        double frontSubmersion = getSubmersionFactor(context.frontProfile, context.entity.level(), context.frontPos);

        double forceBack = volumeBack * context.getBackDensity() * backSubmersion * buoyancyMul * gravity;
        double forceFront = volumeFront * context.getFrontDensity() * frontSubmersion * buoyancyMul * gravity;

        // Buoyancy is a WORLD FORCE: (0, buoyancy, 0)
        Vec3 buoyancyBackWorld = new Vec3(0, forceBack, 0);
        Vec3 buoyancyFrontWorld = new Vec3(0, forceFront, 0);

        // Transform to LOCAL Space: F_local = q_inv * F_world
        Quaternionf invOrientation = new Quaternionf(context.orientation).invert();

        Vector3f f1Local = new Vector3f((float) buoyancyBackWorld.x, (float) buoyancyBackWorld.y,
                (float) buoyancyBackWorld.z);
        Vector3f f2Local = new Vector3f((float) buoyancyFrontWorld.x, (float) buoyancyFrontWorld.y,
                (float) buoyancyFrontWorld.z);

        invOrientation.transform(f1Local);
        invOrientation.transform(f2Local);

        return ModuleOutput.forceDual(MODULE_ID,
                new Vec3(f1Local.x, f1Local.y, f1Local.z),
                new Vec3(f2Local.x, f2Local.y, f2Local.z));
    }

    /**
     * V7: Continuous submersion factor based on depth within the surface block.
     * <p>
     * <b>Behaviour:</b>
     * <ul>
     * <li>Point in air → <b>0.0</b></li>
     * <li>Point in fluid, fluid above → <b>1.0</b> (fully submerged)</li>
     * <li>Point in fluid, air above → <b>depthFraction</b> (0.0 at block
     * top → 1.0 at block bottom). This is the fraction of the surface block
     * below the point, approximating the submerged volume ratio.</li>
     * </ul>
     * <p>
     * <b>Physics:</b> Archimedes' buoyancy is proportional to displaced volume.
     * At a fluid surface, the displaced volume varies continuously with
     * immersion depth. The depth fraction within the surface block is a
     * first-order approximation of this continuous variation, eliminating the
     * discrete force jumps (0 → 0.5 → 1.0) that caused oscillation at
     * registry boundaries.
     */
    private double getSubmersionFactor(PhysicsProfile profile, Level level,
            Vec3 pos) {
        if (!profile.isFluid())
            return 0.0;

        // Check if fully submerged (fluid block above this point)
        Vec3 topSample = pos.add(0, 0.5, 0);
        PhysicsProfile topProfile = PhysicsRegistry
                .get(level.getBlockState(BlockPos.containing(topSample)));

        if (topProfile.isFluid()) {
            return 1.0; // Deep underwater — full buoyancy
        }

        // At the fluid surface: continuous depth fraction.
        // depthFraction = 0.0 when pos.y is at the top of the block (barely in),
        //                 1.0 when pos.y is at the bottom of the block (fully in).
        double depthFraction = 1.0 - (pos.y - Math.floor(pos.y));
        return Math.max(0.0, Math.min(1.0, depthFraction));
    }

    @Override
    public String getModuleId() {
        return MODULE_ID;
    }

    @Override
    public Phase getPhase() {
        return Phase.FORCE;
    }

    @Override
    public ModuleCapability getCapability() {
        return ModuleCapability.FORCE_DUAL;
    }
}