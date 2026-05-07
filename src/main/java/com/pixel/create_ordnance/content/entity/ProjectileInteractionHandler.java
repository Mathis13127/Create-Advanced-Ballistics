package com.pixel.create_ordnance.content.entity;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.pixel.create_ordnance.api.events.ProjectileExplodeEvent;
import com.pixel.create_ordnance.api.events.ProjectileHurtEvent;
import com.pixel.create_ordnance.api.events.ProjectileImpactEvent;
import com.pixel.create_ordnance.config.OrdnanceConfigs;
import com.pixel.create_ordnance.content.contraption.ProjectileContraption;
import com.pixel.create_ordnance.mechanics.logic.LogicContext;
import com.pixel.create_ordnance.mechanics.logic.ProjectileBehavior;
import com.pixel.create_ordnance.mechanics.scanning.ScrapResult;
import com.pixel.create_ordnance.registry.ProjectileComponentRegistry;
import com.pixel.create_ordnance.registry.ProjectileComponentType;
import com.pixel.create_ordnance.registry.ProjectileData;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;

import net.minecraft.server.level.ServerLevel;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import net.minecraft.tags.DamageTypeTags;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

import net.neoforged.neoforge.common.NeoForge;

import org.joml.Vector3f;

public class ProjectileInteractionHandler {

    /**
     * Triggers a component event based on its index in the projectile structure.
     */
    public static void triggerComponentEvent(ProjectileEntity e, int index, String eventType) {
        ScrapResult result = e.getScrapResult();
        if (result == null || index < 0 || index >= result.blockIds().size())
            return;

        Block block = BuiltInRegistries.BLOCK.get(result.blockIds().get(index));
        Optional<ProjectileData> dataOp = ProjectileComponentRegistry.getData(block);
        if (dataOp.isEmpty())
            return;

        // Position Calculation: structure is linear along Y (0 to Length)
        // We transform (0, index + 0.5, 0) to world space
        Vec3 localPos = new Vec3(0.5, index + 0.5, 0.5);
        Vec3 worldPos = e.position().add(Vec3.directionFromRotation(e.getXRot(), e.getYRot()).scale(localPos.y)); // Simple
                                                                                                                  // approximation?
        // Better: Use the orientation quaternion
        Vec3 transformedPos = e.position().add(
                e.getOrientation()
                        .transform(new Vector3f((float) localPos.x, (float) localPos.y, (float) localPos.z)).x,
                e.getOrientation()
                        .transform(new Vector3f((float) localPos.x, (float) localPos.y, (float) localPos.z)).y,
                e.getOrientation().transform(
                        new Vector3f((float) localPos.x, (float) localPos.y, (float) localPos.z)).z);

        // Actually, ProjectileEntity has the orientation. Let's use a helper.
        Vec3 finalPos = e.getTransformedLocalPos(localPos);

        triggerComponentEventWithCustomPos(e, dataOp.get(), eventType, finalPos);
    }

    public static void triggerComponentEventWithCustomPos(ProjectileEntity e, ProjectileData data, String eventType,
            Vec3 customPos) {

        ProjectileBehavior behavior = data.behavior();
        if (behavior == null)
            return;

        // Resolve all property suppliers to actual values
        Map<String, Object> resolvedParams = new HashMap<>();
        data.allProperties().forEach((k, prop) -> {
            Object val = prop.get();
            if (val != null) {
                resolvedParams.put(k, val);
            }
        });

        LogicContext ctx = new LogicContext(
                e.level(), e, customPos,
                net.minecraft.core.BlockPos.containing(customPos),
                e.getYRot(), e.getXRot(),
                resolvedParams);

        switch (eventType) {
            case "SETUP" -> behavior.onSetup(ctx);
            case "TICK" -> behavior.onTick(ctx);
            case "TRIGGER" -> behavior.onTrigger(ctx);
        }
    }

    public static void impact(ProjectileEntity e) {
        if (e.level().isClientSide)
            return;

        ProjectileImpactEvent event = new ProjectileImpactEvent(
                e, null);

        NeoForge.EVENT_BUS.post(event);
        if (event.isCanceled()) {
            return;
        }

        // 1. Immediate Stop
        e.setDeltaMovement(Vec3.ZERO);

        // 2. Trigger Payload (usually at the end of the list)
        ScrapResult result = e.getScrapResult();
        if (result != null) {
            for (int i = 0; i < result.blockIds().size(); i++) {
                Block block = BuiltInRegistries.BLOCK.get(result.blockIds().get(i));
                Optional<ProjectileData> data = ProjectileComponentRegistry.getData(block);
                if (data.isPresent() && data.get().type() == ProjectileComponentType.PAYLOAD) {
                    triggerComponentEvent(e, i, "TRIGGER");
                }
            }
        }

        // 3. Destroy
        e.discard();
    }

    public static boolean handleHurt(ProjectileEntity e, DamageSource source, float amount) {
        ProjectileHurtEvent event = new ProjectileHurtEvent(
                e, source, amount);

        NeoForge.EVENT_BUS.post(event);
        if (event.isCanceled()) {
            return false;
        }

        if (source.is(DamageTypes.FLY_INTO_WALL))
            return false;
        if (source.is(DamageTypes.FALL))
            return false;

        boolean isExplosion = source.is(DamageTypeTags.IS_EXPLOSION);
        boolean isDirectHit = source.getDirectEntity() != null;

        if (isExplosion || isDirectHit) {
            if (!e.level().isClientSide) {
                e.isRemoving = true;
                boolean payloadTriggered = false;

                var contraption = e.getContraption();
                e.discard();

                if (contraption instanceof ProjectileContraption pc) {
                    ProjectileExplodeEvent explodeEvent = new ProjectileExplodeEvent(
                            e, pc);

                    NeoForge.EVENT_BUS.post(explodeEvent);
                    if (explodeEvent.isCanceled()) {
                        return true;
                    }

                    boolean allowTrigger = OrdnanceConfigs.COMMON.divers.mixins.triggerPayloadOnExplosion
                            .get();

                    ScrapResult result = e.getScrapResult();
                    if (allowTrigger && result != null) {
                        for (int i = 0; i < result.blockIds().size(); i++) {
                            Block block = BuiltInRegistries.BLOCK.get(result.blockIds().get(i));
                            Optional<ProjectileData> data = ProjectileComponentRegistry.getData(block);
                            if (data.isPresent() && data.get().type() == ProjectileComponentType.PAYLOAD) {
                                triggerComponentEvent(e, i, "TRIGGER");
                                payloadTriggered = true;
                            }
                        }
                    }
                }

                if (!payloadTriggered) {
                    e.level().playSound(null, e.getX(), e.getY(), e.getZ(),
                            SoundEvents.GENERIC_EXPLODE,
                            SoundSource.NEUTRAL, 0.5f, 1.0f);
                    ((ServerLevel) e.level()).sendParticles(
                            ParticleTypes.EXPLOSION,
                            e.getX(), e.getY(), e.getZ(), 1, 0, 0, 0, 0);
                }
            }
            return true;
        }
        return false;
    }
}