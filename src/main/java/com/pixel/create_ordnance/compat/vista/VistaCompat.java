package com.pixel.create_ordnance.compat.vista;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import com.pixel.create_ordnance.CreateOrdnance;
import com.pixel.create_ordnance.content.entity.ProjectileEntity;

import com.pixel.create_ordnance.mixin.compat.vista.TVBlockInvoker;

import net.mehvahdjukaar.vista.common.cassette.IBroadcastProvider;
import net.mehvahdjukaar.vista.common.tv.TVBlock;
import net.mehvahdjukaar.vista.common.tv.TVBlockEntity;

import net.minecraft.world.level.block.state.BlockState;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Entry point for Vista camera mod integration.
 * <p>
 * This class is <b>only loaded when Vista is present</b> — the classloader
 * never touches it otherwise, thanks to the {@code Mods.VISTA.executeIfInstalled()}
 * double-Supplier pattern.
 * <p>
 * <b>Functionality:</b>
 * <ul>
 *   <li>Manages the lifecycle of projectile→TV camera links</li>
 *   <li>Creates virtual {@link net.mehvahdjukaar.vista.common.view_finder.ViewFinderBlockEntity}
 *       instances that mirror projectile position/orientation</li>
 *   <li>Provides the bridge between the Mixin on {@code BroadcastManager.getBroadcast()}
 *       and the camera source data</li>
 * </ul>
 * <p>
 * <b>Architecture:</b> All camera management is client-side. The server only
 * passes {@code linkedTvPos} via ProjectileEntity's NBT. The client detects
 * this field, creates a virtual camera, and overrides the TV cassette's
 * {@code LINKED_FEED_COMPONENT} so Vista's rendering pipeline renders the
 * projectile's POV.
 */
public class VistaCompat {

    // =========================================================
    // CAMERA REGISTRY (Client-side only)
    // =========================================================

    /** Camera UUID → ProjectileCameraSource mapping. */
    private static final Map<UUID, ProjectileCameraSource> activeCameras = new HashMap<>();

    /** Entity IDs we've already processed (avoid creating duplicate links). */
    private static final Set<Integer> trackedEntityIds = new HashSet<>();

    // =========================================================
    // INITIALIZATION
    // =========================================================

    /**
     * Called from {@code CreateOrdnance.commonSetup()} via
     * {@code Mods.VISTA.executeIfInstalled(() -> VistaCompat::init)}.
     * <p>
     * Registers client-side event listeners for camera management.
     */
    public static void init() {
        CreateOrdnance.LOGGER.info("[Ordnance] Vista compat module loaded.");

        if (FMLEnvironment.dist == Dist.CLIENT) {
            NeoForge.EVENT_BUS.addListener(VistaCompat::onClientTick);
            CreateOrdnance.LOGGER.info("[Ordnance] Vista client-side camera system registered.");
        }
    }

    // =========================================================
    // CLIENT TICK — Camera Lifecycle Management
    // =========================================================

