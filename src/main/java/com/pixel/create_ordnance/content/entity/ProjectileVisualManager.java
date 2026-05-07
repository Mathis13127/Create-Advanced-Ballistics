package com.pixel.create_ordnance.content.entity;

import com.mojang.blaze3d.vertex.PoseStack;

import com.pixel.create_ordnance.content.contraption.ProjectileContraption;
import com.pixel.create_ordnance.mechanics.physics.core.PhysicsConstants;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * ProjectileVisualManager (PROTOCOL V5.0)
 * 
 * Encapsulates all client-side interpolation, rotation logic, and rendering
 * transforms
 * to ensure that ProjectileEntity remains focused on physics and logic.
 * This class protects the interpolation system from regressions.
 */
public class ProjectileVisualManager {

    private final ProjectileEntity entity;

    // Interpolation Targets
    private double clientTargetX, clientTargetY, clientTargetZ;
    private float clientTargetYaw, clientTargetPitch;
    private final Quaternionf clientTargetOrientation = new Quaternionf();
    private int positionLerpSteps;
    private int rotationLerpSteps;
    private boolean firstSync = true;
    private final Quaternionf renderQuaternion = new Quaternionf();

    public ProjectileVisualManager(ProjectileEntity entity) {
        this.entity = entity;
    }

    /**
     * Called when networked data changes on the client.
     */
    public void onSyncedDataUpdated(EntityDataAccessor<?> key, EntityDataAccessor<Quaternionf> orientationKey) {
        if (orientationKey.equals(key)) {
            Quaternionf q = entity.getEntityData().get(orientationKey);
            this.clientTargetOrientation.set(q);

            // Only reset rotation lerp if we finished the previous one (prevents "stuck"
            // smoothing on high-freq packets)
            if (this.rotationLerpSteps <= 0) {
                this.rotationLerpSteps = PhysicsConstants.getRotationInterpolationSteps();
            }

            Vector3f noseDir = new Vector3f(0.0f, 1.0f, 0.0f);
            q.transform(noseDir);

            this.clientTargetYaw = (float) Math.toDegrees(Math.atan2(-noseDir.x, noseDir.z));
            this.clientTargetPitch = (float) Math.toDegrees(Math.asin(-noseDir.y));
        }
    }

    /**
     * Standard Minecraft lerpTo override handler.
     */
    public void lerpTo(double x, double y, double z, float yaw, float pitch, int steps) {
        this.clientTargetX = x;
        this.clientTargetY = y;
        this.clientTargetZ = z;
        this.clientTargetYaw = yaw;
        this.clientTargetPitch = pitch;
        this.positionLerpSteps = steps;

        // PROTOCOL V5.1: Initial Snap Fix
        // On the very first sync packet, we snap immediately to avoid the "sliding from
        // 0,0,0" glitch.
        if (firstSync) {
            entity.setPos(x, y, z);
            entity.xOld = x;
            entity.yOld = y;
            entity.zOld = z;
            entity.xo = x;
            entity.yo = y;
            entity.zo = z;
            entity.orientation.set(clientTargetOrientation);
            entity.setOrientation(entity.orientation);
            firstSync = false;
        }
    }

    /**
     * Handles smooth movement and rotation approach per tick on the client.
     */
    public void tickInterpolation() {
        // Handle Position Interpolation
        if (positionLerpSteps > 0) {
            double nextX = entity.getX() + (clientTargetX - entity.getX()) / (double) positionLerpSteps;
            double nextY = entity.getY() + (clientTargetY - entity.getY()) / (double) positionLerpSteps;
            double nextZ = entity.getZ() + (clientTargetZ - entity.getZ()) / (double) positionLerpSteps;

            entity.setPos(nextX, nextY, nextZ);
            this.positionLerpSteps--;
        }

        // Handle Rotation Interpolation (Slerp)
        if (rotationLerpSteps > 0) {
            entity.orientation.slerp(clientTargetOrientation, 1.0f / (float) rotationLerpSteps);
            entity.setOrientation(entity.orientation);

            // Update rotation angles for legacy renderers
            Vector3f noseDir = new Vector3f(0.0f, 1.0f, 0.0f);
            entity.orientation.transform(noseDir);
            entity.setYRot((float) Math.toDegrees(Math.atan2(-noseDir.x, noseDir.z)));
            entity.setXRot((float) Math.toDegrees(Math.asin(-noseDir.y)));

            this.rotationLerpSteps--;
        }
    }

