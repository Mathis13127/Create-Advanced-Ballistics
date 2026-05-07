package com.pixel.create_ordnance.commands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import com.pixel.create_ordnance.mechanics.logic.LogicContext;
import com.pixel.create_ordnance.mechanics.logic.ProjectileBehavior;
import com.pixel.create_ordnance.mechanics.physics.core.IPhysicsModule;
import com.pixel.create_ordnance.mechanics.physics.core.PhysicsConstants;
import com.pixel.create_ordnance.mechanics.physics.core.PhysicsEngine;
import com.pixel.create_ordnance.mechanics.physics.core.PhysicsRegistry;
import com.pixel.create_ordnance.mechanics.physics.core.PhysicsRegistry.PhysicsProfile;
import com.pixel.create_ordnance.registry.ProjectileComponentRegistry;
import com.pixel.create_ordnance.registry.ProjectileData;

import net.minecraft.ChatFormatting;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import net.minecraft.resources.ResourceLocation;

import net.minecraft.server.level.ServerPlayer;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * In-game command handler for inspecting the projectile component registry
 * and physics module state.
 *
 * <p>Registered under {@code /ordnance registry}. Supports:
 * <ul>
 *   <li>{@code /ordnance registry list} — lists all registered component blocks.</li>
 *   <li>{@code /ordnance registry info <block>} — shows detailed data for a component.</li>
 *   <li>{@code /ordnance registry scan <pos>} — performs a structural scan at a position.</li>
 *   <li>{@code /ordnance registry physics} — lists active physics modules and profiles.</li>
 * </ul>
 *
 * @see ProjectileComponentRegistry
 * @see PhysicsEngine
 */
public class RegistryCommand {

        public static ArgumentBuilder<CommandSourceStack, ?> register() {
                return Commands.literal("registry")
                                .requires(source -> source.hasPermission(2))
                                .then(Commands.literal("components_registery")
                                                .executes(RegistryCommand::listRegistry))
                                .then(Commands.literal("physics")
                                                .executes(RegistryCommand::listPhysicsRegistry)
                                                .then(Commands.literal("status")
                                                                .executes(RegistryCommand::listModuleStatus))
                                                .then(Commands.literal("toggle")
                                                                .executes(RegistryCommand::listModuleStatus)
                                                                .then(Commands.argument("moduleId",
                                                                                StringArgumentType.word())
                                                                                .suggests(RegistryCommand::suggestModuleIds)
                                                                                .executes(RegistryCommand::showSpecificModuleStatus)
                                                                                .then(Commands
                                                                                                .argument("enabled",
                                                                                                                BoolArgumentType
                                                                                                                                .bool())
                                                                                                .executes(RegistryCommand::toggleModule))))
                                                .then(Commands.literal("reset")
                                                                .executes(RegistryCommand::resetModules))
                                                .then(Commands.literal("pressure_log")
                                                                .executes(RegistryCommand::runPressureLog)))
                                .then(Commands.literal("trigger_event")
                                                .then(Commands.argument("block", ResourceLocationArgument.id())
                                                                .suggests(RegistryCommand::suggestBlocks)
                                                                .then(Commands.argument("event",
                                                                                StringArgumentType.word())
                                                                                .suggests(RegistryCommand::suggestEvents)
                                                                                .executes(RegistryCommand::triggerEvent))));
        }

