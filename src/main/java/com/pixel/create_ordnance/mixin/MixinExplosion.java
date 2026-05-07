package com.pixel.create_ordnance.mixin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.pixel.create_ordnance.config.OrdnanceConfigs;
import com.pixel.create_ordnance.mechanics.logic.LogicContext;
import com.pixel.create_ordnance.mechanics.logic.ProjectileBehavior;
import com.pixel.create_ordnance.registry.ProjectileComponentRegistry;
import com.pixel.create_ordnance.registry.ProjectileComponentType;
import com.pixel.create_ordnance.registry.ProjectileData;

import net.minecraft.core.BlockPos;

import net.minecraft.network.chat.Component;

import net.minecraft.world.Container;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin into vanilla {@link Explosion} to integrate
 * projectile payload detonation logic.
 *
 * <p>When an explosion destroys blocks that are registered projectile components,
 * this mixin triggers their {@link ProjectileBehavior} to handle chain reactions
 * and secondary detonations.</p>
 *
 * @see ProjectileData
 */
@Mixin(Explosion.class)
public abstract class MixinExplosion {

    @Shadow
    @Final
    private Level level;

    @Shadow
    @Final
    private it.unimi.dsi.fastutil.objects.ObjectArrayList<BlockPos> toBlow;

    // Track positions already processed to prevent infinite loops
    private static final Set<BlockPos> alreadyProcessed = Collections
            .synchronizedSet(new HashSet<>());

    @Inject(method = "finalizeExplosion", at = @At("HEAD"))
    private void create_ordnance$triggerPayloadOnExplosion(boolean spawnParticles, CallbackInfo ci) {
        if (level.isClientSide)
            return;

        // Iterate through all blocks that are about to be destroyed
        for (BlockPos pos : toBlow) {
            // Skip if already processed (prevents infinite loop)
            if (alreadyProcessed.contains(pos))
                continue;

            BlockState state = level.getBlockState(pos);

            // --- BLOCK PAYLOAD LOGIC ---
            Optional<ProjectileData> dataOpt = ProjectileComponentRegistry.getData(state.getBlock());
            if (dataOpt.isPresent()) {
                ProjectileData data = dataOpt.get();
                if (data.type() == ProjectileComponentType.PAYLOAD && data.behavior() != null) {
                    // Broadcast debug message (controlled by config)
                    if (OrdnanceConfigs.COMMON.divers.mixins.mixinDebug.get()) {
                        level.players().forEach(p -> p.sendSystemMessage(
                                Component
                                        .literal("§c[DEBUG] EXPLOSION sur " + state.getBlock().getName().getString())));
                    }

                    // Resolve params from component properties
                    Map<String, Object> resolvedParams = new HashMap<>();
                    for (Map.Entry<String, ProjectileData.ProjectileProperty<?>> entry : data.allProperties()
                            .entrySet()) {
                        Object val = entry.getValue().get();
                        if (val != null)
                            resolvedParams.put(entry.getKey(), val);
                    }

                    // Remove the block immediately to prevent redundant triggers
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                    alreadyProcessed.add(pos); // Mark as processed

                    // Schedule delayed execution (1-5 ticks) for propagating explosion effect
                    final ProjectileBehavior behavior = data.behavior();
                    final Map<String, Object> finalParams = resolvedParams;
                    final Vec3 explosionPos = Vec3.atCenterOf(pos);
                    int delay = 1 + level.random.nextInt(5); // 1 to 5 ticks

                    level.getServer().tell(new net.minecraft.server.TickTask(
                            level.getServer().getTickCount() + delay,
                            () -> {
                                LogicContext ctx = new LogicContext(level, null, explosionPos,
                                        net.minecraft.core.BlockPos.containing(explosionPos),
                                        0, 0, finalParams);
                                behavior.onTrigger(ctx);
                            }));

                    continue; // Skip container check for payload blocks
                }
            }

            // --- CONTAINER PAYLOAD LOGIC ---
            // Check if the block has a container (chest, barrel, etc.)
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof Container container) {
                List<ProjectileData> payloadsFound = new ArrayList<>();

                // Scan all slots for payload items
                for (int i = 0; i < container.getContainerSize(); i++) {
                    ItemStack stack = container.getItem(i);
                    if (stack.isEmpty())
                        continue;

                    // Check if item is a BlockItem with payload data
                    if (stack.getItem() instanceof BlockItem blockItem) {
                        Optional<ProjectileData> itemDataOpt = ProjectileComponentRegistry
                                .getData(blockItem.getBlock());
                        if (itemDataOpt.isPresent()) {
                            ProjectileData itemData = itemDataOpt.get();
                            if (itemData.type() == ProjectileComponentType.PAYLOAD
                                    && itemData.behavior() != null) {
                                payloadsFound.add(itemData);
                            }
                        }
                    }
                }

                // If payloads found, pick one randomly and trigger it
                if (!payloadsFound.isEmpty()) {
                    ProjectileData chosenPayload = payloadsFound.get(level.random.nextInt(payloadsFound.size()));

                    // Broadcast debug message (controlled by config)
                    if (OrdnanceConfigs.COMMON.divers.mixins.mixinDebug.get()) {
                        level.players().forEach(p -> p.sendSystemMessage(
                                Component.literal("§6[DEBUG] CONTAINER EXPLOSION - Payload déclenché!")));
                    }

                    // Resolve params from payload data
                    Map<String, Object> resolvedParams = new HashMap<>();
                    for (Map.Entry<String, ProjectileData.ProjectileProperty<?>> entry : chosenPayload.allProperties()
                            .entrySet()) {
                        Object val = entry.getValue().get();
                        if (val != null)
                            resolvedParams.put(entry.getKey(), val);
                    }

                    // Execute the payload trigger at container position with delay
                    alreadyProcessed.add(pos); // Mark as processed BEFORE trigger

                    final ProjectileBehavior behavior = chosenPayload.behavior();
                    final Map<String, Object> finalParams = resolvedParams;
                    final Vec3 explosionPos = Vec3.atCenterOf(pos);
                    int delay = 1 + level.random.nextInt(5); // 1 to 5 ticks

                    level.getServer().tell(new net.minecraft.server.TickTask(
                            level.getServer().getTickCount() + delay,
                            () -> {
                                LogicContext ctx = new LogicContext(level, null, explosionPos,
                                        net.minecraft.core.BlockPos.containing(explosionPos),
                                        0, 0, finalParams);
                                behavior.onTrigger(ctx);
                            }));
                }
            }
        }

        // Cleanup: prevent memory leak by clearing processed positions after each
        // explosion
        alreadyProcessed.clear();
    }
}