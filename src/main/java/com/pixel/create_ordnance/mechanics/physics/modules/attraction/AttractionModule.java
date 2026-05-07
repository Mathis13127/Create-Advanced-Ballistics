package com.pixel.create_ordnance.mechanics.physics.modules.attraction;

import java.util.Collection;

import com.pixel.create_ordnance.api.physics.AttractionParams;
import com.pixel.create_ordnance.config.OrdnanceConfigs;
import com.pixel.create_ordnance.mechanics.physics.attraction.AttractionSource;
import com.pixel.create_ordnance.mechanics.physics.attraction.SpatialForceManager;
import com.pixel.create_ordnance.mechanics.physics.core.IPhysicsModule;
import com.pixel.create_ordnance.mechanics.physics.core.ModuleOutput;
import com.pixel.create_ordnance.mechanics.physics.core.PhysicsContext;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Attraction Module (V2 Protocol) — FORCE WITH TORQUE.
 * <p>
 * Computes gravitational-like attraction forces from all active
 * {@link AttractionSource}s managed by {@link SpatialForceManager}.
 * <p>
 * For each source within range and in the same dimension:
 * <ol>
 *   <li><b>Radial force:</b> {@code strength / distance^exponent} towards the source</li>
 *   <li><b>PD orbital controller:</b> if {@code targetRadius > 0}, corrects distance
 *       to maintain orbital holding distance</li>
 *   <li><b>Mass dependency:</b> if {@code massDependent}, force is multiplied by mass
 *       (so all projectiles experience the same acceleration)</li>
 *   <li><b>Vortex force:</b> tangential force for spiral orbits</li>
 *   <li><b>Orient towards:</b> torque that aligns the projectile's nose toward the source</li>
 * </ol>
 * <p>
 * Forces are computed in WORLD space, then transformed to LOCAL space
 * following the same pattern as {@code GravityModule} (Quaternionf.invert → transform).
 */
public class AttractionModule implements IPhysicsModule {

    private static final String MODULE_ID = "attraction";

    /** Minimum distance to avoid singularity at source center. */
    private static final double MIN_DISTANCE = 0.1;

    @Override
    public ModuleOutput compute(PhysicsContext context) {
        // Global kill switch
        if (!OrdnanceConfigs.COMMON.physics.attraction.enabled.get()) {
            return ModuleOutput.none(MODULE_ID);
        }

        // Manager not yet initialized (server not started)
        SpatialForceManager manager = SpatialForceManager.getInstance();
        if (manager == null) {
            return ModuleOutput.none(MODULE_ID);
        }

        Collection<AttractionSource> sources = manager.getActiveSources();
        if (sources.isEmpty()) {
            return ModuleOutput.none(MODULE_ID);
        }

        ResourceKey<Level> entityDim = context.entity.level().dimension();
        Vec3 entityPos = context.position;
        Vec3 entityVel = context.velocityWorld;
        double mass = context.totalMass;

        Vec3 totalForceWorld = Vec3.ZERO;
        Vec3 totalTorqueLocal = Vec3.ZERO;

        for (AttractionSource source : sources) {
            // Skip sources in different dimensions
            if (!source.getDimension().equals(entityDim)) continue;

            AttractionParams params = source.getParams();
            Vec3 toSource = source.getPosition().subtract(entityPos);
            double distance = toSource.length();

            // Skip if out of range or at singularity
            if (distance > params.maxRange() || distance < MIN_DISTANCE) continue;

            Vec3 direction = toSource.normalize();

            // ─── 1. BASE RADIAL FORCE: k / r^n (towards source) ───
            double radialMagnitude = params.strength() / Math.pow(distance, params.exponent());

            // ─── 2. PD ORBITAL CONTROLLER ───
            if (params.targetRadius() > 0) {
                double error = distance - params.targetRadius();
                double radialSpeed = entityVel.dot(direction); // positive = moving towards source
                double pdCorrection = params.kp() * error - params.kd() * radialSpeed;
                // PD overrides the base radial force for orbital behavior
                radialMagnitude = pdCorrection;
            }

            // ─── 3. MASS DEPENDENCY ───
            // massDependent = true → F = m × a → constant acceleration for all masses
            // massDependent = false → F = a → lighter projectiles accelerate more
            if (params.massDependent()) {
                radialMagnitude *= mass;
            }

            Vec3 radialForce = direction.scale(radialMagnitude);

            // ─── 4. VORTEX (TANGENTIAL) FORCE ───
            Vec3 vortexForce = Vec3.ZERO;
            if (Math.abs(params.vortexFactor()) > 1e-6) {
                // Tangent = perpendicular to both UP and direction-to-source
                Vec3 up = new Vec3(0, 1, 0);
                Vec3 tangent = up.cross(direction);
                if (tangent.lengthSqr() < 1e-6) {
                    // Direction is nearly vertical, use X axis as fallback
                    tangent = new Vec3(1, 0, 0).cross(direction);
                }
                tangent = tangent.normalize();
                vortexForce = tangent.scale(params.vortexFactor() * Math.abs(radialMagnitude));
            }

            totalForceWorld = totalForceWorld.add(radialForce).add(vortexForce);

            // ─── 5. ORIENT TOWARDS (TORQUE) ───
            if (params.orientTowards() > 0) {
                Vec3 noseDir = context.getNoseDirection();
                Vec3 cross = noseDir.cross(direction);
                // Scale torque by orientTowards factor and force magnitude
                Vec3 orientTorqueWorld = cross.scale(params.orientTowards() * Math.abs(radialMagnitude) * 0.1);

                // Convert torque from WORLD to LOCAL
                Vector3f torqueLocalF = new Vector3f(
                        (float) orientTorqueWorld.x, (float) orientTorqueWorld.y, (float) orientTorqueWorld.z);
                Quaternionf invOrientation = new Quaternionf(context.orientation).invert();
                invOrientation.transform(torqueLocalF);
                totalTorqueLocal = totalTorqueLocal.add(
                        new Vec3(torqueLocalF.x, torqueLocalF.y, torqueLocalF.z));
            }
        }

        // No force from any source
        if (totalForceWorld.lengthSqr() < 1e-12 && totalTorqueLocal.lengthSqr() < 1e-12) {
            return ModuleOutput.none(MODULE_ID);
        }

        // ─── CONVERT FORCE: WORLD → LOCAL ───
        // Same pattern as GravityModule (lines 34-40)
        Vector3f forceLocalF = new Vector3f(
                (float) totalForceWorld.x, (float) totalForceWorld.y, (float) totalForceWorld.z);
        Quaternionf invOrientation = new Quaternionf(context.orientation).invert();
        invOrientation.transform(forceLocalF);
        Vec3 forceLocal = new Vec3(forceLocalF.x, forceLocalF.y, forceLocalF.z);

        if (totalTorqueLocal.lengthSqr() > 1e-12) {
            return ModuleOutput.forceSimpleWithTorque(MODULE_ID, forceLocal, totalTorqueLocal);
        }
        return ModuleOutput.forceSimple(MODULE_ID, forceLocal);
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
        return ModuleCapability.FORCE_WITH_TORQUE;
    }
}