package com.pixel.create_ordnance.content.items;

import java.util.List;

import com.pixel.create_ordnance.compat.Mods;
import com.pixel.create_ordnance.config.OrdnanceConfigs;
import com.pixel.create_ordnance.content.entity.ProjectileEntity;
import com.pixel.create_ordnance.content.items.TransformerMode;
import com.pixel.create_ordnance.foundation.events.OrdnanceServerEvents;
import com.pixel.create_ordnance.mechanics.physics.logic.SimulationEngine;
import com.pixel.create_ordnance.mechanics.scanning.CriticalGeometryError;
import com.pixel.create_ordnance.mechanics.scanning.IProjectileScrapper;
import com.pixel.create_ordnance.mechanics.scanning.ProjectileAnalysisException;
import com.pixel.create_ordnance.mechanics.scanning.ProjectileAnalyzer;
import com.pixel.create_ordnance.mechanics.scanning.ProjectileStats;
import com.pixel.create_ordnance.mechanics.scanning.ScrapResult;
import com.pixel.create_ordnance.registry.ProjectileComponentRegistry;
import com.pixel.create_ordnance.registry.ProjectileScrapperRegistry;

import net.minecraft.ChatFormatting;

import net.minecraft.CrashReport;

import net.minecraft.CrashReportCategory;

import net.minecraft.ReportedException;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;

import net.minecraft.nbt.CompoundTag;

import net.minecraft.network.chat.Component;

import net.minecraft.resources.ResourceLocation;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import net.neoforged.neoforge.common.property.Properties;

/**
 * Debug tool item used to test projectile launching, scanning, and respawning.
 *
 * <p>Right-click with this item to execute the currently selected
 * {@link TransformerMode}. Shift-scroll to cycle modes. This item is intended
 * for development and testing — it will be replaced or hidden in production.</p>
 *
 * <p>Modes include launching projectiles, respawning previous shots,
 * performing scans, and toggling redstone blocks for trigger testing.</p>
 *
 * @see TransformerMode
 * @see ProjectileAnalyzer
 */
public class OrdnanceDebuggerItem extends Item {

    public OrdnanceDebuggerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            TransformerMode mode = getMode(stack);

            // --- LAUNCH MODE: AIR INTERACTION (Spawn & Save Params) ---
            if (mode == TransformerMode.LAUNCH) {
                CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
                CompoundTag tag = customData.copyTag();

                if (tag.contains("SavedProjectile", net.minecraft.nbt.Tag.TAG_COMPOUND)) {
                    CompoundTag scrapTag = tag.getCompound("SavedProjectile");
                    try {
                        ScrapResult result = ScrapResult.readFromNBT(scrapTag);

                        // 1. Calculate Spawn Parameters
                        Vec3 look = player.getLookAngle();
                        Vec3 spawnPos = player.getEyePosition().add(look.scale(2.5));
                        float initialSpeed = OrdnanceConfigs.COMMON.divers.transformerTester.initialSpeed.get()
                                .floatValue();
                        Vec3 velocity = look.scale(initialSpeed);
                        float yaw = player.getYRot();
                        float pitch = player.getXRot();

                        // 2a. Vista Compat: set pending TV link before spawn
                        if (tag.contains("LinkedTV", net.minecraft.nbt.Tag.TAG_COMPOUND)) {
                            CompoundTag tvTag = tag.getCompound("LinkedTV");
                            ProjectileEntity.setPendingLinkedTvPos(new BlockPos(
                                    tvTag.getInt("x"), tvTag.getInt("y"), tvTag.getInt("z")));
                        }

                        // 2b. Spawn using Helper
                        ProjectileEntity.spawn(
                                level,
                                result,
                                spawnPos,
                                velocity,
                                yaw,
                                pitch);

                        // 3. Save "Last Spawn Data" for Respawn Mode
                        CustomData.update(DataComponents.CUSTOM_DATA, stack, launchTag -> {
                            CompoundTag spawnData = new CompoundTag();
                            spawnData.putDouble("posX", spawnPos.x);
                            spawnData.putDouble("posY", spawnPos.y);
                            spawnData.putDouble("posZ", spawnPos.z);
                            spawnData.putDouble("velX", velocity.x);
                            spawnData.putDouble("velY", velocity.y);
                            spawnData.putDouble("velZ", velocity.z);
                            spawnData.putFloat("yaw", yaw);
                            spawnData.putFloat("pitch", pitch);
                            launchTag.put("LastSpawnData", spawnData);
                        });

                        player.displayClientMessage(
                                Component.literal("Projectile Launched & Memorized!").withStyle(ChatFormatting.GREEN),
                                true);

                    } catch (Exception e) {
                        throw new IllegalStateException("Failed to load/spawn projectile: " + e.getMessage(), e);
                    }

                } else {
                    player.displayClientMessage(
                            Component.literal("No projectile saved. Scan a block first.")
                                    .withStyle(ChatFormatting.RED),
                            true);
                }
                return InteractionResultHolder.success(stack);
            }

