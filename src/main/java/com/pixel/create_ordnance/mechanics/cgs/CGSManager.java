package com.pixel.create_ordnance.mechanics.cgs;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.pixel.create_ordnance.CreateOrdnance;
import com.pixel.create_ordnance.api.cgs.CGSContext;
import com.pixel.create_ordnance.api.cgs.CGSResult;
import com.pixel.create_ordnance.api.cgs.CGSType;
import com.pixel.create_ordnance.api.cgs.ICGSController;
import com.pixel.create_ordnance.api.events.ProjectileCGSEvent;
import com.pixel.create_ordnance.content.entity.ProjectileEntity;

import net.neoforged.neoforge.common.NeoForge;

/**
 * Singleton manager for CGS (Command, Guidance, Stabilization) controllers.
 * <p>
 * Manages the attachment, execution, and lifecycle of {@link ICGSController}
 * instances across all active projectile entities. Each entity can have
 * at most one controller per {@link CGSType}.
 * <p>
 * <b>Usage from addons or blocks:</b>
 * <pre>{@code
 * ICGSController myController = new ProNavController(targetPos);
 * CGSManager.getInstance().attach(projectile, myController);
 * }</pre>
 *
 * @see ICGSController
 * @see CGSContext
 */
public class CGSManager {

    private static final CGSManager INSTANCE = new CGSManager();

    /**
     * Map: EntityUUID -> (CGSType -> Controller).
     * <p>
     * ConcurrentHashMap for the outer map because cleanup() may be called
     * from entity removal callbacks. EnumMap for the inner map because
     * CGSType is a small fixed enum.
     */
    private final Map<UUID, EnumMap<CGSType, ICGSController>> controllers = new ConcurrentHashMap<>();

    public static CGSManager getInstance() {
        return INSTANCE;
    }

    private CGSManager() {
    }

    // =========================================================
    // ATTACHMENT API
    // =========================================================

    /**
     * Attaches a CGS controller to a projectile entity by UUID.
     * <p>
     * If a controller of the same {@link CGSType} is already attached,
     * it is replaced silently. Use
     * {@link #attach(ProjectileEntity, ICGSController)} for proper
     * onAttach/onDetach lifecycle callbacks.
     *
     * @param entityId   UUID of the projectile entity
     * @param controller The controller to attach
     */
    public void attach(UUID entityId, ICGSController controller) {
        EnumMap<CGSType, ICGSController> typeMap = controllers.computeIfAbsent(
                entityId, k -> new EnumMap<>(CGSType.class));

        CGSType type = controller.getType();

        ICGSController existing = typeMap.get(type);
        if (existing != null) {
            CreateOrdnance.LOGGER.debug(
                    "[CGS] Replacing controller '{}' with '{}' for entity {} (type: {})",
                    existing.getId(), controller.getId(), entityId, type.getId());
        }

        typeMap.put(type, controller);

        CreateOrdnance.LOGGER.debug(
                "[CGS] Attached controller '{}' to entity {} (type: {})",
                controller.getId(), entityId, type.getId());
    }

    /**
     * Attaches a CGS controller to a projectile entity.
     * Calls {@link ICGSController#onAttach(ProjectileEntity)} immediately
     * and properly detaches any existing controller of the same type.
     *
     * @param entity     The projectile entity
     * @param controller The controller to attach
     */
    public void attach(ProjectileEntity entity, ICGSController controller) {
        UUID entityId = entity.getUUID();
        EnumMap<CGSType, ICGSController> typeMap = controllers.computeIfAbsent(
                entityId, k -> new EnumMap<>(CGSType.class));

        CGSType type = controller.getType();

        // Detach existing controller of the same type
        ICGSController existing = typeMap.get(type);
        if (existing != null) {
            existing.onDetach(entity);
            CreateOrdnance.LOGGER.debug(
                    "[CGS] Detached controller '{}' from entity {} (replaced by '{}')",
                    existing.getId(), entityId, controller.getId());
        }

        typeMap.put(type, controller);
        controller.onAttach(entity);

        CreateOrdnance.LOGGER.debug(
                "[CGS] Attached controller '{}' to entity {} (type: {})",
                controller.getId(), entityId, type.getId());
    }

