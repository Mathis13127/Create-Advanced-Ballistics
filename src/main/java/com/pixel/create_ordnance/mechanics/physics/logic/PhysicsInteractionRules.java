package com.pixel.create_ordnance.mechanics.physics.logic;

import com.pixel.create_ordnance.config.OrdnanceCommonConfig.ThrustType;
import com.pixel.create_ordnance.content.entity.ProjectileEntity;
import com.pixel.create_ordnance.mechanics.physics.core.PhysicsRegistry;
import com.pixel.create_ordnance.mechanics.physics.core.PhysicsRegistry.PhysicsProfile;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import org.joml.Vector3f;

/**
 * Handles "Edge Case" physics logic that isn't part of the standard
 * force/torque pipeline.
 * Evaluated before the main PhysicsEngine tick.
 */
public class PhysicsInteractionRules {

    public static void applyRules(ProjectileEntity entity) {
        if (entity.level().isClientSide)
            return;

        // --- RULE 1: Afterburners extinguish in fluid ---
        if (entity.getThrustType() == ThrustType.AFTERBURNER && !entity.isExtinguished()) {
            checkAfterburnerExtinguish(entity);
        }
    }

    private static void checkAfterburnerExtinguish(ProjectileEntity entity) {
        // Sample environment at the back (where the nozzle is)
        Vec3 pos = entity.position();
        Vector3f backLocal = new Vector3f((float) entity.getCenterBack().x, (float) entity.getCenterBack().y,
                (float) entity.getCenterBack().z);
        entity.getOrientation().transform(backLocal);
        Vec3 backPos = pos.add(backLocal.x, backLocal.y, backLocal.z);

        BlockState state = entity.level().getBlockState(BlockPos.containing(backPos));
        PhysicsProfile profile = PhysicsRegistry.get(state);

        if (profile.isFluid()) {
            entity.setExtinguished(true);
            // Optional: trigger a sound or particles here in the future
        }
    }
}