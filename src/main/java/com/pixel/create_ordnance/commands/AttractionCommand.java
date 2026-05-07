package com.pixel.create_ordnance.commands;

import java.util.Collection;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import com.pixel.create_ordnance.api.physics.AttractionParams;
import com.pixel.create_ordnance.mechanics.physics.attraction.AttractionSource;
import com.pixel.create_ordnance.mechanics.physics.attraction.SpatialForceManager;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;

import net.minecraft.network.chat.Component;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Command handler for {@code /prj-lib attraction}.
 * <p>
 * Provides full CRUD control over spatial attraction sources.
 * <p>
 * <b>Syntax:</b>
 * <pre>
 * /prj-lib attraction spawn &lt;pos&gt;
 * /prj-lib attraction spawn &lt;pos&gt; &lt;strength&gt; &lt;exponent&gt; &lt;range&gt;
 * /prj-lib attraction spawn_preset &lt;pos&gt; blackhole|deflector|magnet
 * /prj-lib attraction remove &lt;id&gt;
 * /prj-lib attraction list
 * /prj-lib attraction track &lt;id&gt; &lt;entity&gt;
 * /prj-lib attraction teleport &lt;id&gt; &lt;pos&gt;
 * /prj-lib attraction configure &lt;id&gt; &lt;param&gt; &lt;value&gt;
 * </pre>
 */
public class AttractionCommand {

    public static ArgumentBuilder<CommandSourceStack, ?> register() {
        return Commands.literal("attraction")
                .requires(source -> source.hasPermission(2))
                // /attraction spawn <pos>
                .then(Commands.literal("spawn")
                        .then(Commands.argument("pos", Vec3Argument.vec3())
                                // Default params
                                .executes(AttractionCommand::runSpawnDefault)
                                // Custom params: <strength> <exponent> <range>
                                .then(Commands.argument("strength", DoubleArgumentType.doubleArg(-1000.0, 1000.0))
                                        .then(Commands.argument("exponent", DoubleArgumentType.doubleArg(0.0, 10.0))
                                                .then(Commands.argument("range", DoubleArgumentType.doubleArg(1.0, 256.0))
                                                        .executes(AttractionCommand::runSpawnCustom))))))
                // /attraction spawn_preset <pos> <preset>
                .then(Commands.literal("spawn_preset")
                        .then(Commands.argument("pos", Vec3Argument.vec3())
                                .then(Commands.literal("blackhole")
                                        .executes(ctx -> runSpawnPreset(ctx, "blackhole", AttractionParams.blackhole())))
                                .then(Commands.literal("deflector")
                                        .executes(ctx -> runSpawnPreset(ctx, "deflector", AttractionParams.deflector())))
                                .then(Commands.literal("magnet")
                                        .executes(ctx -> runSpawnPreset(ctx, "magnet", AttractionParams.magnet())))))
                // /attraction remove <id>
                .then(Commands.literal("remove")
                        .then(Commands.argument("id", IntegerArgumentType.integer(1))
                                .executes(AttractionCommand::runRemove)))
                // /attraction list
                .then(Commands.literal("list")
                        .executes(AttractionCommand::runList))
                // /attraction track <id> <entity>
                .then(Commands.literal("track")
                        .then(Commands.argument("id", IntegerArgumentType.integer(1))
                                .then(Commands.argument("entity", EntityArgument.entity())
                                        .executes(AttractionCommand::runTrack))))
                // /attraction teleport <id> <pos>
                .then(Commands.literal("teleport")
                        .then(Commands.argument("id", IntegerArgumentType.integer(1))
                                .then(Commands.argument("pos", Vec3Argument.vec3())
                                        .executes(AttractionCommand::runTeleport))))
                // /attraction configure <id> <param> <value>
                .then(Commands.literal("configure")
                        .then(Commands.argument("id", IntegerArgumentType.integer(1))
                                .then(registerConfigParam("strength"))
                                .then(registerConfigParam("exponent"))
                                .then(registerConfigParam("maxRange"))
                                .then(registerConfigParam("targetRadius"))
                                .then(registerConfigParam("kp"))
                                .then(registerConfigParam("kd"))
                                .then(registerConfigParam("vortexFactor"))
                                .then(registerConfigParam("orientTowards"))
                                .then(Commands.literal("massDependent")
                                        .then(Commands.argument("value", BoolArgumentType.bool())
                                                .executes(ctx -> runConfigureBool(ctx, "massDependent"))))));
    }