    /**
     * Client tick handler. Scans for new projectiles with linked TVs,
     * updates active cameras, and cleans up dead ones.
     */
    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.isPaused()) return;

        // 1. Scan for new projectiles with linkedTvPos
        for (Entity entity : level.entitiesForRendering()) {
            if (entity instanceof ProjectileEntity proj
                    && !trackedEntityIds.contains(proj.getId())) {
                BlockPos tvPos = proj.getLinkedTvPos();
                if (tvPos != null) {
                    trackedEntityIds.add(proj.getId());
                    createCameraLink(level, proj, tvPos);
                }
            }
        }

        // 2. Update active cameras and cleanup dead ones
        Iterator<Map.Entry<UUID, ProjectileCameraSource>> it = activeCameras.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, ProjectileCameraSource> entry = it.next();
            ProjectileCameraSource source = entry.getValue();

            if (source.isAlive()) {
                source.tick();
            } else {
                source.cleanup();
                trackedEntityIds.remove(source.getEntityId());
                it.remove();
                CreateOrdnance.LOGGER.debug(
                        "[Ordnance/Vista] Camera {} destroyed (projectile dead).",
                        entry.getKey());
            }
        }
    }

    // =========================================================
    // CAMERA LINK CREATION
    // =========================================================

    /**
     * Creates a camera link between a projectile and a TV.
     * <p>
     * The TV must contain a cassette item. The cassette's
     * {@code LINKED_FEED_COMPONENT} is overridden with the virtual camera UUID
     * so that Vista's rendering pipeline resolves to our virtual ViewFinderBE
     * via the {@link LiveFeedConnectionManagerMixin BroadcastManager mixin}.
     *
     * @param level The client-side level
     * @param proj  The projectile entity with a linkedTvPos
     * @param tvPos The position of the TV block
     */
    private static void createCameraLink(Level level, ProjectileEntity proj, BlockPos tvPos) {
        // Resolve the TV block entity (must be the master tile for multi-block TVs)
        if (!(level.getBlockEntity(tvPos) instanceof TVBlockEntity tv)) {
            CreateOrdnance.LOGGER.warn(
                    "[Ordnance/Vista] No TVBlockEntity found at {} — link skipped.", tvPos);
            return;
        }

        // Generate a unique UUID for this virtual camera
        UUID cameraUUID = UUID.randomUUID();

        // Create the camera source (virtual ViewFinderBlockEntity)
        // The constructor overrides the TV cassette's LINKED_FEED_COMPONENT
        ProjectileCameraSource source = new ProjectileCameraSource(proj, tv, cameraUUID);
        activeCameras.put(cameraUUID, source);

        CreateOrdnance.LOGGER.info(
                "[Ordnance/Vista] Camera link created: projectile {} -> TV at {} (camera UUID: {})",
                proj.getId(), tvPos, cameraUUID);
    }

    // =========================================================
    // MIXIN CALLBACK — Virtual Camera Resolution
    // =========================================================

    /**
     * Called by {@link com.pixel.create_ordnance.mixin.compat.vista.LiveFeedConnectionManagerMixin}
     * when Vista's normal broadcast lookup returns null.
     * <p>
     * If the UUID matches one of our projectile cameras, returns the
     * virtual ViewFinderBlockEntity (which implements {@link IBroadcastProvider}).
     * Otherwise returns null.
     *
     * @param uuid The camera UUID being looked up
     * @return The virtual camera as IBroadcastProvider, or null if not a projectile camera
     */
    @Nullable
    public static IBroadcastProvider resolveProjectileCamera(UUID uuid) {
        ProjectileCameraSource source = activeCameras.get(uuid);
        if (source != null && source.isAlive()) {
            return source.getVirtualViewFinder();
        }
        return null;
    }

    // =========================================================
    // UTILITY — TV Block Detection & Master Resolution
    // =========================================================

    /**
     * Resolves the master position of a Vista TV at the given position.
     * <p>
     * In multi-block TVs, the {@link TVBlockEntity} only exists at the
     * {@code BOTTOM_LEFT} (master) position. This method navigates from
     * any clicked position to the master, returning its {@link BlockPos}.
     * <p>
     * Called from {@code OrdnanceDebuggerItem} via the
     * {@code Mods.VISTA.runIfInstalled()} proxy pattern to avoid
     * direct Vista class references outside this package.
     *
     * @param level The level
     * @param pos   The block position to check (any part of the TV)
     * @return The master position if this is a Vista TV, or null if not
     */
    @Nullable
    public static BlockPos getVistaTVMasterPos(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof TVBlock tvBlock)) return null;

        // Use mixin invoker to access TVBlock's private getMasterBlockEntity()
        TVBlockEntity master = ((TVBlockInvoker) tvBlock).ordnance$getMasterBlockEntity(level, pos, state);
        return master != null ? master.getBlockPos() : null;
    }
}
