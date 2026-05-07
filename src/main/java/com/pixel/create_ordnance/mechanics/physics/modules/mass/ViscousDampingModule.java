package com.pixel.create_ordnance.mechanics.physics.modules.mass;

import com.pixel.create_ordnance.mechanics.physics.core.IPhysicsModule;
import com.pixel.create_ordnance.mechanics.physics.core.ModuleOutput;
import com.pixel.create_ordnance.mechanics.physics.core.PhysicsConstants;
import com.pixel.create_ordnance.mechanics.physics.core.PhysicsContext;

import net.minecraft.world.phys.Vec3;

import org.joml.Vector3f;

/**
 * Viscous Damping Module (V7) — TORQUE ONLY.
 * <p>
 * Opposes the projectile's angular velocity (spin rate).
 * This is a pure rotational drag — no linear force contribution.
 * <p>
 * <b>V7 Two-regime angular drag</b> (mirrors SurfaceDragModule's dual-pressure
 * model for linear drag):
 * <ul>
 * <li><b>Inertial (pressure) drag:</b> τ ∝ ρ × |ω| × ω × A × r² — dominates
 * at high angular velocity and in dense fluids (e.g. water). Quadratic in ω,
 * proportional to fluid density.</li>
 * <li><b>Viscous (friction) drag:</b> τ ∝ μ × ω × A × r² — dominates at low
 * angular velocity and in viscous fluids (e.g. lava). Linear in ω,
 * proportional to fluid viscosity.</li>
 * </ul>
 * <p>
 * <b>Physics:</b> A surface element at lever arm r from the CoM has tangential
 * velocity v = ω × r. The drag on that element follows the standard drag
 * equation: F = ½ρv²CdA = ½ρ(ωr)²CdA. The resulting torque is F × r =
 * ½ρω²r³CdA. Summing over front/back halves with their respective
 * areas and lever arms yields the inertial component. The viscous
 * (Stokes-regime) component adds a linear-in-ω floor for low-speed damping.
 * <p>
 * <b>V7 Semi-implicit stability:</b> The raw damping coefficient {@code c} can
 * be very large in dense fluids, causing explicit Euler to overshoot
 * ({@code ω_new = ω(1 - c·dt/I)}, unstable when {@code c·dt/I > 2}).
 * Instead of outputting {@code τ = -ω·c} directly, we output the
 * <b>semi-implicit equivalent</b>:
 * <pre>
 *   τ_eff = -ω × c / (1 + c·dt/I)
 * </pre>
 * When Euler integrates this, the result is identical to semi-implicit
 * integration: {@code ω_new = ω / (1 + c·dt/I)} — unconditionally stable,
 * never reverses ω, never amplifies. In weak-damping regimes (air), the
 * denominator ≈ 1 and the output is unchanged.
 * <p>
 * <b>Phase:</b> TORQUE — runs after all force modules to read accumulated
 * torques from Phase 1.
 */
public class ViscousDampingModule implements IPhysicsModule {

    private static final String MODULE_ID = "viscous_damping";

    @Override
    public ModuleOutput compute(PhysicsContext context) {
        Vector3f angVel = context.angularVelocity;

        // No damping if not spinning
        if (angVel.lengthSquared() < 1e-10) {
            return ModuleOutput.none(MODULE_ID);
        }

        double viscousMul = PhysicsConstants.getAngularDampingMultiplier();

        // V6.5: Use centralized Sticky Viscosity Hysteresis from entity state
        double envViscosity = context.entity.getResidualViscosity();
        double fluidDensity = context.getEnvironmentDensity();

        // 1. Geometric Damping Factor (Mass-independent)
        // Torque depends on surface area exposed and the distance from the pivot
        // (lever arm).
        double rBack = Math.abs(context.stats.centerBack().y);
        double rFront = Math.abs(context.stats.centerFront().y);

        // Sum of (Area × Radius²) for the two application points
        double geometricFactor = (context.stats.areaLateralBack() * rBack * rBack)
                + (context.stats.areaLateralFront() * rFront * rFront);

        // 2. Angular speed for the quadratic (inertial) component
        double angSpeed = Math.sqrt(angVel.x * angVel.x + angVel.y * angVel.y + angVel.z * angVel.z);

        // 3. Two-regime damping coefficient:
        //    Inertial: ½ × ρ × |ω| × geometry  (quadratic in ω when applied to -ω)
        //    Viscous:  μ × geometry              (linear in ω)
        double inertialDamping = 0.5 * fluidDensity * angSpeed * geometricFactor;
        double viscousDamping = envViscosity * geometricFactor;

        double rawDamping = (inertialDamping + viscousDamping) * viscousMul;

        // 4. Semi-implicit stability: prevent Euler overshoot.
        //    Compute MOI (same formula as SimulationEngine.integrateAngular)
        //    to express the output as the semi-implicit equivalent torque.
        //    τ_eff = -ω × c / (1 + c·dt/I)
        //    → Euler gives: ω_new = ω / (1 + c·dt/I)  [unconditionally stable]
        double length = context.stats.centerFront().y - context.stats.centerBack().y;
        if (length < PhysicsConstants.AOA_SAFETY_EPSILON) length = 1.0;
        double moi = context.totalMass * length * length / 12.0;
        if (moi < PhysicsConstants.AOA_SAFETY_EPSILON) moi = 1.0;

        double dt = context.deltaTime;
        double implicitDenom = 1.0 + (rawDamping * dt / moi);
        double effectiveDamping = rawDamping / implicitDenom;

        Vec3 dampingTorqueLocal = new Vec3(
                -angVel.x * effectiveDamping,
                -angVel.y * effectiveDamping,
                -angVel.z * effectiveDamping);

        return ModuleOutput.torqueOnly(MODULE_ID, dampingTorqueLocal);
    }

    @Override
    public String getModuleId() {
        return MODULE_ID;
    }

    @Override
    public Phase getPhase() {
        return Phase.TORQUE;
    }

    @Override
    public ModuleCapability getCapability() {
        return ModuleCapability.TORQUE_ONLY;
    }
}
