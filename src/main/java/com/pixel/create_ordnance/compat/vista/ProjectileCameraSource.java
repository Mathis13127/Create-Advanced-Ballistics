package com.pixel.create_ordnance.compat.vista;

import java.lang.ref.WeakReference;
import java.util.UUID;

import javax.annotation.Nullable;

import com.pixel.create_ordnance.content.entity.ProjectileEntity;
import com.pixel.create_ordnance.mixin.compat.vista.BlockEntityAccessor;
import com.pixel.create_ordnance.mixin.compat.vista.TVBlockEntityAccessor;
import com.pixel.create_ordnance.mixin.compat.vista.ViewFinderBlockEntityAccessor;

import net.mehvahdjukaar.vista.VistaMod;
import net.mehvahdjukaar.vista.client.video_source.BroadcastVideoSource;
import net.mehvahdjukaar.vista.client.video_source.IVideoSource;
import net.mehvahdjukaar.vista.common.tv.TVBlockEntity;
import net.mehvahdjukaar.vista.common.view_finder.ViewFinderBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/**
 * Manages a virtual {@link ViewFinderBlockEntity} that mirrors a
 * {@link ProjectileEntity}'s position and orientation as a camera source
 * for Vista's rendering pipeline.
 * <p>
 * <b>How it works (Vista 3.x):</b>
 * <ol>
 *   <li>Creates a {@link VirtualViewFinderBE} — a subclass of ViewFinderBlockEntity
 *       that updates {@code worldPosition} via mixin accessor (field is final)</li>
 *   <li>Directly overrides the TV's cached {@code videoSource} field with a
 *       {@code BroadcastVideoSource(cameraUUID)}. This is necessary because
 *       {@code videoSource} is only set in {@code updateClientVisualsOnLoad()}
 *       (on server→client sync), so modifying the cassette client-side has no effect.</li>
 *   <li>Every tick, reads the projectile's world position and nose direction</li>
 *   <li>Converts the orientation quaternion to yaw/pitch and writes them
 *       directly to the virtual BE via mixin accessors</li>
 *   <li>Vista's {@code BroadcastManager.getBroadcast()} is intercepted by our mixin —
 *       when the UUID matches a projectile camera, the virtual ViewFinderBE is returned
 *       as an {@code IBroadcastProvider}</li>
 *   <li>Vista's renderer uses the virtual BE exactly like a real ViewFinder —
 *       it reads position from {@code getBlockPos().getCenter()} and rotation
 *       from {@code getPitch()} / {@code getYaw()}</li>
 * </ol>
 * <p>
 * <b>Lifecycle:</b> Created by {@link VistaCompat#createCameraLink}, ticked
 * from the client tick handler, destroyed when the projectile entity dies.
 * On cleanup, the TV's original videoSource is restored.
 */
public class ProjectileCameraSource {

    private final WeakReference<ProjectileEntity> projectileRef;
    private final int entityId;
    private final WeakReference<TVBlockEntity> linkedTVRef;
    private final UUID cameraUUID;
    @Nullable
    private final IVideoSource originalVideoSource;
    private final VirtualViewFinderBE virtualViewFinder;

    /**
     * @param projectile The projectile entity to track
     * @param tv         The TV block entity to display on
     * @param cameraUUID The unique UUID for this virtual camera
     */
    public ProjectileCameraSource(ProjectileEntity projectile, TVBlockEntity tv, UUID cameraUUID) {
        this.projectileRef = new WeakReference<>(projectile);
        this.entityId = projectile.getId();
        this.linkedTVRef = new WeakReference<>(tv);
        this.cameraUUID = cameraUUID;

        // Save original videoSource and override with our BroadcastVideoSource.
        // In Vista 3.x, the videoSource field is cached and only set during
        // updateClientVisualsOnLoad() (on server sync). Modifying the cassette
        // DataComponent client-side has NO effect. We must directly override
        // the field via mixin accessor.
        TVBlockEntityAccessor tvAccessor = (TVBlockEntityAccessor) tv;
        this.originalVideoSource = tvAccessor.ordnance$getVideoSource();
        tvAccessor.ordnance$setVideoSource(new BroadcastVideoSource(cameraUUID));

        // Create virtual ViewFinder BE
        this.virtualViewFinder = new VirtualViewFinderBE(
                BlockPos.containing(projectile.position()),
                cameraUUID);

        // CRITICAL: Vista's setupSceneCamera() calls tile.getLevel() and passes
        // it to a Display.BlockDisplay constructor. Without a Level, this NPEs.
        // We assign the projectile's level (ClientLevel on client-side).
        this.virtualViewFinder.setLevel(projectile.level());

        // Initial camera update
        tick();
    }

    // =========================================================
    // TICK — Update Camera from Projectile State
    // =========================================================

