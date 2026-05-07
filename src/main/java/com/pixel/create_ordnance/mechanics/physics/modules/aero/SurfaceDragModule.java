package com.pixel.create_ordnance.mechanics.physics.modules.aero;

import com.pixel.create_ordnance.mechanics.physics.core.IPhysicsModule;
import com.pixel.create_ordnance.mechanics.physics.core.ModuleOutput;
import com.pixel.create_ordnance.mechanics.physics.core.PhysicsConstants;
import com.pixel.create_ordnance.mechanics.physics.core.PhysicsContext;

import net.minecraft.world.phys.Vec3;

/**
 * Surface Drag Module (V7) — FORCE 2 POINTS.
 * <p>
 * Always acts as a brake: opposes the velocity vector.
 * The force magnitude depends on frontal area, fluid density, and speed².
 * <p>
 * Differential front/back frontal areas create emergent torque
 * (projectile rotates to align nosecone with velocity).
 * <p>
 * <b>Constraint:</b> Drag MUST NOT reverse velocity. It only decelerates.
 * <p>
 * AoA affects the effective frontal area (higher AoA = more exposed surface).
 * <p>
 * <b>V7 Semi-implicit stability:</b> In dense fluids (water, lava), the raw
 * drag force {@code F = ½ρv²CdA} can be enormous at high speed, causing
 * explicit Euler to overshoot velocity past zero ({@code v_new = v(1 - c·dt/m)},
 * reversing when {@code c·dt/m > 1}). Instead of outputting {@code F_raw},
 * the module outputs the semi-implicit equivalent:
 * <pre>
 *   F_eff = F_raw / (1 + c·dt/m)
 * </pre>
 * where {@code c = totalDrag / speed}. Euler then gives:
 * {@code v_new = v / (1 + c·dt/m)} — unconditionally stable, monotonically
 * decays towards zero, never reverses velocity. Both dual-point forces are
 * scaled by the same factor, preserving the force differential that creates
 * emergent torque while also stabilizing the angular response.
 */
public class SurfaceDragModule implements IPhysicsModule {

    private static final String MODULE_ID = "surface_drag";

    @Override
    public ModuleOutput compute(PhysicsContext context) {
        double speed = context.getSpeed();

        // No drag at zero speed
        if (speed < PhysicsConstants.AOA_SAFETY_EPSILON) {
            return ModuleOutput.none(MODULE_ID);
        }

        double fluidDensity = context.getEnvironmentDensity();
        double frontalMul = PhysicsConstants.getFrontalDragMultiplier();
        double lateralMul = PhysicsConstants.getLateralDragMultiplier();
        double aoa = Math.abs(context.angleOfAttack);

        // Effective frontal area increases with AoA:
        double sinAoA = Math.sin(aoa);
        double absCosAoA = Math.abs(Math.cos(aoa)); // V6.2: Ensure frontal drag is always a brake

        // Apply distinct multipliers to each component of the effective area
        double effectiveAreaBack = (context.stats.areaFrontalBack() * absCosAoA * frontalMul)
                + (context.stats.areaLateralBack() * sinAoA * lateralMul);
        double effectiveAreaFront = (context.stats.areaFrontalFront() * absCosAoA * frontalMul)
                + (context.stats.areaLateralFront() * sinAoA * lateralMul);

        // Dynamic pressure = 0.5 × ρ × v²
        double dynamicPressure = 0.5 * fluidDensity * speed * speed;

        // V6.6: Viscous Pressure (Skin Friction) ∝ Viscosity × Speed
        // Uses the "Sticky" residual viscosity to smooth transitions
        double viscousPressure = context.entity.getResidualViscosity() * speed
                * PhysicsConstants.getViscousDragMultiplier();

        // Total pressure acting on the surfaces
        double totalPressure = dynamicPressure + viscousPressure;

        // Drag force per half (opposing velocity) - PURE LOCAL
        Vec3 dragDirLocal = context.velocityLocal.normalize().scale(-1);

        // The multipliers are already baked into effectiveArea
        double dragBack = totalPressure * effectiveAreaBack;
        double dragFront = totalPressure * effectiveAreaFront;

        // V7: Semi-implicit stability — prevent Euler velocity reversal.
        //   Raw drag can be so large in dense fluids (water, lava) that
        //   explicit Euler overshoots: v_new = v(1 - c·dt/m), reversing
        //   when c·dt/m > 1.  Instead of outputting F_raw, output the
        //   semi-implicit equivalent:
        //     F_eff = F_raw / (1 + c·dt/m)
        //   where c = totalDrag / speed.
        //   Euler then gives: v_new = v / (1 + c·dt/m) — unconditionally
        //   stable, monotonically decays towards zero, never reverses.
        //   Both force1 and force2 are scaled by the SAME factor, so the
        //   emergent torque (from their differential) is also stabilized.
        double totalDrag = dragBack + dragFront;
        double implicitDenom = 1.0 + (totalDrag * context.deltaTime)
                / (context.totalMass * speed);
        dragBack /= implicitDenom;
        dragFront /= implicitDenom;

        Vec3 forceBack = dragDirLocal.scale(dragBack);
        Vec3 forceFront = dragDirLocal.scale(dragFront);

        return ModuleOutput.forceDual(MODULE_ID, forceBack, forceFront);
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