            // --- RESPAWN MODE: Re-spawn at last launch point ---
            if (mode == TransformerMode.RESPAWN) {
                CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
                CompoundTag tag = customData.copyTag();

                if (tag.contains("LastSpawnData", net.minecraft.nbt.Tag.TAG_COMPOUND)
                        && tag.contains("SavedProjectile")) {
                    CompoundTag spawnTag = tag.getCompound("LastSpawnData");
                    CompoundTag scrapTag = tag.getCompound("SavedProjectile");

                    try {
                        ScrapResult result = ScrapResult.readFromNBT(scrapTag);
                        Vec3 spawnPos = new Vec3(spawnTag.getDouble("posX"), spawnTag.getDouble("posY"),
                                spawnTag.getDouble("posZ"));
                        Vec3 velocity = new Vec3(spawnTag.getDouble("velX"), spawnTag.getDouble("velY"),
                                spawnTag.getDouble("velZ"));
                        float yaw = spawnTag.getFloat("yaw");
                        float pitch = spawnTag.getFloat("pitch");

                        // Vista Compat: inject pending TV link
                        if (tag.contains("LinkedTV", net.minecraft.nbt.Tag.TAG_COMPOUND)) {
                            CompoundTag tvTag = tag.getCompound("LinkedTV");
                            ProjectileEntity.setPendingLinkedTvPos(new BlockPos(
                                    tvTag.getInt("x"), tvTag.getInt("y"), tvTag.getInt("z")));
                        }

                        ProjectileEntity.spawn(
                                level,
                                result,
                                spawnPos,
                                velocity,
                                yaw,
                                pitch);

                        player.displayClientMessage(
                                Component.literal("Projectile Re-Launched!").withStyle(ChatFormatting.AQUA), true);
                    } catch (Exception e) {
                        player.displayClientMessage(
                                Component.literal("Failed to respawn: " + e.getMessage()).withStyle(ChatFormatting.RED),
                                true);
                    }
                } else {
                    player.displayClientMessage(
                            Component.literal("No spawn data memorized. Launch a projectile first.")
                                    .withStyle(ChatFormatting.YELLOW),
                            true);
                }
                return InteractionResultHolder.success(stack);
            }

            // --- RELAUNCH_RANDOM MODE: Re-spawn at last launch point with ±5° jitter ---
            if (mode == TransformerMode.RELAUNCH_RANDOM) {
                CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
                CompoundTag tag = customData.copyTag();

                if (tag.contains("LastSpawnData", net.minecraft.nbt.Tag.TAG_COMPOUND)
                        && tag.contains("SavedProjectile")) {
                    CompoundTag spawnTag = tag.getCompound("LastSpawnData");
                    CompoundTag scrapTag = tag.getCompound("SavedProjectile");

                    try {
                        ScrapResult result = ScrapResult.readFromNBT(scrapTag);
                        Vec3 spawnPos = new Vec3(spawnTag.getDouble("posX"), spawnTag.getDouble("posY"),
                                spawnTag.getDouble("posZ"));

                        // 1. Fetch original orientation and speed
                        float savedYaw = spawnTag.getFloat("yaw");
                        float savedPitch = spawnTag.getFloat("pitch");
                        Vec3 lastVel = new Vec3(spawnTag.getDouble("velX"), spawnTag.getDouble("velY"),
                                spawnTag.getDouble("velZ"));
                        double speed = lastVel.length();

                        // 2. Apply ±5° Jitter
                        net.minecraft.util.RandomSource rnd = level.getRandom();
                        float yaw = savedYaw + (rnd.nextFloat() * 10f - 5f);
                        float pitch = savedPitch + (rnd.nextFloat() * 10f - 5f);

                        // 3. Recalculate Velocity Vector
                        Vec3 velocity = Vec3.directionFromRotation(pitch, yaw).scale(speed);

                        // Vista Compat: inject pending TV link
                        if (tag.contains("LinkedTV", net.minecraft.nbt.Tag.TAG_COMPOUND)) {
                            CompoundTag tvTag = tag.getCompound("LinkedTV");
                            ProjectileEntity.setPendingLinkedTvPos(new BlockPos(
                                    tvTag.getInt("x"), tvTag.getInt("y"), tvTag.getInt("z")));
                        }

                        ProjectileEntity.spawn(
                                level,
                                result,
                                spawnPos,
                                velocity,
                                yaw,
                                pitch);

                        player.displayClientMessage(
                                Component.literal("Randomized Relaunch (±5°)!").withStyle(ChatFormatting.GOLD), true);
                    } catch (Exception e) {
                        player.displayClientMessage(
                                Component.literal("Failed to relaunch: " + e.getMessage())
                                        .withStyle(ChatFormatting.RED),
                                true);
                    }
                } else {
                    player.displayClientMessage(
                            Component.literal("No spawn data memorized. Launch a projectile first.")
                                    .withStyle(ChatFormatting.YELLOW),
                            true);
                }
                return InteractionResultHolder.success(stack);
            }