    /**
     * Updates the virtual camera's position and rotation from the
     * projectile entity's current state. Called every client tick.
     */
    public void tick() {
        ProjectileEntity proj = projectileRef.get();
        if (proj == null || proj.isRemoved()) return;

        // --- Orientation → Yaw/Pitch ---
        // We use the entity's synced Euler angles (xRot/yRot) instead of
        // the quaternion from SynchedEntityData. The quaternion suffers from
        // a stale-reference bug: SimulationEngine mutates it in-place, so
        // entityData.set() sees the same object reference → equals() returns
        // true → dirty flag never set → client never receives updates.
        // The Euler angles (setXRot/setYRot) use primitive floats, so they
        // sync correctly every tick.
        float yaw = proj.getYRot();
        float pitch = proj.getXRot();

        // --- Position: Sub-block precision at the NOSE of the projectile ---
        // The entity position is at the Center of Mass (V5.2 CoM-centric).
        // We offset forward to the nose tip using synced Euler angles.
        // Nose local Y = 0.5 + (projectileLength - 1) = projectileLength - 0.5
        // Offset from CoM = noseLocalY - centerOfMass.y
        double noseLocalY = proj.getProjectileLength() - 0.5;
        double noseOffset = noseLocalY - proj.getCenterOfMass().y;
        Vec3 forward = Vec3.directionFromRotation(pitch, yaw);
        Vec3 nosePos = proj.position().add(forward.scale(noseOffset));

        // Set precise position for the VistaLevelRendererMixin to use
        virtualViewFinder.setPrecisePosition(nosePos);
        // Keep BlockPos updated as fallback for getBlockPos()
        virtualViewFinder.setDynamicPosition(BlockPos.containing(nosePos));

        // Smooth interpolation: shift current → prev, set new current
        ViewFinderBlockEntityAccessor accessor = (ViewFinderBlockEntityAccessor) virtualViewFinder;
        accessor.ordnance$setPrevPitch(accessor.ordnance$getPitch());
        accessor.ordnance$setPrevYaw(accessor.ordnance$getYaw());
        accessor.ordnance$setPitch(pitch);
        accessor.ordnance$setYaw(yaw);
    }

    // =========================================================
    // LIFECYCLE
    // =========================================================

    /**
     * @return true if the tracked projectile entity still exists
     */
    public boolean isAlive() {
        ProjectileEntity proj = projectileRef.get();
        return proj != null && !proj.isRemoved();
    }

    /**
     * Restores the TV's original videoSource and releases resources.
     * Called when the projectile dies.
     */
    public void cleanup() {
        TVBlockEntity tv = linkedTVRef.get();
        if (tv != null && !tv.isRemoved()) {
            ((TVBlockEntityAccessor) tv).ordnance$setVideoSource(
                    originalVideoSource != null ? originalVideoSource : IVideoSource.EMPTY);
        }
    }

    // =========================================================
    // ACCESSORS
    // =========================================================

    public int getEntityId() {
        return entityId;
    }

    public UUID getCameraUUID() {
        return cameraUUID;
    }

    public ViewFinderBlockEntity getVirtualViewFinder() {
        return virtualViewFinder;
    }

    // =========================================================
    // INNER CLASS — Virtual ViewFinder Block Entity
    // =========================================================

    /**
     * A ViewFinderBlockEntity subclass with sub-block-precision positioning.
     * <p>
     * Implements {@link IPrecisePositionCamera} so that the
     * {@link com.pixel.create_ordnance.mixin.compat.vista.VistaLevelRendererMixin}
     * can override the camera position with a precise Vec3 instead of
     * the default {@code getBlockPos().getCenter()} (integer-snapped).
     * <p>
     * In MC 1.21.1, {@code BlockEntity.worldPosition} is {@code final},
     * so we use a {@link BlockEntityAccessor} mixin (with {@code @Mutable})
     * to update the BlockPos for fallback compatibility.
     */
    static class VirtualViewFinderBE extends ViewFinderBlockEntity implements IPrecisePositionCamera {

        private Vec3 precisePosition;

        VirtualViewFinderBE(BlockPos initialPos, UUID cameraUUID) {
            super(initialPos, VistaMod.VIEWFINDER.get().defaultBlockState());
            // Override the auto-generated UUID with our camera UUID
            ((ViewFinderBlockEntityAccessor) this).ordnance$setMyUUID(cameraUUID);
            this.precisePosition = Vec3.atCenterOf(initialPos);
        }

        void setDynamicPosition(BlockPos pos) {
            ((BlockEntityAccessor) this).ordnance$setWorldPosition(pos);
        }

        void setPrecisePosition(Vec3 pos) {
            this.precisePosition = pos;
        }

        @Override
        public Vec3 getPrecisePosition() {
            return precisePosition;
        }
    }
}