    // =========================================================
    // SPAWN
    // =========================================================

    private static int runSpawnDefault(CommandContext<CommandSourceStack> ctx) {
        SpatialForceManager manager = getManager(ctx);
        if (manager == null) return 0;

        Vec3 pos = Vec3Argument.getVec3(ctx, "pos");
        int id = manager.spawn(AttractionParams.defaultAttraction(),
                ctx.getSource().getLevel().dimension(), pos);

        if (id < 0) {
            ctx.getSource().sendFailure(Component.literal(
                    "\u00a7c[Attraction] Failed to spawn: max sources reached."));
            return 0;
        }

        ctx.getSource().sendSuccess(() -> Component.literal(String.format(
                "\u00a7a[Attraction] Spawned source \u00a7e#%d\u00a7a at \u00a7e%.1f %.1f %.1f\u00a7a with default params.",
                id, pos.x, pos.y, pos.z)), true);
        return 1;
    }

    private static int runSpawnCustom(CommandContext<CommandSourceStack> ctx) {
        SpatialForceManager manager = getManager(ctx);
        if (manager == null) return 0;

        Vec3 pos = Vec3Argument.getVec3(ctx, "pos");
        double strength = DoubleArgumentType.getDouble(ctx, "strength");
        double exponent = DoubleArgumentType.getDouble(ctx, "exponent");
        double range = DoubleArgumentType.getDouble(ctx, "range");

        AttractionParams params = new AttractionParams(
                strength, exponent, range,
                0.0, 0.5, 0.2, 0.0, true, 0.0);

        int id = manager.spawn(params, ctx.getSource().getLevel().dimension(), pos);

        if (id < 0) {
            ctx.getSource().sendFailure(Component.literal(
                    "\u00a7c[Attraction] Failed to spawn: max sources reached."));
            return 0;
        }

        ctx.getSource().sendSuccess(() -> Component.literal(String.format(
                "\u00a7a[Attraction] Spawned source \u00a7e#%d\u00a7a at \u00a7e%.1f %.1f %.1f\u00a7a (str=%.1f, exp=%.1f, range=%.0f)",
                id, pos.x, pos.y, pos.z, strength, exponent, range)), true);
        return 1;
    }

    // =========================================================
    // SPAWN PRESET
    // =========================================================

    private static int runSpawnPreset(CommandContext<CommandSourceStack> ctx, String presetName, AttractionParams params) {
        SpatialForceManager manager = getManager(ctx);
        if (manager == null) return 0;

        Vec3 pos = Vec3Argument.getVec3(ctx, "pos");
        int id = manager.spawn(params, ctx.getSource().getLevel().dimension(), pos);

        if (id < 0) {
            ctx.getSource().sendFailure(Component.literal(
                    "\u00a7c[Attraction] Failed to spawn: max sources reached."));
            return 0;
        }

        ctx.getSource().sendSuccess(() -> Component.literal(String.format(
                "\u00a7a[Attraction] Spawned \u00a7d%s\u00a7a preset \u00a7e#%d\u00a7a at \u00a7e%.1f %.1f %.1f\u00a7a (str=%.1f, range=%.0f)",
                presetName, id, pos.x, pos.y, pos.z, params.strength(), params.maxRange())), true);
        return 1;
    }

    // =========================================================
    // REMOVE
    // =========================================================