            // --- REDSTONE_BLOCK MODE: Long-range air pulse ---
            if (mode == TransformerMode.REDSTONE_BLOCK) {
                // Raycast HARDCODED VALUES, it's normal here
                double dist = 350.0;
                Vec3 start = player.getEyePosition();
                Vec3 end = start.add(player.getLookAngle().scale(dist));
                BlockHitResult hit = level.clip(new net.minecraft.world.level.ClipContext(
                        start, end, net.minecraft.world.level.ClipContext.Block.OUTLINE,
                        net.minecraft.world.level.ClipContext.Fluid.NONE, player));

                if (hit.getType() == HitResult.Type.BLOCK) {
                    BlockPos target = hit.getBlockPos().relative(hit.getDirection());
                    if (level.getBlockState(target).canBeReplaced()) {
                        level.setBlock(target, Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
                        OrdnanceServerEvents.scheduleBlockRemoval(level, target, 2);

                        player.displayClientMessage(
                                Component.literal("Sent Redstone Pulse!").withStyle(ChatFormatting.RED), true);
                    }
                }
                return InteractionResultHolder.success(stack);
            }
        }

        return InteractionResultHolder.success(stack);
    }

    private static final String TAG_MODE = "Mode";

    public static TransformerMode getMode(ItemStack stack) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        if (tag.contains(TAG_MODE)) {
            try {
                return TransformerMode.valueOf(tag.getString(TAG_MODE));
            } catch (IllegalArgumentException e) {
            }
        }
        return TransformerMode.LAUNCH; // Default
    }

    public static void setMode(ItemStack stack, TransformerMode mode) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putString(TAG_MODE, mode.name()));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents,
            TooltipFlag tooltipFlag) {
        TransformerMode mode = getMode(stack);
        tooltipComponents.add(Component.literal("Mode: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(mode.name()).withStyle(ChatFormatting.WHITE)));

        // Show linked TV coordinates if present (Vista compat)
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        if (tag.contains("LinkedTV", net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            CompoundTag tvTag = tag.getCompound("LinkedTV");
            String coords = tvTag.getInt("x") + ", " + tvTag.getInt("y") + ", " + tvTag.getInt("z");
            tooltipComponents.add(Component.literal("Linked TV: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(coords).withStyle(ChatFormatting.LIGHT_PURPLE)));
        }

        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }

    @Override
    public net.minecraft.world.InteractionResult useOn(net.minecraft.world.item.context.UseOnContext context) {
        TransformerMode mode = getMode(context.getItemInHand());

        if (mode == TransformerMode.REDSTONE_BLOCK) {
            if (!context.getLevel().isClientSide) {
                BlockPos target = context.getClickedPos().relative(context.getClickedFace());
                Level level = context.getLevel();
                if (level.getBlockState(target).canBeReplaced()) {
                    level.setBlock(target, Blocks.REDSTONE_BLOCK.defaultBlockState(),
                            3);
                    OrdnanceServerEvents.scheduleBlockRemoval(level, target, 2);
                    context.getPlayer().displayClientMessage(
                            Component.literal("Sent Redstone Pulse!").withStyle(ChatFormatting.RED), true);
                }
            }
            return net.minecraft.world.InteractionResult.SUCCESS;
        }

        // --- VISTA COMPAT: Click TV in LAUNCH mode → link TV (priority over scan) ---
        // For multi-block TVs, resolves to the master (BOTTOM_LEFT) position
        // where the actual TVBlockEntity resides.
        if (mode == TransformerMode.LAUNCH && !context.getLevel().isClientSide
                && context.getPlayer() != null) {
            BlockPos clickedPos = context.getClickedPos();
            BlockPos masterPos = Mods.VISTA.runIfInstalled(
                    () -> () -> com.pixel.create_ordnance.compat.vista.VistaCompat
                            .getVistaTVMasterPos(context.getLevel(), clickedPos))
                    .orElse(null);
            if (masterPos != null) {
                ItemStack tvStack = context.getItemInHand();
                CustomData.update(DataComponents.CUSTOM_DATA, tvStack, tag -> {
                    CompoundTag tvTag = new CompoundTag();
                    tvTag.putInt("x", masterPos.getX());
                    tvTag.putInt("y", masterPos.getY());
                    tvTag.putInt("z", masterPos.getZ());
                    tag.put("LinkedTV", tvTag);
                });
                context.getPlayer().displayClientMessage(
                        Component.literal("TV Linked!").withStyle(ChatFormatting.LIGHT_PURPLE), true);
                return net.minecraft.world.InteractionResult.SUCCESS;
            }
        }

        if (mode == TransformerMode.LAUNCH || mode == TransformerMode.DUMB_SCAN) {
            if (!context.getLevel().isClientSide) {
                BlockState state = context.getLevel().getBlockState(context.getClickedPos());
                var type = ProjectileComponentRegistry.getType(state.getBlock());

                if (type.isPresent() || mode == TransformerMode.DUMB_SCAN) {
                    // 1. Determine Scrapper ID
                    ResourceLocation scrapperId = (mode == TransformerMode.DUMB_SCAN)
                            ? ProjectileScrapperRegistry.DUMB
                            : ProjectileScrapperRegistry.LINEAR;

                    // 2. Scan using Registry
                    IProjectileScrapper scrapper = ProjectileScrapperRegistry
                            .get(scrapperId)
                            .orElse(ProjectileScrapperRegistry.getDefault())
                            .create(context.getLevel(), context.getClickedPos(),
                                    state.getValue(DirectionalBlock.FACING));

                    float scale = OrdnanceConfigs.COMMON.divers.transformerTester.testerScale.get().floatValue();
                    ScrapResult rawResult = scrapper.scan(scale);

                    // --- ANALYSIS (FORGING FATAL CRASH) ---
                    try {
                        ProjectileStats stats = ProjectileAnalyzer.analyze(rawResult);

                        // --- BLACKBOX: FORCE FOCUS ON SCAN ---
                        SimulationEngine.getInstance()
                                .getBlackBox()
                                .setFocus(null, stats, rawResult);

                    } catch (CriticalGeometryError error) {
                        // FORCE HARD CRASH VIA MINECRAFT SYSTEM
                        CrashReport report = CrashReport.forThrowable(error,
                                "Ordnance: Critical Projectile Geometry Violation");
                        CrashReportCategory category = report.addCategory("Projectile Details");
                        category.setDetail("Scrapper Mode", mode.name());
                        category.setDetail("Total Blocks", String.valueOf(rawResult.blockIds().size()));

                        // This stops the game and shows the crash screen
                        throw new ReportedException(report);
                    } catch (Throwable t) {
                        if (t instanceof ProjectileAnalysisException)
                            throw (ProjectileAnalysisException) t;
                        throw new RuntimeException("Unexpected Analyzer Error", t);
                    }

                    // --- If we reach here, scan is valid (at least for geometry) ---

                    // 3. Save RAW Structure (IDs) to Item
                    ItemStack stack = context.getItemInHand();
                    CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
                        tag.put("SavedProjectile", rawResult.writeToNBT());
                    });

                    // 5. Detailed Feedback for DUMB_SCAN
                    if (mode == TransformerMode.DUMB_SCAN) {
                        context.getPlayer().sendSystemMessage(Component.literal("§6--- [DUMB SCAN ORDER] ---"));
                        List<ResourceLocation> ids = rawResult.blockIds();
                        for (int i = 0; i < ids.size(); i++) {
                            context.getPlayer().sendSystemMessage(Component.literal("§7" + i + ": §f" + ids.get(i)));
                        }
                        context.getPlayer().sendSystemMessage(Component.literal("§6-------------------------"));
                    }

                    context.getPlayer().displayClientMessage(
                            Component
                                    .literal("Projectile Structure Saved! (" + rawResult.blockIds().size()
                                            + " blocks)")
                                    .withStyle(ChatFormatting.GREEN),
                            true);

                    return net.minecraft.world.InteractionResult.SUCCESS;
                } else {
                    context.getPlayer().displayClientMessage(
                            Component.literal("Not a valid projectile component.").withStyle(ChatFormatting.RED),
                            true);
                    return net.minecraft.world.InteractionResult.SUCCESS;
                }
            }
            return net.minecraft.world.InteractionResult.SUCCESS;
        }

        return super.useOn(context);
    }
}