    /**
     * Detaches the controller of a specific type from an entity by UUID.
     *
     * @param entityId UUID of the projectile entity
     * @param type     The CGS type to detach
     */
    public void detach(UUID entityId, CGSType type) {
        EnumMap<CGSType, ICGSController> typeMap = controllers.get(entityId);
        if (typeMap == null)
            return;

        ICGSController removed = typeMap.remove(type);
        if (removed != null) {
            CreateOrdnance.LOGGER.debug(
                    "[CGS] Detached controller '{}' from entity {} (type: {})",
                    removed.getId(), entityId, type.getId());
        }

        if (typeMap.isEmpty()) {
            controllers.remove(entityId);
        }
    }

    /**
     * Detaches the controller of a specific type from an entity.
     * Calls {@link ICGSController#onDetach(ProjectileEntity)}.
     */
    public void detach(ProjectileEntity entity, CGSType type) {
        EnumMap<CGSType, ICGSController> typeMap = controllers.get(entity.getUUID());
        if (typeMap == null)
            return;

        ICGSController removed = typeMap.remove(type);
        if (removed != null) {
            removed.onDetach(entity);
            CreateOrdnance.LOGGER.debug(
                    "[CGS] Detached controller '{}' from entity {} (type: {})",
                    removed.getId(), entity.getUUID(), type.getId());
        }

        if (typeMap.isEmpty()) {
            controllers.remove(entity.getUUID());
        }
    }

    /**
     * Detaches ALL controllers from an entity by UUID.
     */
    public void detachAll(UUID entityId) {
        controllers.remove(entityId);
    }

    /**
     * Detaches ALL controllers from an entity.
     * Calls {@link ICGSController#onDetach(ProjectileEntity)} for each.
     */
    public void detachAll(ProjectileEntity entity) {
        EnumMap<CGSType, ICGSController> typeMap = controllers.remove(entity.getUUID());
        if (typeMap != null) {
            typeMap.values().forEach(controller -> controller.onDetach(entity));
        }
    }

    /**
     * Returns the controller attached for a specific type, or null.
     */
    public ICGSController getController(UUID entityId, CGSType type) {
        EnumMap<CGSType, ICGSController> typeMap = controllers.get(entityId);
        if (typeMap == null)
            return null;
        return typeMap.get(type);
    }

    /**
     * Returns true if any controller is attached to this entity.
     */
    public boolean hasController(UUID entityId) {
        EnumMap<CGSType, ICGSController> typeMap = controllers.get(entityId);
        return typeMap != null && !typeMap.isEmpty();
    }

    // =========================================================
    // TICK UPDATE
    // =========================================================