    /**
     * Bounding box recalculation logic for rotated projectiles.
     */
    public void refreshBoundingBox() {
        ProjectileContraption contraption = entity.getProjectileContraption();
        if (contraption == null || contraption.bounds == null)
            return;

        AABB localBounds = contraption.bounds;
        Vec3[] corners = new Vec3[] {
                new Vec3(localBounds.minX, localBounds.minY, localBounds.minZ),
                new Vec3(localBounds.maxX, localBounds.minY, localBounds.minZ),
                new Vec3(localBounds.minX, localBounds.maxY, localBounds.minZ),
                new Vec3(localBounds.maxX, localBounds.maxY, localBounds.minZ),
                new Vec3(localBounds.minX, localBounds.minY, localBounds.maxZ),
                new Vec3(localBounds.maxX, localBounds.minY, localBounds.maxZ),
                new Vec3(localBounds.minX, localBounds.maxY, localBounds.maxZ),
                new Vec3(localBounds.maxX, localBounds.maxY, localBounds.maxZ)
        };

        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, minZ = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;

        Vec3 pivot = contraption.getPivotOffset();
        float scale = entity.getScale();
        Quaternionf renderOri = entity.getOrientation();

        for (Vec3 corner : corners) {
            // V5.4: Removing the -0.5 bias as pivot (CoM) already accounts for block centers.
            Vec3 p = corner.add(0.5, 0.5, 0.5).subtract(pivot).scale(scale);
            Vector3f v = new Vector3f((float) p.x, (float) p.y, (float) p.z);
            renderOri.transform(v);

            Vec3 rotated = new Vec3(v.x, v.y, v.z);

            if (rotated.x < minX)
                minX = rotated.x;
            if (rotated.x > maxX)
                maxX = rotated.x;
            if (rotated.y < minY)
                minY = rotated.y;
            if (rotated.y > maxY)
                maxY = rotated.y;
            if (rotated.z < minZ)
                minZ = rotated.z;
            if (rotated.z > maxZ)
                maxZ = rotated.z;
        }

    }

    /**
     * Rotates a local position into world space based on partialTicks.
     */
    public Vec3 applyRotation(Vec3 localPos, float partialTicks) {
        ProjectileContraption contraption = entity.getProjectileContraption();
        if (contraption == null)
            return localPos;

        Vec3 pivot = contraption.getPivotOffset();
        float scale = entity.getScale();

        Vec3 centerOffset = new Vec3(0.5, 0.5, 0.5);
        Vec3 p = localPos.add(centerOffset).subtract(pivot).scale(scale);

        renderQuaternion.set(entity.getPrevOrientation()).slerp(entity.getOrientation(), partialTicks);
        Vector3f v = new Vector3f((float) p.x, (float) p.y, (float) p.z);
        renderQuaternion.transform(v);

        return new Vec3(v.x, v.y, v.z).add(pivot).subtract(centerOffset);
    }

    /**
     * Un-rotates a global vector back into local contraption space.
     */
    public Vec3 reverseRotation(Vec3 globalPos, float partialTicks) {
        ProjectileContraption contraption = entity.getProjectileContraption();
        if (contraption == null)
            return globalPos;

        Vec3 pivot = contraption.getPivotOffset();
        float scale = entity.getScale();
        Vec3 centerOffset = new Vec3(0.5, 0.5, 0.5);

        Vec3 p = globalPos.subtract(pivot).add(centerOffset);

        renderQuaternion.set(entity.getPrevOrientation()).slerp(entity.getOrientation(), partialTicks);
        Quaternionf invOri = renderQuaternion.invert();
        Vector3f v = new Vector3f((float) p.x, (float) p.y, (float) p.z);
        invOri.transform(v);

        return new Vec3(v.x, v.y, v.z).scale(1.0 / scale).add(pivot).subtract(centerOffset);
    }

    /**
     * Applies PoseStack transformations for Contraption rendering.
     */
    public void applyLocalTransforms(PoseStack matrixStack, float partialTicks) {
        ProjectileContraption contraption = entity.getProjectileContraption();
        if (contraption == null)
            return;

        Vec3 pivot = contraption.getPivotOffset();
        float scale = entity.getScale();

        // matrixStack.translate(-0.5f, -0.5f, -0.5f); // V5.4: Removed to align with CoM origin
        //
        // nudge(id) removed to avoid Z-fighting/lighting artifacts with Flywheel scale
        // logic

        // V5.3: Since entity origin is at CoM (pivot), we only need to rotate
        // and then apply the final negative pivot to align blocks.
        renderQuaternion.set(entity.getPrevOrientation()).slerp(entity.getOrientation(), partialTicks);
        matrixStack.mulPose(renderQuaternion);
        matrixStack.scale(scale, scale, scale);
        matrixStack.translate(-pivot.x, -pivot.y, -pivot.z);
    }
}