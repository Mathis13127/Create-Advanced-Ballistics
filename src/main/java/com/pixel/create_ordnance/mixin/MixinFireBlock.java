package com.pixel.create_ordnance.mixin;

import java.util.HashMap;
import java.util.Map;

import com.pixel.create_ordnance.config.OrdnanceConfigs;
import com.pixel.create_ordnance.mechanics.logic.LogicContext;
import com.pixel.create_ordnance.mechanics.logic.ProjectileBehavior;
import com.pixel.create_ordnance.registry.ProjectileComponentRegistry;
import com.pixel.create_ordnance.registry.ProjectileComponentType;
import com.pixel.create_ordnance.registry.ProjectileData;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import net.minecraft.network.chat.Component;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin into vanilla {@link FireBlock} to
 * integrate projectile component fire-spread behavior.
 *
 * <p>Allows payload components to trigger their {@link ProjectileBehavior}
 * when fire is placed on or next to them.</p>
 *
 * @see ProjectileData
 */
@Mixin(FireBlock.class)
public class MixinFireBlock {

    @Inject(method = "onPlace", at = @At("HEAD"))
    private void create_ordnance$triggerPayloadOnFire(BlockState state, Level level, BlockPos pos,
            BlockState oldState, boolean isMoving, CallbackInfo ci) {
        if (level.isClientSide)
            return;

        // Check the block that was at the fire's position (e.g. if fire replaced a
        // payload)
        checkAndTrigger(level, pos, oldState.getBlock());

        // Also check all 6 neighbors (e.g. if fire was placed ON or NEXT TO a payload)
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.relative(dir);
            BlockState neighborState = level.getBlockState(neighborPos);
            checkAndTrigger(level, neighborPos, neighborState.getBlock());
        }
    }

    private void checkAndTrigger(Level level, BlockPos pos, Block block) {
        ProjectileComponentRegistry.getData(block).ifPresent(data -> {
            if (data.type() == ProjectileComponentType.PAYLOAD) {
                // Broadcast debug message (controlled by config)
                if (OrdnanceConfigs.COMMON.divers.mixins.mixinDebug.get()) {
                    level.players().forEach(p -> p.sendSystemMessage(
                            Component.literal("§6[DEBUG] FEUUU sur " + block.getName().getString())));
                }

                ProjectileBehavior behavior = data.behavior();
                if (behavior != null) {
                    // Resolve params from component properties
                    Map<String, Object> resolvedParams = new HashMap<>();
                    for (Map.Entry<String, ProjectileData.ProjectileProperty<?>> entry : data.allProperties()
                            .entrySet()) {
                        Object val = entry.getValue().get();
                        if (val != null)
                            resolvedParams.put(entry.getKey(), val);
                    }

                    // --- BLOCK BREAKING ---
                    // Remove the block to prevent double triggers
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);

                    LogicContext ctx = new LogicContext(level, null, Vec3.atCenterOf(pos),
                            pos, 0, 0, resolvedParams);
                    behavior.onTrigger(ctx);
                }
            }
        });
    }
}