    /**
     * Executes all attached CGS controllers for a projectile entity.
     * <p>
     * Called once per server tick in {@code ProjectileEntity.tick()},
     * AFTER the PreTickEvent and BEFORE tickContraption/physics.
     * <p>
     * For each attached controller (per CGSType):
     * <ol>
     *   <li>Build a {@link CGSContext} snapshot from the entity</li>
     *   <li>Check {@link ICGSController#isActive(CGSContext)}</li>
     *   <li>Call {@link ICGSController#compute(CGSContext)} to get base demands</li>
     *   <li>Fire {@link ProjectileCGSEvent} for additive modification</li>
     *   <li>If not cancelled, apply final clamped demands to the entity</li>
     * </ol>
     *
     * @param entity The projectile entity to update
     */
    public void update(ProjectileEntity entity) {
        UUID entityId = entity.getUUID();
        EnumMap<CGSType, ICGSController> typeMap = controllers.get(entityId);
        if (typeMap == null || typeMap.isEmpty()) {
            return; // No controllers — demands stay at current values
        }

        // Build context once, shared by all controllers this tick
        CGSContext context = CGSContext.fromEntity(entity);

        for (Map.Entry<CGSType, ICGSController> entry : typeMap.entrySet()) {
            CGSType type = entry.getKey();
            ICGSController controller = entry.getValue();

            // Skip inactive controllers
            if (!controller.isActive(context)) {
                continue;
            }

            // Compute base result
            CGSResult baseResult;
            try {
                baseResult = controller.compute(context);
            } catch (Exception e) {
                CreateOrdnance.LOGGER.error(
                        "[CGS] Controller '{}' threw exception for entity {}. Skipping.",
                        controller.getId(), entityId, e);
                continue;
            }

            if (baseResult == null) {
                CreateOrdnance.LOGGER.warn(
                        "[CGS] Controller '{}' returned null for entity {}. Using NEUTRAL.",
                        controller.getId(), entityId);
                baseResult = CGSResult.NEUTRAL;
            }

            // Fire additive event
            ProjectileCGSEvent event = new ProjectileCGSEvent(
                    entity, context, type, baseResult);
            NeoForge.EVENT_BUS.post(event);

            if (event.isCanceled()) {
                continue; // Cancelled — demands stay at previous values
            }

            // Apply final result to entity
            CGSResult finalResult = event.getFinalResult();
            applyResult(entity, type, finalResult);
        }
    }

    /**
     * Applies a CGSResult to the entity's demand fields.
     * <p>
     * Currently, both THRUST_VECTOR and AERODYNAMIC types write to the
     * same demand fields. When AERODYNAMIC is implemented, this method
     * will dispatch to different entity data accessors based on type.
     */
    private void applyResult(ProjectileEntity entity, CGSType type, CGSResult result) {
        switch (type) {
            case THRUST_VECTOR -> {
                entity.setPitchDemand(result.pitchDemand());
                entity.setYawDemand(result.yawDemand());
                entity.setRollDemand(result.rollDemand());
                entity.setThrottle(result.throttle());

                // Debug trace: log non-neutral results so operators can verify
                if (result.pitchDemand() != 0 || result.yawDemand() != 0
                        || result.rollDemand() != 0 || result.throttle() != 1.0f) {
                    CreateOrdnance.LOGGER.debug(
                            "[CGS] Applied THRUST_VECTOR to entity {}: P={} Y={} R={} T={}",
                            entity.getUUID(), result.pitchDemand(), result.yawDemand(),
                            result.rollDemand(), result.throttle());
                }
            }
            case AERODYNAMIC -> {
                // RESERVED: Future implementation.
                CreateOrdnance.LOGGER.warn(
                        "[CGS] AERODYNAMIC type is not yet implemented. Ignoring result for entity {}.",
                        entity.getUUID());
            }
        }
    }

    // =========================================================
    // CLEANUP
    // =========================================================

    /**
     * Cleans up all controllers for a removed entity.
     * <p>
     * Called from {@code ProjectileEntity.remove()}.
     * Invokes {@link ICGSController#onDetach(ProjectileEntity)} for each attached controller.
     *
     * @param entity The entity being removed
     */
    public void cleanup(ProjectileEntity entity) {
        EnumMap<CGSType, ICGSController> typeMap = controllers.remove(entity.getUUID());
        if (typeMap != null) {
            for (ICGSController controller : typeMap.values()) {
                try {
                    controller.onDetach(entity);
                } catch (Exception e) {
                    CreateOrdnance.LOGGER.error(
                            "[CGS] Controller '{}' threw exception during cleanup for entity {}.",
                            controller.getId(), entity.getUUID(), e);
                }
            }
            CreateOrdnance.LOGGER.debug(
                    "[CGS] Cleaned up {} controller(s) for entity {}.",
                    typeMap.size(), entity.getUUID());
        }
    }
}