    private static int runRemove(CommandContext<CommandSourceStack> ctx) {
        SpatialForceManager manager = getManager(ctx);
        if (manager == null) return 0;

        int id = IntegerArgumentType.getInteger(ctx, "id");
        AttractionSource source = manager.get(id);

        if (source == null) {
            ctx.getSource().sendFailure(Component.literal(
                    String.format("\u00a7c[Attraction] Source #%d not found.", id)));
            return 0;
        }

        manager.remove(id);
        ctx.getSource().sendSuccess(() -> Component.literal(String.format(
                "\u00a7a[Attraction] Removed source \u00a7e#%d\u00a7a.", id)), true);
        return 1;
    }

    // =========================================================
    // LIST
    // =========================================================

    private static int runList(CommandContext<CommandSourceStack> ctx) {
        SpatialForceManager manager = getManager(ctx);
        if (manager == null) return 0;

        Collection<AttractionSource> sources = manager.getActiveSources();
        if (sources.isEmpty()) {
            ctx.getSource().sendSuccess(() -> Component.literal(
                    "\u00a77[Attraction] No active sources."), false);
            return 0;
        }

        ctx.getSource().sendSuccess(() -> Component.literal(String.format(
                "\u00a76[Attraction] %d active source(s):", sources.size())), false);

        for (AttractionSource source : sources) {
            AttractionParams p = source.getParams();
            Vec3 pos = source.getPosition();
            String tracking = source.getTrackedEntityId() != null ? " \u00a7b[tracking]" : "";

            ctx.getSource().sendSuccess(() -> Component.literal(String.format(
                    "\u00a77  #%d: \u00a7e%.1f %.1f %.1f \u00a77(%s) str=%.1f exp=%.1f range=%.0f%s",
                    source.getId(), pos.x, pos.y, pos.z,
                    source.getDimension().location().getPath(),
                    p.strength(), p.exponent(), p.maxRange(), tracking)), false);
        }
        return sources.size();
    }

    // =========================================================
    // TRACK
    // =========================================================

    private static int runTrack(CommandContext<CommandSourceStack> ctx)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        SpatialForceManager manager = getManager(ctx);
        if (manager == null) return 0;

        int id = IntegerArgumentType.getInteger(ctx, "id");
        Entity entity = EntityArgument.getEntity(ctx, "entity");
        AttractionSource source = manager.get(id);

        if (source == null) {
            ctx.getSource().sendFailure(Component.literal(
                    String.format("\u00a7c[Attraction] Source #%d not found.", id)));
            return 0;
        }