        private static int listRegistry(CommandContext<CommandSourceStack> context) {
                CommandSourceStack source = context.getSource();
                Map<Block, ProjectileData> registry = ProjectileComponentRegistry.getRegistry();

                if (registry.isEmpty()) {
                        source.sendSuccess(() -> Component.literal("Projectile Component Registry is empty.")
                                        .withStyle(ChatFormatting.RED), false);
                        return 1;
                }

                source.sendSuccess(() -> Component.literal("======= PROJECTILE COMPONENT REGISTRY =======")
                                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);

                registry.forEach((block, data) -> {
                        String blockId = BuiltInRegistries.BLOCK.getKey(block).toString();

                        // Header: Block ID [TYPE]
                        MutableComponent header = Component.literal("\n> " + blockId)
                                        .withStyle(ChatFormatting.YELLOW)
                                        .append(Component.literal(" [" + data.type().name() + "]")
                                                        .withStyle(ChatFormatting.DARK_GRAY));
                        source.sendSuccess(() -> header, false);

                        // Properties (Dynamic)
                        data.allProperties().forEach((name, prop) -> {
                                Object value = prop.get();
                                String valStr = (value != null) ? value.toString() : "null";

                                MutableComponent propComp = Component.literal("  - " + name + ": ")
                                                .withStyle(ChatFormatting.GRAY)
                                                .append(Component.literal(valStr).withStyle(ChatFormatting.WHITE));

                                if (prop.showInTooltip()) {
                                        propComp.append(Component.literal(" [SHIFT]")
                                                        .withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC));
                                }

                                source.sendSuccess(() -> propComp, false);
                        });

                        // Behavior
                        if (data.behavior() != null) {
                                ProjectileBehavior b = data.behavior();
                                MutableComponent behaviorComp = Component.literal("  - Behavior: ")
                                                .withStyle(ChatFormatting.GRAY)
                                                .append(Component.literal(b.getBehaviorId())
                                                                .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD))
                                                .append(Component.literal(" [SETUP TICK TRIGGER]")
                                                                .withStyle(ChatFormatting.DARK_GREEN));
                                source.sendSuccess(() -> behaviorComp, false);
                        }
                });

                source.sendSuccess(() -> Component.literal("\n=============================================")
                                .withStyle(ChatFormatting.GOLD), false);

                return registry.size();
        }

        private static int listPhysicsRegistry(CommandContext<CommandSourceStack> context) {
                CommandSourceStack source = context.getSource();
                Map<Block, PhysicsProfile> registry = PhysicsRegistry.getRegistry();

                if (registry.isEmpty()) {
                        source.sendSuccess(() -> Component.literal("Physics Registry is empty.")
                                        .withStyle(ChatFormatting.RED), false);
                        return 1;
                }

                source.sendSuccess(() -> Component.literal("======= PHYSICS PROFILE REGISTRY =======")
                                .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), false);

                // Group blocks by PhysicsProfile
                Map<PhysicsProfile, List<Block>> grouped = new HashMap<>();
                registry.forEach((block, profile) -> {
                        grouped.computeIfAbsent(profile, k -> new ArrayList<>()).add(block);
                });

                grouped.forEach((profile, blocks) -> {
                        // Header: Profile Name
                        source.sendSuccess(
                                        () -> Component.literal("\n[ " + profile.name().toUpperCase() + " REGISTRY ]")
                                                        .withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD),
                                        false);

                        // Profile Stats
                        source.sendSuccess(() -> Component.literal("  > Properties:")
                                        .withStyle(ChatFormatting.GOLD), false);
                        source.sendSuccess(() -> Component.literal("    - Density: ")
                                        .withStyle(ChatFormatting.GRAY)
                                        .append(Component.literal(String.valueOf(profile.density()))
                                                        .withStyle(ChatFormatting.WHITE))
                                        .append(Component.literal(" kg/m³").withStyle(ChatFormatting.DARK_GRAY)),
                                        false);
                        source.sendSuccess(() -> Component.literal("    - Viscosity: ")
                                        .withStyle(ChatFormatting.GRAY)
                                        .append(Component.literal(String.valueOf(profile.viscosity()))
                                                        .withStyle(ChatFormatting.WHITE)),
                                        false);
                        source.sendSuccess(() -> Component.literal("    - Type: ")
                                        .withStyle(ChatFormatting.GRAY)
                                        .append(Component
                                                        .literal(profile.compressible() ? "Compressible (Gas)"
                                                                        : "Incompressible (Liquid)")
                                                        .withStyle(profile.compressible() ? ChatFormatting.AQUA
                                                                        : ChatFormatting.BLUE)),
                                        false);

                        // Blocks
                        source.sendSuccess(() -> Component.literal("  > Affected Blocks:")
                                        .withStyle(ChatFormatting.GOLD), false);

                        blocks.forEach(block -> {
                                String blockId = BuiltInRegistries.BLOCK.getKey(block).toString();
                                source.sendSuccess(() -> Component.literal("    - " + blockId)
                                                .withStyle(ChatFormatting.GRAY), false);
                        });
                });

                source.sendSuccess(() -> Component.literal("\n=========================================")
                                .withStyle(ChatFormatting.AQUA), false);

                return grouped.size();
        }

        private static int listModuleStatus(CommandContext<CommandSourceStack> context) {
                CommandSourceStack source = context.getSource();
                var engine = PhysicsEngine.getInstance();
                var modules = engine.getAllModules();

                source.sendSuccess(() -> Component.literal("======= PHYSICS MODULE STATUS =======")
                                .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), false);

                for (var module : modules) {
                        String id = module.getModuleId();
                        boolean enabled = engine.getModuleStatus(id);
                        String phase = module.getPhase().name();

                        MutableComponent line = Component.literal("- " + id)
                                        .withStyle(ChatFormatting.YELLOW)
                                        .append(Component.literal(" [" + phase + "] ")
                                                        .withStyle(ChatFormatting.DARK_GRAY))
                                        .append(Component.literal("(" + module.getCapability().getLabel() + ") ")
                                                        .withStyle(ChatFormatting.BLUE))
                                        .append(enabled ? Component.literal("[ON]").withStyle(ChatFormatting.GREEN,
                                                        ChatFormatting.BOLD)
                                                        : Component.literal("[OFF]").withStyle(ChatFormatting.RED,
                                                                        ChatFormatting.BOLD));

                        source.sendSuccess(() -> line, false);
                }

                source.sendSuccess(() -> Component.literal("=====================================")
                                .withStyle(ChatFormatting.AQUA), false);

                return modules.size();
        }

        private static int showSpecificModuleStatus(CommandContext<CommandSourceStack> context) {
                String moduleId = StringArgumentType.getString(context, "moduleId");
                var engine = PhysicsEngine.getInstance();

                var moduleOpt = engine.getAllModules().stream()
                                .filter(m -> m.getModuleId().equals(moduleId))
                                .findFirst();

                if (moduleOpt.isEmpty()) {
                        context.getSource().sendFailure(Component.literal("Module '" + moduleId + "' not found."));
                        return 0;
                }

                var module = moduleOpt.get();
                boolean enabled = engine.getModuleStatus(moduleId);
                String phase = module.getPhase().name();

                MutableComponent line = Component.literal("Physics Module: ")
                                .withStyle(ChatFormatting.AQUA)
                                .append(Component.literal(moduleId).withStyle(ChatFormatting.YELLOW,
                                                ChatFormatting.BOLD))
                                .append(Component.literal(" [" + phase + "] ").withStyle(ChatFormatting.DARK_GRAY))
                                .append(Component.literal("(" + module.getCapability().getLabel() + ") ")
                                                .withStyle(ChatFormatting.BLUE))
                                .append(enabled ? Component.literal("ENABLED").withStyle(ChatFormatting.GREEN,
                                                ChatFormatting.BOLD)
                                                : Component.literal("DISABLED").withStyle(ChatFormatting.RED,
                                                                ChatFormatting.BOLD));

                context.getSource().sendSuccess(() -> line, false);
                return 1;
        }

        private static int toggleModule(CommandContext<CommandSourceStack> context) {
                String moduleId = StringArgumentType.getString(context, "moduleId");
                boolean enabled = BoolArgumentType.getBool(context, "enabled");
                var engine = PhysicsEngine.getInstance();

                // Verify module exists
                boolean exists = engine.getAllModules().stream().anyMatch(m -> m.getModuleId().equals(moduleId));
                if (!exists) {
                        context.getSource().sendFailure(Component.literal("Module '" + moduleId + "' not found."));
                        return 0;
                }

                engine.setModuleStatus(moduleId, enabled);

                context.getSource().sendSuccess(() -> Component.literal("Physics module '")
                                .append(Component.literal(moduleId).withStyle(ChatFormatting.YELLOW))
                                .append("' is now ")
                                .append(enabled ? Component.literal("ENABLED").withStyle(ChatFormatting.GREEN,
                                                ChatFormatting.BOLD)
                                                : Component.literal("DISABLED").withStyle(ChatFormatting.RED,
                                                                ChatFormatting.BOLD)),
                                true);

                return 1;
        }

        private static int resetModules(CommandContext<CommandSourceStack> context) {
                var engine = PhysicsEngine.getInstance();
                engine.resetModules();
                context.getSource().sendSuccess(() -> Component.literal("All physics modules have been ").append(
                                Component.literal("RESET to ENABLED").withStyle(ChatFormatting.GREEN,
                                                ChatFormatting.BOLD)),
                                true);
                return 1;
        }

        private static CompletableFuture<Suggestions> suggestModuleIds(CommandContext<CommandSourceStack> context,
                        SuggestionsBuilder builder) {
                var engine = PhysicsEngine.getInstance();
                return SharedSuggestionProvider.suggest(
                                engine.getAllModules().stream()
                                                .map(IPhysicsModule::getModuleId),
                                builder);
        }

        private static CompletableFuture<Suggestions> suggestEvents(CommandContext<CommandSourceStack> context,
                        SuggestionsBuilder builder) {
                try {
                        ResourceLocation id = ResourceLocationArgument.getId(context, "block");
                        Block block = BuiltInRegistries.BLOCK.get(id);
                        if (block != null) {
                                ProjectileComponentRegistry.getData(block).ifPresent(data -> {
                                        if (data.behavior() != null) {
                                                builder.suggest("setup");
                                                builder.suggest("tick");
                                                builder.suggest("trigger");
                                        }
                                });
                        }
                } catch (Exception e) {
                        // Context might not be fully ready yet, just return empty
                }
                return builder.buildFuture();
        }

        private static CompletableFuture<Suggestions> suggestBlocks(CommandContext<CommandSourceStack> context,
                        SuggestionsBuilder builder) {
                return SharedSuggestionProvider.suggestResource(
                                ProjectileComponentRegistry.getRegistry().entrySet().stream()
                                                .filter(entry -> entry.getValue().behavior() != null)
                                                .map(entry -> BuiltInRegistries.BLOCK.getKey(entry.getKey())),
                                builder);
        }

        private static int triggerEvent(CommandContext<CommandSourceStack> context) {
                CommandSourceStack source = context.getSource();
                ResourceLocation blockId = ResourceLocationArgument.getId(context, "block");
                String eventType = StringArgumentType.getString(context, "event");

                Block block = BuiltInRegistries.BLOCK.get(blockId);
                if (block == null || !ProjectileComponentRegistry.isComponent(block)) {
                        source.sendFailure(Component
                                        .literal("Block '" + blockId + "' is not a registered projectile component."));
                        return 0;
                }

                ProjectileData data = ProjectileComponentRegistry.getData(block).get();
                ProjectileBehavior behavior = data.behavior();

                if (behavior == null) {
                        source.sendFailure(Component
                                        .literal("No behavior defined for block " + blockId));
                        return 0;
                }

                String normalizedEvent = eventType.toLowerCase();
                if (!normalizedEvent.equals("setup") && !normalizedEvent.equals("tick")
                                && !normalizedEvent.equals("trigger")) {
                        source.sendFailure(
                                        Component.literal("Invalid event type: " + eventType
                                                        + ". Use setup, tick, or trigger."));
                        return 0;
                }

                source.sendSuccess(() -> Component
                                .literal("Triggering event '" + eventType + "' for " + blockId
                                                + " [behavior: " + behavior.getBehaviorId() + "]")
                                .withStyle(ChatFormatting.GRAY), false);

                try {
                        // Resolve params from component properties
                        Map<String, Object> resolvedParams = new HashMap<>();
                        data.allProperties().forEach((k, prop) -> {
                                Object val = prop.get();
                                if (val != null)
                                        resolvedParams.put(k, val);
                        });

                        // Build LogicContext — use player position as fallback
                        Level level = source.getLevel();
                        net.minecraft.world.phys.Vec3 pos = source.getEntity() != null
                                        ? source.getEntity().position()
                                        : source.getPosition();
                        LogicContext ctx = new LogicContext(level, source.getEntity(), pos,
                                        BlockPos.containing(pos),
                                        source.getEntity() != null ? source.getEntity().getYRot() : 0,
                                        source.getEntity() != null ? source.getEntity().getXRot() : 0,
                                        resolvedParams);

                        switch (normalizedEvent) {
                                case "setup" -> behavior.onSetup(ctx);
                                case "tick" -> behavior.onTick(ctx);
                                case "trigger" -> behavior.onTrigger(ctx);
                        }

                        source.sendSuccess(() -> Component.literal("Execution successful!")
                                        .withStyle(ChatFormatting.GREEN), false);
                        return 1;
                } catch (Exception e) {
                        source.sendFailure(Component.literal("Execution failed: " + e.getMessage()));
                        return 0;
                }
        }

        private static int runPressureLog(CommandContext<CommandSourceStack> context) {
                try {
                        ServerPlayer player = context.getSource().getPlayerOrException();
                        Level level = player.level();
                        BlockPos pos = player.blockPosition();

                        // 1. Get Environmental Data (V6.1 - Real Data)
                        PhysicsProfile profileAtCom = PhysicsRegistry.get(level.getBlockState(pos));
                        double density = profileAtCom.density();
                        double barometricFactor = density / PhysicsConstants.AIR_DENSITY;

                        // Percentage Logic:
                        // Sea Level (Y=64) => Factor 1.0 => 100%
                        // Vacuum => Factor 0.0 => 0%
                        // Deep Underground => Factor > 1.0 => >100%
                        double pressurePercent = barometricFactor * 100.0;

                        // 2. Get Block Data
                        BlockState state = level.getBlockState(pos);
                        PhysicsProfile profile = PhysicsRegistry.get(state);
                        boolean isRegistered = (profile != null);
                        boolean compressible = isRegistered && profile.compressible();

                        // 3. Format Output
                        player.sendSystemMessage(Component.literal("§e=== Physics Pressure Log ==="));
                        player.sendSystemMessage(Component.literal(
                                        String.format("§7Pos: §f[%d, %d, %d]", pos.getX(), pos.getY(), pos.getZ())));
                        player.sendSystemMessage(Component.literal(
                                        String.format("§7Block: §f%s", state.getBlock().getName().getString())));
                        player.sendSystemMessage(
                                        Component.literal(String.format("§7Registry: §f%s §7(Compressible: %s)",
                                                        isRegistered ? "YES" : "NO",
                                                        compressible ? "§aTRUE" : "§cFALSE")));

                        player.sendSystemMessage(Component.literal("§b--- Environment ---"));
                        // Color Coding for Pressure
                        String pColor = "§f";
                        if (pressurePercent < 50)
                                pColor = "§b"; // Thin Air
                        if (pressurePercent < 10)
                                pColor = "§c"; // Vacuum
                        if (pressurePercent > 120)
                                pColor = "§6"; // High Pressure

                        player.sendSystemMessage(Component
                                        .literal(String.format("§7Atmospheric Pressure: %s%.1f%% §7(Factor: %.4f)",
                                                        pColor, pressurePercent, barometricFactor)));
                        player.sendSystemMessage(
                                        Component.literal(String.format("§7Effective Density: §f%.4f",
                                                        density)));

                        // 4. Efficiency Preview (Theoretical)
                        player.sendSystemMessage(Component.literal("§d--- Thrust Efficiency Preview ---"));

                        // Afterburner (Opt: 0.1, Γ: 0.5)
                        double afEff = 1.0 / (1.0 + Math.pow((density - 0.1) / 0.5, 2));
                        player.sendSystemMessage(
                                        Component.literal(
                                                        String.format("§7Afterburner: §f%.1f%% §8(Jet)", afEff * 100)));

                        // Propeller (Opt: 1000.0, Γ: 500.0)
                        double propEff = 1.0 / (1.0 + Math.pow((density - 1000.0) / 500.0, 2));
                        player.sendSystemMessage(Component.literal(
                                        String.format("§7Propeller: §f%.1f%% §8(Hydro)", propEff * 100)));

                        player.sendSystemMessage(
                                        Component.literal(
                                                        "§8(Tip: Use 'optimal_density' in your Tail config to match Effective Density)"));

                        // 4. Theoretical Drag Force (at 20m/s)
                        double v = 20.0;
                        double targetDrag = 0.5 * density * v * v * 1.0; // Cd=1, A=1
                        player.sendSystemMessage(
                                        Component.literal(String.format("§7Ref Drag (v=20): §f%.2f N", targetDrag)));

                        return com.mojang.brigadier.Command.SINGLE_SUCCESS;
                } catch (Exception e) {
                        context.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
                        return 0;
                }
        }
}