        source.setTrackedEntity(entity);
        ctx.getSource().sendSuccess(() -> Component.literal(String.format(
                "\u00a7a[Attraction] Source \u00a7e#%d\u00a7a now tracking entity \u00a7e%s\u00a7a.",
                id, entity.getName().getString())), true);
        return 1;
    }

    // =========================================================
    // TELEPORT
    // =========================================================

    private static int runTeleport(CommandContext<CommandSourceStack> ctx) {
        SpatialForceManager manager = getManager(ctx);
        if (manager == null) return 0;

        int id = IntegerArgumentType.getInteger(ctx, "id");
        Vec3 pos = Vec3Argument.getVec3(ctx, "pos");
        AttractionSource source = manager.get(id);

        if (source == null) {
            ctx.getSource().sendFailure(Component.literal(
                    String.format("\u00a7c[Attraction] Source #%d not found.", id)));
            return 0;
        }

        source.setPosition(pos);
        ctx.getSource().sendSuccess(() -> Component.literal(String.format(
                "\u00a7a[Attraction] Teleported source \u00a7e#%d\u00a7a to \u00a7e%.1f %.1f %.1f\u00a7a.",
                id, pos.x, pos.y, pos.z)), true);
        return 1;
    }

    // =========================================================
    // CONFIGURE
    // =========================================================

    private static ArgumentBuilder<CommandSourceStack, ?> registerConfigParam(String param) {
        return Commands.literal(param)
                .then(Commands.argument("value", DoubleArgumentType.doubleArg())
                        .executes(ctx -> runConfigureDouble(ctx, param)));
    }

    private static int runConfigureDouble(CommandContext<CommandSourceStack> ctx, String param) {
        SpatialForceManager manager = getManager(ctx);
        if (manager == null) return 0;

        int id = IntegerArgumentType.getInteger(ctx, "id");
        double value = DoubleArgumentType.getDouble(ctx, "value");
        AttractionSource source = manager.get(id);

        if (source == null) {
            ctx.getSource().sendFailure(Component.literal(
                    String.format("\u00a7c[Attraction] Source #%d not found.", id)));
            return 0;
        }

        AttractionParams old = source.getParams();
        AttractionParams updated = switch (param) {
            case "strength" -> new AttractionParams(value, old.exponent(), old.maxRange(),
                    old.targetRadius(), old.kp(), old.kd(), old.vortexFactor(), old.massDependent(), old.orientTowards());
            case "exponent" -> new AttractionParams(old.strength(), value, old.maxRange(),
                    old.targetRadius(), old.kp(), old.kd(), old.vortexFactor(), old.massDependent(), old.orientTowards());
            case "maxRange" -> new AttractionParams(old.strength(), old.exponent(), value,
                    old.targetRadius(), old.kp(), old.kd(), old.vortexFactor(), old.massDependent(), old.orientTowards());
            case "targetRadius" -> new AttractionParams(old.strength(), old.exponent(), old.maxRange(),
                    value, old.kp(), old.kd(), old.vortexFactor(), old.massDependent(), old.orientTowards());
            case "kp" -> new AttractionParams(old.strength(), old.exponent(), old.maxRange(),
                    old.targetRadius(), value, old.kd(), old.vortexFactor(), old.massDependent(), old.orientTowards());
            case "kd" -> new AttractionParams(old.strength(), old.exponent(), old.maxRange(),
                    old.targetRadius(), old.kp(), value, old.vortexFactor(), old.massDependent(), old.orientTowards());
            case "vortexFactor" -> new AttractionParams(old.strength(), old.exponent(), old.maxRange(),
                    old.targetRadius(), old.kp(), old.kd(), value, old.massDependent(), old.orientTowards());
            case "orientTowards" -> new AttractionParams(old.strength(), old.exponent(), old.maxRange(),
                    old.targetRadius(), old.kp(), old.kd(), old.vortexFactor(), old.massDependent(), value);
            default -> old;
        };

        manager.configure(id, updated);
        ctx.getSource().sendSuccess(() -> Component.literal(String.format(
                "\u00a7a[Attraction] Source \u00a7e#%d\u00a7a: %s = \u00a7e%.4f", id, param, value)), true);
        return 1;
    }

    private static int runConfigureBool(CommandContext<CommandSourceStack> ctx, String param) {
        SpatialForceManager manager = getManager(ctx);
        if (manager == null) return 0;

        int id = IntegerArgumentType.getInteger(ctx, "id");
        boolean value = BoolArgumentType.getBool(ctx, "value");
        AttractionSource source = manager.get(id);

        if (source == null) {
            ctx.getSource().sendFailure(Component.literal(
                    String.format("\u00a7c[Attraction] Source #%d not found.", id)));
            return 0;
        }

        AttractionParams old = source.getParams();
        AttractionParams updated = new AttractionParams(old.strength(), old.exponent(), old.maxRange(),
                old.targetRadius(), old.kp(), old.kd(), old.vortexFactor(), value, old.orientTowards());

        manager.configure(id, updated);
        ctx.getSource().sendSuccess(() -> Component.literal(String.format(
                "\u00a7a[Attraction] Source \u00a7e#%d\u00a7a: %s = \u00a7e%s", id, param, value)), true);
        return 1;
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private static SpatialForceManager getManager(CommandContext<CommandSourceStack> ctx) {
        SpatialForceManager manager = SpatialForceManager.getInstance();
        if (manager == null) {
            ctx.getSource().sendFailure(Component.literal(
                    "\u00a7c[Attraction] System not initialized (server not started?)."));
        }
        return manager;
    }
}